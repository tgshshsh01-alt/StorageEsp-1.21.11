package com.storagesp.gui;

import com.storagesp.config.ModConfig;
import com.storagesp.config.TrackedBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Главное меню настроек StorageSP.
 * Открывается по хоткею (по умолчанию не назначен — задаётся в Controls,
 * либо через /storagesp, если добавите команду) или из общего меню Mod Menu.
 */
public class ConfigScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget addBlockField;
    private final List<ButtonWidget> blockRowButtons = new ArrayList<>();

    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 70;
    private int listBottom;

    public ConfigScreen(Screen parent) {
        super(Text.translatable("storagesp.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig cfg = ModConfig.INSTANCE;
        listBottom = this.height - 60;
        int cx = this.width / 2;

        // ---- Верхняя панель: общие переключатели ----
        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.masterEnabled)
                .build(cx - 205, 30, 130, 20,
                        Text.translatable("storagesp.screen.master_toggle"),
                        (btn, val) -> cfg.masterEnabled = val));

        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.renderBoxes)
                .build(cx - 65, 30, 130, 20,
                        Text.translatable("storagesp.screen.render.boxes"),
                        (btn, val) -> cfg.renderBoxes = val));

        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.renderTracers)
                .build(cx + 75, 30, 130, 20,
                        Text.translatable("storagesp.screen.render.lines"),
                        (btn, val) -> cfg.renderTracers = val));

        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.renderThroughWalls)
                .build(cx - 205, 52, 200, 20,
                        Text.translatable("storagesp.screen.render.through_walls"),
                        (btn, val) -> cfg.renderThroughWalls = val));

        // Слайдер радиуса сканирования (в чанках, 1..16)
        this.addDrawableChild(new SliderWidget(cx + 5, 52, 200, 20,
                Text.translatable("storagesp.screen.scan_radius", cfg.scanRadiusChunks),
                (cfg.scanRadiusChunks - 1) / 15.0) {
            @Override
            protected void updateMessage() {
                setMessage(Text.translatable("storagesp.screen.scan_radius", cfg.scanRadiusChunks));
            }

            @Override
            protected void applyValue() {
                cfg.scanRadiusChunks = 1 + (int) Math.round(this.value * 15.0);
            }
        });

        // ---- Список отслеживаемых блоков ----
        buildBlockList();

        // ---- Поле добавления кастомного блока ----
        addBlockField = new TextFieldWidget(this.textRenderer, cx - 150, listBottom + 8, 220, 20,
                Text.translatable("storagesp.screen.add_block"));
        addBlockField.setPlaceholder(Text.literal("minecraft:my_block"));
        this.addDrawableChild(addBlockField);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("storagesp.screen.add_block"), btn -> {
            String id = addBlockField.getText().trim();
            if (!id.isEmpty()) {
                cfg.addCustomBlock(id);
                addBlockField.setText("");
                this.clearAndInit();
            }
        }).dimensions(cx + 75, listBottom + 8, 100, 20).build());

        // ---- Готово ----
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("storagesp.screen.done"), btn -> {
            ModConfig.save();
            this.close();
        }).dimensions(cx - 50, this.height - 28, 100, 20).build());
    }

    private void buildBlockList() {
        blockRowButtons.clear();
        List<TrackedBlock> blocks = ModConfig.INSTANCE.trackedBlocks;
        int cx = this.width / 2;

        for (int i = 0; i < blocks.size(); i++) {
            TrackedBlock tb = blocks.get(i);
            int y = LIST_TOP + i * ROW_HEIGHT - scrollOffset;
            if (y < LIST_TOP - ROW_HEIGHT || y > listBottom) continue; // не рисуем то, что за пределами видимой области

            ButtonWidget toggle = CyclingButtonWidget.onOffBuilder(tb.enabled)
                    .build(cx - 210, y, 60, 20, Text.literal(""), (btn, val) -> {
                        tb.enabled = val;
                        ModConfig.save();
                    });
            this.addDrawableChild(toggle);
            blockRowButtons.add(toggle);

            ButtonWidget removeBtn = ButtonWidget.builder(Text.literal("✕"), btn -> {
                ModConfig.INSTANCE.removeBlock(tb.blockId);
                this.clearAndInit();
            }).dimensions(cx + 205, y, 20, 20).build();
            // Не даём удалять базовые контейнеры случайно — только custom и furnaces
            removeBtn.active = !tb.category.equals("containers");
            this.addDrawableChild(removeBtn);
            blockRowButtons.add(removeBtn);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        // Подписи строк списка (текст id + категория), кнопки рисуются отдельно виджетами
        int cx = this.width / 2;
        List<TrackedBlock> blocks = ModConfig.INSTANCE.trackedBlocks;
        for (int i = 0; i < blocks.size(); i++) {
            TrackedBlock tb = blocks.get(i);
            int y = LIST_TOP + i * ROW_HEIGHT - scrollOffset;
            if (y < LIST_TOP - ROW_HEIGHT || y > listBottom) continue;

            int colorInt = ((int) (tb.colorR * 255) << 16) | ((int) (tb.colorG * 255) << 8) | (int) (tb.colorB * 255);
            context.drawText(this.textRenderer, Text.literal(tb.blockId + "  [" + tb.category + "]"),
                    cx - 145, y + 6, 0xFF000000 | colorInt, false);
        }

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Прокрутка: колесо мыши"), this.width / 2, listBottom + 32, 0x808080);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, ModConfig.INSTANCE.trackedBlocks.size() * ROW_HEIGHT - (listBottom - LIST_TOP));
        scrollOffset -= (int) (verticalAmount * ROW_HEIGHT);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        this.clearAndInit();
        return true;
    }

    @Override
    public void close() {
        ModConfig.save();
        if (this.client != null) this.client.setScreen(parent);
    }
}
