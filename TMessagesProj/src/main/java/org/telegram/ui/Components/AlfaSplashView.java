package org.telegram.ui.Components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import org.telegram.messenger.AlfaFeatures;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;

/**
 * AlfaGram — ilova yopiq holatdan ochilganda logo "chiziladi": avval kvadrat, keyin "A" ning chap va o'ng
 * oyog'i, so'ng samolyot, oxirida logo to'liq yonadi (~1 s).
 * <ul>
 * <li>Ilova (chatlar ro'yxati) tezroq tayyor bo'lsa — qolgan chizish ~0,2 s da tugatiladi va ekran ochiladi.</li>
 * <li>Sekinroq bo'lsa — chizish tugagach logo "nafas oladi" (to'lqin halqa), tayyor bo'lishi bilan ochiladi.</li>
 * </ul>
 * Hech qachon 3,5 s dan ko'p turmaydi. Telefonda "Barcha animatsiyalarni o'chirish" yoqilgan bo'lsa chiqmaydi.
 */
public class AlfaSplashView extends View {

    public interface ReadyCheck {
        boolean isReady();
    }

    private static boolean shownInProcess;

    /** Jarayon boshida bir marta (LaunchActivity.onCreate, savedInstanceState == null). */
    public static void showIfNeeded(Activity activity, ReadyCheck ready) {
        if (shownInProcess || activity == null) {
            return;
        }
        shownInProcess = true;
        if (AlfaFeatures.disableAnimations) {
            return;
        }
        try {
            ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
            AlfaSplashView view = new AlfaSplashView(activity, ready);
            decor.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            view.start();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    // ── vaqtlar (ms), oddiy tezlikda ──
    private static final float DRAW_END = 1000f;
    private static final float BG_START = 0f, BG_END = 220f;
    private static final float FULL_START = 880f, FULL_END = 1000f;
    /** har chiziq: boshlanish, tugash, cho'tka qalinligi (100 birlikda), qatlam (0 — "A", 1 — samolyot) */
    private static final float[][] SEGS = {
            {120f, 340f, 26f, 0},
            {320f, 560f, 28f, 0},
            {540f, 820f, 26f, 1},
            {790f, 930f, 20f, 1},
    };
    private static final long FINISH_FAST_MS = 200;
    private static final long EXIT_MS = 240;
    private static final long BREATH_MS = 1100;
    private static final long MAX_TOTAL_MS = 3500;

    private static final int COLOR_SCREEN = 0xFF0B0F1A;
    private static final int COLOR_SQUARE = 0xFF020A2E;
    private static final int COLOR_RIM = 0xFF2F6BFF;
    private static final int COLOR_RING = 0xFF4F9DFF;

    private final ReadyCheck readyCheck;
    private final Bitmap bmpA, bmpP, bmpFull;

    private final Path[] basePaths = new Path[SEGS.length];
    private final Path[] scaledPaths = new Path[SEGS.length];
    private final PathMeasure[] measures = new PathMeasure[SEGS.length];
    private final Path segPath = new Path();
    private final Path clipPath = new Path();
    private final RectF logoRect = new RectF();
    private final RectF tmpRect = new RectF();
    private final float[] pos = new float[2];

    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint srcInPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** chizish vaqti (0..DRAW_END) */
    private float t;
    /** nafas olish fazasi (0..1, takrorlanadi), -1 — nafas yo'q */
    private float breath = -1f;
    /** chiqish (0..1) */
    private float exit;

    private ValueAnimator drawAnimator;
    private ValueAnimator breathAnimator;
    private boolean finishing;
    private boolean exiting;
    private long startedAt;

    private final Runnable readyPoll = new Runnable() {
        @Override
        public void run() {
            if (exiting) {
                return;
            }
            boolean ready;
            try {
                ready = readyCheck == null || readyCheck.isReady();
            } catch (Throwable e) {
                ready = true;
            }
            if (ready || SystemClock.elapsedRealtime() - startedAt > MAX_TOTAL_MS) {
                onReady();
            } else {
                AndroidUtilities.runOnUIThread(this, 50);
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private AlfaSplashView(Context context, ReadyCheck readyCheck) {
        super(context);
        this.readyCheck = readyCheck;
        setClickable(true); // ochilish paytida bosishlar ilovaga o'tmasin
        setBackgroundColor(COLOR_SCREEN);
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        bmpA = BitmapFactory.decodeResource(getResources(), R.drawable.alfa_splash_a, o);
        bmpP = BitmapFactory.decodeResource(getResources(), R.drawable.alfa_splash_p, o);
        bmpFull = BitmapFactory.decodeResource(getResources(), R.drawable.alfa_splash_full, o);

        basePaths[0] = line(20, 64, 50, 13);
        basePaths[1] = line(50, 13, 86, 86);
        Path swoosh = new Path();
        swoosh.moveTo(8, 82);
        swoosh.quadTo(40, 54, 90, 31);
        basePaths[2] = swoosh;
        basePaths[3] = line(86, 33, 57, 76);
        for (int i = 0; i < SEGS.length; i++) {
            scaledPaths[i] = new Path();
            measures[i] = new PathMeasure();
        }

        maskPaint.setStyle(Paint.Style.STROKE);
        maskPaint.setStrokeCap(Paint.Cap.ROUND);
        maskPaint.setStrokeJoin(Paint.Join.ROUND);
        maskPaint.setColor(0xFFFFFFFF);
        srcInPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        fillPaint.setColor(COLOR_SQUARE);
        strokePaint.setStyle(Paint.Style.STROKE);
        tipPaint.setColor(0xFFFFFFFF);
    }

    private static Path line(float x1, float y1, float x2, float y2) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        return p;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float size = Math.min(AndroidUtilities.dp(124), Math.min(w, h) * 0.42f);
        float left = (w - size) / 2f;
        float top = (h - size) / 2f;
        logoRect.set(left, top, left + size, top + size);
        Matrix m = new Matrix();
        m.setScale(size / 100f, size / 100f);
        m.postTranslate(left, top);
        for (int i = 0; i < SEGS.length; i++) {
            scaledPaths[i].reset();
            basePaths[i].transform(m, scaledPaths[i]);
            measures[i].setPath(scaledPaths[i], false);
        }
        float r = size * 0.21f;
        clipPath.reset();
        clipPath.addRoundRect(logoRect, r, r, Path.Direction.CW);
    }

    private void start() {
        startedAt = SystemClock.elapsedRealtime();
        drawAnimator = ValueAnimator.ofFloat(0f, DRAW_END);
        drawAnimator.setDuration((long) DRAW_END);
        drawAnimator.setInterpolator(new LinearInterpolator());
        drawAnimator.addUpdateListener(a -> {
            t = (float) a.getAnimatedValue();
            invalidate();
        });
        drawAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (finishing || exiting) {
                    return;
                }
                // chizish tugadi, ilova hali tayyor emas — logo "nafas oladi"
                startBreathing();
            }
        });
        drawAnimator.start();
        AndroidUtilities.runOnUIThread(readyPoll, 50);
    }

    private void onReady() {
        if (exiting || finishing) {
            return;
        }
        if (drawAnimator != null && drawAnimator.isRunning()) {
            // ilova chizish tugashidan oldin tayyor — qolgan qismini tez tugatamiz
            finishing = true;
            drawAnimator.removeAllListeners();
            drawAnimator.cancel();
            float from = t;
            ValueAnimator fast = ValueAnimator.ofFloat(from, DRAW_END);
            fast.setDuration(Math.max(60, Math.min(FINISH_FAST_MS, (long) (DRAW_END - from))));
            fast.setInterpolator(new LinearInterpolator());
            fast.addUpdateListener(a -> {
                t = (float) a.getAnimatedValue();
                invalidate();
            });
            fast.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    startExit();
                }
            });
            drawAnimator = fast;
            fast.start();
        } else {
            startExit();
        }
    }

    private void startBreathing() {
        breathAnimator = ValueAnimator.ofFloat(0f, 1f);
        breathAnimator.setDuration(BREATH_MS);
        breathAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breathAnimator.setInterpolator(new LinearInterpolator());
        breathAnimator.addUpdateListener(a -> {
            breath = (float) a.getAnimatedValue();
            invalidate();
        });
        breathAnimator.start();
    }

    private void startExit() {
        if (exiting) {
            return;
        }
        exiting = true;
        AndroidUtilities.cancelRunOnUIThread(readyPoll);
        t = DRAW_END;
        ValueAnimator out = ValueAnimator.ofFloat(0f, 1f);
        out.setDuration(EXIT_MS);
        out.setInterpolator(CubicBezierInterpolator.EASE_IN);
        out.addUpdateListener(a -> {
            exit = (float) a.getAnimatedValue();
            setAlpha(1f - exit);
            invalidate();
        });
        out.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                remove();
            }
        });
        out.start();
    }

    private void remove() {
        if (breathAnimator != null) {
            breathAnimator.cancel();
        }
        if (getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).removeView(this);
        }
        bmpA.recycle();
        bmpP.recycle();
        bmpFull.recycle();
    }

    private static float progress(float time, float start, float end) {
        if (time <= start) return 0f;
        if (time >= end) return 1f;
        float x = (time - start) / (end - start);
        // yumshoq boshlanib, yumshoq tugaydi
        return x * x * (3f - 2f * x);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (logoRect.isEmpty() || bmpA == null || bmpA.isRecycled()) {
            return;
        }
        final float size = logoRect.width();
        final float u = size / 100f;
        final float cx = logoRect.centerX(), cy = logoRect.centerY();

        float scale = 1f;
        if (breath >= 0f) {
            scale += 0.03f * (float) Math.sin(breath * Math.PI * 2);
        }
        if (exiting) {
            scale *= 1f + 0.12f * exit;
        }

        canvas.save();
        canvas.scale(scale, scale, cx, cy);

        // 1) logoning kvadrati
        float bg = progress(t, BG_START, BG_END);
        if (bg > 0f) {
            float s = 0.86f + 0.14f * bg;
            canvas.save();
            canvas.scale(s, s, cx, cy);
            fillPaint.setAlpha((int) (255 * bg));
            float r = size * 0.21f;
            canvas.drawRoundRect(logoRect, r, r, fillPaint);
            strokePaint.setColor(COLOR_RIM);
            strokePaint.setStrokeWidth(1.4f * u);
            strokePaint.setAlpha((int) (255 * bg));
            canvas.drawRoundRect(logoRect, r, r, strokePaint);
            canvas.restore();
        }

        // 2) "A" va samolyot — chiziq bo'ylab ochiladi
        canvas.save();
        canvas.clipPath(clipPath);
        drawLayer(canvas, bmpA, 0, u);
        drawLayer(canvas, bmpP, 1, u);
        // 3) oxirida to'liq logo (nur, soya bilan)
        float full = progress(t, FULL_START, FULL_END);
        if (full > 0f) {
            bitmapPaint.setAlpha((int) (255 * full));
            canvas.drawBitmap(bmpFull, null, logoRect, bitmapPaint);
        }
        canvas.restore();

        // qalam uchi
        if (!exiting) {
            for (int i = 0; i < SEGS.length; i++) {
                float p = progress(t, SEGS[i][0], SEGS[i][1]);
                if (p > 0f && p < 1f) {
                    measures[i].getPosTan(measures[i].getLength() * p, pos, null);
                    tipPaint.setAlpha(230);
                    canvas.drawCircle(pos[0], pos[1], 1.8f * u, tipPaint);
                }
            }
        }

        // 4) kutish paytida to'lqin halqa
        if (breath >= 0f && !exiting) {
            float rs = 1f + 0.28f * breath;
            float half = size / 2f * rs;
            tmpRect.set(cx - half, cy - half, cx + half, cy + half);
            strokePaint.setColor(COLOR_RING);
            strokePaint.setStrokeWidth(2f * u);
            strokePaint.setAlpha((int) (200 * (1f - breath)));
            float r = size * 0.21f * rs;
            canvas.drawRoundRect(tmpRect, r, r, strokePaint);
        }
        canvas.restore();
    }

    private void drawLayer(Canvas canvas, Bitmap bmp, int layer, float u) {
        boolean any = false;
        for (float[] seg : SEGS) {
            if ((int) seg[3] == layer && t > seg[0]) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        int sc = canvas.saveLayer(logoRect.left - 30 * u, logoRect.top - 30 * u, logoRect.right + 30 * u, logoRect.bottom + 30 * u, null);
        for (int i = 0; i < SEGS.length; i++) {
            if ((int) SEGS[i][3] != layer) continue;
            float p = progress(t, SEGS[i][0], SEGS[i][1]);
            if (p <= 0f) continue;
            maskPaint.setStrokeWidth(SEGS[i][2] * u);
            if (p >= 1f) {
                canvas.drawPath(scaledPaths[i], maskPaint);
            } else {
                segPath.reset();
                measures[i].getSegment(0, measures[i].getLength() * p, segPath, true);
                canvas.drawPath(segPath, maskPaint);
            }
        }
        canvas.drawBitmap(bmp, null, logoRect, srcInPaint);
        canvas.restoreToCount(sc);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        AndroidUtilities.cancelRunOnUIThread(readyPoll);
        if (drawAnimator != null) {
            drawAnimator.removeAllListeners();
            drawAnimator.cancel();
        }
        if (breathAnimator != null) {
            breathAnimator.cancel();
        }
    }
}
