package com.storagesp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Конфиг мода. Хранится в .minecraft/config/storagesp.json
 * Полностью редактируемый вручную или через игровое меню.
 */
public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("storagesp.json");

    public static ModConfig INSTANCE;

    // ---- Общие настройки ----
    public boolean masterEnabled = true;
    public boolean renderBoxes = true;
    public boolean renderTracers = true;
    public boolean renderThroughWalls = true;
    public int scanRadiusChunks = 6; // радиус сканирования чанков вокруг игрока
    public float lineWidth = 2.0f;
    public float boxAlpha = 0.35f;

    // ---- Список отслеживаемых блоков ----
    public List<TrackedBlock> trackedBlocks = new ArrayList<>();

    // ---- Свернутые категории в UI (запоминаем состояние меню) ----
    public Map<String, Boolean> categoryExpanded = new LinkedHashMap<>();

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException | RuntimeException e) {
                e.printStackTrace();
                INSTANCE = null;
            }
        }
        if (INSTANCE == null) {
            INSTANCE = createDefault();
            save();
        } else {
            // На случай обновления мода — досыпаем новые блоки по умолчанию, если их нет в файле
            mergeMissingDefaults(INSTANCE);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void mergeMissingDefaults(ModConfig cfg) {
        ModConfig defaults = createDefault();
        boolean changed = false;
        for (TrackedBlock def : defaults.trackedBlocks) {
            boolean exists = cfg.trackedBlocks.stream()
                    .anyMatch(tb -> tb.blockId.equals(def.blockId));
            if (!exists) {
                cfg.trackedBlocks.add(def);
                changed = true;
            }
        }
        if (changed) save();
    }

    private static ModConfig createDefault() {
        ModConfig cfg = new ModConfig();

        // Базовые контейнеры
        cfg.trackedBlocks.add(new TrackedBlock("minecraft:chest", true, 1.0f, 0.85f, 0.2f, "containers"));
        cfg.trackedBlocks.add(new TrackedBlock("minecraft:trapped_chest", true, 1.0f, 0.55f, 0.2f, "containers"));
        cfg.trackedBlocks.add(new TrackedBlock("minecraft:ender_chest", true, 0.4f, 0.85f, 0.85f, "containers"));
        cfg.trackedBlocks.add(new TrackedBlock("minecraft:barrel", true, 0.8f, 0.55f, 0.25f, "containers"));

        // Все цветные шалкеры маской (обрабатывается как startsWith/endsWith см. ChunkScanner)
        cfg.trackedBlocks.add(new TrackedBlock("minecraft:*_shulker_box", true, 0.7f, 0.3f, 0.9f, "containers"));
        cfg.trackedBlocks.add(new TrackedBlock("minecraft:shulker_box", true, 0.7f, 0.3f, 0.9f, "containers"));

        // Технические/полезные для склада (выключены по умолчанию — включаются в меню)
        cfg.trackedBlocks.add(new TrackedBlock("minecraft:furnace", false, 0.9f, 0.4f, 0.2f, "furnaces"));
        cfg.trackedBlocks.add(new TrackedBlock("minecraft:blast_furnace", false, 0.9f, 0.4f, 0.2f, "furnaces"));
        cfg.trackedBlocks.add(new TrackedBlock("minecraft:smoker", false, 0.9f, 0.4f, 0.2f, "furnaces"));
        cfg.trackedBlocks.add(new TrackedBlock("minecraft:hopper", false, 0.6f, 0.6f, 0.6f, "furnaces"));
        cfg.trackedBlocks.add(new TrackedBlock("minecraft:dispenser", false, 0.5f, 0.5f, 0.7f, "furnaces"));
        cfg.trackedBlocks.add(new TrackedBlock("minecraft:dropper", false, 0.5f, 0.5f, 0.7f, "furnaces"));

        cfg.categoryExpanded.put("containers", true);
        cfg.categoryExpanded.put("furnaces", false);
        cfg.categoryExpanded.put("custom", false);

        return cfg;
    }

    public void addCustomBlock(String blockId) {
        boolean exists = trackedBlocks.stream().anyMatch(tb -> tb.blockId.equals(blockId));
        if (!exists) {
            trackedBlocks.add(new TrackedBlock(blockId, true, 0.9f, 0.9f, 0.9f, "custom"));
            save();
        }
    }

    public void removeBlock(String blockId) {
        trackedBlocks.removeIf(tb -> tb.blockId.equals(blockId));
        save();
    }
}
