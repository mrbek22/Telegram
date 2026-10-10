package org.telegram.ui.Components;

// AlfaGram: brend gradientlari (ko'k logotip palitrasi, alfagram-design/icons.json bilan mos)
public enum IconBackgroundColors {
    BLUE(0xFF0034B6, 0xFF0081FE),
    BLUE_ALT(0xFF06B6D4, 0xFF2563EB),
    BLUE_DEEP(0xFF2563EB, 0xFF0034B6),
    BLUE_LIGHT(0xFF0034B6, 0xFF0081FE),

    ORANGE(0xFFEF4444, 0xFFF97316),
    ORANGE_DEEP(0xFFF97316, 0xFFEF4444),

    GREEN(0xFF10B981, 0xFF06B6D4),
    RED(0xFFEF4444, 0xFFDC2626),
    CYAN(0xFF06B6D4, 0xFF2563EB),
    PURPLE(org.telegram.messenger.AlfaFeatures.BRAND_START, org.telegram.messenger.AlfaFeatures.BRAND_END),
    GRAY(0xFF8494A8, 0xFF64748B);

    public final int top;
    public final int bottom;

    private IconBackgroundColors(int top, int bottom) {
        this.top = top;
        this.bottom = bottom;
    }
}
