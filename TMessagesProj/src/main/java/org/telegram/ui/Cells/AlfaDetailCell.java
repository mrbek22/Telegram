package org.telegram.ui.Cells;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * AlfaGram — profildagi maxsus qator: chapda rangli yumaloq ikonka, o'ngda qiymat + izoh.
 * Faqat AlfaGram qatorlari (ID, havola, onlayn, ro'yxatdan o'tgan) uchun ishlatiladi.
 */
public class AlfaDetailCell extends FrameLayout {

    private final FrameLayout iconWrap;
    private final ImageView iconView;
    private final TextView valueView;
    private final TextView labelView;
    private final TextView actionView;
    private final GradientDrawable iconBg = new GradientDrawable();
    private final Theme.ResourcesProvider rp;
    private boolean needDivider;

    public AlfaDetailCell(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.rp = resourcesProvider;
        setWillNotDraw(false);

        iconWrap = new FrameLayout(context);
        iconBg.setCornerRadius(dp(10));
        iconWrap.setBackground(iconBg);
        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.CENTER);
        iconWrap.addView(iconView, LayoutHelper.createFrame(20, 20, Gravity.CENTER));
        addView(iconWrap, LayoutHelper.createFrame(34, 34, Gravity.LEFT | Gravity.CENTER_VERTICAL, 16, 0, 0, 0));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        valueView = new TextView(context);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        valueView.setTypeface(AndroidUtilities.bold());
        valueView.setMaxLines(1);
        valueView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(valueView);
        labelView = new TextView(context);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        labelView.setMaxLines(1);
        labelView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(labelView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        addView(texts, LayoutHelper.createFrame(0, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 64, 0, 76, 0));

        actionView = new TextView(context);
        actionView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        actionView.setTypeface(AndroidUtilities.bold());
        actionView.setVisibility(GONE);
        addView(actionView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

        updateColors();
    }

    public void updateColors() {
        valueView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, rp));
        labelView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, rp));
        actionView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText, rp));
    }

    public void set(int icon, int color, CharSequence value, CharSequence label, CharSequence action, boolean divider) {
        iconBg.setColor(color);
        if (icon != 0) {
            Drawable d = getContext().getResources().getDrawable(icon).mutate();
            d.setColorFilter(new PorterDuffColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN));
            iconView.setImageDrawable(d);
            iconView.setVisibility(VISIBLE);
        } else {
            iconView.setVisibility(GONE);
        }
        valueView.setText(value);
        labelView.setText(label);
        if (action != null) {
            actionView.setText(action);
            actionView.setVisibility(VISIBLE);
        } else {
            actionView.setVisibility(GONE);
        }
        needDivider = divider;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(60) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY)
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(dp(64), getHeight() - 1, getWidth(), getHeight() - 1, Theme.dividerPaint);
        }
    }
}
