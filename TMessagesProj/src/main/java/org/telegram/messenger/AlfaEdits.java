package org.telegram.messenger;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

/**
 * AlfaGram — tahrirlangan xabarlarning eski versiyalarini saqlaydi.
 * Har tahrirdan OLDIN eski matn shu bazaga yoziladi; foydalanuvchi keyin ko'ra oladi.
 */
public class AlfaEdits extends SQLiteOpenHelper {

    private static volatile AlfaEdits instance;

    public static AlfaEdits getInstance() {
        if (instance == null) {
            synchronized (AlfaEdits.class) {
                if (instance == null) {
                    instance = new AlfaEdits(ApplicationLoader.applicationContext);
                }
            }
        }
        return instance;
    }

    private AlfaEdits(Context context) {
        super(context, "alfa_edits.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS edits (" +
                "account INTEGER, dialog_id INTEGER, message_id INTEGER, " +
                "edited_at INTEGER, from_id INTEGER, text TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_edits ON edits(account, dialog_id, message_id, edited_at)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /** Xabar tahrir qilinishidan oldin eski matnni saqlaydi. */
    public void saveBeforeEdit(int account, long dialogId, TLRPC.Message oldMessage) {
        if (oldMessage == null || oldMessage.id == 0) {
            return;
        }
        String text = oldMessage.message;
        if (text == null || text.length() == 0) {
            return;
        }
        try {
            SQLiteDatabase db = getWritableDatabase();
            long fromId = 0;
            if (oldMessage.from_id != null) {
                fromId = oldMessage.from_id.user_id != 0 ? oldMessage.from_id.user_id
                        : (oldMessage.from_id.channel_id != 0 ? -oldMessage.from_id.channel_id
                        : -oldMessage.from_id.chat_id);
            }
            long editedAt = System.currentTimeMillis();
            // Bir xil matnni ikki marta saqlab yubormaslik uchun oxirgisini tekshiramiz
            Cursor c = db.rawQuery("SELECT text FROM edits WHERE account=? AND dialog_id=? AND message_id=? ORDER BY edited_at DESC LIMIT 1",
                    new String[]{String.valueOf(account), String.valueOf(dialogId), String.valueOf(oldMessage.id)});
            String last = c.moveToNext() ? c.getString(0) : null;
            c.close();
            if (text.equals(last)) {
                return;
            }
            db.execSQL("INSERT INTO edits(account, dialog_id, message_id, edited_at, from_id, text) VALUES(?,?,?,?,?,?)",
                    new Object[]{account, dialogId, oldMessage.id, editedAt, fromId, text});
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public static class Record {
        public int messageId;
        public long editedAt;
        public long fromId;
        public String text;
    }

    /** Chat uchun barcha tahrir tarixini beradi (yangi->eski). */
    public ArrayList<Record> get(int account, long dialogId) {
        ArrayList<Record> out = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT message_id, edited_at, from_id, text FROM edits WHERE account=? AND dialog_id=? ORDER BY edited_at DESC",
                    new String[]{String.valueOf(account), String.valueOf(dialogId)});
            while (c.moveToNext()) {
                Record r = new Record();
                r.messageId = c.getInt(0);
                r.editedAt = c.getLong(1);
                r.fromId = c.getLong(2);
                r.text = c.getString(3);
                out.add(r);
            }
            c.close();
        } catch (Throwable t) {
            FileLog.e(t);
        }
        return out;
    }

    /** Bitta xabar uchun tahrir tarixi. */
    public ArrayList<Record> getForMessage(int account, long dialogId, int messageId) {
        ArrayList<Record> out = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT message_id, edited_at, from_id, text FROM edits WHERE account=? AND dialog_id=? AND message_id=? ORDER BY edited_at DESC",
                    new String[]{String.valueOf(account), String.valueOf(dialogId), String.valueOf(messageId)});
            while (c.moveToNext()) {
                Record r = new Record();
                r.messageId = c.getInt(0);
                r.editedAt = c.getLong(1);
                r.fromId = c.getLong(2);
                r.text = c.getString(3);
                out.add(r);
            }
            c.close();
        } catch (Throwable t) {
            FileLog.e(t);
        }
        return out;
    }

    public void clear(int account, long dialogId) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("DELETE FROM edits WHERE account=? AND dialog_id=?",
                    new Object[]{account, dialogId});
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }
}
