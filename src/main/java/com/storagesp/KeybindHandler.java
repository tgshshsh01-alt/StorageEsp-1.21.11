package com.storagesp;

import com.storagesp.config.ModConfig;
import com.storagesp.gui.ConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {

    private static KeyBinding toggleKey;
    private static KeyBinding menuKey;

    public static void register() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.storagesp.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // не назначено по умолчанию — задаётся в Controls, безопаснее для случайных конфликтов
                "key.category.storagesp"
        ));

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.storagesp.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.category.storagesp"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                ModConfig.INSTANCE.masterEnabled = !ModConfig.INSTANCE.masterEnabled;
                ModConfig.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.translatable(
                            ModConfig.INSTANCE.masterEnabled
                                    ? "storagesp.toast.enabled"
                                    : "storagesp.toast.disabled"
                    ), true);
                }
            }
            while (menuKey.wasPressed()) {
                Screen current = client.currentScreen;
                if (current == null) {
                    client.setScreen(new ConfigScreen(null));
                }
            }
        });
    }
}
