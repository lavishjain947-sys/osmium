package com.terraformersmc.modmenu.api;

import net.minecraft.client.gui.screen.Screen;

@FunctionalInterface
public interface ConfigScreenFactory<T extends Screen> {
    T create(Screen parent);
}
