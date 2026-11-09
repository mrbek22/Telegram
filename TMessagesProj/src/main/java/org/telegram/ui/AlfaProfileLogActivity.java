package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.AlfaProfileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * AlfaGram — "Profil o'zgarishlari": kontaktlar username'i, rasmi, raqami yoki ismini qachon o'zgartirgani
 * (AlfaProfileLog). Kontaktlar → "Oxirgi qo'ng'iroqlar" ostidagi qatordan ochiladi.
 */
public class AlfaProfileLogActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int MENU_CLEAR = 1;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private LinearLayout emptyView;

    /** String — sana sarlavhasi, AlfaProfileLog.Entry — yozuv */
    private final ArrayList<Object> rows = new ArrayList<>();

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.alfaProfileLogUpdated);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.alfaProfileLogUpdated);
        super.onFragmentDestroy();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.alfaProfileLogUpdated) {
            reload();
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(AlfaCyrillic.ui("Profil o'zgarishlari"));
        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(MENU_CLEAR, R.drawable.msg_delete);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_CLEAR) {
                    confirmClear();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        fragmentView = frameLayout;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        adapter = new ListAdapter(context);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= rows.size()) return;
            Object row = rows.get(position);
            if (row instanceof AlfaProfileLog.Entry) {
                Bundle args = new Bundle();
                args.putLong("user_id", ((AlfaProfileLog.Entry) row).userId);
                presentFragment(new ProfileActivity(args));
            }
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // bo'sh holat: o'rdak + izoh
        emptyView = new LinearLayout(context);
        emptyView.setOrientation(LinearLayout.VERTICAL);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(AndroidUtilitiesDp(32), 0, AndroidUtilitiesDp(32), 0);
        RLottieImageView duck = new RLottieImageView(context);
        duck.setAutoRepeat(true);
        duck.setAnimation(R.raw.utyan_empty, 120, 120);
        duck.playAnimation();
        emptyView.addView(duck, LayoutHelper.createLinear(120, 120, Gravity.CENTER_HORIZONTAL));
        TextView title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        title.setTypeface(org.telegram.messenger.AndroidUtilities.bold());
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setText(AlfaCyrillic.ui("Hozircha o'zgarish yo'q"));
        emptyView.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 0));
        TextView text = new TextView(context);
        text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        text.setGravity(Gravity.CENTER);
        text.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        text.setText(AlfaCyrillic.ui("Kontaktlaringiz yoki yozishgan odamlaringiz username'ini, profil rasmini yoki raqamini o'zgartirsa, shu yerda ko'rinadi. Ma'lumot faqat telefoningizda saqlanadi."));
        emptyView.addView(text, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));
        emptyView.setVisibility(View.GONE);
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        reload();
        return fragmentView;
    }

    private static int AndroidUtilitiesDp(float v) {
        return org.telegram.messenger.AndroidUtilities.dp(v);
    }

    private void reload() {
        AlfaProfileLog.load(currentAccount, 0, entries -> {
            rows.clear();
            String lastHeader = null;
            for (AlfaProfileLog.Entry e : entries) {
                String header = dayHeader(e.time);
                if (!header.equals(lastHeader)) {
                    rows.add(header);
                    lastHeader = header;
                }
                rows.add(e);
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            if (emptyView != null) {
                emptyView.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private static String dayHeader(long timeMs) {
        Calendar now = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeMs);
        if (now.get(Calendar.YEAR) == c.get(Calendar.YEAR)) {
            int diff = now.get(Calendar.DAY_OF_YEAR) - c.get(Calendar.DAY_OF_YEAR);
            if (diff == 0) return AlfaCyrillic.ui("Bugun");
            if (diff == 1) return AlfaCyrillic.ui("Kecha");
        }
        return LocaleController.formatDateChat(timeMs / 1000, true);
    }

    private void confirmClear() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        b.setTitle(AlfaCyrillic.ui("Tarixni tozalash"));
        b.setMessage(AlfaCyrillic.ui("Barcha yozuvlar o'chiriladi. Yangi o'zgarishlar yozilishda davom etadi."));
        b.setPositiveButton(AlfaCyrillic.ui("Tozalash"), (d, w) -> AlfaProfileLog.clear(currentAccount, this::reload));
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = b.create();
        showDialog(dialog);
        View btn = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (btn instanceof TextView) {
            ((TextView) btn).setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ENTRY = 1;
        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position) instanceof String ? TYPE_HEADER : TYPE_ENTRY;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == TYPE_ENTRY;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_HEADER) {
                view = new GraySectionCell(context);
            } else {
                view = new UserCell(context, 6, 0, false);
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Object row = rows.get(position);
            if (holder.getItemViewType() == TYPE_HEADER) {
                ((GraySectionCell) holder.itemView).setText((String) row);
            } else {
                AlfaProfileLog.Entry e = (AlfaProfileLog.Entry) row;
                TLRPC.User user = getMessagesController().getUser(e.userId);
                if (user == null) {
                    user = new TLRPC.TL_user();
                    user.id = e.userId;
                    user.first_name = "ID " + e.userId;
                }
                String time = LocaleController.getInstance().getFormatterDay().format(e.time);
                CharSequence name = UserObject.getUserName(user);
                CharSequence status = time + " · " + AlfaProfileLog.describe(e);
                boolean divider = position + 1 < rows.size() && rows.get(position + 1) instanceof AlfaProfileLog.Entry;
                ((UserCell) holder.itemView).setData(user, name, status, 0, divider);
            }
        }
    }
}
