package org.telegram.ui.Components;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * AlfaGram — «Chatni eksport qilish» (iOS bilan bir xil): oxirgi 5000 tagacha xabar serverdan sahifalab
 * (100 tadan) olinadi va HTML sahifa yoki TXT fayl bo'lib ulashish oynasiga beriladi.
 */
public final class AlfaChatExport {

    private static final int LIMIT = 5000;

    private static final class Item {
        int date;
        String author;
        boolean out;
        String text;
        String media;
    }

    private AlfaChatExport() {
    }

    /** Format so'raladi (HTML / TXT), keyin eksport boshlanadi. */
    public static void show(BaseFragment fragment, long dialogId, String title) {
        Activity activity = fragment.getParentActivity();
        if (activity == null) {
            return;
        }
        CharSequence[] items = new CharSequence[]{AlfaCyrillic.ui("HTML sahifa"), AlfaCyrillic.ui("Matn fayl (TXT)")};
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, fragment.getResourceProvider());
        builder.setTitle(AlfaCyrillic.ui("Chatni eksport qilish"));
        builder.setMessage(AlfaCyrillic.ui("Oxirgi 5000 tagacha xabar. Formatni tanlang."));
        builder.setItems(items, (dialog, which) -> start(fragment, dialogId, title, which == 0));
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        fragment.showDialog(builder.create());
    }

    private static void start(BaseFragment fragment, long dialogId, String title, boolean html) {
        Activity activity = fragment.getParentActivity();
        if (activity == null) {
            return;
        }
        int account = fragment.getCurrentAccount();
        TLRPC.InputPeer peer = MessagesController.getInstance(account).getInputPeer(dialogId);
        if (peer == null) {
            return;
        }
        AlertDialog progress = new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER);
        progress.setCanCancel(false);
        progress.show();
        loadPage(account, peer, dialogId, 0, new ArrayList<>(), items -> {
            try {
                progress.dismiss();
            } catch (Throwable ignore) {
            }
            if (items.isEmpty()) {
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.info, AlfaCyrillic.ui("Eksport qilish uchun xabar topilmadi.")).show();
                return;
            }
            Collections.reverse(items);
            share(activity, title, html ? html(title, items) : text(title, items), html);
        });
    }

    private interface Done {
        void run(ArrayList<Item> items);
    }

    private static void loadPage(int account, TLRPC.InputPeer peer, long dialogId, int offsetId, ArrayList<Item> collected, Done done) {
        TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
        req.peer = peer;
        req.offset_id = offsetId;
        req.limit = 100;
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (!(response instanceof TLRPC.messages_Messages)) {
                done.run(collected);
                return;
            }
            TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
            HashMap<Long, TLRPC.User> users = new HashMap<>();
            for (TLRPC.User user : res.users) {
                users.put(user.id, user);
            }
            HashMap<Long, TLRPC.Chat> chats = new HashMap<>();
            for (TLRPC.Chat chat : res.chats) {
                chats.put(chat.id, chat);
            }
            int oldest = 0;
            for (TLRPC.Message message : res.messages) {
                if (message == null || message instanceof TLRPC.TL_messageEmpty) {
                    continue;
                }
                oldest = message.id;
                Item item = new Item();
                item.date = message.date;
                item.out = message.out;
                item.text = message.message != null ? message.message : "";
                item.author = authorName(account, message, dialogId, users, chats);
                item.media = mediaLabel(message);
                collected.add(item);
            }
            if (res.messages.isEmpty() || oldest == 0 || collected.size() >= LIMIT) {
                done.run(collected);
            } else {
                loadPage(account, peer, dialogId, oldest, collected, done);
            }
        }));
    }

    private static String authorName(int account, TLRPC.Message message, long dialogId, HashMap<Long, TLRPC.User> users, HashMap<Long, TLRPC.Chat> chats) {
        TLRPC.Peer from = message.from_id != null ? message.from_id : message.peer_id;
        if (from instanceof TLRPC.TL_peerUser) {
            TLRPC.User user = users.get(from.user_id);
            if (user == null) {
                user = MessagesController.getInstance(account).getUser(from.user_id);
            }
            return user != null ? UserObject.getUserName(user) : "";
        }
        long chatId = from != null ? (from.channel_id != 0 ? from.channel_id : from.chat_id) : 0;
        TLRPC.Chat chat = chats.get(chatId);
        if (chat == null && chatId != 0) {
            chat = MessagesController.getInstance(account).getChat(chatId);
        }
        return chat != null && chat.title != null ? chat.title : "";
    }

    private static String mediaLabel(TLRPC.Message message) {
        if (message instanceof TLRPC.TL_messageService) {
            return AlfaCyrillic.ui("[Xizmat xabari]");
        }
        if (message.media == null || message.media instanceof TLRPC.TL_messageMediaEmpty || message.media instanceof TLRPC.TL_messageMediaWebPage) {
            return null;
        }
        if (message.media instanceof TLRPC.TL_messageMediaPhoto) {
            return AlfaCyrillic.ui("[Rasm]");
        }
        TLRPC.Document document = message.media.document;
        if (document != null) {
            if (MessageObject.isVoiceDocument(document)) {
                return AlfaCyrillic.ui("[Ovozli xabar]");
            } else if (MessageObject.isRoundVideoDocument(document)) {
                return AlfaCyrillic.ui("[Dumaloq video]");
            } else if (MessageObject.isStickerDocument(document) || MessageObject.isAnimatedStickerDocument(document, true)) {
                return AlfaCyrillic.ui("[Stiker]");
            } else if (MessageObject.isGifDocument(document)) {
                return "[GIF]";
            } else if (MessageObject.isVideoDocument(document)) {
                return AlfaCyrillic.ui("[Video]");
            }
            String name = "";
            for (TLRPC.DocumentAttribute attribute : document.attributes) {
                if (attribute instanceof TLRPC.TL_documentAttributeFilename) {
                    name = attribute.file_name;
                }
            }
            return AlfaCyrillic.ui("[Fayl]") + (name.isEmpty() ? "" : " " + name);
        }
        return AlfaCyrillic.ui("[Media]");
    }

    private static String date(int timestamp) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(new Date(timestamp * 1000L));
    }

    private static String text(String title, ArrayList<Item> items) {
        StringBuilder b = new StringBuilder();
        b.append(title).append('\n');
        b.append(AlfaCyrillic.ui("AlfaGram eksporti")).append(" — ").append(date((int) (System.currentTimeMillis() / 1000))).append("\n\n");
        for (Item item : items) {
            String body = item.text;
            if (item.media != null) {
                body = item.media + (body.isEmpty() ? "" : " " + body);
            }
            b.append('[').append(date(item.date)).append("] ").append(item.author).append(": ").append(body).append('\n');
        }
        return b.toString();
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("\n", "<br>");
    }

    private static String html(String title, ArrayList<Item> items) {
        StringBuilder h = new StringBuilder();
        h.append("<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        h.append("<title>").append(esc(title)).append("</title><style>");
        h.append("body{font-family:-apple-system,Roboto,Helvetica,Arial,sans-serif;background:#eef1f5;margin:0;padding:16px;color:#111}");
        h.append("h1{font-size:20px;margin:0 0 4px}.sub{color:#777;font-size:13px;margin-bottom:16px}");
        h.append(".m{max-width:75%;margin:6px 0;padding:8px 12px;border-radius:14px;background:#fff;box-shadow:0 1px 1px rgba(0,0,0,.08)}");
        h.append(".o{margin-left:auto;background:#dcf3ff}.a{font-weight:600;font-size:13px;color:#2563eb}.t{font-size:15px;line-height:1.35;word-wrap:break-word}");
        h.append(".d{font-size:11px;color:#999;text-align:right;margin-top:2px}.md{font-style:italic;color:#555}");
        h.append("@media (prefers-color-scheme:dark){body{background:#0e1621;color:#eee}.m{background:#182533}.o{background:#2b5278}.sub,.d{color:#8a99a8}.md{color:#aab}}");
        h.append("</style></head><body>");
        h.append("<h1>").append(esc(title)).append("</h1>");
        h.append("<div class=\"sub\">").append(esc(AlfaCyrillic.ui("AlfaGram eksporti"))).append(" — ").append(date((int) (System.currentTimeMillis() / 1000))).append(" · ").append(items.size()).append("</div>");
        for (Item item : items) {
            h.append("<div class=\"m").append(item.out ? " o" : "").append("\">");
            h.append("<div class=\"a\">").append(esc(item.author)).append("</div>");
            if (item.media != null) {
                h.append("<div class=\"t md\">").append(esc(item.media)).append("</div>");
            }
            if (!item.text.isEmpty()) {
                h.append("<div class=\"t\">").append(esc(item.text)).append("</div>");
            }
            h.append("<div class=\"d\">").append(date(item.date)).append("</div></div>");
        }
        h.append("</body></html>");
        return h.toString();
    }

    private static void share(Activity activity, String title, String content, boolean html) {
        try {
            File dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "cache");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String safe = title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
            if (safe.isEmpty()) {
                safe = "chat";
            }
            File file = new File(dir, safe + (html ? ".html" : ".txt"));
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(content.getBytes(StandardCharsets.UTF_8));
            stream.close();
            Uri uri = FileProvider.getUriForFile(activity, ApplicationLoader.getApplicationId() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(html ? "text/html" : "text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(intent, AlfaCyrillic.ui("Chatni eksport qilish")));
        } catch (Throwable ignore) {
        }
    }
}
