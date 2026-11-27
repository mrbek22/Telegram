package org.telegram.ui;

import android.view.View;

import org.telegram.messenger.AlfaAccountsPrivacy;
import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * AlfaGram — «Maxfiy akkauntlar» (iOS bilan bir xil): qaysi akkaunt ro'yxatda ko'rinmasligi va
 * to'xtatilganlari; maxfiy akkauntga faqat shu yerdan kiriladi. Ochishdan oldin barmoq izi / PIN so'raladi.
 */
public class AlfaHiddenAccountsActivity extends UniversalFragment {

    private static final int HIDE_BASE = 1000;
    private static final int PAUSE_BASE = 2000;
    private static final int OPEN_BASE = 3000;

    private android.view.View alfaHero;

    @Override
    protected CharSequence getTitle() {
        return AlfaCyrillic.ui("Maxfiy akkauntlar");
    }

    private static String name(int account) {
        TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
        return user != null ? UserObject.getUserName(user) : "";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        ArrayList<Integer> accounts = AlfaAccountsActivity.getAccounts(-1, true);

        // tepada qimirlaydigan 🙈
        if (alfaHero == null && getContext() != null) {
            android.widget.FrameLayout hero = new android.widget.FrameLayout(getContext());
            android.view.View monkey = org.telegram.ui.Components.AlfaAnim.emoji(getContext(), "🙈", 90);
            hero.addView(monkey, org.telegram.ui.Components.LayoutHelper.createFrame(90, 90, android.view.Gravity.CENTER, 0, 14, 0, 8));
            org.telegram.ui.Components.AlfaAnim.popIn(monkey, 100);
            alfaHero = hero;
        }
        if (alfaHero != null) {
            UItem heroItem = UItem.asCustom(alfaHero);
            heroItem.intValue = org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT;
            items.add(heroItem);
        }

        items.add(UItem.asHeader(AlfaCyrillic.ui("Yashirish")));
        for (int account : accounts) {
            items.add(UItem.asCheck(HIDE_BASE + account, name(account)).setChecked(AlfaAccountsPrivacy.isHidden(account)));
        }
        items.add(UItem.asShadow(AlfaCyrillic.ui("Yoqilgan akkaunt «Hisoblar» ro'yxatida va akkaunt almashtirishda ko'rinmaydi, bildirishnomasi chiqmaydi. Unga faqat shu yerdan kirasiz.")));

        items.add(UItem.asHeader(AlfaCyrillic.ui("Vaqtincha to'xtatish")));
        for (int account : accounts) {
            items.add(UItem.asCheck(PAUSE_BASE + account, name(account)).setChecked(AlfaAccountsPrivacy.isPaused(account)));
        }
        items.add(UItem.asShadow(AlfaCyrillic.ui("To'xtatilgan akkauntdan qayta yoqilmaguncha hech qanday bildirishnoma kelmaydi.")));

        boolean anyHidden = false;
        for (int account : accounts) {
            if (AlfaAccountsPrivacy.isHidden(account) && account != currentAccount) {
                if (!anyHidden) {
                    items.add(UItem.asHeader(AlfaCyrillic.ui("Maxfiy akkauntga kirish")));
                    anyHidden = true;
                }
                items.add(UItem.asButton(OPEN_BASE + account, org.telegram.messenger.R.drawable.msg_openprofile, AlfaCyrillic.uiFormat("Kirish: %s", name(account))));
            }
        }
        if (anyHidden) {
            items.add(UItem.asShadow(null));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (id >= OPEN_BASE) {
            int account = id - OPEN_BASE;
            if (LaunchActivity.instance != null) {
                LaunchActivity.instance.switchToAccount(account, true);
            }
        } else if (id >= PAUSE_BASE) {
            int account = id - PAUSE_BASE;
            AlfaAccountsPrivacy.setPaused(account, !AlfaAccountsPrivacy.isPaused(account));
            listView.adapter.update(true);
        } else if (id >= HIDE_BASE) {
            int account = id - HIDE_BASE;
            AlfaAccountsPrivacy.setHidden(account, !AlfaAccountsPrivacy.isHidden(account));
            listView.adapter.update(true);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
