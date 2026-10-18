package org.telegram.messenger;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * AlfaGram — kontaktlarning onlayn tarixi.
 * Telegram foydalanuvchi holati yangilanishlarini (updateUserStatus) yozib boradi:
 * onlayn bo'lgan va chiqib ketgan vaqt. Faqat ilova serverga ulangan paytdagi yangilanishlar yoziladi.
 */
public class AlfaOnlineLog extends SQLiteOpenHelper {

    public static class Session {
        public long start;
        public long end; // 0 — hali onlayn
    }

    private static final int MAX_ROWS_PER_USER = 200;
    private static volatile AlfaOnlineLog instance;

    public static AlfaOnlineLog getInstance() {
        if (instance == null) {
            synchronized (AlfaOnlineLog.class) {
                if (instance == null) {
                    instance = new AlfaOnlineLog(ApplicationLoader.applicationContext);
                }
            }
        }
        return instance;
    }

    private AlfaOnlineLog(Context context) {
        super(context, "alfa_online.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS online (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, account INTEGER, user_id INTEGER, start_at INTEGER, end_at INTEGER DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_online ON online(account, user_id, start_at)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /** Holat yangilanishi (UI oqimidan chaqiriladi, yozish fon oqimida). */
    public static void onUserStatus(int account, long userId, TLRPC.UserStatus status) {
        if (!AlfaFeatures.onlineTracker || status == null || userId == 0) return;
        if (userId == UserConfig.getInstance(account).getClientUserId()) return;
        // faqat kontaktlar va shaxsiy chati bor odamlar (guruhdagi begonalar bazani to'ldirmasin)
        try {
            if (ContactsController.getInstance(account).contactsDict.get(userId) == null
                    && MessagesController.getInstance(account).dialogs_dict.get(userId) == null) {
                return;
            }
        } catch (Throwable ignore) {
            return;
        }
        final boolean online = status instanceof TLRPC.TL_userStatusOnline;
        // TL_userStatusOffline: oxirgi onlayn vaqti expires maydonida (sekund)
        final long offlineAt = status instanceof TLRPC.TL_userStatusOffline && status.expires > 0 ? status.expires * 1000L : 0;
        final long now = System.currentTimeMillis();
        Utilities.globalQueue.postRunnable(() -> {
            try {
                getInstance().apply(account, userId, online, offlineAt > 0 ? Math.min(offlineAt, now) : now);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        });
    }

    private void apply(int account, long userId, boolean online, long time) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT id, start_at FROM online WHERE account=? AND user_id=? AND end_at=0 ORDER BY start_at DESC LIMIT 1",
                new String[]{String.valueOf(account), String.valueOf(userId)});
        long openId = 0;
        long openStart = 0;
        if (c.moveToNext()) {
            openId = c.getLong(0);
            openStart = c.getLong(1);
        }
        c.close();
        if (online) {
            if (openId == 0) {
                db.execSQL("INSERT INTO online(account, user_id, start_at, end_at) VALUES(?,?,?,0)", new Object[]{account, userId, time});
                trim(db, account, userId);
            }
        } else if (openId != 0) {
            db.execSQL("UPDATE online SET end_at=? WHERE id=?", new Object[]{Math.max(time, openStart), openId});
        }
    }

    private void trim(SQLiteDatabase db, int account, long userId) {
        db.execSQL("DELETE FROM online WHERE account=? AND user_id=? AND id NOT IN (SELECT id FROM online WHERE account=? AND user_id=? ORDER BY start_at DESC LIMIT " + MAX_ROWS_PER_USER + ")",
                new Object[]{account, userId, account, userId});
    }

    /** Oxirgi sessiyalar (yangisi birinchi). Fon oqimida chaqirish tavsiya etiladi. */
    public ArrayList<Session> getSessions(int account, long userId, int limit) {
        ArrayList<Session> list = new ArrayList<>();
        try {
            Cursor c = getReadableDatabase().rawQuery("SELECT start_at, end_at FROM online WHERE account=? AND user_id=? ORDER BY start_at DESC LIMIT " + limit,
                    new String[]{String.valueOf(account), String.valueOf(userId)});
            while (c.moveToNext()) {
                Session s = new Session();
                s.start = c.getLong(0);
                s.end = c.getLong(1);
                list.add(s);
            }
            c.close();
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return list;
    }

    public void clear(int account, long userId) {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                getWritableDatabase().execSQL("DELETE FROM online WHERE account=? AND user_id=?", new Object[]{account, userId});
            } catch (Throwable e) {
                FileLog.e(e);
            }
        });
    }
}
