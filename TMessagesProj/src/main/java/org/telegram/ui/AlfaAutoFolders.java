package org.telegram.ui;

import android.content.SharedPreferences;

import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.AlfaFeatures;
import org.telegram.messenger.AlfaLocalFolders;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

/**
 * AlfaGram — bir bosishda tayyor papkalar: Shaxsiy, Guruhlar, Kanallar, Botlar, O'qilmagan.
 * Papkalar faqat shu qurilmada saqlanadi (AlfaLocalFolders) — boshqa Telegram ilovalarida ko'rinmaydi.
 * Bir xil turdagi papka allaqachon bo'lsa — qayta yaratilmaydi.
 */
public class AlfaAutoFolders {

    public static final String BUTTON_TEXT = "Avto papkalar yaratish";
    private static final String PREF_DONE = "alfaLocalAutoFoldersDone";

    private static final int NO_ARCHIVE = MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED;
    private static final String[] NAMES = {"Shaxsiy", "Guruhlar", "Kanallar", "Botlar", "O'qilmagan"};
    private static final int[] FLAGS = {
            MessagesController.DIALOG_FILTER_FLAG_CONTACTS | MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS | NO_ARCHIVE,
            MessagesController.DIALOG_FILTER_FLAG_GROUPS | NO_ARCHIVE,
            MessagesController.DIALOG_FILTER_FLAG_CHANNELS | NO_ARCHIVE,
            MessagesController.DIALOG_FILTER_FLAG_BOTS | NO_ARCHIVE,
            MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS | MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ | NO_ARCHIVE,
    };

    // tur bo'yicha papkalarning odatiy nomlari (foydalanuvchi nomini o'zgartirgan bo'lsa — nom ko'rsatiladi)
    private static final String[] DEFAULT_NAMES = {
            "shaxsiy", "guruhlar", "kanallar", "botlar", "o'qilmagan", "o'qilmaganlar",
            "personal", "groups", "channels", "bots", "unread", "contacts", "non-contacts",
            "личные", "группы", "каналы", "боты", "непрочитанные", "контакты",
    };

    /**
     * Papka tabida nom o'rniga ko'rsatiladigan ikonka (0 — nom ko'rsatiladi).
     * Faqat "Hammasi" va tur bo'yicha (shaxsiy/guruh/kanal/bot/o'qilmagan) odatiy nomli papkalar uchun;
     * foydalanuvchi o'zi chat qo'shgan yoki nomlagan papkalarda nom qoladi.
     */
    public static int tabIcon(MessagesController.DialogFilter filter) {
        if (filter == null) {
            return 0;
        }
        if (filter.isDefault()) {
            return R.drawable.msg_msgbubble3;
        }
        if (filter.isChatlist() || !filter.alwaysShow.isEmpty() || !filter.neverShow.isEmpty() || !isDefaultName(filter.name)) {
            return 0;
        }
        final int all = MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS;
        final int type = filter.flags & (all | MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ);
        if (type == (all | MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ)) {
            return R.drawable.msg_markunread;
        } else if (type == (MessagesController.DIALOG_FILTER_FLAG_CONTACTS | MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS)
                || type == MessagesController.DIALOG_FILTER_FLAG_CONTACTS || type == MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS) {
            return R.drawable.msg_folders_private;
        } else if (type == MessagesController.DIALOG_FILTER_FLAG_GROUPS) {
            return R.drawable.msg_folders_groups;
        } else if (type == MessagesController.DIALOG_FILTER_FLAG_CHANNELS) {
            return R.drawable.msg_folders_channels;
        } else if (type == MessagesController.DIALOG_FILTER_FLAG_BOTS) {
            return R.drawable.msg_folders_bots;
        }
        return 0;
    }

    private static boolean isDefaultName(String name) {
        if (name == null) {
            return false;
        }
        String n = name.trim().toLowerCase().replace('ʻ', '\'').replace('‘', '\'').replace('’', '\'');
        for (String d : DEFAULT_NAMES) {
            if (d.equals(n) || AlfaCyrillic.toCyrillic(d).toLowerCase().equals(n)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sozlamalardagi tugma: avto papkalar o'chiq bo'lsa yoqiladi, yoqiq bo'lsa
     * o'chirib yuborilgan papkalar qayta yaratiladi.
     */
    public static void ask(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        b.setTitle(AlfaCyrillic.ui("Avto papkalar"));
        b.setMessage(AlfaCyrillic.ui("Chatlar ro'yxati tepasida 5 ta papka paydo bo'ladi: Shaxsiy, Guruhlar, Kanallar, Botlar va O'qilmagan. Ular faqat AlfaGram'da ko'rinadi — boshqa Telegram ilovalarida chiqmaydi. Bor papkalaringizga tegilmaydi."));
        b.setPositiveButton(AlfaCyrillic.ui("Yaratish"), (d, w) -> {
            int created;
            if (!AlfaFeatures.autoFolders) {
                AlfaFeatures.setFlag("autoFolders", true);
                created = countLocal(fragment.getMessagesController());
            } else {
                created = ensure(fragment.getCurrentAccount());
            }
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, AlfaCyrillic.ui(
                    created > 0 ? "Avto papkalar tayyor" : "Bu papkalar allaqachon bor")).show();
        });
        b.setNegativeButton(AlfaCyrillic.ui("Bekor qilish"), null);
        fragment.showDialog(b.create());
    }

    /** Server papkalari yuklandi: avto papkalar yoqiq bo'lsa — har akkauntda bir marta yaratiladi. */
    public static void onRemoteFiltersLoaded(int account) {
        if (!AlfaFeatures.autoFolders) {
            return;
        }
        SharedPreferences prefs = MessagesController.getMainSettings(account);
        if (prefs.getBoolean(PREF_DONE, false)) {
            return; // foydalanuvchi o'chirgan papkani qayta tiklamaymiz
        }
        if (ensure(account) >= 0) {
            prefs.edit().putBoolean(PREF_DONE, true).apply();
        }
    }

    /** Sozlamalardagi "Avto papkalar" switch'i. */
    public static void onToggled(boolean enabled) {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!UserConfig.isValidAccount(a)) {
                continue;
            }
            SharedPreferences prefs = MessagesController.getMainSettings(a);
            if (enabled) {
                if (ensure(a) >= 0) {
                    prefs.edit().putBoolean(PREF_DONE, true).apply();
                }
            } else {
                removeLocal(a);
                prefs.edit().putBoolean(PREF_DONE, false).apply();
            }
        }
    }

    /**
     * Yetishmayotgan avto papkalarni lokal yaratadi. Avval AlfaGram serverga yozib qo'ygan eski
     * avto papkalar (shu nom va turdagi) lokalga ko'chiriladi. Natija: yaratilganlar soni,
     * papkalar hali yuklanmagan bo'lsa -1.
     */
    static int ensure(int account) {
        MessagesController mc = MessagesController.getInstance(account);
        if (!mc.dialogFiltersLoaded || !UserConfig.getInstance(account).filtersLoaded) {
            return -1;
        }
        migrateServerFolders(account);
        int created = 0;
        for (int i = 0; i < FLAGS.length; i++) {
            if (mc.dialogFiltersById.get(AlfaLocalFolders.BASE_ID + i) != null || exists(mc, FLAGS[i], NAMES[i])) {
                continue;
            }
            MessagesController.DialogFilter filter = new MessagesController.DialogFilter();
            filter.id = AlfaLocalFolders.BASE_ID + i;
            filter.name = AlfaCyrillic.ui(NAMES[i]);
            filter.flags = FLAGS[i];
            filter.color = -1;
            filter.pendingUnreadCount = filter.unreadCount = -1;
            mc.addFilter(filter, false);
            MessagesStorage.getInstance(account).saveDialogFilter(filter, false, true);
            created++;
        }
        if (created > 0) {
            NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogFiltersUpdated);
        }
        return created;
    }

    /** Oldingi versiyalar avto papkalarni serverga yozgan — ularni serverdan olib, lokalga o'tkazamiz. */
    private static void migrateServerFolders(int account) {
        MessagesController mc = MessagesController.getInstance(account);
        ArrayList<MessagesController.DialogFilter> filters = new ArrayList<>(mc.getDialogFilters());
        for (MessagesController.DialogFilter f : filters) {
            if (f.isDefault() || AlfaLocalFolders.isLocal(f) || f.isChatlist() || !f.alwaysShow.isEmpty() || !f.neverShow.isEmpty() || f.color >= 0 || f.name == null) {
                continue;
            }
            for (int i = 0; i < FLAGS.length; i++) {
                if (f.flags == FLAGS[i] && (f.name.equalsIgnoreCase(NAMES[i]) || f.name.equalsIgnoreCase(AlfaCyrillic.toCyrillic(NAMES[i])))) {
                    TLRPC.TL_messages_updateDialogFilter req = new TLRPC.TL_messages_updateDialogFilter();
                    req.id = f.id;
                    ConnectionsManager.getInstance(account).sendRequest(req, null);
                    mc.removeFilter(f);
                    MessagesStorage.getInstance(account).deleteDialogFilter(f);
                    break;
                }
            }
        }
    }

    private static void removeLocal(int account) {
        MessagesController mc = MessagesController.getInstance(account);
        ArrayList<MessagesController.DialogFilter> filters = new ArrayList<>(mc.getDialogFilters());
        for (MessagesController.DialogFilter f : filters) {
            if (AlfaLocalFolders.isLocal(f)) {
                mc.removeFilter(f);
                MessagesStorage.getInstance(account).deleteDialogFilter(f);
            }
        }
    }

    private static int countLocal(MessagesController mc) {
        return AlfaLocalFolders.count(mc.getDialogFilters());
    }

    /** Arxiv tepasidagi tur tugmalari: birinchisi "Hammasi", qolganlari NAMES tartibida. */
    public static final String[] ARCHIVE_TABS = {"Hammasi", "Shaxsiy", "Guruhlar", "Kanallar", "Botlar", "O'qilmagan"};
    private static MessagesController.DialogFilter[] archiveCategoryFilters;

    /** Arxivdagi chat shu turga (0..4, NAMES tartibida) kiradimi. category < 0 — hammasi. */
    public static boolean matchesArchiveCategory(int account, int category, TLRPC.Dialog dialog) {
        if (category < 0 || category >= FLAGS.length) {
            return true;
        }
        if (archiveCategoryFilters == null) {
            archiveCategoryFilters = new MessagesController.DialogFilter[FLAGS.length];
            for (int i = 0; i < FLAGS.length; i++) {
                MessagesController.DialogFilter f = new MessagesController.DialogFilter();
                f.flags = FLAGS[i] & ~NO_ARCHIVE;
                archiveCategoryFilters[i] = f;
            }
        }
        return archiveCategoryFilters[category].includesDialog(org.telegram.messenger.AccountInstance.getInstance(account), dialog.id, dialog);
    }

    // Papka turini belgilovchi bitlar; arxiv/ovozsizni chiqarish farqi papkani boshqa qilmaydi
    private static final int TYPE_MASK = MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS | MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ;

    /** Shu turdagi (yoki shu nomdagi) papka bormi — masalan Telegram tavsiyasidan qo'shilgan "Shaxsiy" ham hisoblanadi. */
    private static boolean exists(MessagesController mc, int flags, String name) {
        ArrayList<MessagesController.DialogFilter> filters = mc.getDialogFilters();
        for (int i = 0; i < filters.size(); i++) {
            MessagesController.DialogFilter f = filters.get(i);
            if (f.isDefault()) {
                continue;
            }
            if (name != null && f.name != null && (f.name.equalsIgnoreCase(name) || f.name.equalsIgnoreCase(AlfaCyrillic.ui(name)))) {
                return true;
            }
            if ((f.flags & TYPE_MASK) == (flags & TYPE_MASK) && f.alwaysShow.isEmpty() && f.neverShow.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Telegram tavsiya qilgan papka allaqachon bormi (bo'lsa "Tavsiya etilgan" ro'yxatida ko'rsatilmaydi). */
    public static boolean isSuggestedCovered(MessagesController mc, TLRPC.DialogFilter s) {
        if (s == null || !s.include_peers.isEmpty() || !s.exclude_peers.isEmpty()) {
            return false;
        }
        int flags = 0;
        if (s.contacts) flags |= MessagesController.DIALOG_FILTER_FLAG_CONTACTS;
        if (s.non_contacts) flags |= MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS;
        if (s.groups) flags |= MessagesController.DIALOG_FILTER_FLAG_GROUPS;
        if (s.broadcasts) flags |= MessagesController.DIALOG_FILTER_FLAG_CHANNELS;
        if (s.bots) flags |= MessagesController.DIALOG_FILTER_FLAG_BOTS;
        if (s.exclude_read) flags |= MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ;
        return flags != 0 && exists(mc, flags, null);
    }
}
