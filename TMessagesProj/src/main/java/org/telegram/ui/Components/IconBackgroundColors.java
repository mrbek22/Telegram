package org.telegram.ui.Components;

// AlfaGram: brend gradientlari (alfagram-design/icons.json bilan mos)
public enum IconBackgroundColors {
    BLUE(0xFF4F46E5, 0xFF8B5CF6),
    BLUE_ALT(0xFF06B6D4, 0xFF6366F1),
    BLUE_DEEP(0xFF6366F1, 0xFF7C3AED),
    BLUE_LIGHT(0xFF4F46E5, 0xFF8B5CF6),

    ORANGE(0xFFEC4899, 0xFFF97316),
    ORANGE_DEEP(0xFFF97316, 0xFFEC4899),

    GREEN(0xFF10B981, 0xFF06B6D4),
    RED(0xFFEC4899, 0xFFF43F5E),
    CYAN(0xFF06B6D4, 0xFF6366F1),
    PURPLE(0xFF7C3AED, 0xFFEC4899),
    GRAY(0xFF8B87A0, 0xFF6B6880);

    public final int top;
    public final int bottom;

    private IconBackgroundColors(int top, int bottom) {
        this.top = top;
        this.bottom = bottom;
    }
}
