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
                    "Sozlamalar → AlfaGram → Maxfiylik va Ghost → «Anonim story ko'rish».",
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
            {R.drawable.alfa_ic_contacts, IconBackgroundColors.BLUE_DEEP, "Maxfiy va to'xtatilgan akkauntlar",
                    "Sozlamalardagi akkauntni bosib turing → «Yashirish» yoki «Vaqtincha to'xtatish»; hammasi Sozlamalar → AlfaGram → Maxfiylik → «Maxfiy akkauntlar».",
                    "Yashirilgan akkaunt ro'yxatda va almashtirishda ko'rinmaydi, unga faqat «Maxfiy akkauntlar»dan kirasiz. To'xtatilgan akkauntdan bildirishnoma kelmaydi.",
                    "Akkauntni yana bosib turing → «Qayta yoqish», yoki «Maxfiy akkauntlar»da o'chiring."},
            {R.drawable.alfa_ic_chats, IconBackgroundColors.CYAN, "Chatni eksport qilish",
                    "Chat ichida ⋮ → «Chatni eksport qilish».",
                    "Oxirgi 5000 tagacha xabar HTML sahifa yoki TXT fayl bo'lib tayyorlanadi; uni saqlaysiz yoki istalgan joyga yuborasiz.",
                    null},
            {R.drawable.alfa_ic_stealth, IconBackgroundColors.ORANGE, "Dumaloq video kamerasi",
                    "Sozlamalar → AlfaGram → Xabarlar → «Dumaloq video orqa kamerada».",
                    "Yoqilsa, dumaloq video old emas, orqa kamerada boshlanadi. Yozish paytida kamerani almashtirish tugmasi ham bor.",
                    "O'sha bandni o'chiring."},
            {R.drawable.alfa_ic_settings, IconBackgroundColors.PURPLE, "Bot token bilan kirish",
                    "Kirish ekranida (telefon raqami so'raladigan joy) o'ng tepadagi robot tugmasini bosing va @BotFather bergan tokenni kiriting.",
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
                    "Sukut bo'yicha yoqilgan: Sozlamalar → AlfaGram.",
                    "Soxta bank, Click, Payme yoki Telegram saytlari, «sovg'a» havolalari haqida ogohlantiradi; chatdagi virus APK o'rnatilmaydi.",
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
