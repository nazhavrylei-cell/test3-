package com.example.techmod;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Усі числа тирів машин в одному місці.
 * Тир 1: базова машина. Тир 2: швидша, економніша, більший буфер. Тир 3: ще більше.
 */
public final class Upgrade {
    public static final int MAX_TIER = 3;

    public record Cost(Item item, int count) {
    }

    private Upgrade() {
    }

    /** Множник швидкості: 1x, 2x, 4x. */
    public static int speed(int tier) {
        return switch (tier) {
            case 2 -> 2;
            case 3 -> 4;
            default -> 1;
        };
    }

    /** Скільки відсотків енергії на одиницю роботи витрачає машина (менше означає економніше). */
    public static int efficiencyPercent(int tier) {
        return switch (tier) {
            case 2 -> 80;
            case 3 -> 60;
            default -> 100;
        };
    }

    public static int capacity(int tier) {
        return switch (tier) {
            case 2 -> 8000;
            case 3 -> 16000;
            default -> 4000;
        };
    }

    /** Пауза кар'єра між блоками в тіках. */
    public static int quarryInterval(int tier) {
        return switch (tier) {
            case 2 -> 5;
            case 3 -> 2;
            default -> 10;
        };
    }

    /** Що треба віддати, щоб піднятися з вказаного тиру на наступний. */
    public static List<Cost> costFor(int currentTier) {
        if (currentTier == 1) {
            return List.of(
                    new Cost(Items.IRON_INGOT, 16),
                    new Cost(Items.REDSTONE, 8),
                    new Cost(Items.GOLD_INGOT, 4));
        }
        if (currentTier == 2) {
            return List.of(
                    new Cost(Items.GOLD_INGOT, 16),
                    new Cost(Items.DIAMOND, 8),
                    new Cost(Items.REDSTONE, 16));
        }
        return List.of();
    }

    public static int count(Container inventory, Item item) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean has(Container inventory, List<Cost> costs) {
        for (Cost cost : costs) {
            if (count(inventory, cost.item()) < cost.count()) {
                return false;
            }
        }
        return true;
    }

    public static void take(Container inventory, List<Cost> costs) {
        for (Cost cost : costs) {
            int left = cost.count();
            for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.is(cost.item())) {
                    int taken = Math.min(left, stack.getCount());
                    stack.shrink(taken);
                    left -= taken;
                }
            }
        }
        inventory.setChanged();
    }
}
