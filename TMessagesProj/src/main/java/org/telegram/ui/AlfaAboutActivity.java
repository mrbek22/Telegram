package org.telegram.ui;

import android.view.View;

import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.R;
import org.telegram.ui.Components.IconBackgroundColors;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * AlfaGram — «AlfaGram haqida»: har bir funksiya ikonka bilan va YOQISH / QANDAY ISHLAYDI / O'CHIRISH izohi
 * (iOS'dagi sahifa bilan bir xil). Faqat Android'da haqiqatan bor funksiyalar; yangisi qo'shilganda shu yerga ham.
 */
public class AlfaAboutActivity extends UniversalFragment {

    /** ikonka, rang, sarlavha, yoqish, qanday ishlaydi, o'chirish (null — yo'q) */
    private static final Object[][] FEATURES = {
            {R.drawable.alfa_ic_ghost, IconBackgroundColors.GRAY, "Ghost rejim",
                    "Chatlar ro'yxati tepasidagi 👻 tugmasini bosing yoki Sozlamalar → AlfaGram → Maxfiylik va Ghost.",
                    "Siz xabarlarni o'qiysiz, lekin suhbatdoshga «o'qildi», «yozyapti...» va «onlayn» belgilari bormaydi.",
                    "👻 tugmasini qayta bosing yoki o'sha bandlarni o'chiring."},
            {R.drawable.alfa_ic_deleted, IconBackgroundColors.RED, "O'chirilgan xabarlar",
                    "Sozlamalar → AlfaGram → Xabarlar → «O'chirilgan xabarlarni saqlash» (sukut bo'yicha yoqilgan).",
                    "Suhbatdosh xabarni o'chirsa ham, u sizning chatingizda 🗑 belgisi bilan qoladi.",
                    "O'sha bandni o'chiring."},
            {R.drawable.alfa_ic_edited, IconBackgroundColors.GREEN, "Tahrir tarixi",
                    "Sozlamalar → AlfaGram → Xabarlar → «Tahrir tarixini yozish» (sukut bo'yicha yoqilgan).",
                    "Tahrirlangan xabarni bosib turing → tarix: barcha oldingi matnlari ko'rinadi.",
                    null},
            {R.drawable.alfa_ic_stealth, IconBackgroundColors.PURPLE, "Story'larni anonim ko'rish",
                    "Sukut bo'yicha yoqiq. Sozlamalar → AlfaGram → Maxfiylik va Ghost → «Anonim story ko'rish».",
                    "Story muallifi siz ko'rganingizni bilmaydi — ko'rganlar ro'yxatida chiqmaysiz.",
                    null},
            {R.drawable.alfa_ic_calls, IconBackgroundColors.GREEN, "Qo'ng'iroqdan oldin so'rash",
                    "Sukut bo'yicha yoqilgan: Sozlamalar → AlfaGram → Xabarlar → «Qo'ng'iroqdan oldin so'rash».",
                    "Qo'ng'iroq tugmasini bossangiz, avval «Qo'ng'iroq qilinsinmi?» deb so'raladi — tasodifiy qo'ng'iroq ketmaydi.",
                    "O'sha bandni o'chiring."},
            {R.drawable.alfa_ic_lock, IconBackgroundColors.BLUE, "Chat qulfi",
                    "Chat ichida ⋮ → «Chatni qulflash».",
                    "Chat barmoq izi, yuz yoki telefon PIN'i bilan ochiladi. Ro'yxatda va bildirishnomada xabar matni ko'rinmaydi. Ilovadan chiqsangiz, yana qulflanadi.",
                    "Chat ichida ⋮ → «Qulfni olib tashlash»."},
            {R.drawable.alfa_ic_privacy, IconBackgroundColors.PURPLE, "Yashirin chatlar",
                    "Sozlamalar → AlfaGram → Maxfiylik → «Yashirin chatlar»: qulf turini (4 raqamli PIN yoki parol) tanlab kod qo'ying. Keyin chat ichida ⋮ → «Chatni yashirish».",
                    "Yashirilgan chat ro'yxatdan va uzatish oynasidan yo'qoladi va ovozsiz bo'ladi. Ko'rish uchun «AlfaGram» sarlavhasini bosib turing va kodni kiriting; ilovadan chiqsangiz, yana yashirinadi.",
                    "Chat ichida ⋮ → «Yashirishdan chiqarish», yoki sozlamada kod bilan «O'chirish»."},
            {R.drawable.alfa_ic_language, IconBackgroundColors.GREEN, "Yangi alifbo (Ş Ç Ö Ğ)",
                    "Sozlamalar → AlfaGram → Ko'rinish va til → «Yangi alifbo» (ilova matnlari) va «Yangi alifboda yuborilsin» (xabarlar).",
                    "sh, ch, o', g' → ş, ç, ö, ğ. Siz odatdagidek yozasiz, xabar yangi alifboda ketadi. Havola, @username va teglar o'zgarmaydi. Kirill bilan bir vaqtda faqat bittasi yoqiladi.",
                    "O'sha bandlarni o'chiring."},
            {R.drawable.alfa_ic_contacts, IconBackgroundColors.BLUE_DEEP, "Maxfiy va to'xtatilgan akkauntlar",
                    "Sozlamalardagi akkauntni bosib turing → «Yashirish» yoki «Vaqtincha to'xtatish»; hammasi Sozlamalar → AlfaGram → Maxfiylik → «Maxfiy akkauntlar» yoki «Akkaunt qo'shish»ni bosib turing.",
                    "Yashirilgan akkaunt ro'yxatda va almashtirishda ko'rinmaydi, unga faqat «Maxfiy akkauntlar»dan kirasiz. To'xtatilgan akkauntdan bildirishnoma kelmaydi.",
                    "Akkauntni yana bosib turing → «Qayta yoqish», yoki «Maxfiy akkauntlar»da o'chiring."},
            {R.drawable.alfa_ic_chats, IconBackgroundColors.CYAN, "Chatni eksport qilish",
                    "Chat ichida ⋮ → «Chatni eksport qilish».",
                    "Oxirgi 5000 tagacha xabar HTML sahifa yoki TXT fayl bo'lib tayyorlanadi; uni saqlaysiz yoki istalgan joyga yuborasiz.",
                    null},
            {R.drawable.alfa_ic_stealth, IconBackgroundColors.ORANGE, "Dumaloq video kamerasi",
                    "Sozlamalar → AlfaGram → Xabarlar → «Dumaloq video orqa kamerada» yoki «har safar kamerani so'rash».",
                    "Doim old, doim orqa kamera yoki har safar so'rash: yozish tugmasini kamera rejimiga o'tkazganingizda «Old kamera / Orqa kamera» tanlanadi. Yozish paytida kamerani almashtirish tugmasi ham bor.",
                    "O'sha bandlarni o'chiring."},
            {R.drawable.alfa_ic_stealth, IconBackgroundColors.ORANGE_DEEP, "Galereyadan dumaloq video",
                    "Chatda 📎 → «Dumaloq video» → videoni tanlang.",
                    "1 daqiqagacha bo'lgan video o'rtasidan kvadrat qilib kesiladi va xuddi kamerada yozilgandek dumaloq video bo'lib ketadi. Uzunroq video qabul qilinmaydi.",
                    null},
            {R.drawable.alfa_ic_settings, IconBackgroundColors.PURPLE, "Bot token bilan kirish",
                    "Kirish ekranida (telefon raqami so'raladigan joy) o'ng tepadagi robot tugmasini bosing: ochilgan oynaga @BotFather bergan tokenni kiriting va «Tizimga kirish»ni bosing.",
                    "Ilova bot nomidan ochiladi. Telegram botlarga eski chatlar va tarixni bermaydi — faqat kirgandan keyingi yangi xabarlar keladi.",
                    "Bot akkauntidan chiqing."},
            {R.drawable.alfa_ic_contacts, IconBackgroundColors.GREEN, "Kontaktlardan guruh va kanal",
                    "Kontaktlar tabining tepasidagi «Yangi guruh» yoki «Yangi kanal» qatorini bosing.",
                    "Guruh uchun a'zolarni tanlab nom berasiz, kanal uchun nom va tavsif kiritasiz.",
                    null},
            {R.drawable.alfa_ic_folders, IconBackgroundColors.ORANGE_DEEP, "Boshiga o'tish",
                    "Profil yoki kanal sahifasida ⋮ → «Boshiga o'tish».",
                    "Chat yoki kanal eng birinchi xabaridan ochiladi — uzoq tepaga surish shart emas.",
                    null},
            {R.drawable.alfa_ic_edited, IconBackgroundColors.ORANGE, "Yozdi, yubormadi",
                    "Sozlamalar → AlfaGram → Xabarlar → «Yozdi-yubormadi jurnali»; profilda ⋮ → «Yozdi, yubormadi».",
                    "Sizga yozishni boshlab, yubormagan paytlar yozib boriladi. Profilda faqat shu odamniki ko'rinadi.",
                    null},
            {R.drawable.alfa_ic_contacts, IconBackgroundColors.BLUE_DEEP, "10 ta akkaunt",
                    "Sozlamalar → «Akkaunt qo'shish».",
                    "Bitta ilovada 10 tagacha Telegram akkaunti.",
                    null},
            {R.drawable.alfa_ic_folders, IconBackgroundColors.CYAN, "Avtomatik papkalar",
                    "Sukut bo'yicha yoqilgan: Sozlamalar → AlfaGram → Chatlar ro'yxati.",
                    "Chatlar shaxsiy, guruh, kanal, bot va o'qilmaganlarga ajraladi — faqat shu telefonda.",
                    null},
            {R.drawable.alfa_ic_privacy, IconBackgroundColors.BLUE, "Havola va APK himoyasi",
                    "Sukut bo'yicha yoqilgan: Sozlamalar → Maxfiylik va xavfsizlik → APK himoyasi.",
                    "Soxta bank, Click, Payme yoki Telegram saytlari, «sovg'a» havolalari haqida ogohlantiradi; chatdagi virus APK o'rnatilmaydi.",
                    null},
            {R.drawable.alfa_ic_premium, IconBackgroundColors.ORANGE, "Oy yakuni (AlfaGram Wrapped)",
                    "Sozlamalar → AlfaGram → «Oy yakuni (AlfaGram Wrapped)».",
                    "Oy davomida nechta xabar yozganingiz, eng ko'p yozishgan odamlaringiz, faol soat va kuningiz, sevimli emojilaringiz — Telegram serveridagi aniq sonlar bilan. Rasm qilib ulashish mumkin.",
                    null},
            {R.drawable.alfa_ic_devices, IconBackgroundColors.BLUE, "QR kod bilan kirish",
                    "Kirish ekranida «QR kod bilan kirish» tugmasini bosing.",
                    "Boshqa telefondagi Telegram'da Sozlamalar → Qurilmalar → «Qurilma ulash» orqali QR kodni skanerlang — SMS kod kutish shart emas.",
                    null},
            {R.drawable.alfa_ic_language, IconBackgroundColors.CYAN, "O'zbekcha va kirillcha",
                    "Sozlamalar → Til → O'zbek tili; kirill uchun Sozlamalar → AlfaGram → Ko'rinish va til.",
                    "Ilova o'zbekcha, xohlasangiz kirillda. Lotincha yozganingiz kirillda yuborilishi ham mumkin.",
                    null},
    };

    @Override
    protected CharSequence getTitle() {
        return AlfaCyrillic.ui("AlfaGram haqida");
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(AlfaCyrillic.ui("AlfaGram nima?")));
        items.add(UItem.asShadow(AlfaCyrillic.ui("AlfaGram — ustiga qo'shimcha imkoniyatlar qo'shilgan Telegram. Quyidagilarning hammasi bepul, obunasiz. Har birini Sozlamalar → AlfaGram bo'limida yoqib-o'chirasiz.")));
        for (int i = 0; i < FEATURES.length; i++) {
            Object[] f = FEATURES[i];
            IconBackgroundColors colors = (IconBackgroundColors) f[1];
            items.add(SettingsActivity.SettingCell.Factory.of(i + 1, colors.top, colors.bottom, (Integer) f[0], AlfaCyrillic.ui((String) f[2])));
            StringBuilder text = new StringBuilder();
            text.append(AlfaCyrillic.ui("YOQISH")).append(": ").append(AlfaCyrillic.ui((String) f[3]));
            text.append("\n\n").append(AlfaCyrillic.ui("QANDAY ISHLAYDI")).append(": ").append(AlfaCyrillic.ui((String) f[4]));
            if (f[5] != null) {
                text.append("\n\n").append(AlfaCyrillic.ui("O'CHIRISH")).append(": ").append(AlfaCyrillic.ui((String) f[5]));
            }
            items.add(UItem.asShadow(text.toString()));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
