package com.example.techmod.blockentity;

import com.example.techmod.Upgrade;
import com.example.techmod.menu.MachineMenu;
import com.example.techmod.network.ItemNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;
import java.util.function.Predicate;

/**
 * Основа всіх машин: тир (прокачка), буфер енергії, меню, обмін предметами зі скринями
 * (поруч або через предметні кабелі).
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int KIND_GENERATOR = 0;
    public static final int KIND_FURNACE = 1;
    public static final int KIND_CRUSHER = 2;
    public static final int KIND_QUARRY = 3;

    protected int energy;
    protected int tier = 1;
    private int linkedContainers;

    /** Дані, які синхронізуються з клієнтом для меню. Усі значення мають вміщатись у short. */
    public final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy;
                case 1 -> getCapacity();
                case 2 -> tier;
                case 3 -> progressPercent();
                case 4 -> linkedContainers;
                case 5 -> extraValue();
                case 6 -> kind();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return MachineMenu.DATA_COUNT;
        }
    };

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ---------- що має знати кожна машина ----------

    /** Одна з констант KIND_*. Від неї залежить, що малює меню. */
    public abstract int kind();

    protected abstract String machineName();

    /** Прогрес роботи в відсотках (0..100). */
    protected int progressPercent() {
        return 0;
    }

    /** Додаткове число для меню: паливо, черга, шар кар'єра. */
    protected int extraValue() {
        return 0;
    }

    // ---------- енергія ----------

    public int getTier() {
        return tier;
    }

    public int getCapacity() {
        return Upgrade.capacity(tier);
    }

    public int getEnergy() {
        return energy;
    }

    /** Чи приймає машина енергію з кабелів (генератор - ні). */
    public boolean acceptsEnergy() {
        return true;
    }

    /** @return скільки енергії реально прийнято */
    public int receiveEnergy(int amount) {
        int moved = Math.max(0, Math.min(amount, getCapacity() - energy));
        if (moved > 0) {
            energy += moved;
            setChanged();
        }
        return moved;
    }

    /** @return скільки енергії реально забрано */
    public int extractEnergy(int amount) {
        int moved = Math.max(0, Math.min(amount, energy));
        energy -= moved;
        return moved;
    }

    // ---------- взаємодія ----------

    /** Клік предметом по машині. Порожня рука відкриває меню (див. MachineBlock). */
    public abstract InteractionResult onUse(Player player, ItemStack held);

    /** Викликається при ламанні блока: викидає вміст машини. */
    public void dropContents(Level level, BlockPos pos) {
    }

    public void openMenu(Player player) {
        if (player instanceof ServerPlayer) {
            player.openMenu(this);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(machineName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(containerId, menuData, this);
    }

    /** Спроба підняти тир, викликається з меню. Предмети беруться з інвентарю гравця. */
    public boolean tryUpgrade(Player player) {
        if (tier >= Upgrade.MAX_TIER) {
            return false;
        }
        List<Upgrade.Cost> costs = Upgrade.costFor(tier);
        if (!player.isCreative()) {
            if (!Upgrade.has(player.getInventory(), costs)) {
                say(player, "Не вистачає предметів для прокачки");
                return false;
            }
            Upgrade.take(player.getInventory(), costs);
        }
        tier++;
        setChanged();
        return true;
    }

    protected void say(Player player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }

    // ---------- предмети: скрині поруч і предметні кабелі ----------

    /** Раз на секунду перераховує, скільки контейнерів під'єднано (для меню). */
    protected void refreshLinks(ServerLevel level) {
        if (level.getGameTime() % 20 == 0) {
            linkedContainers = ItemNetwork.findContainers(level, getBlockPos()).size();
        }
    }

    /**
     * Віддає готовий предмет у контейнери поруч або у скрині, під'єднані предметним кабелем.
     * Якщо місця немає, предмет падає над машиною.
     */
    protected void eject(ServerLevel level, ItemStack stack) {
        ItemStack remaining = stack;
        for (Container container : ItemNetwork.findContainers(level, getBlockPos())) {
            remaining = HopperBlockEntity.addItem(null, container, remaining, null);
            if (remaining.isEmpty()) {
                return;
            }
        }
        Block.popResource(level, getBlockPos().above(), remaining);
    }

    /**
     * Забирає до {@code max} предметів, що підходять під фільтр, з контейнерів поруч
     * або з контейнерів, під'єднаних предметним кабелем.
     */
    protected ItemStack takeFromNetwork(ServerLevel level, Predicate<ItemStack> filter, int max) {
        if (max <= 0) {
            return ItemStack.EMPTY;
        }
        for (Container container : ItemNetwork.findContainers(level, getBlockPos())) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack inSlot = container.getItem(slot);
                if (inSlot.isEmpty() || !filter.test(inSlot)) {
                    continue;
                }
                ItemStack taken = container.removeItem(slot, Math.min(max, inSlot.getCount()));
                container.setChanged();
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", energy);
        output.putInt("tier", tier);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy = input.getIntOr("energy", 0);
        tier = Math.max(1, Math.min(Upgrade.MAX_TIER, input.getIntOr("tier", 1)));
    }
}
