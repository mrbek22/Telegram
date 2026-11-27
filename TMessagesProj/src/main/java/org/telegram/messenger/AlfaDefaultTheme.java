package org.telegram.messenger;

import android.app.Activity;
import android.content.SharedPreferences;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.EmojiThemes;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ChatThemeBottomSheet;

import java.util.ArrayList;

/**
 * AlfaGram: sukut mavzu — 🎮 rangli mavzuning qorong'i varianti (qora fon + o'yin naqshi).
 * Bir marta qo'llanadi: yangi o'rnatishda yoki user hali sukut "Night" (home) mavzusini o'zgartirmagan bo'lsa.
 * Mavzular ro'yxati serverdan kelgach (MediaDataController.defaultEmojiThemes) chaqiriladi.
 */
public final class AlfaDefaultTheme {

    private static final String PREF = "alfa_game_theme_v1";
    private static final String EMOTICON = "🎮"; // 🎮
    private static final int DARK_INDEX = 2;

    private AlfaDefaultTheme() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Activity.MODE_PRIVATE);
    }

    /** user hali sukut qorong'i mavzuda (Night, home accent, o'z fon rasmisiz) */
    private static boolean isStillDefault() {
        Theme.ThemeInfo active = Theme.getActiveTheme();
        if (active == null || !"Night".equals(active.getKey())) {
            return false;
        }
        Theme.ThemeAccent accent = active.getAccent(false);
        if (accent != null && !Theme.isHome(accent)) {
            return false;
        }
        if (accent != null && accent.overrideWallpaper != null) {
            return false;
        }
        return active.overrideWallpaper == null;
    }

    public static void maybeApply(int account, ArrayList<ChatThemeBottomSheet.ChatThemeItem> items) {
        try {
            if (items == null || items.isEmpty() || prefs().getBoolean(PREF, false)) {
                return;
            }
            if (org.telegram.ui.LaunchActivity.instance == null || org.telegram.ui.LaunchActivity.instance.isFinishing()) {
                return; // mavzu almashtirish xabarini qabul qiladigan oyna hali yo'q — keyingi yuklashda
            }
            if (!isStillDefault()) {
                prefs().edit().putBoolean(PREF, true).apply();
                return;
            }
            EmojiThemes game = null;
            for (ChatThemeBottomSheet.ChatThemeItem item : items) {
                if (item != null && item.chatTheme != null && EMOTICON.equals(item.chatTheme.getEmoticonOrSlug())) {
                    game = item.chatTheme;
                    break;
                }
            }
            if (game == null) {
                return; // ro'yxatda hali yo'q — keyingi yuklashda yana urinamiz
            }
            Theme.ThemeInfo info = game.getThemeInfo(DARK_INDEX);
            int accentId = -1;
            if (info == null) {
                TLRPC.TL_theme theme = game.getTlTheme(DARK_INDEX);
                if (theme == null || theme.settings == null || theme.settings.isEmpty()) {
                    return;
                }
                int settingsIndex = game.getSettingsIndex(DARK_INDEX);
                if (settingsIndex < 0 || settingsIndex >= theme.settings.size()) {
                    settingsIndex = 0;
                }
                info = Theme.getTheme(Theme.getBaseThemeKey(theme.settings.get(settingsIndex)));
                if (info == null) {
                    return;
                }
                Theme.ThemeAccent accent = info.accentsByThemeId.get(theme.id);
                if (accent == null) {
                    accent = info.createNewAccent(theme, account);
                }
                accentId = accent.id;
                info.setCurrentAccentId(accentId);
            }
            prefs().edit()
                    .putBoolean(PREF, true)
                    .putString("lastDarkTheme", info.getKey())
                    .apply();
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme, info, false, null, accentId);
            // Mavzu fonidagi naqsh (o'yin rasmlari) fayli hali yuklanmagan — yuklab, tayyor bo'lgach fonni qayta chizadi.
            // (Qo'lda tanlaganda ro'yxat oldindan yuklab qo'yadi; avtomatik qo'llashda bu qadam kerak.)
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    Theme.PatternsLoader.createLoader(true);
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }, 600);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
