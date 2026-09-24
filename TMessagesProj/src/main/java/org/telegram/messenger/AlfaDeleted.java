package org.telegram.messenger;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * AlfaGram — o'chirilgan xabarlar arxivi.
 * Server "o'chir" desa, xabarni o'z bazamizga ko'chiramiz (asosiy DB tegilmaydi).
 * Foydalanuvchi keyin ko'ra oladi. Har chat uchun oxirgi 500 xabar saqlanadi.
 */
public class AlfaDeleted extends SQLiteOpenHelper {

    private static volatile AlfaDeleted instance;
    private static final int LIMIT_PER_DIALOG = 500;

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
        super(context, "alfa_deleted.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS deleted (" +
                "account INTEGER, dialog_id INTEGER, message_id INTEGER, from_id INTEGER, " +
                "date INTEGER, deleted_at INTEGER, text TEXT, data BLOB, " +
                "PRIMARY KEY(account, dialog_id, message_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_dialog ON deleted(account, dialog_id, deleted_at)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /** Bitta xabarni saqlaydi (o'chirilishdan oldin). */
    public void save(int account, long dialogId, TLRPC.Message msg) {
        if (msg == null || msg.id == 0) {
            return;
        }
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
            db.execSQL("INSERT OR REPLACE INTO deleted(account, dialog_id, message_id, from_id, date, deleted_at, text, data) VALUES(?,?,?,?,?,?,?,?)",
                    new Object[]{account, dialogId, msg.id, fromId, msg.date, now, text, blob});
            // limit — eng eskisini o'chir
            db.execSQL("DELETE FROM deleted WHERE account=? AND dialog_id=? AND message_id NOT IN (SELECT message_id FROM deleted WHERE account=? AND dialog_id=? ORDER BY deleted_at DESC LIMIT ?)",
                    new Object[]{account, dialogId, account, dialogId, LIMIT_PER_DIALOG});
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public static class Record {
        public int messageId;
        public long fromId;
        public int date;
        public long deletedAt;
        public String text;
    }

    /** Chat uchun o'chirilgan xabarlar ro'yxati (yangi->eski). */
    public ArrayList<Record> get(int account, long dialogId) {
        ArrayList<Record> out = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT message_id, from_id, date, deleted_at, text FROM deleted WHERE account=? AND dialog_id=? ORDER BY deleted_at DESC",
                    new String[]{String.valueOf(account), String.valueOf(dialogId)});
            while (c.moveToNext()) {
                Record r = new Record();
                r.messageId = c.getInt(0);
                r.fromId = c.getLong(1);
                r.date = c.getInt(2);
                r.deletedAt = c.getLong(3);
                r.text = c.getString(4);
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
            SQLiteDatabase db = getWritableDatabase();
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
