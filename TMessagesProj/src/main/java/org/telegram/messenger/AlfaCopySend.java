package org.telegram.messenger;

import android.text.TextUtils;
import android.widget.Toast;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * AlfaGram — himoyalangan (uzatish taqiqlangan) chat/kanaldan "forward".
 * Server bunday xabarlarni forward qilishni rad etadi (CHAT_FORWARDS_RESTRICTED),
 * shuning uchun media avval yuklab olinadi va galereyadan yuborgandek yangi xabar
 * sifatida jo'natiladi ("Forwarded from" ko'rinmaydi).
 * Barcha holat faqat UI oqimida o'zgaradi.
 */
public class AlfaCopySend implements NotificationCenter.NotificationCenterDelegate {

    private static final int KIND_TEXT = 0;
    private static final int KIND_PHOTO = 1;
    private static final int KIND_VIDEO = 2;
    private static final int KIND_DOCUMENT = 3;
    private static final int KIND_UNSUPPORTED = 4;

    private static volatile AlfaCopySend instance;

    public static AlfaCopySend getInstance() {
        if (instance == null) {
            synchronized (AlfaCopySend.class) {
                if (instance == null) {
                    instance = new AlfaCopySend();
                }
            }
        }
        return instance;
    }

    private static class Item {
        MessageObject obj;
        int kind;
        String fileName;
        String path;
        boolean failed;
        int retries;
    }

    // chatdan chiqilganda ChatMessageCell/DownloadController yuklashni bekor qiladi — qayta boshlaymiz
    private static final int MAX_RETRIES = 5;

    private static class Job {
        int account;
        long peer;
        boolean hideCaption;
        boolean notify;
        int scheduleDate;
        int scheduleRepeatPeriod;
        MessageObject replyToTopMsg;
        long payStars;
        long monoForumPeerId;
        final ArrayList<Item> items = new ArrayList<>();
        int waiting;
    }

    // fayl nomi -> yuklanishini kutayotgan elementlar (faqat UI oqimida)
    private final HashMap<String, ArrayList<Object[]>> pending = new HashMap<>();
    private final boolean[] observing = new boolean[UserConfig.MAX_ACCOUNT_COUNT];

    /** Xabar himoyalangan chatdan bo'lsa va unrestrictedSave yoqiq bo'lsa — nusxa sifatida yuboriladi. */
    public static boolean isProtected(int account, MessageObject obj) {
        if (!AlfaFeatures.unrestrictedSave || obj == null || obj.messageOwner == null || obj.getId() <= 0) {
            return false;
        }
        if (obj.messageOwner.noforwards) {
            return true;
        }
        return MessagesController.getInstance(account).isPeerNoForwardsRaw(obj.getDialogId());
    }

    public static boolean hasProtected(int account, ArrayList<MessageObject> messages) {
        if (!AlfaFeatures.unrestrictedSave || messages == null) {
            return false;
        }
        for (int a = 0; a < messages.size(); a++) {
            if (isProtected(account, messages.get(a))) {
                return true;
            }
        }
        return false;
    }

    /**
     * SendMessagesHelper.sendMessage(ArrayList...) dan chaqiriladi.
     * Himoyalangan xabarlarni nusxa sifatida yuboradi, qolganlarini (oddiy forward uchun) qaytaradi.
     */
    public static ArrayList<MessageObject> sendProtected(int account, ArrayList<MessageObject> messages, long peer, boolean hideCaption, boolean notify, int scheduleDate, int scheduleRepeatPeriod, MessageObject replyToTopMsg, long payStars, long monoForumPeerId) {
        ArrayList<MessageObject> rest = new ArrayList<>();
        Job job = new Job();
        job.account = account;
        job.peer = peer;
        job.hideCaption = hideCaption;
        job.notify = notify;
        job.scheduleDate = scheduleDate;
        job.scheduleRepeatPeriod = scheduleRepeatPeriod;
        job.replyToTopMsg = replyToTopMsg;
        job.payStars = payStars;
        job.monoForumPeerId = monoForumPeerId;
        for (int a = 0; a < messages.size(); a++) {
            MessageObject obj = messages.get(a);
            if (!isProtected(account, obj)) {
                rest.add(obj);
                continue;
            }
            Item item = new Item();
            item.obj = obj;
            item.kind = detectKind(obj);
            job.items.add(item);
        }
        if (!job.items.isEmpty()) {
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    getInstance().start(job);
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            });
        }
        return rest;
    }

    private static int detectKind(MessageObject obj) {
        TLRPC.MessageMedia media = MessageObject.getMedia(obj.messageOwner);
        if (media == null || media instanceof TLRPC.TL_messageMediaEmpty || media instanceof TLRPC.TL_messageMediaWebPage) {
            return KIND_TEXT;
        }
        if (media.ttl_seconds != 0) {
            return KIND_UNSUPPORTED;
        }
        if (media instanceof TLRPC.TL_messageMediaPhoto && media.photo != null && !media.photo.sizes.isEmpty()) {
            return KIND_PHOTO;
        }
        if (media instanceof TLRPC.TL_messageMediaDocument && media.document != null && media.document.key == null) {
            TLRPC.Document document = media.document;
            if (MessageObject.isVideoDocument(document) && !MessageObject.isRoundVideoDocument(document) && !MessageObject.isGifDocument(document)) {
                return KIND_VIDEO;
            }
            return KIND_DOCUMENT;
        }
        return KIND_UNSUPPORTED;
    }

    private void observe(int account) {
        if (account < 0 || account >= observing.length || observing[account]) {
            return;
        }
        observing[account] = true;
        NotificationCenter nc = NotificationCenter.getInstance(account);
        nc.addObserver(this, NotificationCenter.fileLoaded);
        nc.addObserver(this, NotificationCenter.fileLoadFailed);
    }

    private void start(Job job) {
        FileLoader loader = FileLoader.getInstance(job.account);
        ArrayList<Item> toLoad = new ArrayList<>();
        for (int a = 0; a < job.items.size(); a++) {
            Item item = job.items.get(a);
            if (item.kind != KIND_PHOTO && item.kind != KIND_VIDEO && item.kind != KIND_DOCUMENT) {
                continue;
            }
            item.fileName = item.obj.getFileName();
            if (TextUtils.isEmpty(item.fileName)) {
                item.failed = true;
                continue;
            }
            String existing = findLocalFile(job.account, item.obj);
            if (existing != null) {
                item.path = existing;
                continue;
            }
            toLoad.add(item);
        }
        if (toLoad.isEmpty()) {
            send(job);
            return;
        }
        toast("Himoyalangan kontent yuklab olinmoqda, so'ng yuboriladi…");
        observe(job.account);
        // FileLoader fayl diskda bo'lsa darhol fileLoaded yuborishi mumkin — avval hammasini ro'yxatga olamiz
        job.waiting = toLoad.size() + 1;
        for (int a = 0; a < toLoad.size(); a++) {
            addPending(job, toLoad.get(a));
        }
        for (int a = 0; a < toLoad.size(); a++) {
            // allaqachon yuklanayotgan bo'lsa ham chaqiramiz — FileLoader faqat ustuvorlikni oshiradi
            startLoad(loader, toLoad.get(a));
        }
        release(job);
    }

    private void addPending(Job job, Item item) {
        ArrayList<Object[]> arr = pending.get(item.fileName);
        if (arr == null) {
            arr = new ArrayList<>();
            pending.put(item.fileName, arr);
        }
        arr.add(new Object[]{job, item});
    }

    private static void startLoad(FileLoader loader, Item item) {
        if (item.kind == KIND_PHOTO) {
            TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(MessageObject.getMedia(item.obj.messageOwner).photo.sizes, AndroidUtilities.getPhotoSize());
            loader.loadFile(ImageLocation.getForObject(size, item.obj.photoThumbsObject), item.obj, null, FileLoader.PRIORITY_HIGH, 0);
        } else {
            loader.loadFile(item.obj.getDocument(), item.obj, FileLoader.PRIORITY_HIGH, 0);
        }
    }

    private static String findLocalFile(int account, MessageObject obj) {
        try {
            String attach = obj.messageOwner.attachPath;
            if (!TextUtils.isEmpty(attach) && new File(attach).exists()) {
                return attach;
            }
            File f = FileLoader.getInstance(account).getPathToMessage(obj.messageOwner);
            if (f != null && f.exists() && f.length() > 0) {
                return f.getAbsolutePath();
            }
            f = FileLoader.getInstance(account).getPathToMessage(obj.messageOwner, true, true);
            if (f != null && f.exists() && f.length() > 0) {
                return f.getAbsolutePath();
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return null;
    }

    private void release(Job job) {
        job.waiting--;
        if (job.waiting == 0) {
            send(job);
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        try {
            if (id != NotificationCenter.fileLoaded && id != NotificationCenter.fileLoadFailed) {
                return;
            }
            if (args == null || args.length < 1 || !(args[0] instanceof String)) {
                return;
            }
            ArrayList<Object[]> arr = pending.get((String) args[0]);
            if (arr == null) {
                return;
            }
            ArrayList<Object[]> mine = new ArrayList<>();
            for (int a = arr.size() - 1; a >= 0; a--) {
                if (((Job) arr.get(a)[0]).account == account) {
                    mine.add(0, arr.remove(a));
                }
            }
            if (arr.isEmpty()) {
                pending.remove((String) args[0]);
            }
            String path = null;
            if (id == NotificationCenter.fileLoaded && args.length >= 2 && args[1] instanceof File && ((File) args[1]).exists()) {
                path = ((File) args[1]).getAbsolutePath();
            }
            for (int a = 0; a < mine.size(); a++) {
                Job job = (Job) mine.get(a)[0];
                Item item = (Item) mine.get(a)[1];
                if (path != null) {
                    item.path = path;
                } else if (item.retries < MAX_RETRIES) {
                    // bekor qilindi (chatdan chiqildi) yoki tarmoq xatosi — biroz kutib qayta yuklaymiz
                    item.retries++;
                    addPending(job, item);
                    AndroidUtilities.runOnUIThread(() -> {
                        try {
                            startLoad(FileLoader.getInstance(job.account), item);
                        } catch (Throwable e) {
                            FileLog.e(e);
                        }
                    }, 1000L * item.retries);
                    continue;
                } else {
                    item.failed = true;
                }
                release(job);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    // ── Yuborish (UI oqimida, asl tartibda) ──────────────────────

    private void send(Job job) {
        int failed = 0;
        ArrayList<SendMessagesHelper.SendingMediaInfo> album = new ArrayList<>();
        long albumGroupId = 0;
        for (int a = 0; a < job.items.size(); a++) {
            Item item = job.items.get(a);
            long groupId = item.obj.getGroupId();
            boolean media = (item.kind == KIND_PHOTO || item.kind == KIND_VIDEO) && !item.failed && item.path != null;
            if (!album.isEmpty() && (!media || groupId == 0 || groupId != albumGroupId)) {
                sendAlbum(job, album);
                album = new ArrayList<>();
            }
            try {
                if (media) {
                    album.add(makeMediaInfo(job, item));
                    albumGroupId = groupId;
                    if (groupId == 0) {
                        sendAlbum(job, album);
                        album = new ArrayList<>();
                    }
                } else if (item.kind == KIND_TEXT) {
                    if (!sendText(job, item)) {
                        failed++;
                    }
                } else if (item.kind == KIND_DOCUMENT && !item.failed && item.path != null) {
                    sendDocument(job, item);
                } else {
                    // yuklab bo'lmadi yoki qo'llab-quvvatlanmaydi — hech bo'lmasa matnini yuboramiz
                    sendText(job, item);
                    failed++;
                }
            } catch (Throwable e) {
                FileLog.e(e);
                failed++;
            }
        }
        if (!album.isEmpty()) {
            sendAlbum(job, album);
        }
        if (failed > 0) {
            toast("Ba'zi xabarlarni yuborib bo'lmadi (" + failed + ")");
        }
    }

    private String caption(Job job, Item item) {
        if (job.hideCaption || TextUtils.isEmpty(item.obj.messageOwner.message)) {
            return null;
        }
        return item.obj.messageOwner.message;
    }

    private ArrayList<TLRPC.MessageEntity> entities(Job job, Item item) {
        if (job.hideCaption || item.obj.messageOwner.entities == null || item.obj.messageOwner.entities.isEmpty()) {
            return null;
        }
        return new ArrayList<>(item.obj.messageOwner.entities);
    }

    private SendMessagesHelper.SendingMediaInfo makeMediaInfo(Job job, Item item) {
        SendMessagesHelper.SendingMediaInfo info = new SendMessagesHelper.SendingMediaInfo();
        info.path = item.path;
        info.isVideo = item.kind == KIND_VIDEO;
        info.caption = caption(job, item);
        info.entities = entities(job, item);
        TLRPC.MessageMedia media = MessageObject.getMedia(item.obj.messageOwner);
        info.hasMediaSpoilers = media != null && media.spoiler;
        return info;
    }

    private void sendAlbum(Job job, ArrayList<SendMessagesHelper.SendingMediaInfo> album) {
        try {
            SendMessagesHelper.prepareSendingMedia(AccountInstance.getInstance(job.account), album, job.peer, null, job.replyToTopMsg, null, null,
                    false, album.size() > 1, null, job.notify, job.scheduleDate, job.scheduleRepeatPeriod, 0, false, null, null, 0, 0, false,
                    job.payStars, job.monoForumPeerId, null);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private boolean sendText(Job job, Item item) {
        String text = item.obj.messageOwner.message;
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        ArrayList<TLRPC.MessageEntity> ents = item.obj.messageOwner.entities == null || item.obj.messageOwner.entities.isEmpty() ? null : new ArrayList<>(item.obj.messageOwner.entities);
        SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(text, job.peer, null, job.replyToTopMsg, null, true, ents, null, null, job.notify, job.scheduleDate, job.scheduleRepeatPeriod, null, false);
        params.payStars = job.payStars;
        params.monoForumPeer = job.monoForumPeerId;
        SendMessagesHelper.getInstance(job.account).sendMessage(params);
        return true;
    }

    private void sendDocument(Job job, Item item) {
        TLRPC.Document src = item.obj.getDocument();
        // yangi (yuklanadigan) hujjat: access_hash=0 → SendMessagesHelper faylni qaytadan yuklaydi
        TLRPC.TL_document doc = new TLRPC.TL_document();
        doc.file_reference = new byte[0];
        doc.dc_id = Integer.MIN_VALUE;
        doc.id = SharedConfig.getLastLocalId();
        doc.user_id = UserConfig.getInstance(job.account).getClientUserId();
        doc.date = ConnectionsManager.getInstance(job.account).getCurrentTime();
        doc.mime_type = TextUtils.isEmpty(src.mime_type) ? "application/octet-stream" : src.mime_type;
        doc.size = new File(item.path).length();
        for (int a = 0; a < src.attributes.size(); a++) {
            TLRPC.DocumentAttribute attr = src.attributes.get(a);
            if (attr instanceof TLRPC.TL_documentAttributeSticker) {
                // asl stiker to'plamiga bog'lanmasin
                TLRPC.TL_documentAttributeSticker sticker = new TLRPC.TL_documentAttributeSticker();
                sticker.alt = attr.alt;
                sticker.stickerset = new TLRPC.TL_inputStickerSetEmpty();
                doc.attributes.add(sticker);
            } else {
                doc.attributes.add(attr);
            }
        }
        SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(doc, null, item.path, job.peer, null, job.replyToTopMsg,
                caption(job, item), entities(job, item), null, null, job.notify, job.scheduleDate, job.scheduleRepeatPeriod, 0, null, null, false);
        params.payStars = job.payStars;
        params.monoForumPeer = job.monoForumPeerId;
        TLRPC.MessageMedia media = MessageObject.getMedia(item.obj.messageOwner);
        params.hasMediaSpoilers = media != null && media.spoiler;
        SendMessagesHelper.getInstance(job.account).sendMessage(params);
    }

    private static void toast(String text) {
        try {
            Toast.makeText(ApplicationLoader.applicationContext, AlfaCyrillic.ui(text), Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
