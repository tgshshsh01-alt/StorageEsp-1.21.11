package com.storagesp.scan;

import com.storagesp.config.ModConfig;
import com.storagesp.config.TrackedBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сканирует загруженные клиентом чанки в заданном радиусе и собирает
 * BlockEntity, соответствующие отслеживаемым блокам из конфига.
 *
 * Работает только с уже загруженными на клиенте чанками — данные приходят
 * от сервера в обычном режиме игры (стандартная синхронизация чанков),
 * поэтому дополнительных пакетов/серверных модов не требуется.
 */
public class ChunkScanner {

    public static class FoundEntry {
        public final BlockPos pos;
        public final TrackedBlock rule;

        public FoundEntry(BlockPos pos, TrackedBlock rule) {
            this.pos = pos;
            this.rule = rule;
        }
    }

    private static List<FoundEntry> cache = new ArrayList<>();
    private static int tickCounter = 0;
    private static final int RESCAN_INTERVAL_TICKS = 20; // раз в секунду, не каждый тик — экономим CPU

    public static List<FoundEntry> getCached() {
        return cache;
    }

    public static void tick() {
        tickCounter++;
        if (tickCounter < RESCAN_INTERVAL_TICKS) return;
        tickCounter = 0;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            cache = new ArrayList<>();
            return;
        }
        if (!ModConfig.INSTANCE.masterEnabled) {
            cache = new ArrayList<>();
            return;
        }

        Map<String, TrackedBlock> exactRules = new HashMap<>();
        List<TrackedBlock> maskRules = new ArrayList<>();
        for (TrackedBlock tb : ModConfig.INSTANCE.trackedBlocks) {
            if (!tb.enabled) continue;
            if (tb.blockId.contains("*")) {
                maskRules.add(tb);
            } else {
                exactRules.put(tb.blockId, tb);
            }
        }
        if (exactRules.isEmpty() && maskRules.isEmpty()) {
            cache = new ArrayList<>();
            return;
        }

        int radius = ModConfig.INSTANCE.scanRadiusChunks;
        ChunkPos center = new ChunkPos(client.player.getBlockPos());
        List<FoundEntry> result = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = center.x + dx;
                int cz = center.z + dz;
                if (!client.world.isChunkLoaded(cx, cz)) continue;

                WorldChunk chunk = client.world.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    BlockEntity be = e.getValue();
                    Identifier id = Registries.BLOCK_ENTITY_TYPE.getId(be.getType());
                    if (id == null) continue;
                    String idStr = id.toString();

                    TrackedBlock rule = exactRules.get(idStr);
                    if (rule == null) {
                        // Также сверяем по фактическому блоку в мире (надежнее для шалкеров/вариантов)
                        Identifier blockId = Registries.BLOCK.getId(client.world.getBlockState(e.getKey()).getBlock());
                        if (blockId != null) {
                            rule = exactRules.get(blockId.toString());
                            if (rule == null) {
                                for (TrackedBlock mr : maskRules) {
                                    if (matchesMask(blockId.toString(), mr.blockId)) {
                                        rule = mr;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (rule != null) {
                        result.add(new FoundEntry(e.getKey().toImmutable(), rule));
                    }
                }
            }
        }

        cache = result;
    }

    private static boolean matchesMask(String actual, String mask) {
        // Простая маска вида "minecraft:*_shulker_box"
        String regex = "^" + mask.replace("*", ".*") + "$";
        return actual.matches(regex);
    }
}
