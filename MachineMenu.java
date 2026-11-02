package com.example.techmod.menu;

import com.example.techmod.ModMenus;
import com.example.techmod.blockentity.MachineBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Меню машини без слотів: показує стан і має кнопку прокачки.
 * Дані (енергія, тир, прогрес) приходять з сервера через ContainerData.
 */
public class MachineMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 7;
    public static final int BUTTON_UPGRADE = 0;

    private final ContainerData data;
    private final MachineBlockEntity machine;

    /** Клієнтський конструктор. */
    public MachineMenu(int containerId, Inventory inventory) {
        this(containerId, new SimpleContainerData(DATA_COUNT), null);
    }

    /** Серверний конструктор. */
    public MachineMenu(int containerId, ContainerData data, MachineBlockEntity machine) {
        super(ModMenus.MACHINE, containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        this.machine = machine;
        addDataSlots(data);
    }

    public int energy() {
        return data.get(0);
    }

    public int capacity() {
        return data.get(1);
    }

    public int tier() {
        return data.get(2);
    }

    public int progress() {
        return data.get(3);
    }

    public int linkedContainers() {
        return data.get(4);
    }

    public int extra() {
        return data.get(5);
    }

    public int kind() {
        return data.get(6);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_UPGRADE && machine != null) {
            return machine.tryUpgrade(player);
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (machine == null) {
            return true;
        }
        return !machine.isRemoved()
                && player.distanceToSqr(Vec3.atCenterOf(machine.getBlockPos())) <= 64.0;
    }
}
