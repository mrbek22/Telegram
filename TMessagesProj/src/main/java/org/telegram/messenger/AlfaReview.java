package org.telegram.messenger;

import android.app.Activity;
import android.content.SharedPreferences;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import org.telegram.ui.ActionBar.BaseFragment;

/**
 * AlfaGram — Google Play'ning ichki "Ilovani baholang ⭐" oynasi. Faqat Play'dan o'rnatilgan buildda,
 * o'rnatilganidan 3 kun o'tib va ilova kamida 8 marta ochilgach so'raladi; keyin 60 kunda bir martadan
 * ko'p emas (Google o'zi ham cheklaydi — oyna har safar chiqmasligi mumkin).
 */
public class AlfaReview {

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long MIN_INSTALL_AGE = 3 * DAY;
    private static final int MIN_OPENS = 8;
    private static final long REPEAT_AFTER = 60 * DAY;

    private static boolean countedThisProcess;
    private static boolean askedThisProcess;

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("alfa_review", Activity.MODE_PRIVATE);
    }

    /** Chatlar ro'yxati ochilganda (DialogsActivity.onResume). */
    public static void onMainScreen(BaseFragment fragment) {
        if (!AlfaFeatures.PLAY_BUILD || askedThisProcess || fragment == null) {
            return;
        }
        try {
            SharedPreferences p = prefs();
            long now = System.currentTimeMillis();
            long first = p.getLong("first", 0);
            if (first == 0) {
                p.edit().putLong("first", now).apply();
                first = now;
            }
            int opens = p.getInt("opens", 0);
            if (!countedThisProcess) {
                countedThisProcess = true;
                opens++;
                p.edit().putInt("opens", opens).apply();
            }
            if (now - first < MIN_INSTALL_AGE || opens < MIN_OPENS) {
                return;
            }
            long last = p.getLong("last_ask", 0);
            if (last != 0 && now - last < REPEAT_AFTER) {
                return;
            }
            Activity activity = fragment.getParentActivity();
            if (activity == null || !isFromPlay(activity)) {
                return;
            }
            askedThisProcess = true;
            AndroidUtilities.runOnUIThread(() -> launch(fragment), 3000);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static boolean isFromPlay(Activity activity) {
        try {
            String installer = activity.getPackageManager().getInstallerPackageName(activity.getPackageName());
            return "com.android.vending".equals(installer);
        } catch (Throwable e) {
            return false;
        }
    }

    private static void launch(BaseFragment fragment) {
        Activity activity = fragment.getParentActivity();
        if (activity == null || fragment.isPaused() || fragment.getVisibleDialog() != null) {
            // keyingi safar (ilova qayta ochilganda) yana urinib ko'riladi
            askedThisProcess = false;
            return;
        }
        try {
            ReviewManager manager = ReviewManagerFactory.create(activity);
            manager.requestReviewFlow().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    return;
                }
                Activity current = fragment.getParentActivity();
                if (current == null) {
                    return;
                }
                ReviewInfo info = task.getResult();
                prefs().edit().putLong("last_ask", System.currentTimeMillis()).apply();
                manager.launchReviewFlow(current, info);
            });
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
