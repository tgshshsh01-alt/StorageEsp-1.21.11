package com.storagesp;

import com.storagesp.config.ModConfig;
import com.storagesp.render.EspRenderer;
import com.storagesp.scan.ChunkScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class StorageSPClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        KeybindHandler.register();

        // Пересканирование чанков — не каждый кадр, см. ChunkScanner.RESCAN_INTERVAL_TICKS
        ClientTickEvents.END_CLIENT_TICK.register(client -> ChunkScanner.tick());

        // Отрисовка ESP поверх полупрозрачной геометрии мира
        WorldRenderEvents.AFTER_TRANSLUCENT.register(EspRenderer::render);
    }
}
