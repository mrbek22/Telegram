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
    private static boolean promoShown;

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
        Activity activity = fragment.getParentActivity();
        if (activity == null) {
            return;
        }
        if (!bannerShown && AlfaConfig.hasUpdate()) {
            bannerShown = true;
            showSheet(activity);
            return;
        }
        if (!promoShown && hasPromo()) {
            promoShown = true;
            showPromoSheet(activity);
        }
    }

    private static void downloadAndInstall(final Activity activity, final String url) {
        if (activity == null || url == null || url.length() == 0) {
            return;
        }
        try {
            android.widget.Toast.makeText(activity, "Yangilanish yuklanmoqda…", android.widget.Toast.LENGTH_SHORT).show();
        } catch (Throwable ignore) {
        }
        Utilities.globalQueue.postRunnable(() -> {
            try {
                java.io.File dir = new java.io.File(activity.getFilesDir(), "cache");
                dir.mkdirs();
                final java.io.File out = new java.io.File(dir, "AlfaGram-update.apk");
                if (out.exists()) {
                    out.delete();
                }
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(60000);
                c.setInstanceFollowRedirects(true);
                c.setRequestProperty("User-Agent", "AlfaGram");
                java.io.InputStream in = c.getInputStream();
                java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
                byte[] buf = new byte[32768];
                int n;
                while ((n = in.read(buf)) > 0) {
                    fos.write(buf, 0, n);
                }
                fos.flush();
                fos.close();
                in.close();
                c.disconnect();
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        AndroidUtilities.openForView(out, "AlfaGram.apk", "application/vnd.android.package-archive", activity, null, false);
                    } catch (Throwable t) {
                        FileLog.e(t);
                        try { Browser.openUrl(activity, url); } catch (Throwable ignore) {}
                    }
                });
            } catch (Throwable t) {
                FileLog.e(t);
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        android.widget.Toast.makeText(activity, "Yuklab bo'lmadi — brauzerda ochilmoqda", android.widget.Toast.LENGTH_SHORT).show();
                        Browser.openUrl(activity, url);
                    } catch (Throwable ignore) {
                    }
                });
            }
        });
    }

    private static boolean hasPromo() {
        // Faqat e'lon (announcement) bo'lsa pastdan chiqadi. Oddiy tugmalar
        // startupda popup bo'lmaydi — ular Sozlamalar ichida ko'rsatiladi.
        return AlfaConfig.announcementEnabled
                && AlfaConfig.announcementText != null && AlfaConfig.announcementText.length() > 0;
    }

    private static void showPromoSheet(Context context) {
        try {
            final BottomSheet[] ref = new BottomSheet[1];
            LinearLayout ll = new LinearLayout(context);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setPadding(AndroidUtilities.dp(22), AndroidUtilities.dp(12), AndroidUtilities.dp(22), AndroidUtilities.dp(16));

            TextView title = new TextView(context);
            title.setText("AlfaGram");
            title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            title.setTypeface(AndroidUtilities.bold());
            ll.addView(title);

            if (AlfaConfig.announcementEnabled && AlfaConfig.announcementText != null && AlfaConfig.announcementText.length() > 0) {
                TextView ann = new TextView(context);
                ann.setText(AlfaConfig.announcementText);
                ann.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
                ann.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                ann.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(6));
                ll.addView(ann);
                if (AlfaConfig.announcementUrl != null && AlfaConfig.announcementUrl.length() > 0) {
                    ll.addView(makeButton(context, "Ochish", AlfaConfig.announcementUrl, ref));
                }
            }

            for (int i = 0; i < AlfaConfig.buttons.size(); i++) {
                String[] b = AlfaConfig.buttons.get(i);
                if (b != null && b.length >= 2 && b[0] != null && b[0].length() > 0 && b[1] != null && b[1].length() > 0) {
                    ll.addView(makeButton(context, b[0], b[1], ref));
                }
            }

            BottomSheet sheet = new BottomSheet.Builder(context, false).setCustomView(ll).create();
            ref[0] = sheet;
            sheet.show();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static TextView makeButton(Context context, String text, final String url, final BottomSheet[] ref) {
        TextView btn = new TextView(context);
        btn.setText(text);
        btn.setGravity(Gravity.CENTER);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        btn.setTypeface(AndroidUtilities.bold());
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF7C3AED, 0xFFEC4899});
        bg.setCornerRadius(AndroidUtilities.dp(12));
        btn.setBackground(bg);
        btn.setOnClickListener(v -> {
            try {
                Browser.openUrl(context, url);
            } catch (Exception ignore) {
            }
            if (ref[0] != null) {
                ref[0].dismiss();
            }
        });
        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48);
        lp.topMargin = AndroidUtilities.dp(8);
        btn.setLayoutParams(lp);
        return btn;
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
                if (ref[0] != null) {
                    ref[0].dismiss();
                }
                if (context instanceof Activity) {
                    downloadAndInstall((Activity) context, AlfaConfig.updateUrl);
                } else {
                    try { Browser.openUrl(context, AlfaConfig.updateUrl); } catch (Exception ignore) {}
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
