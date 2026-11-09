package org.telegram.messenger;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * AlfaGram — profil o'zgarishlari tarixi: kontaktlar va shaxsiy chati bor odamlar username'ini, rasmini,
 * telefon raqamini yoki (kontakt bo'lmasa) ismini o'zgartirsa, shu telefonda yozib boriladi.
 * Manba — serverdan kelgan har bir foydalanuvchi obyekti (MessagesController.putUser, keshdan emas).
 * Birinchi ko'rilganda faqat holat eslab qolinadi, keyingi farqlar jurnalga yoziladi.
 * Kontaktning ismi yozilmaydi: server kontakt ismini siz saqlagan nom bilan beradi.
 */
public class AlfaProfileLog extends SQLiteOpenHelper {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_PHOTO = "photo";
    public static final String FIELD_PHOTO_REMOVED = "photo_removed";
    public static final String FIELD_PHONE = "phone";

    public static class Entry {
        public long userId;
        public long time;
        public String field;
        public String oldValue;
        public String newValue;
    }

    private static final int MAX_ROWS_PER_ACCOUNT = 1000;

    private static volatile AlfaProfileLog instance;

    public static AlfaProfileLog getInstance() {
        if (instance == null) {
            synchronized (AlfaProfileLog.class) {
                if (instance == null) {
                    instance = new AlfaProfileLog(ApplicationLoader.applicationContext);
                }
            }
        }
        return instance;
    }

    private AlfaProfileLog(Context context) {
        super(context, "alfa_profile_log.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS snapshot (account INTEGER, user_id INTEGER, contact INTEGER, " +
                "first TEXT, last TEXT, username TEXT, photo_id INTEGER, phone TEXT, PRIMARY KEY(account, user_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS log (id INTEGER PRIMARY KEY AUTOINCREMENT, account INTEGER, user_id INTEGER, " +
                "time INTEGER, field TEXT, old_value TEXT, new_value TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_log ON log(account, time)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    // ───────────────────────────────────────────── yozish

    /** Holat surati (UI oqimida olinadi, fon oqimida solishtiriladi). */
    private static class Snapshot {
        int account;
        long userId;
        boolean contact;
        String first;
        String last;
        String username;
        /** -1 — noma'lum (shaxsiy qo'yilgan rasm), 0 — rasm yo'q */
        long photoId;
        String phone;
        long time;
    }

    private static final DispatchQueue QUEUE = new DispatchQueue("alfaProfileLog");
    private static final ArrayList<Snapshot> PENDING = new ArrayList<>();
    private static boolean flushScheduled;

    /** MessagesController.putUser'dan (serverdan kelgan, min bo'lmagan foydalanuvchi). */
    public static void onUser(int account, TLRPC.User user) {
        if (!AlfaFeatures.profileLog || user == null || user.min || user.id == 0) return;
        try {
            if (user.self || user.bot || user.deleted || user.id == UserConfig.getInstance(account).getClientUserId()) return;
            if (user.id == 777000 || user.id / 1000 == 333) return;
            // faqat kontaktlar va shaxsiy chati bor odamlar (guruhdagi begonalar bazani to'ldirmasin)
            if (ContactsController.getInstance(account).contactsDict.get(user.id) == null
                    && MessagesController.getInstance(account).dialogs_dict.get(user.id) == null) {
                return;
            }
        } catch (Throwable ignore) {
            return;
        }
        Snapshot s = new Snapshot();
        s.account = account;
        s.userId = user.id;
        s.contact = user.contact;
        s.first = user.first_name;
        s.last = user.last_name;
        s.username = UserObject.getPublicUsername(user);
        if (user.photo == null || user.photo instanceof TLRPC.TL_userProfilePhotoEmpty) {
            s.photoId = 0;
        } else if (user.photo.personal) {
            s.photoId = -1;
        } else {
            s.photoId = user.photo.photo_id;
        }
        s.phone = user.phone;
        s.time = System.currentTimeMillis();
        synchronized (PENDING) {
            PENDING.add(s);
            if (!flushScheduled) {
                flushScheduled = true;
                QUEUE.postRunnable(AlfaProfileLog::flush, 500);
            }
        }
    }

    private static void flush() {
        ArrayList<Snapshot> batch;
        synchronized (PENDING) {
            batch = new ArrayList<>(PENDING);
            PENDING.clear();
            flushScheduled = false;
        }
        if (batch.isEmpty()) return;
        AlfaProfileLog log = getInstance();
        SQLiteDatabase db;
        try {
            db = log.getWritableDatabase();
        } catch (Throwable e) {
            FileLog.e(e);
            return;
        }
        boolean added = false;
        db.beginTransaction();
        try {
            for (Snapshot s : batch) {
                added |= apply(db, s);
            }
            db.setTransactionSuccessful();
        } catch (Throwable e) {
            FileLog.e(e);
        } finally {
            try {
                db.endTransaction();
            } catch (Throwable ignore) {
            }
        }
        if (added) {
            AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.alfaProfileLogUpdated));
        }
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim();
    }

    private static String fullName(String first, String last) {
        String f = norm(first);
        String l = norm(last);
        if (f.isEmpty()) return l;
        if (l.isEmpty()) return f;
        return f + " " + l;
    }

    /** @return jurnalga yangi yozuv qo'shildimi */
    private static boolean apply(SQLiteDatabase db, Snapshot s) {
        String[] key = new String[]{String.valueOf(s.account), String.valueOf(s.userId)};
        Cursor c = db.rawQuery("SELECT contact, first, last, username, photo_id, phone FROM snapshot WHERE account=? AND user_id=?", key);
        boolean exists = false;
        boolean oldContact = false;
        String oldFirst = null, oldLast = null, oldUsername = null, oldPhone = null;
        long oldPhotoId = 0;
        if (c.moveToNext()) {
            exists = true;
            oldContact = c.getInt(0) != 0;
            oldFirst = c.getString(1);
            oldLast = c.getString(2);
            oldUsername = c.getString(3);
            oldPhotoId = c.getLong(4);
            oldPhone = c.getString(5);
        }
        c.close();

        boolean added = false;
        if (exists) {
            // ism — faqat ikkala holatda ham kontakt bo'lmasa (kontakt ismi = siz saqlagan nom)
            if (!s.contact && !oldContact) {
                String o = fullName(oldFirst, oldLast);
                String n = fullName(s.first, s.last);
                if (!o.equals(n) && !n.isEmpty()) {
                    insert(db, s, FIELD_NAME, o, n);
                    added = true;
                }
            }
            String ou = norm(oldUsername);
            String nu = norm(s.username);
            if (!ou.equalsIgnoreCase(nu)) {
                insert(db, s, FIELD_USERNAME, ou, nu);
                added = true;
            }
            if (s.photoId >= 0 && oldPhotoId >= 0 && s.photoId != oldPhotoId) {
                insert(db, s, s.photoId == 0 ? FIELD_PHOTO_REMOVED : FIELD_PHOTO, "", "");
                added = true;
            }
            String op = norm(oldPhone);
            String np = norm(s.phone);
            if (!op.isEmpty() && !np.isEmpty() && !op.equals(np)) {
                insert(db, s, FIELD_PHONE, op, np);
                added = true;
            }
        }
        // surat yangilanadi; noma'lum qiymatlar (shaxsiy rasm, yashirin raqam) eskisi bilan qoladi
        long photoToStore = s.photoId >= 0 ? s.photoId : (exists ? oldPhotoId : -1);
        String phoneToStore = !TextUtils.isEmpty(s.phone) ? s.phone : (exists ? oldPhone : null);
        db.execSQL("INSERT OR REPLACE INTO snapshot(account, user_id, contact, first, last, username, photo_id, phone) VALUES(?,?,?,?,?,?,?,?)",
                new Object[]{s.account, s.userId, s.contact ? 1 : 0, norm(s.first), norm(s.last), norm(s.username), photoToStore, phoneToStore});
        if (added) {
            trim(db, s.account);
        }
        return added;
    }

    private static void insert(SQLiteDatabase db, Snapshot s, String field, String oldValue, String newValue) {
        db.execSQL("INSERT INTO log(account, user_id, time, field, old_value, new_value) VALUES(?,?,?,?,?,?)",
                new Object[]{s.account, s.userId, s.time, field, oldValue, newValue});
    }

    private static void trim(SQLiteDatabase db, int account) {
        db.execSQL("DELETE FROM log WHERE account=? AND id NOT IN (SELECT id FROM log WHERE account=? ORDER BY time DESC LIMIT " + MAX_ROWS_PER_ACCOUNT + ")",
                new Object[]{account, account});
    }

    // ───────────────────────────────────────────── o'qish

    public interface Callback {
        void onLoaded(ArrayList<Entry> entries);
    }

    /** Jurnal (yangisi birinchi). userId = 0 — hamma odamlar. Natija UI oqimida. */
    public static void load(int account, long userId, Callback callback) {
        QUEUE.postRunnable(() -> {
            ArrayList<Entry> result = new ArrayList<>();
            try {
                SQLiteDatabase db = getInstance().getReadableDatabase();
                Cursor c;
                if (userId != 0) {
                    c = db.rawQuery("SELECT user_id, time, field, old_value, new_value FROM log WHERE account=? AND user_id=? ORDER BY time DESC LIMIT 300",
                            new String[]{String.valueOf(account), String.valueOf(userId)});
                } else {
                    c = db.rawQuery("SELECT user_id, time, field, old_value, new_value FROM log WHERE account=? ORDER BY time DESC LIMIT 500",
                            new String[]{String.valueOf(account)});
                }
                while (c.moveToNext()) {
                    Entry e = new Entry();
                    e.userId = c.getLong(0);
                    e.time = c.getLong(1);
                    e.field = c.getString(2);
                    e.oldValue = c.getString(3);
                    e.newValue = c.getString(4);
                    result.add(e);
                }
                c.close();
            } catch (Throwable e) {
                FileLog.e(e);
            }
            AndroidUtilities.runOnUIThread(() -> callback.onLoaded(result));
        });
    }

    /** Jurnalni tozalash (suratlar qoladi — keyingi o'zgarishlar yozilaveradi). */
    public static void clear(int account, Runnable done) {
        QUEUE.postRunnable(() -> {
            try {
                getInstance().getWritableDatabase().execSQL("DELETE FROM log WHERE account=?", new Object[]{account});
            } catch (Throwable e) {
                FileLog.e(e);
            }
            if (done != null) {
                AndroidUtilities.runOnUIThread(done);
            }
        });
    }

    /** Jurnal yozuvining matni ("username: @eski → @yangi"). */
    public static String describe(Entry e) {
        String o = e.oldValue == null ? "" : e.oldValue;
        String n = e.newValue == null ? "" : e.newValue;
        switch (e.field) {
            case FIELD_NAME:
                return AlfaCyrillic.ui("Ismini o'zgartirdi: ") + o + " → " + n;
            case FIELD_USERNAME:
                if (o.isEmpty()) return AlfaCyrillic.ui("Username qo'ydi: ") + "@" + n;
                if (n.isEmpty()) return AlfaCyrillic.ui("Username'ni olib tashladi: ") + "@" + o;
                return "@" + o + " → @" + n;
            case FIELD_PHOTO:
                return AlfaCyrillic.ui("Profil rasmini o'zgartirdi");
            case FIELD_PHOTO_REMOVED:
                return AlfaCyrillic.ui("Profil rasmini olib tashladi yoki yashirdi");
            case FIELD_PHONE:
                return AlfaCyrillic.ui("Telefon raqami o'zgardi: ") + "+" + o + " → +" + n;
            default:
                return e.field;
        }
    }
}
