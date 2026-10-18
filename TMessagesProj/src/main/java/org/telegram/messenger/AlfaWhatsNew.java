package org.telegram.messenger;

import android.content.SharedPreferences;

import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;

/**
 * AlfaGram — yangilanishdan keyin bir marta chiqadigan "Yangi nimalar?" oynasi.
 * Har yangi versiyada NEWS_VERSION oshiriladi va matn yangilanadi.
 */
public class AlfaWhatsNew {

    /** Yangiliklar ro'yxati versiyasi (matn o'zgarganda oshiring). */
    private static final int NEWS_VERSION = 1;

    private static final String NEWS =
            "🔗 Shaxsiy havola — alfagram.uz/username: nusxalang, ulashing yoki QR kod (Sozlamalar → Mening AlfaGram havolam)\n\n" +
            "✨ Kirill alifbosi — butun ilova o'zbek kirillida (AlfaGram sozlamalari → Til)\n\n" +
            "🔤 Xabarni kirill ↔ lotinga o'girish — xabarni bosing → «Kirillga o'girish»\n\n" +
            "✍️ Lotincha yozing — kirillda yuborilsin\n\n" +
            "🟢 Onlayn tarixi — kontakt qachon onlayn bo'lgani (profil → ⋮)\n\n" +
            "🆔 ID nusxalash — xabar menyusida va profil ⋮ menyusida\n\n" +
            "👆 Ikki marta bosish — javob, nusxa yoki reaksiya (sozlamalardan tanlang)\n\n" +
            "🌐 Tarjima tugmasi xabar menyusida";

    private static boolean shownThisSession;

    /** force=false — faqat yangi versiyada bir marta. */
    public static void show(BaseFragment fragment, boolean force) {
        try {
            if (fragment == null || fragment.getParentActivity() == null) return;
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("alfa_features", 0);
            if (!force) {
                if (shownThisSession || prefs.getInt("whatsNewSeen", 0) >= NEWS_VERSION) return;
                shownThisSession = true;
            }
            prefs.edit().putInt("whatsNewSeen", NEWS_VERSION).apply();
            AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
            builder.setTitle(AlfaCyrillic.ui("AlfaGram'da yangi"));
            builder.setMessage(AlfaCyrillic.ui(NEWS));
            builder.setPositiveButton(AlfaCyrillic.ui("Sozlamalarni ochish"), (d, w) -> {
                try {
                    fragment.presentFragment(new org.telegram.ui.AlfaSettingsActivity());
                } catch (Throwable ignore) {
                }
            });
            builder.setNegativeButton(AlfaCyrillic.ui("Keyinroq"), null);
            fragment.showDialog(builder.create());
        } catch (Throwable ignore) {
        }
    }
}
