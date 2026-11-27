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
                // aniq tungi variant (BaseThemeNight/Tinted): 0-variant kunduzgi (Classic) — undan olinsa naqsh qorong'i fonda ko'rinmaydi
                for (int i = 0; i < theme.settings.size(); i++) {
                    TLRPC.BaseTheme base = theme.settings.get(i).base_theme;
                    if (base instanceof TLRPC.TL_baseThemeNight || base instanceof TLRPC.TL_baseThemeTinted) {
                        settingsIndex = i;
                        break;
                    }
                }
                if (settingsIndex < 0 || settingsIndex >= theme.settings.size()) {
                    settingsIndex = 0;
                }
                info = Theme.getTheme(Theme.getBaseThemeKey(theme.settings.get(settingsIndex)));
                if (info == null) {
                    return;
                }
                Theme.ThemeAccent accent = info.accentsByThemeId.get(theme.id);
                if (accent == null) {
                    // qorong'i variantning ranglari va naqsh yorqinligi bilan (createNewAccent(theme, account) doim 0-variantni oladi)
                    accent = info.createNewAccent(theme, account, false, settingsIndex);
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
            // Naqsh fayli yuklangach fon o'zi qayta chizilmaydi (QA'da: mavzu tanlangan, naqsh faqat qo'lda qayta tanlaganda chiqardi).
            // Shuning uchun fayl tayyor bo'lishini kutamiz va fonni qayta yuklaymiz.
            waitPatternAndReload(0);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void waitPatternAndReload(int attempt) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                Theme.ThemeInfo active = Theme.getActiveTheme();
                Theme.ThemeAccent accent = active != null ? active.getAccent(false) : null;
                if (accent == null || android.text.TextUtils.isEmpty(accent.patternSlug)) {
                    return;
                }
                java.io.File file = accent.getPathToWallpaper();
                if (file != null && file.exists() && file.length() > 0) {
                    Theme.reloadWallpaper(true);
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewWallpapper);
                    return;
                }
                if (attempt == 10 || attempt == 25) {
                    Theme.PatternsLoader.createLoader(true);
                }
                if (attempt < 45) {
                    waitPatternAndReload(attempt + 1);
                } else {
                    // oxirgi chora: mavzuni qayta qo'llash (qo'lda tanlash bilan bir xil yo'l)
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme, active, false, null, accent.id);
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }, attempt == 0 ? 1500 : 1000);
    }
}
