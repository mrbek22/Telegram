package org.telegram.messenger;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;

import java.util.ArrayList;

/**
 * AlfaGram — kontaktlarning onlayn tarixi.
 * Manbalar: updateUserStatus yangilanishlari, serverdan kelgan foydalanuvchi obyektlari va
 * har daqiqada so'raladigan contacts.getStatuses. Oxirgisi muhim: ilova "oflayn" turganda
 * (Ghost rejim yoki fon) server holat yangilanishlarini yubormaydi, so'rov esa baribir ishlaydi.
 * Faqat ilova serverga ulangan paytdagi holatlar yoziladi.
 */
public class AlfaOnlineLog extends SQLiteOpenHelper {

    public static class Session {
        public long start;
        public long end;  // 0 — hali onlayn (yoki kuzatuv uzilgan, qarang: seen)
        public long seen; // oxirgi marta onlayn ko'rilgan vaqt
    }

    private static final int MAX_ROWS_PER_USER = 200;
    /** Onlayn holat shuncha vaqt tasdiqlanmasa — kuzatuv uzilgan deb hisoblanadi. */
    public static final long GAP_MS = 6 * 60 * 1000L;
    private static final long POLL_FOREGROUND_MS = 45 * 1000L;
    private static final long POLL_BACKGROUND_MS = 3 * 60 * 1000L;

    private static volatile AlfaOnlineLog instance;
    private static final long[] lastPoll = new long[UserConfig.MAX_ACCOUNT_COUNT];
    private static final boolean[] polling = new boolean[UserConfig.MAX_ACCOUNT_COUNT];

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
        super(context, "alfa_online.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS online (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, account INTEGER, user_id INTEGER, start_at INTEGER, end_at INTEGER DEFAULT 0, seen_at INTEGER DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_online ON online(account, user_id, start_at)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE online ADD COLUMN seen_at INTEGER DEFAULT 0");
            db.execSQL("UPDATE online SET seen_at = start_at");
        }
    }

    /**
     * MessagesController.updateTimerProc'dan (har soniyada) chaqiriladi: vaqti kelganda
     * kontaktlar holatini serverdan so'raydi. Ilova ochiq bo'lsa tez-tez, fonda kamroq.
     */
    public static void maybePoll(int account) {
        if (!AlfaFeatures.onlineTracker || account < 0 || account >= lastPoll.length) return;
        final long now = System.currentTimeMillis();
        final long interval = ApplicationLoader.mainInterfacePaused ? POLL_BACKGROUND_MS : POLL_FOREGROUND_MS;
        synchronized (lastPoll) {
            if (polling[account] || now - lastPoll[account] < interval) return;
            if (!UserConfig.getInstance(account).isClientActivated() || !ContactsController.getInstance(account).contactsLoaded) return;
            lastPoll[account] = now;
            polling[account] = true;
        }
        TLRPC.TL_contacts_getStatuses req = new TLRPC.TL_contacts_getStatuses();
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            synchronized (lastPoll) {
                polling[account] = false;
            }
            if (!(response instanceof Vector)) return;
            MessagesController mc = MessagesController.getInstance(account);
            boolean changed = false;
            for (Object o : ((Vector) response).objects) {
                if (!(o instanceof TLRPC.TL_contactStatus)) continue;
                TLRPC.TL_contactStatus s = (TLRPC.TL_contactStatus) o;
                if (s.status == null) continue;
                onUserStatus(account, s.user_id, s.status);
                // ekrandagi "oxirgi marta" ham yangilansin (Recently/LastWeek kabi maxfiy holatlarga tegmaymiz)
                TLRPC.User user = mc.getUser(s.user_id);
                if (user != null && (s.status instanceof TLRPC.TL_userStatusOnline || s.status instanceof TLRPC.TL_userStatusOffline)
                        && (user.status == null || user.status.getClass() != s.status.getClass() || user.status.expires != s.status.expires)) {
                    user.status = s.status;
                    changed = true;
                }
            }
            if (changed) {
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_STATUS);
            }
        }));
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
                getInstance().apply(account, userId, online, offlineAt > 0 ? Math.min(offlineAt, now) : 0, now);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        });
    }

    /**
     * @param offlineAt server aytgan oxirgi onlayn vaqti (0 — noma'lum, masalan maxfiylik yopiq)
     */
    private void apply(int account, long userId, boolean online, long offlineAt, long now) {
        SQLiteDatabase db = getWritableDatabase();
        String[] args = new String[]{String.valueOf(account), String.valueOf(userId)};
        Cursor c = db.rawQuery("SELECT id, start_at, end_at, seen_at FROM online WHERE account=? AND user_id=? ORDER BY start_at DESC LIMIT 1", args);
        long lastId = 0, lastStart = 0, lastEnd = 0, lastSeen = 0;
        if (c.moveToNext()) {
            lastId = c.getLong(0);
            lastStart = c.getLong(1);
            lastEnd = c.getLong(2);
            lastSeen = Math.max(c.getLong(3), lastStart);
        }
        c.close();
        final boolean open = lastId != 0 && lastEnd == 0;

        if (online) {
            if (open && now - lastSeen <= GAP_MS) {
                db.execSQL("UPDATE online SET seen_at=? WHERE id=?", new Object[]{now, lastId});
                return;
            }
            if (open) {
                // kuzatuv uzilgan (ilova yopiq edi) — eski sessiyani oxirgi ko'rilgan paytda yopamiz
                db.execSQL("UPDATE online SET end_at=? WHERE id=?", new Object[]{lastSeen, lastId});
            }
            db.execSQL("INSERT INTO online(account, user_id, start_at, end_at, seen_at) VALUES(?,?,?,0,?)", new Object[]{account, userId, now, now});
            trim(db, account, userId);
            return;
        }

        if (open) {
            long end;
            if (offlineAt > 0) {
                end = Math.max(offlineAt, lastStart);
            } else {
                end = now - lastSeen > GAP_MS ? lastSeen : now;
            }
            db.execSQL("UPDATE online SET end_at=? WHERE id=?", new Object[]{end, lastId});
        } else if (offlineAt > 0 && (lastId == 0 || offlineAt > lastEnd + 60 * 1000L)) {
            // onlayn paytini ko'rmadik, lekin server oxirgi onlayn vaqtini aytdi — shu nuqtani yozamiz
            db.execSQL("INSERT INTO online(account, user_id, start_at, end_at, seen_at) VALUES(?,?,?,?,?)", new Object[]{account, userId, offlineAt, offlineAt, offlineAt});
            trim(db, account, userId);
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
            Cursor c = getReadableDatabase().rawQuery("SELECT start_at, end_at, seen_at FROM online WHERE account=? AND user_id=? ORDER BY start_at DESC LIMIT " + limit,
                    new String[]{String.valueOf(account), String.valueOf(userId)});
            while (c.moveToNext()) {
                Session s = new Session();
                s.start = c.getLong(0);
                s.end = c.getLong(1);
                s.seen = Math.max(c.getLong(2), s.start);
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
