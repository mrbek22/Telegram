package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AlfaDeleted;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;

import java.io.File;
import java.util.ArrayList;

/**
 * AlfaGram — chat uchun o'chirilgan xabarlar ro'yxatini ko'rsatuvchi pastki panel.
 */
public class AlfaDeletedSheet extends BottomSheet {

    private final int currentAccount;
    private final long dialogId;
    private final Theme.ResourcesProvider resourcesProvider;

    private final ArrayList<AlfaDeleted.Record> items = new ArrayList<>();
    private RecyclerView listView;
    private Adapter adapter;
    private TextView emptyView;
    private TextView clearButton;

    public AlfaDeletedSheet(Context context, int currentAccount, long dialogId, Theme.ResourcesProvider resourcesProvider) {
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
        title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 18);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        title.setText(org.telegram.messenger.AlfaCyrillic.ui("O'chirilgan xabarlar"));
        header.addView(title, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL));

        clearButton = new TextView(context);
        clearButton.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 14);
        clearButton.setTypeface(AndroidUtilities.bold());
        clearButton.setTextColor(getThemedColor(Theme.key_text_RedRegular));
        clearButton.setText(org.telegram.messenger.AlfaCyrillic.ui("Tozalash"));
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
        emptyView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 15);
        emptyView.setTextColor(getThemedColor(Theme.key_dialogTextGray3));
        emptyView.setPadding(dp(24), dp(40), dp(24), dp(40));
        emptyView.setText(org.telegram.messenger.AlfaCyrillic.ui("Bu chatda hali o'chirilgan xabar yo'q.\nKimdir sizga yozib, keyin o'chirsa, xabar shu yerda saqlanadi."));
        content.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        root.addView(content, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f));

        setCustomView(root);
        reload();
    }

    private void reload() {
        items.clear();
        items.addAll(AlfaDeleted.getInstance().get(currentAccount, dialogId));
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
        b.setTitle(org.telegram.messenger.AlfaCyrillic.ui("Tozalash"));
        b.setMessage(org.telegram.messenger.AlfaCyrillic.ui("Ushbu chat uchun saqlangan barcha o'chirilgan xabarlar o'chirilsinmi? Buni qaytarib bo'lmaydi."));
        b.setPositiveButton(LocaleController.getString(R.string.Delete), (d, w) -> {
            AlfaDeleted.getInstance().clear(currentAccount, dialogId);
            reload();
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = b.show();
        TextView btn = (TextView) dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (btn != null) {
            btn.setTextColor(getThemedColor(Theme.key_text_RedBold));
        }
    }

    private String senderName(long fromId) {
        if (fromId == 0) {
            return org.telegram.messenger.AlfaCyrillic.ui("Noma'lum");
        }
        if (fromId > 0) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(fromId);
            if (user != null) {
                String name = ContactsController.formatName(user.first_name, user.last_name);
                if (!TextUtils.isEmpty(name)) {
                    return name;
                }
                if (!TextUtils.isEmpty(user.username)) {
                    return "@" + user.username;
                }
            }
            return org.telegram.messenger.AlfaCyrillic.ui("Foydalanuvchi ") + fromId;
        }
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-fromId);
        if (chat != null && !TextUtils.isEmpty(chat.title)) {
            return chat.title;
        }
        return "Chat " + (-fromId);
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

    /** Media turi nomi (o'chirilganlar ro'yxati uchun). */
    private static String mediaLabel(String type) {
        if (type == null) return null;
        switch (type) {
            case AlfaDeleted.MEDIA_PHOTO: return "📷 Rasm";
            case AlfaDeleted.MEDIA_VIDEO: return "🎬 Video";
            case AlfaDeleted.MEDIA_ROUND: return "⏺ Video xabar";
            case AlfaDeleted.MEDIA_VOICE: return "🎤 Ovozli xabar";
            case AlfaDeleted.MEDIA_MUSIC: return "🎵 Musiqa";
            case AlfaDeleted.MEDIA_GIF: return "GIF";
            case AlfaDeleted.MEDIA_STICKER: return "Stiker";
            case AlfaDeleted.MEDIA_FILE: return "📎 Fayl";
            default: return "[media]";
        }
    }

    private static File existingFile(String path) {
        if (path == null) return null;
        File f = new File(path);
        return f.exists() ? f : null;
    }

    private void openMedia(File file) {
        try {
            android.app.Activity activity = AndroidUtilities.findActivity(getContext());
            if (activity == null || !AndroidUtilities.openForView(file, file.getName(), null, activity, resourcesProvider, false)) {
                BulletinFactory.of(getContainer(), resourcesProvider).createSimpleBulletin(R.raw.error, org.telegram.messenger.AlfaCyrillic.ui("Faylni ochib bo'lmadi")).show();
            }
        } catch (Throwable e) {
            org.telegram.messenger.FileLog.e(e);
        }
    }

    private class Row extends LinearLayout {
        final TextView nameView;
        final TextView timeView;
        final TextView textView;
        final ImageView thumbView;
        String boundPath;

        Row(Context context) {
            super(context);
            setOrientation(VERTICAL);
            setPadding(dp(16), dp(10), dp(16), dp(10));
            setBackground(Theme.getSelectorDrawable(false));
            setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(HORIZONTAL);

            nameView = new TextView(context);
            nameView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 14);
            nameView.setTypeface(AndroidUtilities.bold());
            nameView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText));
            nameView.setSingleLine();
            nameView.setEllipsize(TextUtils.TruncateAt.END);
            topRow.addView(nameView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

            timeView = new TextView(context);
            timeView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 12);
            timeView.setTextColor(getThemedColor(Theme.key_dialogTextGray3));
            topRow.addView(timeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0, 0));

            addView(topRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            textView = new TextView(context);
            textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 15);
            textView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
            textView.setTypeface(Typeface.DEFAULT);
            textView.setLineSpacing(dp(2), 1f);
            addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

            thumbView = new ImageView(context);
            thumbView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbView.setClipToOutline(true);
            thumbView.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(10));
                }
            });
            thumbView.setVisibility(GONE);
            addView(thumbView, LayoutHelper.createLinear(160, 160, 0, 6, 0, 0));
        }

        /** Rasm/video eskizi fon oqimida yasaladi. */
        private void loadThumb(File file, boolean video) {
            final String path = file.getAbsolutePath();
            Utilities.globalQueue.postRunnable(() -> {
                Bitmap bmp = null;
                try {
                    if (video) {
                        bmp = android.media.ThumbnailUtils.createVideoThumbnail(path, android.provider.MediaStore.Images.Thumbnails.MINI_KIND);
                    } else {
                        BitmapFactory.Options o = new BitmapFactory.Options();
                        o.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(path, o);
                        int sample = 1;
                        while (o.outWidth / (sample * 2) >= dp(160) && o.outHeight / (sample * 2) >= dp(160)) {
                            sample *= 2;
                        }
                        o = new BitmapFactory.Options();
                        o.inSampleSize = sample;
                        bmp = BitmapFactory.decodeFile(path, o);
                    }
                } catch (Throwable ignore) {
                }
                final Bitmap result = bmp;
                AndroidUtilities.runOnUIThread(() -> {
                    if (result != null && path.equals(boundPath)) {
                        thumbView.setImageBitmap(result);
                        thumbView.setVisibility(VISIBLE);
                    }
                });
            });
        }

        void bind(AlfaDeleted.Record r) {
            nameView.setText(senderName(r.fromId));
            timeView.setText(LocaleController.formatDateChat(r.deletedAt / 1000L));
            final File file = existingFile(r.mediaPath);
            boundPath = file != null ? file.getAbsolutePath() : null;
            thumbView.setImageDrawable(null);
            thumbView.setVisibility(GONE);
            setOnClickListener(file != null ? v -> openMedia(file) : null);
            setClickable(file != null);
            final String label = mediaLabel(r.mediaType);
            if (label != null) {
                StringBuilder sb = new StringBuilder(org.telegram.messenger.AlfaCyrillic.ui(label));
                if (file == null) {
                    sb.append(org.telegram.messenger.AlfaCyrillic.ui(" (telefonga yuklanmagan edi)"));
                } else if (!AlfaDeleted.MEDIA_PHOTO.equals(r.mediaType) && !AlfaDeleted.MEDIA_VIDEO.equals(r.mediaType) && !AlfaDeleted.MEDIA_ROUND.equals(r.mediaType)) {
                    sb.append(org.telegram.messenger.AlfaCyrillic.ui(" — ochish uchun bosing"));
                }
                if (!TextUtils.isEmpty(r.text)) {
                    sb.append('\n').append(r.text);
                }
                textView.setText(sb);
                textView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
                textView.setTypeface(Typeface.DEFAULT);
                if (file != null && (AlfaDeleted.MEDIA_PHOTO.equals(r.mediaType) || AlfaDeleted.MEDIA_VIDEO.equals(r.mediaType) || AlfaDeleted.MEDIA_ROUND.equals(r.mediaType))) {
                    loadThumb(file, !AlfaDeleted.MEDIA_PHOTO.equals(r.mediaType));
                }
            } else if (TextUtils.isEmpty(r.text)) {
                textView.setText(org.telegram.messenger.AlfaCyrillic.ui("[media]"));
                textView.setTextColor(getThemedColor(Theme.key_dialogTextGray3));
                textView.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
            } else {
                textView.setText(r.text);
                textView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
                textView.setTypeface(Typeface.DEFAULT);
            }
        }
    }
}
