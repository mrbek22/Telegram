package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.YandexAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;

/**
 * AlfaGram — Yandex interstitial reklama amaliyoti (SDK 8.2.0).
 * Bu klass FAQAT reklama yoqilganda (AlfaAds orqali) yuklanadi.
 */
class AlfaAdsYandex {

    private static boolean initialized;
    private static InterstitialAdLoader loader;
    private static InterstitialAdLoadListener loadListener;
    private static InterstitialAd interstitialAd;
    private static boolean loading;
    private static int transitionCount;
    private static long lastShownTime;

    private static void init(Context context) {
        if (initialized || context == null) {
            return;
        }
        initialized = true;
        final Context app = context.getApplicationContext();
        YandexAds.initialize(app, () -> {});
        loader = new InterstitialAdLoader(app);
        loadListener = new InterstitialAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialAd = ad;
                loading = false;
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError error) {
                loading = false;
            }
        };
        preload();
    }

    private static void preload() {
        if (loader == null || loadListener == null || loading || interstitialAd != null) {
            return;
        }
        loading = true;
        try {
            loader.loadAd(new AdRequest.Builder(AlfaConfig.yandexBlockId).build(), loadListener);
        } catch (Throwable t) {
            loading = false;
            FileLog.e(t);
        }
    }

    static void onTransition(Activity activity) {
        init(activity);
        if (loader == null) {
            return;
        }
        transitionCount++;
        if (transitionCount % AlfaConfig.adsInterval != 0) {
            preload();
            return;
        }
        if (System.currentTimeMillis() - lastShownTime < 60_000) {
            return;
        }
        if (interstitialAd == null) {
            preload();
            return;
        }
        final InterstitialAd ad = interstitialAd;
        interstitialAd = null;
        ad.setAdEventListener(new InterstitialAdEventListener() {
            @Override
            public void onAdShown() {
            }

            @Override
            public void onAdFailedToShow(@NonNull AdError adError) {
                ad.setAdEventListener(null);
                preload();
            }

            @Override
            public void onAdDismissed() {
                ad.setAdEventListener(null);
                preload();
            }

            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdImpression(@Nullable ImpressionData impressionData) {
            }
        });
        lastShownTime = System.currentTimeMillis();
        ad.show(activity);
    }
}
