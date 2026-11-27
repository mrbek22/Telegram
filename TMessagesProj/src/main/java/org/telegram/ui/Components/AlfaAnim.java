package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

/**
 * AlfaGram — umumiy animatsiyalar va Telegram'ning qimirlaydigan (animatsiyali) emojilari.
 * Hamma joyda bir xil: LiteMode (quvvat tejash / "animatsiyalarni o'chirish") yoqilgan bo'lsa harakat o'chadi,
 * emoji to'plami hali yuklanmagan bo'lsa — oddiy rangli emoji ko'rinadi. Har qanday telefonga mos.
 */
public final class AlfaAnim {

    private AlfaAnim() {
    }

    public static boolean enabled() {
        try {
            return LiteMode.isEnabled(LiteMode.FLAGS_CHAT);
        } catch (Throwable e) {
            return true;
        }
    }

    /** Telegram animatsiyali emojisi (doim aylanib turadi). */
    public static View emoji(Context ctx, String emoji, int sizeDp) {
        return emoji(ctx, UserConfig.selectedAccount, emoji, sizeDp, true);
    }

    public static View emoji(Context ctx, int account, String emoji, int sizeDp, boolean loop) {
        try {
            TLRPC.Document doc = MediaDataController.getInstance(account).getEmojiAnimatedSticker(emoji);
            if (doc != null) {
                BackupImageView v = new BackupImageView(ctx);
                boolean animate = LiteMode.isEnabled(LiteMode.FLAGS_ANIMATED_STICKERS);
                String filter = sizeDp + "_" + sizeDp + (animate ? "" : "_firstframe");
                v.getImageReceiver().setAutoRepeat(loop ? 1 : 0);
                Drawable thumb = null;
                try {
                    thumb = Emoji.getEmojiBigDrawable(emoji);
                } catch (Throwable ignore) {
                }
                v.setImage(ImageLocation.getForDocument(doc), filter, "tgs", thumb, doc);
                return v;
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        TextView t = new TextView(ctx);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDp * 0.72f);
        t.setGravity(Gravity.CENTER);
        t.setText(Emoji.replaceEmoji(emoji, t.getPaint().getFontMetricsInt(), false));
        return t;
    }

    /** AlertDialog tepasi uchun: markazda katta animatsiyali emoji (Builder.setTopView). */
    public static View dialogTop(Context ctx, String emoji) {
        FrameLayout f = new FrameLayout(ctx);
        View e = emoji(ctx, emoji, 92);
        f.addView(e, LayoutHelper.createFrame(92, 92, Gravity.CENTER));
        popIn(e, 80);
        return f;
    }

    /** Sarlavha oldiga qo'yiladigan kichik animatsiyali emoji. */
    public static View titleEmoji(Context ctx, String emoji) {
        View e = emoji(ctx, emoji, 26);
        popIn(e, 120);
        return e;
    }

    /** Bo'sh holat: katta animatsiyali emoji + izoh matni. */
    public static LinearLayout emptyState(Context ctx, String emoji, TextView text) {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setPadding(0, dp(18), 0, 0);
        View e = emoji(ctx, emoji, 96);
        l.addView(e, LayoutHelper.createLinear(96, 96, Gravity.CENTER_HORIZONTAL));
        if (text.getParent() instanceof ViewGroup) {
            ((ViewGroup) text.getParent()).removeView(text);
        }
        text.setPadding(dp(24), dp(10), dp(24), dp(30));
        l.addView(text, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        popIn(e, 100);
        fadeUp(text, 180);
        return l;
    }

    /** Kichikdan kattalashib "sakrab" chiqish. */
    public static void popIn(View v, long delay) {
        if (v == null || !enabled()) {
            return;
        }
        v.setScaleX(0.4f);
        v.setScaleY(0.4f);
        v.setAlpha(0f);
        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(delay).setDuration(420)
                .setInterpolator(new OvershootInterpolator(1.6f)).start();
    }

    /** Pastdan suzib, xira holatdan paydo bo'lish. */
    public static void fadeUp(View v, long delay) {
        if (v == null || !enabled()) {
            return;
        }
        v.setAlpha(0f);
        v.setTranslationY(dp(16));
        v.animate().alpha(1f).translationY(0).setStartDelay(delay).setDuration(320)
                .setInterpolator(new DecelerateInterpolator(1.6f)).start();
    }

    /** Ro'yxat elementlari navbat bilan paydo bo'ladi (birinchi ko'rsatishda). */
    public static void stagger(ViewGroup parent, long startDelay) {
        if (parent == null || !enabled()) {
            return;
        }
        for (int i = 0; i < parent.getChildCount(); i++) {
            fadeUp(parent.getChildAt(i), startDelay + i * 45L);
        }
    }

    /** RecyclerView/ViewGroup birinchi chizilganda qatorlar navbat bilan pastdan suzib chiqadi. */
    public static void listAppear(ViewGroup list) {
        if (list == null || !enabled()) {
            return;
        }
        android.view.animation.AnimationSet set = new android.view.animation.AnimationSet(true);
        android.view.animation.AlphaAnimation alpha = new android.view.animation.AlphaAnimation(0f, 1f);
        android.view.animation.TranslateAnimation tr = new android.view.animation.TranslateAnimation(0, 0, dp(18), 0);
        set.addAnimation(alpha);
        set.addAnimation(tr);
        set.setDuration(300);
        set.setInterpolator(new DecelerateInterpolator(1.6f));
        android.view.animation.LayoutAnimationController c = new android.view.animation.LayoutAnimationController(set, 0.12f);
        list.setLayoutAnimation(c);
    }

    /** Bosilganda yengil "sakrash" (tugma, kafel). */
    public static void bounce(View v) {
        if (v == null || !enabled()) {
            return;
        }
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(360);
        a.addUpdateListener(an -> {
            float t = (float) an.getAnimatedValue();
            float s = t < 0.35f ? 1f - 0.08f * (t / 0.35f) : 0.92f + 0.08f * new OvershootInterpolator(3f).getInterpolation((t - 0.35f) / 0.65f);
            v.setScaleX(s);
            v.setScaleY(s);
        });
        a.start();
    }
}
