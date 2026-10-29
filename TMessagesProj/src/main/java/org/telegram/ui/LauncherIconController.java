package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }

        setIcon(LauncherIcon.DEFAULT);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        for (LauncherIcon i : LauncherIcon.values()) {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    public enum LauncherIcon {
        // AlfaGram: o'z ikonkalari, hammasi bepul (alias nomlari o'zgarmaydi — manifest bilan bog'liq)
        DEFAULT("DefaultIcon", R.mipmap.alfa_icon_bg, R.mipmap.alfa_icon_fg, R.string.AlfaIconDefault),
        VINTAGE("VintageIcon", R.mipmap.alfa_icon_white_bg, R.mipmap.alfa_icon_white_fg, R.string.AlfaIconWhite),
        AQUA("AquaIcon", R.mipmap.alfa_icon_black_bg, R.mipmap.alfa_icon_black_fg, R.string.AlfaIconBlack),
        PREMIUM("PremiumIcon", R.mipmap.alfa_icon_gold_bg, R.mipmap.alfa_icon_gold_fg, R.string.AlfaIconGold),
        TURBO("TurboIcon", R.mipmap.alfa_icon_purple_bg, R.mipmap.alfa_icon_purple_fg, R.string.AlfaIconPurple),
        NOX("NoxIcon", R.mipmap.alfa_icon_neon_bg, R.mipmap.alfa_icon_neon_fg, R.string.AlfaIconNeon);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}
