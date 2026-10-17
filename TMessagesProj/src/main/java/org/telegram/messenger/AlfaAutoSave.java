package org.telegram.messenger;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;

/**
 * AlfaGram — kelgan ovozli xabar / video-notelarni avtomatik telefonga saqlash.
 * Ovoz -> Music/AlfaGram, video-note -> Movies/AlfaGram.
 * Takrorlanmaslik uchun (akkaunt, dialog, xabar) kalitlari SharedPreferences'da saqlanadi.
 * Barcha holat faqat UI oqimida o'zgaradi, fayl nusxalash esa fon oqimida.
 */
public class AlfaAutoSave implements NotificationCenter.NotificationCenterDelegate {

    private static final String PREFS = "alfa_autosave";
    private static final String KEY_DONE = "done";
    private static final int DONE_LIMIT = 500;

    private static final int KIND_VOICE = 1;
    private static final int KIND_ROUND = 2;

    private static volatile AlfaAutoSave instance;

    public static AlfaAutoSave getInstance() {
        if (instance == null) {
            synchronized (AlfaAutoSave.class) {
                if (instance == null) {
                    instance = new AlfaAutoSave();
                }
            }
        }
        return instance;
    }

    private static class Pending {
        int account;
        long dialogId;
        int msgId;
        int date;
        int kind;
        String mime;
    }

    // fayl nomi -> kutilayotgan xabarlar (faqat UI oqimida)
    private final HashMap<String, ArrayList<Pending>> pending = new HashMap<>();
    private final boolean[] observing = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private LinkedHashSet<String> done;

    /** MessagesController.updateInterfaceWithMessages'dan chaqiriladi. */
    public static void onNewMessages(int account, long dialogId, ArrayList<MessageObject> messages) {
        try {
            if (!AlfaFeatures.autoSaveVoice && !AlfaFeatures.autoSaveVideoNotes) {
                return;
            }
            if (messages == null || messages.isEmpty() || DialogObject.isEncryptedDialog(dialogId)) {
                return;
            }
            final ArrayList<Object[]> list = new ArrayList<>();
            for (int a = 0; a < messages.size(); a++) {
                MessageObject obj = messages.get(a);
                if (obj == null || obj.messageOwner == null || obj.isOut() || obj.getId() <= 0 || obj.isSponsored()) {
                    continue;
                }
                int kind;
                if (AlfaFeatures.autoSaveVoice && obj.isVoice()) {
                    kind = KIND_VOICE;
                } else if (AlfaFeatures.autoSaveVideoNotes && obj.isRoundVideo()) {
                    kind = KIND_ROUND;
                } else {
                    continue;
                }
                // bir martalik (view-once) / maxfiy media saqlanmaydi
                TLRPC.MessageMedia media = MessageObject.getMedia(obj.messageOwner);
                if (media == null || media.ttl_seconds != 0 || obj.isSecretMedia()) {
                    continue;
                }
                TLRPC.Document document = obj.getDocument();
                if (document == null || document.key != null) {
                    continue;
                }
                list.add(new Object[]{obj, document, kind});
            }
            if (list.isEmpty()) {
                return;
            }
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    getInstance().enqueue(account, dialogId, list);
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            });
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static String key(int account, long dialogId, int msgId) {
        return account + "_" + dialogId + "_" + msgId;
    }

    private void loadDone() {
        if (done != null) {
            return;
        }
        done = new LinkedHashSet<>();
        try {
            SharedPreferences p = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0);
            String s = p.getString(KEY_DONE, "");
            if (!TextUtils.isEmpty(s)) {
                String[] parts = s.split(",");
                for (String part : parts) {
                    if (!TextUtils.isEmpty(part)) {
                        done.add(part);
                    }
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private void markDone(String k) {
        loadDone();
        done.remove(k);
        done.add(k);
        // eng eskilarini olib tashlaymiz (cheklov)
        while (done.size() > DONE_LIMIT) {
            String first = done.iterator().next();
            done.remove(first);
        }
        try {
            ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0).edit()
                    .putString(KEY_DONE, TextUtils.join(",", done)).apply();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private boolean isPending(String k) {
        for (ArrayList<Pending> arr : pending.values()) {
            for (int a = 0; a < arr.size(); a++) {
                Pending p = arr.get(a);
                if (k.equals(key(p.account, p.dialogId, p.msgId))) {
                    return true;
                }
            }
        }
        return false;
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

    private void enqueue(int account, long dialogId, ArrayList<Object[]> list) {
        loadDone();
        for (int a = 0; a < list.size(); a++) {
            MessageObject obj = (MessageObject) list.get(a)[0];
            TLRPC.Document document = (TLRPC.Document) list.get(a)[1];
            int kind = (Integer) list.get(a)[2];
            String k = key(account, dialogId, obj.getId());
            if (pending.size() > 200 || done.contains(k) || isPending(k)) {
                continue;
            }
            String fileName = FileLoader.getAttachFileName(document);
            if (TextUtils.isEmpty(fileName)) {
                continue;
            }
            Pending p = new Pending();
            p.account = account;
            p.dialogId = dialogId;
            p.msgId = obj.getId();
            p.date = obj.messageOwner.date;
            p.kind = kind;
            p.mime = document.mime_type;
            ArrayList<Pending> arr = pending.get(fileName);
            if (arr == null) {
                arr = new ArrayList<>();
                pending.put(fileName, arr);
            }
            arr.add(p);
            observe(account);
            // allaqachon yuklanayotgan bo'lsa ustuvorlikni buzmaymiz, faqat fileLoaded'ni kutamiz.
            // fayl diskda bo'lsa ham FileLoader darhol fileLoaded yuboradi
            FileLoader loader = FileLoader.getInstance(account);
            if (!loader.isLoadingFile(fileName)) {
                loader.loadFile(document, obj, FileLoader.PRIORITY_NORMAL, 0);
            }
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        try {
            if (args == null || args.length < 1 || !(args[0] instanceof String)) {
                return;
            }
            String fileName = (String) args[0];
            if (id == NotificationCenter.fileLoaded) {
                ArrayList<Pending> arr = pending.get(fileName);
                if (arr == null) {
                    return;
                }
                ArrayList<Pending> mine = new ArrayList<>();
                for (int a = arr.size() - 1; a >= 0; a--) {
                    if (arr.get(a).account == account) {
                        mine.add(arr.remove(a));
                    }
                }
                if (arr.isEmpty()) {
                    pending.remove(fileName);
                }
                if (mine.isEmpty() || args.length < 2 || !(args[1] instanceof File)) {
                    return;
                }
                final File file = (File) args[1];
                for (int a = 0; a < mine.size(); a++) {
                    final Pending p = mine.get(a);
                    // o'chirilgan bo'lsa saqlamaymiz
                    if (p.kind == KIND_VOICE ? !AlfaFeatures.autoSaveVoice : !AlfaFeatures.autoSaveVideoNotes) {
                        continue;
                    }
                    Utilities.globalQueue.postRunnable(() -> {
                        boolean ok = false;
                        try {
                            ok = copyToPublic(file, p);
                        } catch (Throwable e) {
                            FileLog.e(e);
                        }
                        if (ok) {
                            AndroidUtilities.runOnUIThread(() -> markDone(key(p.account, p.dialogId, p.msgId)));
                        }
                    });
                }
            } else if (id == NotificationCenter.fileLoadFailed) {
                ArrayList<Pending> arr = pending.get(fileName);
                if (arr == null) {
                    return;
                }
                for (int a = arr.size() - 1; a >= 0; a--) {
                    if (arr.get(a).account == account) {
                        arr.remove(a);
                    }
                }
                if (arr.isEmpty()) {
                    pending.remove(fileName);
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    // ── Nusxalash (fon oqimida) ──────────────────────

    private static boolean copyToPublic(File src, Pending p) throws Exception {
        if (src == null || !src.exists() || src.length() == 0) {
            return false;
        }
        boolean voice = p.kind == KIND_VOICE;
        String ext = FileLoader.getFileExtension(src);
        if (TextUtils.isEmpty(ext) || ext.length() > 5 || !ext.matches("[A-Za-z0-9]+")) {
            ext = voice ? "ogg" : "mp4";
        }
        String mime = p.mime;
        if (TextUtils.isEmpty(mime)) {
            mime = voice ? "audio/ogg" : "video/mp4";
        }
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date(p.date * 1000L));
        String name = (voice ? "voice_" : "round_") + time + "_" + p.msgId + "." + ext;

        if (Build.VERSION.SDK_INT >= 29) {
            String primaryDir = (voice ? Environment.DIRECTORY_MUSIC : Environment.DIRECTORY_MOVIES) + "/AlfaGram";
            Uri collection = voice ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    : MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            if (insertQ(collection, primaryDir, name, mime, src)) {
                return true;
            }
            // zaxira: Download/AlfaGram/...
            String fallbackDir = Environment.DIRECTORY_DOWNLOADS + "/AlfaGram/" + (voice ? "Voice" : "Video");
            return insertQ(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), fallbackDir, name, mime, src);
        }

        // Eski Android: ruxsat bo'lmasa jim o'tkazamiz
        if (Build.VERSION.SDK_INT >= 23 && ApplicationLoader.applicationContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        File dir = new File(Environment.getExternalStoragePublicDirectory(voice ? Environment.DIRECTORY_MUSIC : Environment.DIRECTORY_MOVIES), "AlfaGram");
        if (!dir.exists() && !dir.mkdirs()) {
            return false;
        }
        File dest = new File(dir, name);
        if (dest.exists() && dest.length() == src.length()) {
            return true;
        }
        if (!AndroidUtilities.copyFile(src, dest)) {
            return false;
        }
        AndroidUtilities.addMediaToGallery(dest);
        return true;
    }

    @RequiresApi(api = 29)
    private static boolean insertQ(Uri collection, String relativePath, String name, String mime, File src) {
        ContentResolver resolver = ApplicationLoader.applicationContext.getContentResolver();
        Uri dst = null;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath + File.separator);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            dst = resolver.insert(collection, values);
            if (dst == null) {
                return false;
            }
            try (FileInputStream in = new FileInputStream(src)) {
                OutputStream out = resolver.openOutputStream(dst);
                if (out == null) {
                    throw new IllegalStateException("no output stream");
                }
                AndroidUtilities.copyFile(in, out);
            }
            ContentValues fin = new ContentValues();
            fin.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(dst, fin, null, null);
            return true;
        } catch (Throwable e) {
            FileLog.e(e);
            if (dst != null) {
                try {
                    resolver.delete(dst, null, null);
                } catch (Throwable ignore) {
                }
            }
            return false;
        }
    }
}
