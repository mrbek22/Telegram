package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AlfaTyping;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

/**
 * AlfaGram — chat uchun "yozdi, lekin yubormadi" tarixini ko'rsatuvchi pastki panel.
 */
public class AlfaTypingSheet extends BottomSheet {

    private final int currentAccount;
    private final long dialogId;
    private final Theme.ResourcesProvider resourcesProvider;

    private final ArrayList<AlfaTyping.Record> items = new ArrayList<>();
    private RecyclerView listView;
    private Adapter adapter;
    private TextView emptyView;
    private TextView clearButton;

    public AlfaTypingSheet(Context context, int currentAccount, long dialogId, Theme.ResourcesProvider resourcesProvider) {
        super(context, false, resourcesProvider);
        this.currentAccount = currentAccount;
        this.dialogId = dialogId;
        this.resourcesProvider = resourcesProvider;

        fixNavigationBar(getThemedColor(Theme.key_dialogBackground));
        setApplyBottomPadding(false);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        FrameLayout header = new FrameLayout(context);
        header.setPadding(dp(16), dp(14), dp(16), dp(10));

        TextView title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        title.setText("Yozdi, yubormadi");
        header.addView(title, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL));

        clearButton = new TextView(context);
        clearButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        clearButton.setTypeface(AndroidUtilities.bold());
        clearButton.setTextColor(getThemedColor(Theme.key_text_RedRegular));
        clearButton.setText("Tozalash");
        clearButton.setPadding(dp(10), dp(6), dp(10), dp(6));
        clearButton.setBackground(Theme.createRadSelectorDrawable(getThemedColor(Theme.key_listSelector), 6, 6));
        clearButton.setOnClickListener(v -> showClearConfirm());
        header.addView(clearButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL));

        root.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        FrameLayout content = new FrameLayout(context);

        listView = new RecyclerView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setClipToPadding(false);
        listView.setPadding(0, 0, 0, dp(12));
        adapter = new Adapter();
        listView.setAdapter(adapter);
        content.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyView = new TextView(context);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        emptyView.setTextColor(getThemedColor(Theme.key_dialogTextGray3));
        emptyView.setPadding(dp(24), dp(40), dp(24), dp(40));
        emptyView.setText("Hozircha hech kim yozib turib to'xtatmagan.\nKimdir yozib, xabar yubormasdan to'xtatsa, shu yerda ko'rinadi.");
        content.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        root.addView(content, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f));

        setCustomView(root);
        reload();
    }

    private void reload() {
        items.clear();
        items.addAll(AlfaTyping.getInstance().getUnsent(currentAccount, dialogId));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        boolean empty = items.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        listView.setVisibility(empty ? View.GONE : View.VISIBLE);
        clearButton.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showClearConfirm() {
        AlertDialog.Builder b = new AlertDialog.Builder(getContext(), resourcesProvider);
        b.setTitle("Tozalash");
        b.setMessage("Ushbu chatning \"yozdi, yubormadi\" jurnali o'chirilsinmi?");
        b.setPositiveButton(LocaleController.getString(R.string.Delete), (d, w) -> {
            AlfaTyping.getInstance().clear(currentAccount, dialogId);
            reload();
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = b.show();
        TextView btn = (TextView) dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (btn != null) {
            btn.setTextColor(getThemedColor(Theme.key_text_RedBold));
        }
    }

    private String senderName(long userId) {
        if (userId == 0) {
            return "Noma'lum";
        }
        if (userId > 0) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
            if (user != null) {
                String name = ContactsController.formatName(user.first_name, user.last_name);
                if (!TextUtils.isEmpty(name)) {
                    return name;
                }
                if (!TextUtils.isEmpty(user.username)) {
                    return "@" + user.username;
                }
            }
            return "Foydalanuvchi " + userId;
        }
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-userId);
        if (chat != null && !TextUtils.isEmpty(chat.title)) {
            return chat.title;
        }
        return "Chat " + (-userId);
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new Holder(new Row(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((Row) holder.itemView).bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class Holder extends RecyclerView.ViewHolder {
        Holder(View v) {
            super(v);
        }
    }

    private class Row extends LinearLayout {
        final TextView nameView;
        final TextView timeView;
        final TextView infoView;

        Row(Context context) {
            super(context);
            setOrientation(VERTICAL);
            setPadding(dp(16), dp(10), dp(16), dp(10));
            setBackground(Theme.getSelectorDrawable(false));
            setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(HORIZONTAL);

            nameView = new TextView(context);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            nameView.setTypeface(AndroidUtilities.bold());
            nameView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText));
            nameView.setSingleLine();
            nameView.setEllipsize(TextUtils.TruncateAt.END);
            topRow.addView(nameView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

            timeView = new TextView(context);
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            timeView.setTextColor(getThemedColor(Theme.key_dialogTextGray3));
            topRow.addView(timeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0, 0));

            addView(topRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            infoView = new TextView(context);
            infoView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            infoView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
            addView(infoView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        }

        void bind(AlfaTyping.Record r) {
            nameView.setText(senderName(r.userId));
            timeView.setText(LocaleController.formatDateChat(r.startedAt / 1000L));
            long durSec = Math.max(1, (r.endedAt - r.startedAt) / 1000L);
            infoView.setText("~" + durSec + " sekund yozdi, lekin xabar yubormadi");
        }
    }
}
