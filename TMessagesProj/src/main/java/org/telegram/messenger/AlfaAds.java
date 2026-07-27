package org.telegram.messenger;

import android.app.Activity;

/**
 * AlfaGram — reklama dispetcheri.
 * BU KLASS Yandex kodiga tegmaydi. Reklama yoqilgan bo'lsagina AlfaAdsYandex
 * chaqiriladi — shunda Yandex klasslari yuklanadi. O'chiq bo'lsa umuman yuklanmaydi
 * (VerifyError/crash bo'lmaydi).
 */
public class AlfaAds {

    private static boolean enabled() {
        return AlfaConfig.adsEnabled
                && AlfaConfig.yandexBlockId != null
                && AlfaConfig.yandexBlockId.length() > 0;
    }

    public static void onTransition(Activity activity) {
        if (!enabled() || activity == null) {
            return;
        }
        try {
            AlfaAdsYandex.onTransition(activity);
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }
}
