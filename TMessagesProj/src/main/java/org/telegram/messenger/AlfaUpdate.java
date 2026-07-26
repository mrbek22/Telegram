package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * AlfaGram — yangilanish banneri.
 * Asosiy ekran ochilganda config'ni oladi va yangi versiya bo'lsa
 * pastdan chiqadigan panel ko'rsatadi ("Yangilash" / "Keyinroq").
 */
public class AlfaUpdate {

    private static boolean fetchStarted;
    private static boolean bannerShown;

    public static void onMainScreen(BaseFragment fragment) {
        if (fragment == null) {
            return;
        }
        if (!fetchStarted) {
            fetchStarted = true;
            AlfaConfig.loadCached();
            maybeShow(fragment);
            AlfaConfig.fetch(() -> maybeShow(fragment));
        } else {
            maybeShow(fragment);
        }
    }

    private static void maybeShow(BaseFragment fragment) {
        if (bannerShown || !AlfaConfig.hasUpdate()) {
            return;
        }
        Activity activity = fragment.getParentActivity();
        if (activity == null) {
            return;
        }
        bannerShown = true;
        showSheet(activity);
    }

    private static void showSheet(Context context) {
        try {
            final BottomSheet[] ref = new BottomSheet[1];

            LinearLayout ll = new LinearLayout(context);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setPadding(AndroidUtilities.dp(22), AndroidUtilities.dp(12), AndroidUtilities.dp(22), AndroidUtilities.dp(16));

            TextView title = new TextView(context);
            String vn = AlfaConfig.latestVersionName != null && AlfaConfig.latestVersionName.length() > 0
                    ? " " + AlfaConfig.latestVersionName : "";
            title.setText("🚀 Yangi versiya" + vn);
            title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            title.setTypeface(AndroidUtilities.bold());
            ll.addView(title);

            TextView msg = new TextView(context);
            msg.setText(AlfaConfig.updateMessage);
            msg.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
            msg.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            msg.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(18));
            ll.addView(msg);

            TextView update = new TextView(context);
            update.setText("Yangilash");
            update.setGravity(Gravity.CENTER);
            update.setTextColor(Color.WHITE);
            update.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            update.setTypeface(AndroidUtilities.bold());
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFF7C3AED, 0xFFEC4899});
            bg.setCornerRadius(AndroidUtilities.dp(12));
            update.setBackground(bg);
            update.setOnClickListener(v -> {
                try {
                    Browser.openUrl(context, AlfaConfig.updateUrl);
                } catch (Exception ignore) {
                }
                if (ref[0] != null) {
                    ref[0].dismiss();
                }
            });
            ll.addView(update, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));

            if (!AlfaConfig.forceUpdate) {
                TextView later = new TextView(context);
                later.setText("Keyinroq");
                later.setGravity(Gravity.CENTER);
                later.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
                later.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                later.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(4));
                later.setOnClickListener(v -> {
                    if (ref[0] != null) {
                        ref[0].dismiss();
                    }
                });
                ll.addView(later);
            }

            BottomSheet sheet = new BottomSheet.Builder(context, false)
                    .setCustomView(ll)
                    .create();
            ref[0] = sheet;
            if (AlfaConfig.forceUpdate) {
                sheet.setCanDismissWithSwipe(false);
                sheet.setCancelable(false);
            }
            sheet.show();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
}
