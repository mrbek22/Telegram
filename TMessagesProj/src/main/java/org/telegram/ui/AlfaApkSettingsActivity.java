package org.telegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.telegram.messenger.AlfaApkGuard;
import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.AlfaFeatures;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SectionsScrollView;

/**
 * AlfaGram — "APK himoyasi" ekrani (Maxfiylik va xavfsizlik → APK himoyasi). Tuzilishi Telegram'ning
 * "Xabarlarni avtomatik o'chirish" ekrani kabi: tepada harakatlanuvchi o'rdak, ostida yoqish/o'chirish.
 * APK va havola himoyasi sozlamasi faqat shu yerda (AlfaGram sozlamalarida yo'q).
 */
public class AlfaApkSettingsActivity extends BaseFragment {

    private static final String KEY_APK = "apkGuard";
    private static final String KEY_LINK = "linkGuard";

    private TextCheckCell apkCell;
    private TextCheckCell linkCell;
    private RLottieImageView duckView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(AlfaCyrillic.ui("APK himoyasi"));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        LinearLayout mainContainer = new SectionsScrollView.SectionsLinearLayout(context);
        SectionsScrollView scrollView = new SectionsScrollView(context, mainContainer, resourceProvider);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(mainContainer);
        frameLayout.addView(scrollView);
        actionBar.setAdaptiveBackground(scrollView);

        // Tepada: politsiyachi o'rdak (to'xtovsiz harakatlanadi, bosilsa boshidan)
        FrameLayout stickerHeaderCell = new FrameLayout(context);
        duckView = new RLottieImageView(context);
        duckView.setAutoRepeat(true);
        duckView.setAnimation(R.raw.report_police, 130, 130);
        duckView.playAnimation();
        stickerHeaderCell.addView(duckView, LayoutHelper.createFrame(130, 130, Gravity.CENTER));
        stickerHeaderCell.setTag(RecyclerListView.TAG_NOT_SECTION);
        mainContainer.addView(stickerHeaderCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 170));

        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        mainContainer.addView(box, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        HeaderCell headerCell = new HeaderCell(context);
        headerCell.setText(AlfaCyrillic.ui("Himoya"));
        box.addView(headerCell);

        apkCell = new TextCheckCell(context);
        apkCell.setBackground(Theme.createSelectorWithBackgroundDrawable(Theme.getColor(Theme.key_windowBackgroundWhite), Theme.getColor(Theme.key_listSelector)));
        apkCell.setTextAndCheck(AlfaCyrillic.ui("APK o'rnatishni bloklash"), AlfaFeatures.getUserPref(KEY_APK), true);
        apkCell.setOnClickListener(v -> toggleApk());
        box.addView(apkCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        linkCell = new TextCheckCell(context);
        linkCell.setBackground(Theme.createSelectorWithBackgroundDrawable(Theme.getColor(Theme.key_windowBackgroundWhite), Theme.getColor(Theme.key_listSelector)));
        linkCell.setTextAndCheck(AlfaCyrillic.ui("Shubhali havolalardan ogohlantirish"), AlfaFeatures.getUserPref(KEY_LINK), false);
        linkCell.setOnClickListener(v -> toggleLink());
        box.addView(linkCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextInfoPrivacyCell infoCell = new TextInfoPrivacyCell(context, 12, resourceProvider);
        infoCell.setText(AlfaCyrillic.ui("Chatlardan kelgan dastur (APK) fayllar o'rnatilmaydi va avtomatik yuklanmaydi. Ochmoqchi bo'lsangiz, AlfaGram faylni tekshirib, xavfini ko'rsatadi.\n\n"
                + "Guruhingiz uchun APK filtri: guruhga kiring → nomini bosing → ✏️ Tahrirlash (admin bo'lsangiz).\n\n"
                + "Hammasi faqat telefoningizda ishlaydi, hech qayerga yuborilmaydi."));
        mainContainer.addView(infoCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        return fragmentView;
    }

    private boolean remoteBlocked(String key) {
        if (AlfaFeatures.isRemoteAllowed(key)) {
            return false;
        }
        try {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, AlfaCyrillic.ui("Bu funksiya vaqtincha o'chirilgan")).show();
        } catch (Throwable ignore) {
        }
        return true;
    }

    private void toggleApk() {
        if (remoteBlocked(KEY_APK)) {
            return;
        }
        if (AlfaFeatures.getUserPref(KEY_APK)) {
            AlfaApkGuard.confirmDisable(this, () -> {
                AlfaFeatures.setFlag(KEY_APK, false);
                updateCells();
            });
        } else {
            AlfaFeatures.setFlag(KEY_APK, true);
            updateCells();
            if (duckView != null) {
                duckView.playAnimation();
            }
        }
    }

    private void toggleLink() {
        if (remoteBlocked(KEY_LINK)) {
            return;
        }
        AlfaFeatures.setFlag(KEY_LINK, !AlfaFeatures.getUserPref(KEY_LINK));
        updateCells();
    }

    private void updateCells() {
        if (apkCell != null) {
            apkCell.setChecked(AlfaFeatures.getUserPref(KEY_APK));
        }
        if (linkCell != null) {
            linkCell.setChecked(AlfaFeatures.getUserPref(KEY_LINK));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCells();
        if (duckView != null) {
            duckView.playAnimation();
        }
    }
}
