package com.example.techmod;

import com.example.techmod.menu.MachineMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    public static final MenuType<MachineMenu> MACHINE = Registry.register(
            BuiltInRegistries.MENU,
            TechMod.id("machine"),
            new MenuType<>(MachineMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {
    }

    public static void initialize() {
    }
}
