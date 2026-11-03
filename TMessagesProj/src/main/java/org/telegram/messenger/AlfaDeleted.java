package org.telegram.messenger;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AlfaGram — o'chirilgan xabarlar arxivi.
 * Server "o'chir" desa, xabarni o'z bazamizga ko'chiramiz (asosiy DB tegilmaydi).
 * Foydalanuvchi keyin ko'ra oladi. Har chat uchun oxirgi 500 xabar saqlanadi.
 * Media (rasm, video, ovoz, fayl) telefonga yuklangan bo'lsa — nusxasi alohida papkaga olinadi,
 * chunki Telegram keshi tozalanganda asl fayl yo'qoladi.
 */
public class AlfaDeleted extends SQLiteOpenHelper {

    private static volatile AlfaDeleted instance;
    private static final int LIMIT_PER_DIALOG = 500;
    /** Bundan katta fayllar nusxalanmaydi. */
    private static final long MAX_MEDIA_BYTES = 150L * 1024 * 1024;

    public static final String MEDIA_PHOTO = "photo";
    public static final String MEDIA_VIDEO = "video";
    public static final String MEDIA_ROUND = "round";
    public static final String MEDIA_VOICE = "voice";
    public static final String MEDIA_MUSIC = "music";
    public static final String MEDIA_GIF = "gif";
    public static final String MEDIA_STICKER = "sticker";
    public static final String MEDIA_FILE = "file";
    public static final String MEDIA_OTHER = "other";

    // Shu sessiyada ushlab qolingan xabarlar (account_dialog_mid) — UI uchun tez tekshiruv, DB'siz
    private final Set<String> keptKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static AlfaDeleted getInstance() {
        if (instance == null) {
            synchronized (AlfaDeleted.class) {
                if (instance == null) {
                    instance = new AlfaDeleted(ApplicationLoader.applicationContext);
                }
            }
        }
        return instance;
    }

    private AlfaDeleted(Context context) {
        super(context, "alfa_deleted.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS deleted (" +
                "account INTEGER, dialog_id INTEGER, message_id INTEGER, from_id INTEGER, " +
                "date INTEGER, deleted_at INTEGER, text TEXT, data BLOB, media_type TEXT, media_path TEXT, " +
                "PRIMARY KEY(account, dialog_id, message_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_dialog ON deleted(account, dialog_id, deleted_at)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE deleted ADD COLUMN media_type TEXT");
            db.execSQL("ALTER TABLE deleted ADD COLUMN media_path TEXT");
        }
    }

    /** Bitta xabarni saqlaydi (o'chirilishdan oldin). */
    public void save(int account, long dialogId, TLRPC.Message msg) {
        if (msg == null || msg.id == 0) {
            return;
        }
        keptKeys.add(account + "_" + dialogId + "_" + msg.id);
        try {
            SQLiteDatabase db = getWritableDatabase();
            long now = System.currentTimeMillis();
            long fromId = 0;
            if (msg.from_id != null) {
                fromId = msg.from_id.user_id != 0 ? msg.from_id.user_id
                        : (msg.from_id.channel_id != 0 ? -msg.from_id.channel_id
                        : -msg.from_id.chat_id);
            }
            String text = msg.message != null ? msg.message : "";
            byte[] blob = null;
            try {
                NativeByteBuffer buf = new NativeByteBuffer(msg.getObjectSize());
                msg.serializeToStream(buf);
                int len = buf.position();
                buf.buffer.rewind();
                blob = new byte[len];
                buf.buffer.get(blob);
                buf.reuse();
            } catch (Throwable ignore) {
            }
            final String mediaType = mediaType(msg);
            db.execSQL("INSERT OR REPLACE INTO deleted(account, dialog_id, message_id, from_id, date, deleted_at, text, data, media_type, media_path) VALUES(?,?,?,?,?,?,?,?,?,NULL)",
                    new Object[]{account, dialogId, msg.id, fromId, msg.date, now, text, blob, mediaType});
            // limit — eng eskisini (va uning media nusxasini) o'chir
            final String overLimit = "account=? AND dialog_id=? AND message_id NOT IN (SELECT message_id FROM deleted WHERE account=? AND dialog_id=? ORDER BY deleted_at DESC LIMIT " + LIMIT_PER_DIALOG + ")";
            deleteMediaFiles(db, overLimit, new String[]{String.valueOf(account), String.valueOf(dialogId), String.valueOf(account), String.valueOf(dialogId)});
            db.execSQL("DELETE FROM deleted WHERE " + overLimit, new Object[]{account, dialogId, account, dialogId});
            if (mediaType != null) {
                // fayl nusxasi fon oqimida — storage navbatini kutdirmaymiz
                Utilities.globalQueue.postRunnable(() -> copyMedia(account, dialogId, msg));
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    /** Xabar media turi (matnli xabar uchun null). */
    private static String mediaType(TLRPC.Message msg) {
        if (msg.media == null || msg.media instanceof TLRPC.TL_messageMediaEmpty || msg.media instanceof TLRPC.TL_messageMediaWebPage) {
            return null;
        }
        if (msg.media instanceof TLRPC.TL_messageMediaPhoto) {
            return MEDIA_PHOTO;
        }
        if (msg.media.document != null) {
            if (MessageObject.isVoiceMessage(msg)) return MEDIA_VOICE;
            if (MessageObject.isRoundVideoMessage(msg)) return MEDIA_ROUND;
            if (MessageObject.isStickerMessage(msg) || MessageObject.isAnimatedStickerMessage(msg)) return MEDIA_STICKER;
            if (MessageObject.isGifMessage(msg)) return MEDIA_GIF;
            if (MessageObject.isVideoMessage(msg)) return MEDIA_VIDEO;
            if (MessageObject.isMusicMessage(msg)) return MEDIA_MUSIC;
            return MEDIA_FILE;
        }
        return MEDIA_OTHER;
    }

    /** Telefonga yuklangan media faylini doimiy papkaga nusxalaydi va yo'lini yozadi. */
    private void copyMedia(int account, long dialogId, TLRPC.Message msg) {
        try {
            FileLoader loader = FileLoader.getInstance(account);
            File src = loader.getPathToMessage(msg, false, false);
            if (src == null || !src.exists() || src.length() == 0) {
                src = loader.getPathToMessage(msg, true, false);
            }
            if (src == null || !src.exists() || src.length() == 0 || src.length() > MAX_MEDIA_BYTES) {
                return;
            }
            File dir = mediaDir();
            if (dir == null) {
                return;
            }
            String name = src.getName();
            int dot = name.lastIndexOf('.');
            String ext = dot >= 0 ? name.substring(dot) : "";
            File dst = new File(dir, account + "_" + dialogId + "_" + msg.id + ext);
            if (!AndroidUtilities.copyFile(src, dst)) {
                return;
            }
            getWritableDatabase().execSQL("UPDATE deleted SET media_path=? WHERE account=? AND dialog_id=? AND message_id=?",
                    new Object[]{dst.getAbsolutePath(), account, dialogId, msg.id});
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    private static File mediaDir() {
        try {
            File base = ApplicationLoader.applicationContext.getExternalFilesDir(null);
            if (base == null) {
                // FileProvider "files/cache" yo'lini biladi
                base = new File(ApplicationLoader.applicationContext.getFilesDir(), "cache");
            }
            File dir = new File(base, "alfa_deleted");
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            return dir;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void deleteMediaFiles(SQLiteDatabase db, String where, String[] args) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT media_path FROM deleted WHERE media_path IS NOT NULL AND " + where, args);
            while (c.moveToNext()) {
                String path = c.getString(0);
                if (path != null) {
                    new File(path).delete();
                }
            }
        } catch (Throwable t) {
            FileLog.e(t);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /** Bu xabar shu sessiyada yuboruvchi tomonidan o'chirilib, ushlab qolinganmi (DB'siz). */
    public boolean isKept(int account, long dialogId, int messageId) {
        return keptKeys.contains(account + "_" + dialogId + "_" + messageId);
    }

    public static class Record {
        public int messageId;
        public long fromId;
        public int date;
        public long deletedAt;
        public String text;
        public String mediaType; // null — matnli xabar
        public String mediaPath; // nusxa fayli (null — telefonga yuklanmagan edi)
    }

    /** Chat uchun o'chirilgan xabarlar ro'yxati (yangi->eski). */
    public ArrayList<Record> get(int account, long dialogId) {
        ArrayList<Record> out = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT message_id, from_id, date, deleted_at, text, media_type, media_path FROM deleted WHERE account=? AND dialog_id=? ORDER BY deleted_at DESC",
                    new String[]{String.valueOf(account), String.valueOf(dialogId)});
            while (c.moveToNext()) {
                Record r = new Record();
                r.messageId = c.getInt(0);
                r.fromId = c.getLong(1);
                r.date = c.getInt(2);
                r.deletedAt = c.getLong(3);
                r.text = c.getString(4);
                r.mediaType = c.getString(5);
                r.mediaPath = c.getString(6);
                out.add(r);
            }
            c.close();
        } catch (Throwable t) {
            FileLog.e(t);
        }
        return out;
    }

    /** Chat uchun barcha o'chirilgan xabarlarni tozalaydi. */
    public void clear(int account, long dialogId) {
        try {
            String prefix = account + "_" + dialogId + "_";
            for (java.util.Iterator<String> it = keptKeys.iterator(); it.hasNext(); ) {
                if (it.next().startsWith(prefix)) {
                    it.remove();
                }
            }
        } catch (Throwable ignore) {
        }
        try {
            SQLiteDatabase db = getWritableDatabase();
            deleteMediaFiles(db, "account=? AND dialog_id=?", new String[]{String.valueOf(account), String.valueOf(dialogId)});
            db.execSQL("DELETE FROM deleted WHERE account=? AND dialog_id=?",
                    new Object[]{account, dialogId});
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public int count(int account, long dialogId) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM deleted WHERE account=? AND dialog_id=?",
                    new String[]{String.valueOf(account), String.valueOf(dialogId)});
            int n = c.moveToNext() ? c.getInt(0) : 0;
            c.close();
            return n;
        } catch (Throwable t) {
            return 0;
        }
    }
}
