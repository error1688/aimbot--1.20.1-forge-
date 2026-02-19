package com.example.yourmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AimScreen extends Screen {
    private final int PANEL_WIDTH = 340;
    private final int PANEL_HEIGHT = 250;
    private int panelX, panelY;
    private int currentTab = 0;

    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    private float tabGlow = 0f;
    private long lastTick = 0;

    public AimScreen() {
        super(Component.literal("§l§bAIM ASSIST TERMINAL"));
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        rebuildUI();
    }

    private void rebuildUI() {
        clearWidgets();

        int titleY = panelY + 5;
        int tabY = panelY + 22;
        int contentY = panelY + 45;
        int leftCol = panelX + 15;
        int rightCol = panelX + 180;

        // 右上角图标按钮
        addRenderableWidget(new CyberIconButton(panelX + PANEL_WIDTH - 50, titleY, 16, 16, "🌫️",
                Config.BLUR_BACKGROUND.get() ? "关闭模糊背景" : "开启模糊背景",
                btn -> {
                    Config.BLUR_BACKGROUND.set(!Config.BLUR_BACKGROUND.get());
                    rebuildUI();
                }));

        addRenderableWidget(new CyberIconButton(panelX + PANEL_WIDTH - 28, titleY, 16, 16, "✕",
                "关闭窗口",
                btn -> onClose()));

        // 选项卡按钮
        int tabWidth = 70;
        int tabStartX = panelX + 20;
        addRenderableWidget(new TabButton(tabStartX, tabY, tabWidth, 16, "核心",
                "基础自瞄参数", 0, currentTab == 0, btn -> {
            currentTab = 0;
            rebuildUI();
        }));
        addRenderableWidget(new TabButton(tabStartX + tabWidth + 5, tabY, tabWidth, 16, "过滤",
                "选择目标类型", 1, currentTab == 1, btn -> {
            currentTab = 1;
            rebuildUI();
        }));
        addRenderableWidget(new TabButton(tabStartX + 2 * (tabWidth + 5), tabY, tabWidth, 16, "娱乐",
                "娱乐功能", 2, currentTab == 2, btn -> {
            currentTab = 2;
            rebuildUI();
        }));

        // 内容区域
        if (currentTab == 0) {
            int rowSpacing = 20;
            int row = 0;

            addRenderableWidget(new GlowSlider(leftCol, contentY + row * rowSpacing, 130, 8, "FOV",
                    "触发自瞄的视野范围 (1-50°)", Config.FOV_DEGREES.get() / 50.0,
                    val -> Config.FOV_DEGREES.set(val * 50),
                    val -> String.format("%.1f°", val * 50)));
            row++;
            addRenderableWidget(new GlowSlider(leftCol, contentY + row * rowSpacing, 130, 8, "距离",
                    "最大锁定距离 (5-100米)", (Config.MAX_DISTANCE.get() - 5) / 95.0,
                    val -> Config.MAX_DISTANCE.set(5 + val * 95),
                    val -> String.format("%.1f m", 5 + val * 95)));
            row++;
            addRenderableWidget(new GlowSlider(leftCol, contentY + row * rowSpacing, 130, 8, "平滑",
                    "静默模式下转向速度 (越小越平滑)", Config.SMOOTHNESS.get(),
                    Config.SMOOTHNESS::set,
                    val -> String.format("%.2f", val)));

            row = 0;
            addRenderableWidget(new CyberToggle(rightCol, contentY + row * rowSpacing, 120, 16, "静默模式",
                    "开启后视角不抖动，仅服务器转向", Config.SILENT_MODE.get(),
                    btn -> {
                        Config.SILENT_MODE.set(!Config.SILENT_MODE.get());
                        rebuildUI();
                    }));
            row++;
            addRenderableWidget(new CyberToggle(rightCol, contentY + row * rowSpacing, 120, 16, "360° 模式",
                    "无视视野限制，全方位锁定", Config.TARGET_360.get(),
                    btn -> {
                        Config.TARGET_360.set(!Config.TARGET_360.get());
                        rebuildUI();
                    }));
            row++;
            addRenderableWidget(new CyberToggle(rightCol, contentY + row * rowSpacing, 120, 16, "总开关",
                    "启用/禁用所有自瞄功能", Config.ENABLED.get(),
                    btn -> {
                        Config.ENABLED.set(!Config.ENABLED.get());
                        rebuildUI();
                    }));

        } else if (currentTab == 1) {
            int col1 = leftCol;
            int col2 = leftCol + 140;
            int rowHeight = 16;
            int row1 = 0; // 左列行索引
            int row2 = 0; // 右列行索引

            // ===== 左列：玩家类 =====
            addFilterToggle(col1, contentY + row1 * rowHeight, 120, "其他玩家", "锁定非好友、非队友的玩家",
                    Config.TARGET_PLAYER.get(), val -> Config.TARGET_PLAYER.set(val));
            row1++;
            // 缩进表示子项
            addFilterToggle(col1 + 10, contentY + row1 * rowHeight, 110, "  好友", "锁定指定名称的好友",
                    Config.TARGET_FRIENDS.get(), val -> Config.TARGET_FRIENDS.set(val));
            row1++;
            addFilterToggle(col1 + 10, contentY + row1 * rowHeight, 110, "  同队伍", "锁定同一队伍的玩家",
                    Config.TARGET_TEAMMATES.get(), val -> Config.TARGET_TEAMMATES.set(val));
            row1++;
            // 空行增加间距
            row1++;

            // ===== 右列：动物类 =====
            addFilterToggle(col2, contentY + row2 * rowHeight, 120, "宠物（已驯服）", "已驯服的动物",
                    Config.TARGET_PET.get(), val -> Config.TARGET_PET.set(val));
            row2++;
            addFilterToggle(col2, contentY + row2 * rowHeight, 120, "其他友善动物", "牛、羊等非宠物",
                    Config.TARGET_PASSIVE.get(), val -> Config.TARGET_PASSIVE.set(val));
            row2++;
            // 空行
            row2++;

            // ===== 左列：其他类型 =====
            addFilterToggle(col1, contentY + row1 * rowHeight, 120, "敌对怪物", "僵尸、骷髅等",
                    Config.TARGET_HOSTILE.get(), val -> Config.TARGET_HOSTILE.set(val));
            row1++;
            addFilterToggle(col1, contentY + row1 * rowHeight, 120, "BOSS", "末影龙、凋灵等",
                    Config.TARGET_BOSS.get(), val -> Config.TARGET_BOSS.set(val));
            row1++;
            addFilterToggle(col1, contentY + row1 * rowHeight, 120, "中立生物", "末影人、狼等",
                    Config.TARGET_NEUTRAL.get(), val -> Config.TARGET_NEUTRAL.set(val));
            row1++;

            // ===== 右列：其他类型 =====
            addFilterToggle(col2, contentY + row2 * rowHeight, 120, "村民", "村民",
                    Config.TARGET_VILLAGER.get(), val -> Config.TARGET_VILLAGER.set(val));
            row2++;
            addFilterToggle(col2, contentY + row2 * rowHeight, 120, "傀儡", "铁傀儡、雪傀儡",
                    Config.TARGET_GOLEM.get(), val -> Config.TARGET_GOLEM.set(val));
            row2++;
            addFilterToggle(col2, contentY + row2 * rowHeight, 120, "盔甲架", "装饰盔甲架",
                    Config.TARGET_STAND.get(), val -> Config.TARGET_STAND.set(val));
            row2++;

            // 编辑好友按钮（放在左列下方）
            addRenderableWidget(new CyberButton(col1 + 50, contentY + row1 * rowHeight + 10, 80, 16, "编辑好友",
                    "点击编辑好友名称列表", btn -> {
                Minecraft.getInstance().setScreen(new FriendListScreen(this));
            }));

        } else if (currentTab == 2) {
            int rowSpacing = 22;
            int row = 0;

            addRenderableWidget(new CyberToggle(leftCol, contentY + row * rowSpacing, 200, 16, "陀螺旋转",
                    "无目标时持续低头旋转 (需开启静默模式)", Config.GYRO_ENABLED.get(),
                    btn -> {
                        Config.GYRO_ENABLED.set(!Config.GYRO_ENABLED.get());
                        rebuildUI();
                    }));
            row++;

            addRenderableWidget(new GlowSlider(leftCol, contentY + row * rowSpacing, 200, 8, "陀螺速度",
                    "旋转速度 (0.1-50°/tick)", Config.GYRO_SPEED.get() / 50.0,
                    val -> Config.GYRO_SPEED.set(val * 50),
                    val -> String.format("%.1f °/t", val * 50)));
            row++;

            addRenderableWidget(new GlowSlider(leftCol, contentY + row * rowSpacing, 200, 8, "陀螺俯仰",
                    "旋转时的俯仰角 (-90~90°)", (Config.GYRO_PITCH.get() + 90) / 180.0,
                    val -> Config.GYRO_PITCH.set(val * 180 - 90),
                    val -> String.format("%.1f°", val * 180 - 90)));
        }
    }

    private void addFilterToggle(int x, int y, int width, String label, String tooltip, boolean current, Consumer<Boolean> setter) {
        addRenderableWidget(new CyberToggle(x, y, width, 14, label, tooltip, current, btn -> {
            setter.accept(!current);
            rebuildUI();
        }));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH &&
                mouseY >= panelY && mouseY <= panelY + 20) {
            dragging = true;
            dragOffsetX = (int) (mouseX - panelX);
            dragOffsetY = (int) (mouseY - panelY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            int newX = (int) (mouseX - dragOffsetX);
            int newY = (int) (mouseY - dragOffsetY);
            newX = Mth.clamp(newX, 0, width - PANEL_WIDTH);
            newY = Mth.clamp(newY, 0, height - PANEL_HEIGHT);
            panelX = newX;
            panelY = newY;
            rebuildUI();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderCustomBackground(graphics);
        drawPanel(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        updateAnimation();

        // 在过滤选项卡上绘制分组标题
        if (currentTab == 1) {
            int contentY = panelY + 45;
            int leftCol = panelX + 15;
            graphics.drawString(font, "§l玩家类", leftCol, contentY - 10, 0xFF00AAFF);
            graphics.drawString(font, "§l动物类", leftCol + 140, contentY - 10, 0xFF00AAFF);
        }
    }

    private void renderCustomBackground(GuiGraphics graphics) {
        if (Config.BLUR_BACKGROUND.get()) {
            graphics.fill(0, 0, width, height, 0xCC000000);
            int gridSize = 20;
            for (int x = 0; x < width; x += gridSize) {
                graphics.fill(x, 0, x + 1, height, 0x3300FFFF);
            }
            for (int y = 0; y < height; y += gridSize) {
                graphics.fill(0, y, width, y + 1, 0x3300FFFF);
            }
        } else {
            graphics.fill(0, 0, width, height, 0xAA000000);
        }
    }

    private void drawPanel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC111122);
        drawGlowBorder(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF00AAFF, 2);

        graphics.fill(panelX + 2, panelY + 20, panelX + PANEL_WIDTH - 2, panelY + 21, 0xFF00AAFF);

        String title = "⚡ AIM ASSIST v2.0 ⚡";
        int titleWidth = font.width(title);
        graphics.drawString(font, title, panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 8, 0xFF00AAFF);

        for (int i = 0; i < 3; i++) {
            graphics.fill(panelX + PANEL_WIDTH - 18 + i * 4, panelY + 14, panelX + PANEL_WIDTH - 15 + i * 4, panelY + 16, 0xFFAAAAAA);
        }
    }

    private void drawGlowBorder(GuiGraphics graphics, int x, int y, int w, int h, int color, int thickness) {
        for (int i = 0; i < thickness; i++) {
            int alpha = 100 - i * 20;
            if (alpha < 0) alpha = 0;
            int col = (alpha << 24) | (color & 0xFFFFFF);
            graphics.fill(x - i - 1, y - i - 1, x + w + i + 1, y - i, col);
            graphics.fill(x - i - 1, y + h + i, x + w + i + 1, y + h + i + 1, col);
            graphics.fill(x - i - 1, y - i, x - i, y + h + i, col);
            graphics.fill(x + w + i, y - i, x + w + i + 1, y + h + i, col);
        }
        graphics.fill(x, y, x + w, y + 1, 0xFF00AAFF);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF00AAFF);
        graphics.fill(x, y, x + 1, y + h, 0xFF00AAFF);
        graphics.fill(x + w - 1, y, x + w, y + h, 0xFF00AAFF);
    }

    private void updateAnimation() {
        long now = System.currentTimeMillis();
        float delta = (now - lastTick) / 50f;
        if (delta > 1) delta = 1;
        tabGlow = Mth.lerp(delta * 0.1f, tabGlow, 0.5f + 0.5f * Mth.sin(now / 200f));
        lastTick = now;
    }

    @Override
    public void onClose() {
        Config.save();
        super.onClose();
    }

    // ==================== 内部控件类 ====================

    private class TabButton extends Button {
        private final String tooltip;
        private final int tabId;
        private final boolean active;
        private float hoverAnim = 0f;

        public TabButton(int x, int y, int w, int h, String text, String tooltip, int tabId, boolean active, OnPress onPress) {
            super(x, y, w, h, Component.literal(text), onPress, DEFAULT_NARRATION);
            this.tooltip = tooltip;
            this.tabId = tabId;
            this.active = active;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            boolean hovered = isHovered();
            hoverAnim = Mth.lerp(0.2f, hoverAnim, hovered ? 1f : 0f);

            int bgColor = active ? 0xFF003366 : 0x44000000;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

            if (active || hoverAnim > 0.01f) {
                int lineColor = active ? 0xFF00AAFF : 0x8800AAFF;
                graphics.fill(getX(), getY() + height - 2, getX() + width, getY() + height, lineColor);
            }

            int textColor = active ? 0xFF00AAFF : (hovered ? 0xFFFFFFFF : 0xFFAAAAAA);
            Component msg = getMessage();
            int textWidth = font.width(msg);
            graphics.drawString(font, msg, getX() + (width - textWidth) / 2, getY() + (height - 8) / 2, textColor);

            if (hovered) {
                graphics.renderTooltip(font, Component.literal(tooltip), mx, my);
            }
        }
    }

    private class GlowSlider extends AbstractSliderButton {
        private final String label;
        private final String tooltip;
        private final Consumer<Double> setter;
        private final ValueFormatter formatter;
        private float handleGlow = 0f;

        interface ValueFormatter {
            String format(double value);
        }

        public GlowSlider(int x, int y, int w, int h, String label, String tooltip, double initialValue,
                          Consumer<Double> setter, ValueFormatter formatter) {
            super(x, y, w, h, Component.empty(), initialValue);
            this.label = label;
            this.tooltip = tooltip;
            this.setter = setter;
            this.formatter = formatter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + formatter.format(value)));
        }

        @Override
        protected void applyValue() {
            setter.accept(value);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            boolean hovered = isHovered();
            handleGlow = Mth.lerp(0.2f, handleGlow, hovered ? 1f : 0f);

            graphics.fill(getX(), getY() + 3, getX() + width, getY() + height - 3, 0xFF333333);
            int fillWidth = (int) (value * width);
            graphics.fill(getX(), getY() + 3, getX() + fillWidth, getY() + height - 3, 0xFF00AAFF);

            int handleX = getX() + fillWidth - 2;
            int glowSize = (int) (4 + handleGlow * 6);
            for (int i = 0; i < glowSize; i++) {
                int alpha = (int) (120 * (1 - i / (float) glowSize) * handleGlow);
                graphics.fill(handleX - i, getY() - i, handleX + 4 + i, getY() + height + i, (alpha << 24) | 0x00AAFF);
            }
            graphics.fill(handleX, getY(), handleX + 2, getY() + height, 0xFFFFFFFF);

            graphics.drawString(font, getMessage(), getX(), getY() - 10, 0xFFCCCCCC);

            if (hovered) {
                graphics.renderTooltip(font, Component.literal(tooltip), mx, my);
            }
        }
    }

    private class CyberToggle extends Button {
        private final boolean state;
        private final String tooltip;
        private float toggleAnim;
        private float glowAnim;

        public CyberToggle(int x, int y, int w, int h, String label, String tooltip, boolean state, OnPress onPress) {
            super(x, y, w, h, Component.literal(label), onPress, DEFAULT_NARRATION);
            this.state = state;
            this.tooltip = tooltip;
            this.toggleAnim = state ? 1f : 0f;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            boolean hovered = isHovered();
            toggleAnim = Mth.lerp(0.2f, toggleAnim, state ? 1f : 0f);
            glowAnim = Mth.lerp(0.2f, glowAnim, hovered ? 1f : 0f);

            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x44000000);

            if (hovered || toggleAnim > 0.01f) {
                int borderColor = state ? 0xFF00FF88 : 0xFFFF4444;
                for (int i = 0; i < 2; i++) {
                    int alpha = (int) (100 * glowAnim * (1 - i * 0.5));
                    graphics.fill(getX() - i - 1, getY() - i - 1, getX() + width + i + 1, getY() - i, (alpha << 24) | borderColor);
                    graphics.fill(getX() - i - 1, getY() + height + i, getX() + width + i + 1, getY() + height + i + 1, (alpha << 24) | borderColor);
                    graphics.fill(getX() - i - 1, getY() - i, getX() - i, getY() + height + i, (alpha << 24) | borderColor);
                    graphics.fill(getX() + width + i, getY() - i, getX() + width + i + 1, getY() + height + i, (alpha << 24) | borderColor);
                }
            }

            int indX = getX() + width - 14;
            int indY = getY() + (height - 6) / 2;
            graphics.fill(indX, indY, indX + 6, indY + 6, state ? 0xFF00FF88 : 0xFFFF4444);

            int textColor = state ? 0xFFFFFFFF : 0xFFAAAAAA;
            graphics.drawString(font, getMessage(), getX() + 4, getY() + (height - 8) / 2, textColor);

            if (hovered) {
                graphics.renderTooltip(font, Component.literal(tooltip), mx, my);
            }
        }
    }

    private class CyberIconButton extends Button {
        private final String tooltip;
        private float hoverAnim = 0f;

        public CyberIconButton(int x, int y, int w, int h, String icon, String tooltip, OnPress onPress) {
            super(x, y, w, h, Component.literal(icon), onPress, DEFAULT_NARRATION);
            this.tooltip = tooltip;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            boolean hovered = isHovered();
            hoverAnim = Mth.lerp(0.2f, hoverAnim, hovered ? 1f : 0f);

            int bgColor = 0x44000000 + ((int) (hoverAnim * 0x33) << 24);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

            if (hovered) {
                drawGlowBorder(graphics, getX(), getY(), width, height, 0xFF00AAFF, 1);
            }

            Component msg = getMessage();
            int textWidth = font.width(msg);
            graphics.drawString(font, msg, getX() + (width - textWidth) / 2, getY() + (height - 8) / 2, 0xFFFFFFFF);

            if (hovered) {
                graphics.renderTooltip(font, Component.literal(tooltip), mx, my);
            }
        }
    }

    private class CyberButton extends Button {
        private final String tooltip;
        private float hoverAnim = 0f;

        public CyberButton(int x, int y, int w, int h, String text, String tooltip, OnPress onPress) {
            super(x, y, w, h, Component.literal(text), onPress, DEFAULT_NARRATION);
            this.tooltip = tooltip;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            boolean hovered = isHovered();
            hoverAnim = Mth.lerp(0.2f, hoverAnim, hovered ? 1f : 0f);

            int bgColor = 0x44000000 + ((int) (hoverAnim * 0x33) << 24);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

            if (hovered) {
                drawGlowBorder(graphics, getX(), getY(), width, height, 0xFF00AAFF, 1);
            }

            Component msg = getMessage();
            int textWidth = font.width(msg);
            graphics.drawString(font, msg, getX() + (width - textWidth) / 2, getY() + (height - 8) / 2, 0xFFFFFFFF);

            if (hovered) {
                graphics.renderTooltip(font, Component.literal(tooltip), mx, my);
            }
        }
    }

    // 新增好友列表编辑屏幕
    private static class FriendListScreen extends Screen {
        private final AimScreen parent;
        private EditBox textBox;

        protected FriendListScreen(AimScreen parent) {
            super(Component.literal("编辑好友列表"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int centerX = width / 2;
            int centerY = height / 2;

            String currentNames = String.join(", ", Config.FRIEND_NAMES.get());
            textBox = new EditBox(font, centerX - 150, centerY - 20, 300, 20, Component.literal("好友名称"));
            textBox.setValue(currentNames);
            textBox.setMaxLength(1000);
            addRenderableWidget(textBox);

            addRenderableWidget(Button.builder(Component.literal("保存"), btn -> {
                String input = textBox.getValue().trim();
                List<String> names = new ArrayList<>();
                if (!input.isEmpty()) {
                    names = Arrays.stream(input.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                }
                Config.FRIEND_NAMES.set(names);
                Config.save();
                minecraft.setScreen(parent);
            }).bounds(centerX - 50, centerY + 10, 100, 20).build());

            addRenderableWidget(Button.builder(Component.literal("取消"), btn -> {
                minecraft.setScreen(parent);
            }).bounds(centerX - 50, centerY + 40, 100, 20).build());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawString(font, "请输入好友名称，用逗号分隔", width / 2 - 150, height / 2 - 40, 0xFFFFFF);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                minecraft.setScreen(parent);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}