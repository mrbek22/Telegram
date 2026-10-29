package org.telegram.ui;

import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

/**
 * AlfaGram — bir bosishda tayyor papkalar: Shaxsiy, Guruhlar, Kanallar, Botlar, O'qilmagan.
 * Papkalar Telegram serverida saqlanadi (boshqa qurilmalarda ham ko'rinadi).
 * Bir xil turdagi papka allaqachon bo'lsa — qayta yaratilmaydi.
 */
public class AlfaAutoFolders {

    public static final String BUTTON_TEXT = "Avto papkalar yaratish";

    private static final int NO_ARCHIVE = MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED;
    private static final String[] NAMES = {"Shaxsiy", "Guruhlar", "Kanallar", "Botlar", "O'qilmagan"};
    private static final int[] FLAGS = {
            MessagesController.DIALOG_FILTER_FLAG_CONTACTS | MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS | NO_ARCHIVE,
            MessagesController.DIALOG_FILTER_FLAG_GROUPS | NO_ARCHIVE,
            MessagesController.DIALOG_FILTER_FLAG_CHANNELS | NO_ARCHIVE,
            MessagesController.DIALOG_FILTER_FLAG_BOTS | NO_ARCHIVE,
            MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS | MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ | NO_ARCHIVE,
    };

    public static void ask(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        b.setTitle(AlfaCyrillic.ui("Avto papkalar"));
        b.setMessage(AlfaCyrillic.ui("Chatlar ro'yxati tepasida 5 ta papka paydo bo'ladi: Shaxsiy, Guruhlar, Kanallar, Botlar va O'qilmagan. Bor papkalaringizga tegilmaydi."));
        b.setPositiveButton(AlfaCyrillic.ui("Yaratish"), (d, w) -> create(fragment));
        b.setNegativeButton(AlfaCyrillic.ui("Bekor qilish"), null);
        fragment.showDialog(b.create());
    }

    private static void create(BaseFragment fragment) {
        MessagesController mc = fragment.getMessagesController();
        ArrayList<Integer> todo = new ArrayList<>();
        for (int i = 0; i < FLAGS.length; i++) {
            if (!exists(mc, FLAGS[i])) {
                todo.add(i);
            }
        }
        if (todo.isEmpty()) {
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, AlfaCyrillic.ui("Bu papkalar allaqachon bor")).show();
            return;
        }
        // Bepul akkauntda papkalar soni cheklangan ("Barcha chatlar" hisobga kirmaydi)
        int limit = fragment.getUserConfig().isPremium() ? mc.dialogFiltersLimitPremium : mc.dialogFiltersLimitDefault;
        int free = Math.max(0, limit - Math.max(0, mc.getDialogFilters().size() - 1));
        if (free == 0) {
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.error, AlfaCyrillic.ui("Papkalar soni limitga yetgan — avval keraksizini o'chiring")).show();
            return;
        }
        int skipped = Math.max(0, todo.size() - free);
        while (todo.size() > free) {
            todo.remove(todo.size() - 1);
        }
        createNext(fragment, todo, 0, skipped);
    }

    private static void createNext(BaseFragment fragment, ArrayList<Integer> todo, int pos, int skipped) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        if (pos >= todo.size()) {
            fragment.getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            String msg = "Yaratildi: " + todo.size() + " ta papka";
            if (skipped > 0) {
                msg += " (" + skipped + " tasi limit sababli qo'shilmadi)";
            }
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, AlfaCyrillic.ui(msg)).show();
            return;
        }
        MessagesController mc = fragment.getMessagesController();
        int idx = todo.get(pos);
        MessagesController.DialogFilter filter = new MessagesController.DialogFilter();
        filter.name = AlfaCyrillic.ui(NAMES[idx]);
        filter.id = 2;
        while (mc.dialogFiltersById.get(filter.id) != null) {
            filter.id++;
        }
        filter.order = mc.getDialogFilters().size();
        filter.pendingUnreadCount = filter.unreadCount = -1;
        filter.flags = FLAGS[idx];
        filter.color = -1;
        // progress=false: filtr darhol mahalliy qo'shiladi, server javobidan keyin keyingisi yaratiladi
        FilterCreateActivity.saveFilterToServer(filter, filter.flags, filter.name, filter.entities, false, filter.color,
                filter.alwaysShow, filter.neverShow, filter.pinnedDialogs, true, false, true, true, false, fragment,
                () -> createNext(fragment, todo, pos + 1, skipped));
    }

    private static boolean exists(MessagesController mc, int flags) {
        ArrayList<MessagesController.DialogFilter> filters = mc.getDialogFilters();
        for (int i = 0; i < filters.size(); i++) {
            MessagesController.DialogFilter f = filters.get(i);
            if (!f.isDefault() && f.flags == flags && f.alwaysShow.isEmpty() && f.neverShow.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
