package com.osmium.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new Screen(Text.literal("Osmium Config")) {
            // Minimal read-only info screen.
            // Full config UI is planned for v1.1.
            @Override
            protected void init() {
                super.init();
                this.addDrawableChild(
                    net.minecraft.client.gui.widget.ButtonWidget.builder(
                        Text.literal("Close"),
                        b -> this.close()
                    ).dimensions(this.width / 2 - 50, this.height - 40,
                                 100, 20).build()
                );
            }
        };
    }
}
