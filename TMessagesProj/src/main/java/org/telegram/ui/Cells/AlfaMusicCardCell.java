package org.telegram.ui.Cells;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ProfileMusicView;

/**
 * AlfaGram — profilga qo'yilgan musiqa: katta karta (chapda album rasmi, o'ngda nom va ijrochi).
 */
public class AlfaMusicCardCell extends FrameLayout {

    private final ImageReceiver cover = new ImageReceiver(this);
    private final TextView titleView;
    private final TextView performerView;
    private final RectF cardRect = new RectF();
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable noteIcon;
    private boolean hasCover;

    private static final int CARD_H = 100;
    private static final int COVER = 76;

    public AlfaMusicCardCell(Context context) {
        super(context);
        setWillNotDraw(false);
        cover.setRoundRadius(dp(12));
        noteIcon = context.getResources().getDrawable(R.drawable.files_music).mutate();
        noteIcon.setColorFilter(new android.graphics.PorterDuffColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.SRC_IN));
        circlePaint.setColor(0x14FFFFFF);

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setMaxLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(titleView);

        performerView = new TextView(context);
        performerView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        performerView.setTextColor(0xFFB9CBE0);
        performerView.setMaxLines(1);
        performerView.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(performerView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));

        // karta: chapda 14 (chekka) + 12 (ichki) + COVER + 14 (oraliq)
        addView(texts, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 14 + 12 + COVER + 14, 0, 14 + 14, 0));
    }

    public void setMusic(TLRPC.Document document) {
        CharSequence title = ProfileMusicView.getTitle(document);
        CharSequence performer = ProfileMusicView.getAuthor(document);
        titleView.setText(TextUtils.isEmpty(title) ? LocaleController.getString(R.string.AudioUnknownTitle) : title);
        performerView.setText(TextUtils.isEmpty(performer) ? LocaleController.getString(R.string.AudioUnknownArtist) : performer);
        hasCover = false;
        try {
            if (document != null && document.thumbs != null && !document.thumbs.isEmpty()) {
                TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 320);
                if (thumb != null) {
                    cover.setImage(ImageLocation.getForDocument(thumb, document), "160_160", (Drawable) null, null, document, 0);
                    hasCover = true;
                }
            }
        } catch (Throwable ignore) {
            hasCover = false;
        }
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        cover.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cover.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(CARD_H + 16), MeasureSpec.EXACTLY)
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float l = dp(14), t = dp(8), r = getWidth() - dp(14), b = t + dp(CARD_H);
        cardRect.set(l, t, r, b);
        cardPaint.setShader(new LinearGradient(l, t, r, b, 0xFF2B5582, 0xFF1C3A5C, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(cardRect, dp(18), dp(18), cardPaint);

        // o'ng tomonda yengil bezak doiralar
        canvas.save();
        canvas.clipRect(cardRect);
        canvas.drawCircle(r - dp(20), t + dp(18), dp(40), circlePaint);
        canvas.drawCircle(r - dp(70), b - dp(6), dp(26), circlePaint);
        canvas.restore();

        final float cx = l + dp(12);
        final float cy = (t + b) / 2f - dp(COVER) / 2f;
        if (hasCover) {
            cover.setImageCoords(cx, cy, dp(COVER), dp(COVER));
            cover.draw(canvas);
        } else {
            // rasm bo'lmasa — rangli kvadrat va nota belgisi
            Paint ph = new Paint(Paint.ANTI_ALIAS_FLAG);
            ph.setColor(0x33FFFFFF);
            canvas.drawRoundRect(cx, cy, cx + dp(COVER), cy + dp(COVER), dp(12), dp(12), ph);
            int s = dp(30);
            int ix = (int) (cx + dp(COVER) / 2f - s / 2f);
            int iy = (int) (cy + dp(COVER) / 2f - s / 2f);
            noteIcon.setBounds(ix, iy, ix + s, iy + s);
            noteIcon.draw(canvas);
        }
    }
}
