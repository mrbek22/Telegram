package org.telegram.ui;

import android.view.View;

import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.IconBackgroundColors;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.Collections;

/**
 * AlfaGram — "Akkauntlarni boshqarish": barcha kirilgan akkauntlar ro'yxati.
 * Sozlamalardagi "Hisoblar" bo'limida faqat SETTINGS_LIMIT ta akkaunt ko'rinadi, qolganlari shu yerda.
 */
public class AlfaAccountsActivity extends UniversalFragment {

    public static final int SETTINGS_LIMIT = 3;

    private static final int BUTTON_ADD_ACCOUNT = 1;

    @Override
    protected CharSequence getTitle() {
        return AlfaCyrillic.ui("Akkauntlarni boshqarish");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(false);
        }
    }

    /** Kirilgan akkauntlar, kirish vaqti bo'yicha (joriy akkaunt ham). */
    public static ArrayList<Integer> getAccounts(int exclude) {
        ArrayList<Integer> accounts = new ArrayList<>();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated() && a != exclude) {
                accounts.add(a);
            }
        }
        Collections.sort(accounts, (o1, o2) -> Long.compare(UserConfig.getInstance(o1).loginTime, UserConfig.getInstance(o2).loginTime));
        return accounts;
    }

    /** Bo'sh akkaunt o'rniga kirish oynasini ochadi. */
    public static void addAccount(BaseFragment fragment) {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!UserConfig.getInstance(a).isClientActivated()) {
                fragment.presentFragment(new LoginActivity(a));
                return;
            }
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        ArrayList<Integer> others = getAccounts(currentAccount);

        items.add(UItem.asHeader(AlfaCyrillic.ui("Joriy akkaunt")));
        items.add(SettingsActivity.AccountCell.Factory.of(0, currentAccount));
        items.add(UItem.asShadow(null));

        if (!others.isEmpty()) {
            items.add(UItem.asHeader(AlfaCyrillic.ui("Boshqa akkauntlar") + " (" + others.size() + ")"));
            for (int i = 0; i < others.size(); i++) {
                items.add(SettingsActivity.AccountCell.Factory.of(i + 1, others.get(i)));
            }
            items.add(UItem.asShadow(null));
        }

        if (UserConfig.getActivatedAccountsCount() < UserConfig.MAX_ACCOUNT_COUNT) {
            items.add(SettingsActivity.SettingCell.Factory.of(BUTTON_ADD_ACCOUNT, IconBackgroundColors.GREEN.top, IconBackgroundColors.GREEN.bottom, org.telegram.messenger.R.drawable.settings_invite, AlfaCyrillic.ui("Akkaunt qo'shish")));
        }
        items.add(UItem.asShadow(AlfaCyrillic.ui("AlfaGram'da " + UserConfig.MAX_ACCOUNT_COUNT + " tagacha akkaunt qo'shish mumkin.")));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.instanceOf(SettingsActivity.AccountCell.Factory.class)) {
            final int account = item.intValue;
            if (account == currentAccount) {
                finishFragment();
            } else if (LaunchActivity.instance != null) {
                LaunchActivity.instance.switchToAccount(account, true);
            }
        } else if (item.instanceOf(SettingsActivity.SettingCell.Factory.class) && item.id == BUTTON_ADD_ACCOUNT) {
            addAccount(this);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
