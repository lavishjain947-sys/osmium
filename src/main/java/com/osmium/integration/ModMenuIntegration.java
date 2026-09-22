package com.osmium.integration;

import com.osmium.OsmiumConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class ModMenuIntegration {
    private ModMenuIntegration() {}

    public static void register() {
        OsmiumConstants.LOGGER.info("[Osmium] ModMenu integration initialized.");
    }

    public static Screen createConfigScreen(Screen parent) {
        return new OsmiumConfigScreen(parent);
    }

    public static class OsmiumConfigScreen extends Screen {
        private final Screen parent;

        public OsmiumConfigScreen(Screen parent) {
            super(Text.translatable("osmium.config.title"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            super.init();
            this.addDrawableChild(
                ButtonWidget.builder(Text.translatable("gui.back"), button -> {
                    if (this.client != null) {
                        this.client.setScreen(this.parent);
                    }
                })
                .dimensions(this.width / 2 - 100, this.height - 30, 200, 20)
                .build()
            );
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Osmium 1.0.0 - Client-Side Optimization"), this.width / 2, 50, 0xAAAAAA);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Use /osmium in-game or edit config/osmium.json"), this.width / 2, 70, 0x888888);
        }

        @Override
        public void close() {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        }
    }
}
