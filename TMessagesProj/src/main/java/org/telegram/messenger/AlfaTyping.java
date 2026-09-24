package org.telegram.messenger;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

/**
 * AlfaGram — "yozdi, lekin yubormadi" tarixi.
 * Kim qachon yozayotgan holatga o'tdi, keyin (a) bekor qildi, yoki (b) xabar yubormasdan to'xtatdi — hammasi shu yerda.
 * Xabar yuborsa, entry "sent=1" qilib belgilanadi va foydalanuvchiga ko'rsatilmaydi.
 */
public class AlfaTyping extends SQLiteOpenHelper {

    private static volatile AlfaTyping instance;

    public static AlfaTyping getInstance() {
        if (instance == null) {
            synchronized (AlfaTyping.class) {
                if (instance == null) {
                    instance = new AlfaTyping(ApplicationLoader.applicationContext);
                }
            }
        }
        return instance;
    }

    private AlfaTyping(Context context) {
        super(context, "alfa_typing.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS typing (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "account INTEGER, dialog_id INTEGER, user_id INTEGER, " +
                "started_at INTEGER, ended_at INTEGER, sent INTEGER DEFAULT 0, action INTEGER DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_typing ON typing(account, dialog_id, user_id, sent)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /** Yangi "yozayotgan" sessiyasini boshlaydi (yoki mavjudini qaytaradi). */
    public long startSession(int account, long dialogId, long userId, int actionType) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            // Oxirgi 30 sekund ichida ochiq (ended_at=0, sent=0) sessiya bormi?
            long now = System.currentTimeMillis();
            Cursor c = db.rawQuery("SELECT id FROM typing WHERE account=? AND dialog_id=? AND user_id=? AND ended_at=0 AND sent=0 AND started_at>?",
                    new String[]{String.valueOf(account), String.valueOf(dialogId), String.valueOf(userId), String.valueOf(now - 60000L)});
            long existing = c.moveToNext() ? c.getLong(0) : 0;
            c.close();
            if (existing != 0) {
                return existing;
            }
            db.execSQL("INSERT INTO typing(account, dialog_id, user_id, started_at, ended_at, sent, action) VALUES(?,?,?,?,0,0,?)",
                    new Object[]{account, dialogId, userId, now, actionType});
            Cursor c2 = db.rawQuery("SELECT last_insert_rowid()", null);
            long id = c2.moveToNext() ? c2.getLong(0) : 0;
            c2.close();
            return id;
        } catch (Throwable t) {
            FileLog.e(t);
            return 0;
        }
    }

    /** Foydalanuvchi yozishni to'xtatdi (bekor qildi yoki timeout) — bu "yubormadi" degani. */
    public void endSession(int account, long dialogId, long userId) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            long now = System.currentTimeMillis();
            db.execSQL("UPDATE typing SET ended_at=? WHERE account=? AND dialog_id=? AND user_id=? AND ended_at=0 AND sent=0",
                    new Object[]{now, account, dialogId, userId});
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    /** Foydalanuvchi xabar yubordi — ochiq sessiyalarni "sent" deb belgilaymiz. */
    public void markSent(int account, long dialogId, long userId) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("UPDATE typing SET sent=1, ended_at=? WHERE account=? AND dialog_id=? AND user_id=? AND sent=0",
                    new Object[]{System.currentTimeMillis(), account, dialogId, userId});
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public static class Record {
        public long id;
        public long userId;
        public long startedAt;
        public long endedAt;
        public int action;
    }

    /** Chat uchun "yozdi, yubormadi" ro'yxati (yangi->eski). Tugagan va yuborilmagan sessiyalar. */
    public ArrayList<Record> getUnsent(int account, long dialogId) {
        ArrayList<Record> out = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT id, user_id, started_at, ended_at, action FROM typing WHERE account=? AND dialog_id=? AND sent=0 AND ended_at>0 ORDER BY started_at DESC",
                    new String[]{String.valueOf(account), String.valueOf(dialogId)});
            while (c.moveToNext()) {
                Record r = new Record();
                r.id = c.getLong(0);
                r.userId = c.getLong(1);
                r.startedAt = c.getLong(2);
                r.endedAt = c.getLong(3);
                r.action = c.getInt(4);
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
            db.execSQL("DELETE FROM typing WHERE account=? AND dialog_id=?",
                    new Object[]{account, dialogId});
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }
}
