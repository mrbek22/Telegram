package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.AlfaApkSettingsActivity;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * AlfaGram — APK himoyasi.
 * <ul>
 *   <li>Chatlardan kelgan APK fayllarni o'rnatish sukut bo'yicha BLOKLANGAN (sozlamalarda o'chiriladi).</li>
 *   <li>APK ichi telefonning o'zida tekshiriladi: xavfli ruxsatlar, soxta nom (bank/Telegram), aldovchi fayl nomi.</li>
 *   <li>Yuboruvchi bo'yicha xavf: kontakt emas, yangi akkaunt, forward.</li>
 *   <li>APK avtomatik yuklanmaydi; admin bo'lgan guruhlarda APK avto-o'chiriladi (ixtiyoriy).</li>
 *   <li>Shubhali havolalar (APK havola, niqoblangan domen, soxta bank/Telegram sayti) ogohlantiriladi.</li>
 * </ul>
 * Hech qanday ma'lumot tashqariga yuborilmaydi — hammasi qurilmada.
 */
public final class AlfaApkGuard {

    private AlfaApkGuard() {
    }

    private static final int RED = 0xFFE53935;

    /** "Baribir o'rnatish" bosilganda — shu fayl bir marta o'tkaziladi. */
    private static String bypassPath;
    /** "Baribir ochish" bosilgan havola. */
    private static String bypassUrl;

    // ───────────────────────────────────────────── APK aniqlash

    public static boolean isApkName(String name) {
        if (name == null) return false;
        String n = name.toLowerCase(Locale.US).trim();
        return n.endsWith(".apk") || n.endsWith(".xapk") || n.endsWith(".apks") || n.endsWith(".apkm");
    }

    public static boolean isApkDocument(TLRPC.Document document) {
        if (document == null) return false;
        if ("application/vnd.android.package-archive".equalsIgnoreCase(document.mime_type)) return true;
        return isApkName(FileLoader.getDocumentFileName(document));
    }

    /** Chat pufagida ko'rsatiladigan qizil yozuv (APK bo'lmasa null). */
    public static CharSequence bubbleLabel(TLRPC.Document document) {
        if (!isApkDocument(document)) return null;
        String text = "⚠ APK · " + AndroidUtilities.formatFileSize(document.size) + " · "
                + (AlfaFeatures.apkGuard ? "o'rnatish bloklangan" : "virus bo'lishi mumkin");
        SpannableString s = new SpannableString(AlfaCyrillic.ui(text));
        s.setSpan(new ForegroundColorSpan(RED), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    /** APK hech qachon avtomatik yuklanmaydi (himoya yoqilgan bo'lsa). */
    public static boolean noAutoDownload(TLRPC.Document document) {
        return AlfaFeatures.apkGuard && isApkDocument(document);
    }

    // ───────────────────────────────────────────── ochishni ushlash

    /**
     * APK ochilmoqchi bo'lganda chaqiriladi. true — ushlandi (ochilmaydi, oyna ko'rsatildi).
     *
     * @param proceed foydalanuvchi himoya o'chiq holatda "Baribir o'rnatish"ni bossa — qayta ochish
     */
    public static boolean intercept(Activity activity, MessageObject message, File file, String fileName, Runnable proceed) {
        String name = fileName;
        if ((name == null || name.isEmpty()) && file != null) name = file.getName();
        boolean apk = isApkName(name) || message != null && isApkDocument(message.getDocument());
        if (!apk || file == null || !file.exists()) return false;
        final String path = file.getAbsolutePath();
        if (path.equals(bypassPath)) {
            return false;
        }
        if (activity == null) {
            return AlfaFeatures.apkGuard;
        }
        final String finalName = name;
        final AlertDialog progress = new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER);
        try {
            progress.showDelayed(250);
        } catch (Throwable ignore) {
        }
        Utilities.globalQueue.postRunnable(() -> {
            final Report report = analyze(file, finalName, message);
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    progress.dismiss();
                } catch (Throwable ignore) {
                }
                showApkDialog(activity, report, file, () -> {
                    bypassPath = path;
                    AndroidUtilities.runOnUIThread(() -> bypassPath = null, 3000);
                    if (proceed != null) proceed.run();
                });
            });
        });
        return true;
    }

    // ───────────────────────────────────────────── tahlil

    private static final int LOW = 0, MEDIUM = 1, HIGH = 2;

    private static final class Report {
        int level = LOW;
        String appLabel;
        String packageName;
        final ArrayList<String> high = new ArrayList<>();
        final ArrayList<String> medium = new ArrayList<>();
        final ArrayList<String> sender = new ArrayList<>();

        void hi(String s) {
            if (!high.contains(s)) high.add(s);
            level = HIGH;
        }

        void med(String s) {
            if (!medium.contains(s)) medium.add(s);
            if (level < MEDIUM) level = MEDIUM;
        }
    }

    /** Odamlar ko'p aldanadigan nomlar: banklar, to'lov tizimlari, Telegram, davlat xizmatlari. */
    private static final String[][] BRANDS = {
            {"telegram", "Telegram"}, {"teleqram", "Telegram"}, {"alfagram", "AlfaGram"},
            {"click", "Click"}, {"payme", "Payme"}, {"uzum", "Uzum"}, {"paynet", "Paynet"},
            {"humo", "Humo"}, {"uzcard", "Uzcard"}, {"apelsin", "Apelsin"}, {"kapital", "Kapitalbank"},
            {"agrobank", "Agrobank"}, {"xalq", "Xalq banki"}, {"ipak", "Ipak Yo'li"}, {"hamkor", "Hamkorbank"},
            {"anorbank", "Anorbank"}, {"tbc", "TBC"}, {"asaka", "Asakabank"}, {"aloqa", "Aloqabank"},
            {"nbu", "Milliy bank"}, {"milliy", "Milliy bank"}, {"oson", "Oson"}, {"upay", "Upay"},
            {"mygov", "my.gov.uz"}, {"my gov", "my.gov.uz"}, {"soliq", "Soliq"}, {"yhxbb", "YHXBB"},
            {"jarima", "Jarima (YHXBB)"}, {"whatsapp", "WhatsApp"}, {"instagram", "Instagram"},
            {"play market", "Play Market"}, {"google play", "Google Play"}, {"sber", "Sber"}, {"kaspi", "Kaspi"},
            {"телеграм", "Telegram"}, {"клик", "Click"}, {"пейми", "Payme"}, {"узум", "Uzum"}, {"хумо", "Humo"},
            {"штраф", "Jarima"}, {"жарима", "Jarima"}, {"сбер", "Sber"},
    };

    /** Fayl nomidagi aldov so'zlari: rasm, video, sovg'a, taklifnoma... */
    private static final Pattern LURE_NAME = Pattern.compile(
            "(rasm|foto|photo|image|img|surat|video|vidio|sovg|sovga|gift|bonus|yutuq|aksiya|tabrik|taklifnoma|"
                    + "toy|to'y|chek|hujjat|document|pdf|scan|skan|jarima|shtraf|premium|stars|"
                    + "фото|видео|подар|бонус|сурат|расм|таклифнома|тўй|той|чек|ҳужжат|документ|штраф)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern DOUBLE_EXT = Pattern.compile(
            "\\.(jpe?g|png|gif|webp|heic|mp4|mov|avi|mkv|3gp|mp3|ogg|m4a|pdf|docx?|xlsx?|txt|zip|rar)\\s*\\.(apk|xapk|apks|apkm)$",
            Pattern.CASE_INSENSITIVE);

    private static Report analyze(File file, String fileName, MessageObject message) {
        Report r = new Report();
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);

        // 1) fayl nomi
        if (DOUBLE_EXT.matcher(lower).find()) {
            r.hi("Fayl nomi niqoblangan («" + fileName + "») — rasm yoki video ko'rinishida, aslida dastur");
        } else {
            String base = lower.replaceAll("\\.(apk|xapk|apks|apkm)$", "");
            if (LURE_NAME.matcher(base).find()) {
                r.hi("Fayl nomi aldovchi: rasm, video, sovg'a yoki hujjat deb ko'rsatilgan — aslida dastur");
            }
        }
        String brandByName = findBrand(lower);
        if (brandByName != null) {
            r.hi("O'zini «" + brandByName + "» deb ko'rsatyapti. Rasmiy ilovalar faqat Play Market'dan o'rnatiladi — Telegram orqali kelgan bunday fayl deyarli har doim soxta");
        }

        // 2) APK ichi
        try {
            inspectPackage(file, r);
        } catch (Throwable e) {
            FileLog.e(e);
        }

        // 3) yuboruvchi
        try {
            inspectSender(message, r);
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return r;
    }

    private static String findBrand(String text) {
        if (text == null) return null;
        String t = text.toLowerCase(Locale.ROOT);
        for (String[] b : BRANDS) {
            if (t.contains(b[0])) {
                return b[1];
            }
        }
        return null;
    }

    private static void inspectPackage(File file, Report r) {
        if (!file.getName().toLowerCase(Locale.US).endsWith(".apk") && !isPlainApk(file)) {
            return; // xapk/apks — zip to'plam, ichini ochmaymiz
        }
        PackageManager pm = ApplicationLoader.applicationContext.getPackageManager();
        int flags = PackageManager.GET_PERMISSIONS | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS;
        PackageInfo pi = pm.getPackageArchiveInfo(file.getAbsolutePath(), flags);
        if (pi == null) {
            pi = pm.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_PERMISSIONS);
        }
        if (pi == null) {
            r.med("Faylni tekshirib bo'lmadi — buzilgan yoki ataylab yashirilgan dastur");
            return;
        }
        r.packageName = pi.packageName;
        ApplicationInfo ai = pi.applicationInfo;
        if (ai != null) {
            ai.sourceDir = file.getAbsolutePath();
            ai.publicSourceDir = file.getAbsolutePath();
            try {
                CharSequence label = ai.loadLabel(pm);
                if (label != null) r.appLabel = label.toString();
            } catch (Throwable ignore) {
            }
        }

        String self = ApplicationLoader.applicationContext.getPackageName();
        if (self.equals(pi.packageName)) {
            r.hi("Bu fayl o'zini AlfaGram deb ko'rsatyapti. AlfaGram faqat Play Market orqali yangilanadi");
        }
        String brand = findBrand(r.appLabel);
        if (brand == null) brand = findBrand(pi.packageName);
        if (brand != null) {
            r.hi("Dastur o'zini «" + brand + "» deb atagan. Rasmiy bank va Telegram ilovalari APK fayl qilib yuborilmaydi — bu soxta");
        }

        String[] perms = pi.requestedPermissions;
        if (perms != null) {
            for (String p : perms) {
                if (p == null) continue;
                switch (p) {
                    case "android.permission.READ_SMS":
                    case "android.permission.RECEIVE_SMS":
                        r.hi("SMS xabarlaringizni o'qiy oladi — bank va Telegram kodlarini o'g'irlaydi");
                        break;
                    case "android.permission.SEND_SMS":
                        r.hi("Sizning nomingizdan SMS yubora oladi");
                        break;
                    case "android.permission.SYSTEM_ALERT_WINDOW":
                        r.med("Boshqa ilovalar ustida oyna ocha oladi (soxta bank oynasi)");
                        break;
                    case "android.permission.REQUEST_INSTALL_PACKAGES":
                        r.med("O'zi boshqa dasturlarni o'rnata oladi");
                        break;
                    case "android.permission.READ_CONTACTS":
                        r.med("Kontaktlaringizni o'qiy oladi (virusni ularga tarqatish uchun)");
                        break;
                    case "android.permission.CALL_PHONE":
                    case "android.permission.READ_CALL_LOG":
                    case "android.permission.PROCESS_OUTGOING_CALLS":
                    case "android.permission.ANSWER_PHONE_CALLS":
                        r.med("Qo'ng'iroqlaringizni boshqara oladi");
                        break;
                    case "android.permission.REQUEST_DELETE_PACKAGES":
                        r.med("Boshqa ilovalarni (antivirusni) o'chira oladi");
                        break;
                    default:
                        break;
                }
            }
        }
        if (pi.services != null) {
            for (ServiceInfo s : pi.services) {
                if (s == null || s.permission == null) continue;
                if ("android.permission.BIND_ACCESSIBILITY_SERVICE".equals(s.permission)) {
                    r.hi("Telefoningizni masofadan boshqara oladi (maxsus imkoniyatlar xizmati)");
                } else if ("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE".equals(s.permission)) {
                    r.hi("Barcha bildirishnomalaringizni (kodlarni ham) o'qiy oladi");
                }
            }
        }
        if (pi.receivers != null) {
            for (ActivityInfo a : pi.receivers) {
                if (a != null && "android.permission.BIND_DEVICE_ADMIN".equals(a.permission)) {
                    r.hi("Qurilma administratori bo'lishga urinadi — keyin o'chirib bo'lmaydi");
                }
            }
        }
    }

    private static boolean isPlainApk(File f) {
        // nomi .apk bo'lmasa ham ichida AndroidManifest.xml bo'lsa — oddiy APK
        try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(f)) {
            return z.getEntry("AndroidManifest.xml") != null;
        } catch (Throwable e) {
            return false;
        }
    }

    private static void inspectSender(MessageObject message, Report r) {
        if (message == null) return;
        if (message.isOut()) return;
        MessagesController mc = MessagesController.getInstance(message.currentAccount);
        long senderId = message.getSenderId();
        if (message.isForwarded()) {
            r.sender.add("Boshqa joydan forward qilingan");
        }
        if (senderId > 0) {
            TLRPC.User u = mc.getUser(senderId);
            if (u != null) {
                if (u.bot) {
                    r.sender.add("Bot yuborgan");
                } else if (!u.contact && !UserObject.isUserSelf(u)) {
                    r.sender.add("Yuboruvchi kontaktlaringizda yo'q");
                }
                if (u.verified) {
                    return;
                }
            }
            long created = AlfaAccountAge.estimate(senderId);
            long now = System.currentTimeMillis() / 1000L;
            if (created > 0 && now - created < 180L * 24 * 3600) {
                r.sender.add("Yuboruvchi akkaunti yaqinda ochilgan (" + AlfaAccountAge.format(senderId) + ")");
            }
        }
        if (message.getDialogId() < 0) {
            r.sender.add("Guruh yoki kanalda tarqatilgan");
        }
        if (r.sender.size() >= 2 && r.level < MEDIUM) {
            r.level = MEDIUM;
        }
    }

    // ───────────────────────────────────────────── oyna

    private static void showApkDialog(Activity activity, Report r, File file, Runnable proceed) {
        if (activity == null || activity.isFinishing()) return;
        StringBuilder sb = new StringBuilder();
        if (AlfaFeatures.apkGuard) {
            sb.append("AlfaGram himoyasi yoqilgan: chatlardan kelgan dasturlar (APK) o'rnatilmaydi.\n\n");
        }
        if (r.appLabel != null) {
            sb.append("Dastur: ").append(r.appLabel);
            if (r.packageName != null) sb.append(" (").append(r.packageName).append(")");
            sb.append("\n\n");
        }
        if (r.level == HIGH) {
            sb.append("🔴 Xavf YUQORI — bu deyarli aniq virus:\n");
        } else if (r.level == MEDIUM) {
            sb.append("🟠 Xavf o'rtacha:\n");
        } else {
            sb.append("🟢 Aniq xavfli belgi topilmadi. Lekin baribir faqat ishonchli odamdan kelgan dasturni o'rnating.\n");
        }
        for (String s : r.high) sb.append("• ").append(s).append('\n');
        for (String s : r.medium) sb.append("• ").append(s).append('\n');
        if (!r.sender.isEmpty()) {
            sb.append("\nYuboruvchi:\n");
            for (String s : r.sender) sb.append("• ").append(s).append('\n');
        }
        if (AlfaFeatures.apkGuard) {
            sb.append("\nO'rnatish kerak bo'lsa: AlfaGram sozlamalari → Xavfsizlik → «APK o'rnatishni bloklash»ni o'chiring.");
        } else {
            sb.append("\nVirus o'rnatilsa SMS kodlaringiz, bank kartangiz va Telegram akkauntingiz o'g'irlanadi.");
        }

        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle(AlfaCyrillic.ui(AlfaFeatures.apkGuard ? "🛡 APK o'rnatish bloklandi" : "⚠️ Dastur fayli (APK)"));
        b.setMessage(AlfaCyrillic.ui(sb.toString().trim()));
        if (AlfaFeatures.apkGuard) {
            b.setPositiveButton(AlfaCyrillic.ui("Tushunarli"), null);
            b.setNegativeButton(AlfaCyrillic.ui("Sozlamalar"), (d, w) -> openSettings());
        } else {
            b.setPositiveButton(AlfaCyrillic.ui("Bekor qilish"), null);
            b.setNegativeButton(AlfaCyrillic.ui("Baribir o'rnatish"), (d, w) -> {
                if (proceed != null) proceed.run();
            });
        }
        b.setNeutralButton(AlfaCyrillic.ui("Faylni o'chirish"), (d, w) -> {
            try {
                if (file != null && file.delete()) {
                    AndroidUtilities.runOnUIThread(() -> {
                        try {
                            BaseFragment f = LaunchActivity.getSafeLastFragment();
                            if (f != null) {
                                org.telegram.ui.Components.BulletinFactory.of(f).createSimpleBulletin(
                                        R.raw.ic_delete, AlfaCyrillic.ui("Fayl telefondan o'chirildi")).show();
                            }
                        } catch (Throwable ignore) {
                        }
                    });
                }
            } catch (Throwable ignore) {
            }
        });
        AlertDialog dialog = b.create();
        try {
            dialog.show();
            android.view.View btn = dialog.getButton(AlfaFeatures.apkGuard ? android.content.DialogInterface.BUTTON_NEUTRAL : android.content.DialogInterface.BUTTON_NEGATIVE);
            if (btn instanceof android.widget.TextView) {
                ((android.widget.TextView) btn).setTextColor(Theme.getColor(Theme.key_text_RedBold));
            }
            if (!AlfaFeatures.apkGuard) {
                android.view.View del = dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL);
                if (del instanceof android.widget.TextView) {
                    ((android.widget.TextView) del).setTextColor(Theme.getColor(Theme.key_text_RedBold));
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void openSettings() {
        try {
            BaseFragment f = LaunchActivity.getSafeLastFragment();
            if (f != null) {
                f.presentFragment(new AlfaApkSettingsActivity());
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    /** Sozlamalarda himoyani o'chirishdan oldingi ogohlantirish. */
    public static void confirmDisable(BaseFragment fragment, Runnable onDisable) {
        if (fragment == null || fragment.getParentActivity() == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        b.setTitle(AlfaCyrillic.ui("Himoyani o'chirasizmi?"));
        b.setMessage(AlfaCyrillic.ui("O'chirsangiz, chatlardan kelgan APK fayllarni o'rnatish mumkin bo'ladi.\n\n"
                + "Hozir Telegram'da «Rasmlar.apk», «Video.apk», «Taklifnoma.apk» kabi viruslar tarqalmoqda: ular SMS kodlaringizni, "
                + "bank kartangizni va Telegram akkauntingizni o'g'irlaydi, keyin kontaktlaringizga o'zini yuboradi.\n\n"
                + "Faqat kerakli dasturni o'rnatib bo'lgach, himoyani qayta yoqing."));
        b.setPositiveButton(AlfaCyrillic.ui("Yoqiq qolsin"), null);
        b.setNegativeButton(AlfaCyrillic.ui("O'chirish"), (d, w) -> {
            if (onDisable != null) onDisable.run();
        });
        AlertDialog dialog = b.create();
        fragment.showDialog(dialog);
        android.view.View btn = dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE);
        if (btn instanceof android.widget.TextView) {
            ((android.widget.TextView) btn).setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    // ───────────────────────────────────────────── guruh filtri

    private static android.content.SharedPreferences groupPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("alfa_apk_groups", 0);
    }

    private static String groupKey(int account, long chatId) {
        return UserConfig.getInstance(account).getClientUserId() + "_" + chatId;
    }

    /** Shu guruhda APK filtri yoqilganmi (guruh sozlamasidan, faqat shu telefonda). */
    public static boolean isGroupFilterOn(int account, long chatId) {
        try {
            return groupPrefs().getBoolean(groupKey(account, chatId), false);
        } catch (Throwable e) {
            return false;
        }
    }

    public static void setGroupFilter(int account, long chatId, boolean on) {
        try {
            android.content.SharedPreferences.Editor e = groupPrefs().edit();
            if (on) {
                e.putBoolean(groupKey(account, chatId), true);
            } else {
                e.remove(groupKey(account, chatId));
            }
            e.apply();
        } catch (Throwable ignore) {
        }
    }

    /** Guruh sozlamasida tugma ko'rsatiladimi: guruh (kanal emas) va xabar o'chirish huquqi bor. */
    public static boolean canUseGroupFilter(TLRPC.Chat chat) {
        return chat != null && !ChatObject.isChannelAndNotMegaGroup(chat)
                && ChatObject.canUserDoAdminAction(chat, ChatObject.ACTION_DELETE_MESSAGES);
    }

    /** Filtr yoqilgan guruhlarda oddiy a'zolar yuborgan APK'ni hamma uchun o'chirish. UI oqimida. */
    public static void onNewMessages(int account, long dialogId, ArrayList<MessageObject> messages) {
        if (dialogId >= 0 || messages == null || messages.isEmpty()) return;
        if (!isGroupFilterOn(account, -dialogId)) return;
        try {
            MessagesController mc = MessagesController.getInstance(account);
            long chatId = -dialogId;
            TLRPC.Chat chat = mc.getChat(chatId);
            if (!canUseGroupFilter(chat)) return;
            ArrayList<Integer> ids = null;
            int topicId = 0;
            for (MessageObject m : messages) {
                if (m == null || m.isOut() || m.messageOwner == null || m.getId() <= 0) continue;
                if (!isApkDocument(m.getDocument())) continue;
                long sender = m.getSenderId();
                if (sender <= 0) continue; // anonim admin yoki kanal nomidan
                if (mc.getAdminRank(chatId, sender) != null) continue; // adminlarga tegmaymiz
                if (ids == null) ids = new ArrayList<>();
                ids.add(m.getId());
            }
            if (ids == null) return;
            FileLog.d("AlfaApkGuard: guruhda APK o'chirildi " + ids + " chat=" + chatId);
            mc.deleteMessages(ids, null, null, dialogId, topicId, true, 0);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    // ───────────────────────────────────────────── havolalar

    private static final String[][] OFFICIAL = {
            {"telegram", "telegram.org", "t.me", "telegram.me", "telegra.ph", "telesco.pe", "fragment.com", "tdesktop.com", "core.telegram.org"},
            {"click", "click.uz"},
            {"payme", "payme.uz", "paycom.uz"},
            {"uzum", "uzum.uz", "uzumbank.uz", "uzum.com"},
            {"paynet", "paynet.uz"},
            {"humo", "humocard.uz", "humo.uz"},
            {"uzcard", "uzcard.uz"},
            {"apelsin", "apelsin.uz"},
            {"kapitalbank", "kapitalbank.uz"},
            {"agrobank", "agrobank.uz"},
            {"hamkorbank", "hamkorbank.uz"},
            {"anorbank", "anorbank.uz"},
            {"soliq", "soliq.uz"},
            {"mygov", "my.gov.uz"},
            {"whatsapp", "whatsapp.com", "whatsapp.net", "wa.me"},
            {"instagram", "instagram.com"},
            {"alfagram", "anonimbot.uz", "alfagram.uz"},
    };

    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern LURE_URL = Pattern.compile(
            "(gift|sovg|bonus|premium|stars|yutuq|aksiya|free|bepul|prize|airdrop|nft|promo|reward|claim|verify|login|jarima)",
            Pattern.CASE_INSENSITIVE);
    private static final String[] BAD_TLD = {"top", "xyz", "icu", "buzz", "click", "rest", "monster", "cfd", "sbs", "online",
            "site", "live", "fun", "pw", "tk", "ml", "ga", "cf", "gq", "cyou", "lat", "bond", "today", "info", "su", "vip", "work"};

    /** Havola xavfi sabablari (bo'sh — xavfsiz). */
    public static ArrayList<String> checkUrl(Uri uri) {
        ArrayList<String> out = new ArrayList<>();
        if (uri == null) return out;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.US);
        if (!scheme.equals("http") && !scheme.equals("https")) return out;
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.US);
        if (host.startsWith("www.")) host = host.substring(4);
        if (host.isEmpty()) return out;
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.US);

        if (isApkName(path)) {
            out.add("Havola dastur (APK) yuklab beradi");
        }
        if (host.startsWith("xn--") || host.contains(".xn--")) {
            out.add("Manzil boshqa alifbo harflari bilan niqoblangan (" + host + ")");
        }
        if (IPV4.matcher(host).matches()) {
            out.add("Manzil sayt nomi emas, raqamli IP");
        }
        String[] labels = host.split("[.-]");
        for (String[] o : OFFICIAL) {
            if (!hostHasBrand(labels, o[0])) continue;
            boolean official = false;
            for (int i = 1; i < o.length; i++) {
                if (host.equals(o[i]) || host.endsWith("." + o[i])) {
                    official = true;
                    break;
                }
            }
            if (o[0].equals("mygov") && host.endsWith(".gov.uz")) official = true;
            if (!official) {
                out.add("Sayt o'zini «" + o[0] + "» qilib ko'rsatyapti, lekin rasmiy manzil emas (" + host + ")");
            }
            break;
        }
        int dot = host.lastIndexOf('.');
        String tld = dot >= 0 ? host.substring(dot + 1) : "";
        boolean badTld = false;
        for (String t : BAD_TLD) {
            if (t.equals(tld)) {
                badTld = true;
                break;
            }
        }
        if (badTld && LURE_URL.matcher(host + path).find()) {
            out.add("Sovg'a/bonus/premium va'da qiluvchi shubhali sayt");
        }
        return out;
    }

    private static boolean hostHasBrand(String[] labels, String token) {
        for (String l : labels) {
            if (l.isEmpty()) continue;
            if (token.equals("click")) {
                // clickup, clickhouse kabi boshqa saytlar bilan adashmaslik uchun — faqat aniq shakllar
                if (l.equals("click") || l.equals("clickuz") || l.equals("myclick") || l.equals("clickpay") || l.equals("uzclick")) return true;
            } else if (l.contains(token)) {
                return true;
            }
        }
        return false;
    }

    /** Browser.openUrl boshida. true — ushlandi (ogohlantirish ko'rsatildi). */
    public static boolean interceptUrl(Context context, Uri uri, Runnable proceed) {
        if (!AlfaFeatures.linkGuard || uri == null) return false;
        String url = uri.toString();
        if (url.equals(bypassUrl)) {
            bypassUrl = null;
            return false;
        }
        ArrayList<String> reasons;
        try {
            reasons = checkUrl(uri);
        } catch (Throwable e) {
            return false;
        }
        if (reasons.isEmpty()) return false;
        Activity activity = findActivity(context);
        if (activity == null) activity = LaunchActivity.instance;
        if (activity == null || activity.isFinishing()) return false;
        StringBuilder sb = new StringBuilder();
        sb.append(url.length() > 120 ? url.substring(0, 120) + "…" : url).append("\n\n");
        for (String s : reasons) sb.append("• ").append(s).append('\n');
        sb.append("\nBunday saytlar Telegram akkauntingiz, karta ma'lumotlaringiz yoki SMS kodingizni o'g'irlashi mumkin. Kodni hech kimga bermang.");
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle(AlfaCyrillic.ui("⚠️ Shubhali havola"));
        b.setMessage(AlfaCyrillic.ui(sb.toString()));
        b.setPositiveButton(AlfaCyrillic.ui("Ochmaslik"), null);
        b.setNegativeButton(AlfaCyrillic.ui("Baribir ochish"), (d, w) -> {
            bypassUrl = url;
            if (proceed != null) proceed.run();
        });
        AlertDialog dialog = b.create();
        try {
            dialog.show();
            android.view.View btn = dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE);
            if (btn instanceof android.widget.TextView) {
                ((android.widget.TextView) btn).setTextColor(Theme.getColor(Theme.key_text_RedBold));
            }
        } catch (Throwable e) {
            FileLog.e(e);
            return false;
        }
        return true;
    }

    private static Activity findActivity(Context c) {
        while (c != null) {
            if (c instanceof Activity) return (Activity) c;
            if (c instanceof ContextWrapper) {
                Context base = ((ContextWrapper) c).getBaseContext();
                if (base == c) break;
                c = base;
            } else {
                break;
            }
        }
        return null;
    }
}
