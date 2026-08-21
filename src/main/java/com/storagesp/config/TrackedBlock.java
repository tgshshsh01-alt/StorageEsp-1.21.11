package com.storagesp.config;

/**
 * Одна запись в списке отслеживаемых блоков.
 * blockId — например "minecraft:chest", "minecraft:barrel", "minecraft:shulker_box".
 * Для всех цветных шалкеров используйте "minecraft:*_shulker_box" — обрабатывается как маска.
 */
public class TrackedBlock {
    public String blockId;
    public boolean enabled = true;
    public float colorR = 1.0f;
    public float colorG = 0.9f;
    public float colorB = 0.2f;
    public String category = "custom"; // containers | custom | furnaces и т.д.

    public TrackedBlock() {
    }

    public TrackedBlock(String blockId, boolean enabled, float r, float g, float b, String category) {
        this.blockId = blockId;
        this.enabled = enabled;
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.category = category;
    }
}
