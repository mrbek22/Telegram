package org.telegram.messenger;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.telegram.tgnet.tl.TL_stories;

import java.util.ArrayList;

/**
 * AlfaGram — Story ko'ruvchilar tarixi.
 * Telegram Story'ni 24 soatdan keyin o'chiradi va ko'ruvchilar ro'yxati yo'qoladi.
 * Bu klass ularni o'z (alohida) SQLite bazasiga saqlaydi — Story o'chsa ham qoladi.
 * Telegram'ning asosiy bazasiga (MessagesStorage) tegmaydi.
 */
public class AlfaStoryViews extends SQLiteOpenHelper {

    private static volatile AlfaStoryViews instance;

    public static AlfaStoryViews getInstance() {
        if (instance == null) {
            synchronized (AlfaStoryViews.class) {
                if (instance == null) {
                    instance = new AlfaStoryViews(ApplicationLoader.applicationContext);
                }
            }
        }
        return instance;
    }

    private AlfaStoryViews(Context context) {
        super(context, "alfa_story_views.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS views (" +
                "account INTEGER, story_id INTEGER, user_id INTEGER, view_date INTEGER, saved_at INTEGER, " +
                "PRIMARY KEY(account, story_id, user_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS stories (" +
                "account INTEGER, story_id INTEGER, first_saved INTEGER, last_saved INTEGER, " +
                "PRIMARY KEY(account, story_id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /** Ko'ruvchilar javobini (getStoryViewsList natijasi) saqlaydi. */
    public void save(int account, int storyId, TL_stories.StoryViewsList res) {
        if (res == null || res.views == null || storyId == 0) {
            return;
        }
        try {
            SQLiteDatabase db = getWritableDatabase();
            long now = System.currentTimeMillis();
            db.beginTransaction();
            try {
                db.execSQL("INSERT OR IGNORE INTO stories(account, story_id, first_saved, last_saved) VALUES(?,?,?,?)",
                        new Object[]{account, storyId, now, now});
                db.execSQL("UPDATE stories SET last_saved=? WHERE account=? AND story_id=?",
                        new Object[]{now, account, storyId});
                for (int i = 0; i < res.views.size(); i++) {
                    TL_stories.StoryView v = res.views.get(i);
                    if (v == null || v.user_id == 0) {
                        continue;
                    }
                    db.execSQL("INSERT OR IGNORE INTO views(account, story_id, user_id, view_date, saved_at) VALUES(?,?,?,?,?)",
                            new Object[]{account, storyId, v.user_id, (long) v.date, now});
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static class ViewerRecord {
        public long userId;
        public int viewDate;
    }

    /** Saqlangan Story ID'lari (oxirgi saqlangani birinchi). */
    public ArrayList<Integer> getStoryIds(int account) {
        ArrayList<Integer> result = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT story_id FROM stories WHERE account=? ORDER BY last_saved DESC", new String[]{String.valueOf(account)});
            while (c.moveToNext()) {
                result.add(c.getInt(0));
            }
            c.close();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return result;
    }

    /** Bitta Story uchun saqlangan ko'ruvchilar (oxirgi ko'rgani birinchi). */
    public ArrayList<ViewerRecord> getViewers(int account, int storyId) {
        ArrayList<ViewerRecord> result = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT user_id, view_date FROM views WHERE account=? AND story_id=? ORDER BY view_date DESC",
                    new String[]{String.valueOf(account), String.valueOf(storyId)});
            while (c.moveToNext()) {
                ViewerRecord r = new ViewerRecord();
                r.userId = c.getLong(0);
                r.viewDate = c.getInt(1);
                result.add(r);
            }
            c.close();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return result;
    }

    /** Umumiy statistika: nechta Story, nechta noyob ko'ruvchi saqlangan. */
    public int[] getStats(int account) {
        int stories = 0;
        int viewers = 0;
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c1 = db.rawQuery("SELECT COUNT(*) FROM stories WHERE account=?", new String[]{String.valueOf(account)});
            if (c1.moveToNext()) stories = c1.getInt(0);
            c1.close();
            Cursor c2 = db.rawQuery("SELECT COUNT(DISTINCT user_id) FROM views WHERE account=?", new String[]{String.valueOf(account)});
            if (c2.moveToNext()) viewers = c2.getInt(0);
            c2.close();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return new int[]{stories, viewers};
    }
}
