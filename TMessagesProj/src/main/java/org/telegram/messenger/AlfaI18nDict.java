package org.telegram.messenger;

import java.util.HashMap;

/**
 * AlfaGram matnlari lug'ati: o'zbekcha manba → ruscha / inglizcha.
 * Kalit — kodda AlfaCyrillic.ui(...) ga beriladigan aynan o'sha satr (format belgilari %s, %1$s, %d saqlanadi).
 * Kontekstli kalit: "kontekst::matn" (AlfaCyrillic.ui(matn, kontekst)).
 * Yangi matn qo'shsangiz — shu yerga ham qo'shing.
 */
final class AlfaI18nDict {

    private AlfaI18nDict() {
    }

    private static HashMap<String, String> ru;
    private static HashMap<String, String> en;

    private static void t(String uz, String r, String e) {
        ru.put(uz, r);
        en.put(uz, e);
    }

    static void fill(HashMap<String, String> ruMap, HashMap<String, String> enMap) {
        ru = ruMap;
        en = enMap;
        part1();
        part2();
        part3();
        ru = null;
        en = null;
    }

    private static void part1() {
        t("Bekor qilish",
                "Отмена",
                "Cancel");
        t("Ghost",
                "Призрак",
                "Ghost");
        t("Story",
                "Истории",
                "Stories");
        t("O'qildi",
                "Прочитано",
                "Read receipts");
        t("Onlayn, «yozyapti» va «o'qildi» yuborilmaydi",
                "Статусы «в сети», «печатает» и «прочитано» не отправляются",
                "Online, typing and read status aren't sent");
        t("Story muallifi siz ko'rganingizni bilmaydi",
                "Автор истории не узнает, что вы её смотрели",
                "The story author won't know you viewed it");
        t("Xabarni o'qiganingiz yuboruvchiga bildirilmaydi",
                "Отправитель не узнает, что вы прочитали сообщение",
                "The sender won't know you've read the message");
        t("Sozlamalar tepasida raqamingiz ko'rinmaydi",
                "Номер не показывается вверху настроек",
                "Your number is hidden at the top of Settings");
        t("Ilovalar ro'yxatida AlfaGram ekrani yopiq turadi",
                "В списке недавних приложений экран AlfaGram скрыт",
                "AlfaGram's screen is hidden in recent apps");
        t("Suhbatdosh o'chirgan xabar sizda 🗑 bilan qoladi",
                "Удалённые собеседником сообщения остаются у вас с 🗑",
                "Messages deleted by others stay with you, marked 🗑");
        t("Tahrirlangan xabarning eski matnlari saqlanadi",
                "Сохраняются прежние версии изменённых сообщений",
                "Previous versions of edited messages are kept");
        t("Sizga yozib, yubormaganlar",
                "Кто начал писать вам, но не отправил",
                "Who started typing to you but didn't send");
        t("Yuborishdan oldin tasdiq so'raladi, tasodifan ketmaydi",
                "Перед отправкой запрашивается подтверждение — ничего не уйдёт случайно",
                "Asks for confirmation so nothing is sent by accident");
        t("Tasodifan bosib, qo'ng'iroq qilib yubormaysiz",
                "Вы не позвоните кому-то случайным нажатием",
                "No more accidental calls");
        t("Dumaloq video old emas, orqa kamerada boshlanadi",
                "Видеосообщение начинается с основной камеры, а не с фронтальной",
                "Video messages start with the back camera instead of the front");
        t("Kamera rejimiga o'tganda old yoki orqa kamerani tanlaysiz",
                "При переключении в режим камеры можно выбрать фронтальную или основную",
                "Choose front or back camera when switching to video mode");
        t("Bir martalik rasm, video va ovoz chatda qoladi",
                "Одноразовые фото, видео и голосовые остаются в чате",
                "View-once photos, videos and voice messages stay in the chat");
        t("Klaviaturadagi Enter xabarni yuboradi",
                "Клавиша Enter отправляет сообщение",
                "The Enter key sends the message");
        t("Xabarlaringiz qabul qiluvchida tovushsiz keladi",
                "Ваши сообщения приходят получателю без звука",
                "Your messages arrive without sound");
        t("Xabarni bosib turganda «ID nusxalash» bandi",
                "Пункт «Копировать ID» при долгом нажатии на сообщение",
                "\"Copy ID\" item when you long-press a message");
        t("Xabarni chapga surib, tez javob bering",
                "Смахните сообщение влево, чтобы быстро ответить",
                "Swipe a message left to reply quickly");
        t("Emoji yozganda stiker takliflari chiqmaydi",
                "При вводе эмодзи не предлагаются стикеры",
                "No sticker suggestions when typing emoji");
        t("Yuborish animatsiyasi qisqaroq",
                "Более короткая анимация отправки",
                "Shorter sending animation");
        t("Shaxsiy, guruh, kanal, bot va o'qilmagan tablar",
                "Вкладки: личные, группы, каналы, боты и непрочитанные",
                "Tabs for private chats, groups, channels, bots and unread");
        t("Chatlar ro'yxati tepasidagi story qatori yashiriladi",
                "Скрывает ленту историй вверху списка чатов",
                "Hides the stories row above the chat list");
        t("Ovozi o'chirilgan chatlar ro'yxatda ko'rinmaydi",
                "Чаты без звука не показываются в списке",
                "Muted chats are hidden from the list");
        t("Chatlar ro'yxati ixchamroq ko'rinadi",
                "Список чатов становится компактнее",
                "A more compact chat list");
        t("Ilova ochilganda oxirgi chat ochiladi",
                "При запуске открывается последний чат",
                "Opens your last chat on launch");
        t("Saqlash yopilgan chatlardan ham saqlash",
                "Сохранение даже из чатов с запретом сохранения",
                "Save even from chats with saving restricted");
        t("Yopiq chatlarda ham skrinshot olish",
                "Скриншоты даже в защищённых чатах",
                "Take screenshots even in protected chats");
        t("Istalgancha chatni tepaga qadash",
                "Закрепляйте сколько угодно чатов",
                "Pin as many chats as you want");
        t("Forward qilinganda muallif ko'rsatilmaydi",
                "При пересылке автор не указывается",
                "Forwards without showing the author");
        t("Forward qilinganda izoh olib tashlanadi",
                "При пересылке подпись удаляется",
                "Removes the caption when forwarding");
        t("Ovozli xabarlar avtomatik saqlanadi",
                "Голосовые сообщения сохраняются автоматически",
                "Voice messages are saved automatically");
        t("Dumaloq videolar avtomatik saqlanadi",
                "Видеосообщения сохраняются автоматически",
                "Video messages are saved automatically");
        t("Kanallardagi homiylik xabarlari ko'rinmaydi",
                "Спонсорские сообщения в каналах скрыты",
                "Sponsored messages in channels are hidden");
        t("O'zbek tili tanlanganda ilova kirillda",
                "При узбекском языке интерфейс на кириллице",
                "The app uses Cyrillic when Uzbek is selected");
        t("Lotincha matn kirillga o'girib yuboriladi",
                "Текст на латинице отправляется кириллицей",
                "Latin text is sent converted to Cyrillic");
        t("O'zbek tili tanlanganda ilova yangi lotin alifbosida",
                "При узбекском языке интерфейс на новой латинице",
                "The app uses the new Latin alphabet when Uzbek is selected");
        t("sh, ch, o', g' → ş, ç, ö, ğ bo'lib ketadi",
                "sh, ch, o', g' отправляются как ş, ç, ö, ğ",
                "sh, ch, o', g' are sent as ş, ç, ö, ğ");
        t("Profil sahifasida foydalanuvchi ID'si",
                "ID пользователя на странице профиля",
                "User ID on the profile page");
        t("Chatlar ro'yxatida ✓✓ belgilari ko'rinmaydi",
                "Галочки ✓✓ в списке чатов скрыты",
                "No ✓✓ checkmarks in the chat list");
        t("Ilova tezroq, animatsiyalarsiz ishlaydi",
                "Приложение работает быстрее, без анимаций",
                "The app runs faster without animations");
        t("Kim qachon onlayn bo'lgani yozib boriladi",
                "Записывает, кто и когда был в сети",
                "Logs who was online and when");
        t("Username, rasm, raqam yoki ism o'zgarishi",
                "Смена имени пользователя, фото, номера или имени",
                "Username, photo, number or name changes");
        t("Xatolarni tekshirish uchun jurnal",
                "Журнал для диагностики ошибок",
                "Log for troubleshooting");
        t("Maxfiylik va Ghost",
                "Приватность и призрак",
                "Privacy & Ghost");
        t("Xabarlar",
                "Сообщения",
                "Messages");
        t("Chatlar ro'yxati",
                "Список чатов",
                "Chat list");
        t("Cheklovlarsiz",
                "Без ограничений",
                "No restrictions");
        t("Ko'rinish va til",
                "Оформление и язык",
                "Appearance & language");
        t("Kuzatuv",
                "Отслеживание",
                "Tracking");
        t("Ilova haqida",
                "О приложении",
                "About app");
        t("AlfaGram sozlamalari",
                "Настройки AlfaGram",
                "AlfaGram Settings");
        t("Ghost, anonim story, o'qildi belgisi",
                "Призрак, анонимные истории, отметки о прочтении",
                "Ghost, anonymous stories, read receipts");
        t("O'chirilgan xabarlar, tahrir tarixi, yuborish",
                "Удалённые сообщения, история изменений, отправка",
                "Deleted messages, edit history, sending");
        t("Avto papkalar, surish, ixcham ro'yxat",
                "Автопапки, смахивания, компактный список",
                "Auto folders, swipes, compact list");
        t("Saqlash, forward, reklama, media",
                "Сохранение, пересылка, реклама, медиа",
                "Saving, forwarding, ads, media");
        t("Kirill alifbosi, ikonka, animatsiyalar",
                "Кириллица, иконка, анимации",
                "Cyrillic, app icon, animations");
        t("Onlayn tarixi, profil o'zgarishlari",
                "История онлайна, изменения профиля",
                "Online history, profile changes");
        t("Ulashish, yangiliklar, sayt",
                "Поделиться, новости, сайт",
                "Share, what's new, website");
        t("Tezkor",
                "Быстрый доступ",
                "Quick toggles");
        t("Ghost — onlayn/yozyapti signali ketmaydi. Story — anonim ko'rish. O'qildi — o'qish tasdig'i yuborilmaydi.",
                "Призрак — статусы «в сети» и «печатает» не отправляются. Истории — анонимный просмотр. Прочитано — отметки о прочтении не отправляются.",
                "Ghost — no online or typing status. Stories — view anonymously. Read receipts — not sent.");
        t("Bo'limlar",
                "Разделы",
                "Sections");
        t("Har bir bo'limni oching — ichida o'sha mavzudagi barcha sozlamalar.",
                "Откройте раздел — внутри все настройки по этой теме.",
                "Open a section to see all of its settings.");
        t("Qo'llanma",
                "Руководство",
                "Guide");
        t("AlfaGram haqida — barcha funksiyalar",
                "Об AlfaGram — все функции",
                "About AlfaGram — all features");
        t("Har bir funksiya qayerdan yoqilishi, qanday ishlashi va qanday o'chirilishi.",
                "Где включить каждую функцию, как она работает и как её отключить.",
                "Where to turn on each feature, how it works and how to turn it off.");
        t("Yangi",
                "Новое",
                "New");
        t("Oy yakuni (AlfaGram Wrapped)",
                "Итоги месяца (AlfaGram Wrapped)",
                "Monthly recap (AlfaGram Wrapped)");
        t("Oy davomida kim bilan ko'p yozishganingiz, faol soatingiz va sevimli emojilaringiz.",
                "С кем вы больше всего переписывались за месяц, ваши активные часы и любимые эмодзи.",
                "Who you chatted with most this month, your most active hours and favorite emoji.");
        t("Ko'rinmaslik",
                "Невидимость",
                "Invisibility");
        t("Ghost rejim (onlayn, o'qildi, yozyapti yashirin)",
                "Режим призрака (скрыть «в сети», «прочитано», «печатает»)",
                "Ghost Mode (hide online, read, typing)");
        t("Anonim story ko'rish",
                "Анонимный просмотр историй",
                "Anonymous story viewing");
        t("O'qish tasdig'ini yubormaslik",
                "Не отправлять отметки о прочтении",
                "Don't send read receipts");
        t("Sizdan hech qanday signal ketmaydi va hech kim sizni kuzatolmaydi.",
                "От вас не уходит никаких сигналов, и никто не сможет за вами следить.",
                "No signals are sent from you, so nobody can track you.");
        t("Telefoningizda",
                "На вашем телефоне",
                "On your phone");
        t("Sozlamalarda telefonni yashirish",
                "Скрыть номер в настройках",
                "Hide phone number in Settings");
        t("So'nggi ilovalar oynasida yashirish",
                "Скрывать приложение в недавних",
                "Hide app in recent apps");
        t("Yashirin chatlar",
                "Скрытые чаты",
                "Hidden chats");
        t("Maxfiy akkauntlar",
                "Скрытые аккаунты",
                "Hidden accounts");
        t("Maxfiy akkauntlarni ochish",
                "Открыть скрытые аккаунты",
                "Open hidden accounts");
        t("PIN kod sozlash",
                "Настроить код-пароль",
                "Set up passcode");
        t("Maxfiylik va akkauntni o'chirish",
                "Конфиденциальность и удаление аккаунта",
                "Privacy and account deletion");
        t("PIN — ilova ochilganda kod so'raladi. APK himoyasi: Maxfiylik va xavfsizlik → APK himoyasi.",
                "Код-пароль запрашивается при открытии приложения. Защита от APK: Конфиденциальность → Защита от APK.",
                "Passcode is asked when the app opens. APK protection: Privacy and Security → APK protection.");
        t("Xabar himoyasi",
                "Защита сообщений",
                "Message protection");
        t("O'chirilgan xabarlarni saqlash",
                "Сохранять удалённые сообщения",
                "Keep deleted messages");
        t("Tahrir tarixini yozish",
                "Сохранять историю изменений",
                "Save edit history");
        t("Yozdi-yubormadi jurnali",
                "Журнал «Печатал, но не отправил»",
                "\"Typed, not sent\" log");
        t("Kim yozib, o'chirsa yoki tahrirlagan bo'lsa — hammasi sizda qoladi. Xabarni bosib turib tarixni ko'ring.",
                "Что бы ни удалили или изменили — всё останется у вас. Нажмите и удерживайте сообщение, чтобы увидеть историю.",
                "Whatever gets deleted or edited stays with you. Long-press a message to see its history.");
        t("Xabar yuborish",
                "Отправка сообщений",
                "Sending messages");
        t("Yuborishdan oldin tasdiq",
                "Подтверждение перед отправкой",
                "Confirm before sending");
        t("Qo'ng'iroqdan oldin so'rash",
                "Спрашивать перед звонком",
                "Confirm before calling");
        t("Dumaloq video: har safar kamerani so'rash",
                "Видеосообщение: каждый раз спрашивать камеру",
                "Video message: ask for camera each time");
        t("Dumaloq video orqa kamerada",
                "Видеосообщение на основную камеру",
                "Video message with back camera");
        t("Enter tugmasi bilan yuborish",
                "Отправка по Enter",
                "Send with Enter");
        t("Ovozsiz yuborish",
                "Отправлять без звука",
                "Send silently");
        t("Yuborishni tezlashtiring yoki tasodifiy yuborishning oldini oling.",
                "Ускорьте отправку или защититесь от случайной отправки.",
                "Send faster or avoid sending by accident.");
        t("Chat ichida",
                "В чате",
                "In chat");
        t("Ikki marta bosish: %s",
                "Двойное нажатие: %s",
                "Double tap: %s");
        t("Xabar menyusida ID nusxalash",
                "«Копировать ID» в меню сообщения",
                "Copy ID in message menu");
        t("Chapga siljitib javob",
                "Ответ смахиванием влево",
                "Swipe left to reply");
        t("Stiker taklifini o'chirish",
                "Отключить подсказки стикеров",
                "Disable sticker suggestions");
        t("Tez yuborish animatsiyasi",
                "Быстрая анимация отправки",
                "Fast send animation");
        t("Xabarni bosib ushlang — menyuda «ID nusxalash» chiqadi.",
                "Нажмите и удерживайте сообщение — в меню появится «Копировать ID».",
                "Long-press a message — \"Copy ID\" appears in the menu.");
        t("Papkalar",
                "Папки",
                "Folders");
        t("Avto papkalar",
                "Автопапки",
                "Auto folders");
        t("Shaxsiy, Guruhlar, Kanallar, Botlar va O'qilmagan papkalari. Ular faqat AlfaGram'da, shu telefonda ko'rinadi.",
                "Папки «Личные», «Группы», «Каналы», «Боты» и «Непрочитанные». Они видны только в AlfaGram на этом телефоне.",
                "Private, Groups, Channels, Bots and Unread folders. They're visible only in AlfaGram on this phone.");
        t("Ro'yxat",
                "Список",
                "List");
        t("Storylar qatorini yashirish",
                "Скрыть ленту историй",
                "Hide stories row");
        t("Ovozsiz chatlarni yashirish",
                "Скрыть чаты без звука",
                "Hide muted chats");
        t("Ixcham (2 qatorli) chat ro'yxati",
                "Компактный список чатов (2 строки)",
                "Compact chat list (2 lines)");
        t("Ochilganda so'nggi chatga o'tish",
                "Открывать последний чат при запуске",
                "Open last chat on launch");
        t("Surish",
                "Смахивания",
                "Swipes");
        t("Chapga surish: %s",
                "Смахивание влево: %s",
                "Swipe left: %s");
        t("O'ngga surish: %s",
                "Смахивание вправо: %s",
                "Swipe right: %s");
        t("Chatni o'ng chetdan chapga yoki chap chetdan o'ngga torting — tanlangan amal bajariladi. O'rtadan surish papkani almashtiradi.",
                "Потяните чат от правого края влево или от левого края вправо — выполнится выбранное действие. Смахивание из середины переключает папки.",
                "Drag a chat from the right edge to the left or from the left edge to the right to run the chosen action. Swiping from the middle switches folders.");
        t("Saqlash va forward",
                "Сохранение и пересылка",
                "Saving & forwarding");
        t("Cheklovlarsiz saqlash/forward",
                "Сохранение и пересылка без ограничений",
                "Save and forward without restrictions");
        t("Bir martalik media o'chmasin",
                "Не удалять одноразовые медиа",
                "Keep view-once media");
        t("Skrinshotga ruxsat",
                "Разрешить скриншоты",
                "Allow screenshots");
        t("Cheksiz qadash",
                "Безлимитное закрепление",
                "Unlimited pins");
        t("Forward — muallifsiz",
                "Пересылка без автора",
                "Forward without author");
        t("Forward — izohsiz",
                "Пересылка без подписи",
                "Forward without caption");
        t("Telegram'ning standart cheklovlarini olib tashlash.",
                "Снимает стандартные ограничения Telegram.",
                "Removes Telegram's default restrictions.");
        t("Media",
                "Медиа",
                "Media");
        t("Barcha ovozli xabarlarni saqlash",
                "Сохранять все голосовые",
                "Save all voice messages");
        t("Barcha dumaloq videolarni saqlash",
                "Сохранять все видеосообщения",
                "Save all video messages");
        t("Kelgan media avtomatik telefoningizga tushiriladi.",
                "Полученные медиа автоматически сохраняются на телефон.",
                "Incoming media is saved to your phone automatically.");
        t("Reklama",
                "Реклама",
                "Ads");
        t("Telegram reklamalarini bloklash",
                "Блокировать рекламу Telegram",
                "Block Telegram ads");
        t("\"Homiylik\" xabarlarni yashirish",
                "Скрывать спонсорские сообщения",
                "Hide sponsored messages");
        t("Til",
                "Язык",
                "Language");
        t("Kirill alifbosi (Ўзбекча)",
                "Кириллица (Ўзбекча)",
                "Uzbek Cyrillic (Ўзбекча)");
        t("Lotincha yozsam, kirillda yuborilsin",
                "Отправлять латиницу кириллицей",
                "Send Latin text as Cyrillic");
        t("Yangi alifbo (Ş Ç Ö Ğ)",
                "Новый алфавит (Ş Ç Ö Ğ)",
                "New alphabet (Ş Ç Ö Ğ)");
        t("Yangi alifboda yuborilsin",
                "Отправлять новым алфавитом",
                "Send in new alphabet");
        t("Kirillni Sozlamalar → Til → «Ўзбекча (кирилл)» orqali ham tanlash mumkin. Xabarni bosib ushlab, matnni kirill ↔ lotinga o'girish ham mumkin.",
                "Кириллицу можно выбрать и в Настройки → Язык → «Ўзбекча (кирилл)». А долгое нажатие на сообщение позволяет перевести текст кириллица ↔ латиница.",
                "You can also pick Cyrillic in Settings → Language → \"Ўзбекча (кирилл)\". Long-press a message to convert its text Cyrillic ↔ Latin.");
        t("Ko'rinish",
                "Внешний вид",
                "Appearance");
        t("Ilova ikonkasini tanlash",
                "Выбрать иконку приложения",
                "Choose app icon");
        t("Profilda ID ko'rinsin",
                "Показывать ID в профиле",
                "Show ID in profile");
        t("Ko'rildi belgisini yashirish",
                "Скрыть галочки прочтения",
                "Hide read checkmarks");
        t("Barcha animatsiyalarni o'chirish",
                "Отключить все анимации",
                "Disable all animations");
        t("Ikonkalar: Oq, Qora, Oltin, Binafsha, Neon — hammasi bepul.",
                "Иконки: белая, чёрная, золотая, фиолетовая, неон — все бесплатно.",
                "Icons: White, Black, Gold, Purple, Neon — all free.");
        t("Onlayn tarixi",
                "История онлайна",
                "Online history");
        t("Kontaktlar onlayn tarixini yozish",
                "Записывать историю онлайна контактов",
                "Log contacts' online history");
        t("Profil → ⋮ → «Onlayn tarixi»: kontakt qachon va qancha vaqt onlayn bo'lgani.",
                "Профиль → ⋮ → «История онлайна»: когда и сколько контакт был в сети.",
                "Profile → ⋮ → \"Online history\": when and for how long a contact was online.");
        t("Profil o'zgarishlari",
                "Изменения профиля",
                "Profile changes");
        t("Username, rasm va raqam o'zgarishlarini yozish",
                "Записывать смену имени пользователя, фото и номера",
                "Log username, photo and number changes");
        t("Profil o'zgarishlarini ko'rish",
                "Посмотреть изменения профилей",
                "View profile changes");
        t("Kontaktlar → «Profil o'zgarishlari» bo'limida ham bor. Ma'lumot faqat telefoningizda saqlanadi.",
                "Также есть в разделе Контакты → «Изменения профиля». Данные хранятся только на вашем телефоне.",
                "Also available in Contacts → \"Profile changes\". Data is stored only on your phone.");
        t("Do'stlarga ulashish",
                "Поделиться с друзьями",
                "Share with friends");
        t("Yangi nimalar?",
                "Что нового?",
                "What's new?");
        t("Yordam guruhi (@alfagram_chat)",
                "Группа поддержки (@alfagram_chat)",
                "Support group (@alfagram_chat)");
        t("Sayt: alfagram.uz",
                "Сайт: alfagram.uz",
                "Website: alfagram.uz");
        t("Hujjatlar",
                "Документы",
                "Documents");
        t("Maxfiylik siyosati",
                "Политика конфиденциальности",
                "Privacy policy");
        t("Hisob va ma'lumotlarni o'chirish",
                "Удаление аккаунта и данных",
                "Delete account and data");
        t("Dasturchilar uchun",
                "Для разработчиков",
                "For developers");
        t("Nosozlik jurnali",
                "Журналы отладки",
                "Debug logs");
        t("AlfaGram %s — Telegram API asosidagi norasmiy klient. Telegram bilan bog'liq emas.",
                "AlfaGram %s — неофициальный клиент на основе Telegram API. Не связан с Telegram.",
                "AlfaGram %s is an unofficial client based on the Telegram API. Not affiliated with Telegram.");
        t("Xabarni ikki marta bosganda",
                "Двойное нажатие на сообщение",
                "Double-tapping a message");
        t("Men AlfaGram'dan foydalanaman — Ghost rejim, o'chirilgan xabarlarni ko'rish, onlayn tarix, kirill alifbosi va boshqa ko'plab qulayliklar. Yuklab oling: ",
                "Я пользуюсь AlfaGram: режим призрака, просмотр удалённых сообщений, история онлайна, узбекская кириллица и много других удобств. Скачать: ",
                "I use AlfaGram: Ghost Mode, deleted messages, online history, Uzbek Cyrillic and lots more. Get it:");
        t("Bu funksiya vaqtincha o'chirilgan",
                "Эта функция временно отключена",
                "This feature is temporarily disabled");
        t("Yashirin funksiyalar · bepul",
                "Скрытые функции · бесплатно",
                "Hidden features · free");
        t("Vaqtincha o'chiq",
                "Временно выкл.",
                "Temporarily off");
        t("Yoqilgan",
                "Вкл.",
                "On");
        t("O'chiq",
                "Выкл.",
                "Off");
        t("AlfaGram haqida",
                "Об AlfaGram",
                "About AlfaGram");
        t("AlfaGram nima?",
                "Что такое AlfaGram?",
                "What is AlfaGram?");
        t("AlfaGram — ustiga qo'shimcha imkoniyatlar qo'shilgan Telegram. Quyidagilarning hammasi bepul, obunasiz. Har birini Sozlamalar → AlfaGram bo'limida yoqib-o'chirasiz.",
                "AlfaGram — это Telegram с дополнительными возможностями. Всё перечисленное ниже бесплатно и без подписки. Каждую функцию можно включить или отключить в Настройки → AlfaGram.",
                "AlfaGram is Telegram with extra features. Everything below is free, no subscription needed. Turn each one on or off in Settings → AlfaGram.");
        t("YOQISH",
                "КАК ВКЛЮЧИТЬ",
                "TURN ON");
        t("QANDAY ISHLAYDI",
                "КАК РАБОТАЕТ",
                "HOW IT WORKS");
        t("O'CHIRISH",
                "КАК ОТКЛЮЧИТЬ",
                "TURN OFF");
        t("Ghost rejim",
                "Режим призрака",
                "Ghost Mode");
        t("Chatlar ro'yxati tepasidagi 👻 tugmasini bosing yoki Sozlamalar → AlfaGram → Maxfiylik va Ghost.",
                "Нажмите кнопку 👻 вверху списка чатов или откройте Настройки → AlfaGram → Приватность и призрак.",
                "Tap the 👻 button above the chat list, or go to Settings → AlfaGram → Privacy & Ghost.");
        t("Siz xabarlarni o'qiysiz, lekin suhbatdoshga «o'qildi», «yozyapti...» va «onlayn» belgilari bormaydi.",
                "Вы читаете сообщения, но собеседник не видит «прочитано», «печатает...» и «в сети».",
                "You read messages, but the other person doesn't see \"read\", \"typing...\" or \"online\".");
        t("👻 tugmasini qayta bosing yoki o'sha bandlarni o'chiring.",
                "Нажмите 👻 ещё раз или отключите эти пункты.",
                "Tap 👻 again or turn those options off.");
        t("O'chirilgan xabarlar",
                "Удалённые сообщения",
                "Deleted messages");
        t("Sozlamalar → AlfaGram → Xabarlar → «O'chirilgan xabarlarni saqlash» (sukut bo'yicha yoqilgan).",
                "Настройки → AlfaGram → Сообщения → «Сохранять удалённые сообщения» (включено по умолчанию).",
                "Settings → AlfaGram → Messages → \"Keep deleted messages\" (on by default).");
        t("Suhbatdosh xabarni o'chirsa ham, u sizning chatingizda 🗑 belgisi bilan qoladi.",
                "Даже если собеседник удалит сообщение, оно останется в вашем чате с отметкой 🗑.",
                "Even if the other person deletes a message, it stays in your chat marked 🗑.");
        t("O'sha bandni o'chiring.",
                "Отключите этот пункт.",
                "Turn that option off.");
        t("Tahrir tarixi",
                "История изменений",
                "Edit history");
        t("Sozlamalar → AlfaGram → Xabarlar → «Tahrir tarixini yozish» (sukut bo'yicha yoqilgan).",
                "Настройки → AlfaGram → Сообщения → «Сохранять историю изменений» (включено по умолчанию).",
                "Settings → AlfaGram → Messages → \"Save edit history\" (on by default).");
        t("Tahrirlangan xabarni bosib turing → tarix: barcha oldingi matnlari ko'rinadi.",
                "Нажмите и удерживайте изменённое сообщение → история: видны все прежние версии текста.",
                "Long-press an edited message → history: all previous versions are shown.");
        t("Story'larni anonim ko'rish",
                "Анонимный просмотр историй",
                "Anonymous story viewing");
        t("Sukut bo'yicha yoqiq. Sozlamalar → AlfaGram → Maxfiylik va Ghost → «Anonim story ko'rish».",
                "Включено по умолчанию. Настройки → AlfaGram → Приватность и призрак → «Анонимный просмотр историй».",
                "On by default. Settings → AlfaGram → Privacy & Ghost → \"Anonymous story viewing\".");
        t("Story muallifi siz ko'rganingizni bilmaydi — ko'rganlar ro'yxatida chiqmaysiz.",
                "Автор истории не узнает, что вы её смотрели, — вас не будет в списке просмотревших.",
                "The story author won't know you viewed it — you won't appear in the viewers list.");
        t("Sukut bo'yicha yoqilgan: Sozlamalar → AlfaGram → Xabarlar → «Qo'ng'iroqdan oldin so'rash».",
                "Включено по умолчанию: Настройки → AlfaGram → Сообщения → «Спрашивать перед звонком».",
                "On by default: Settings → AlfaGram → Messages → \"Confirm before calling\".");
        t("Qo'ng'iroq tugmasini bossangiz, avval «Qo'ng'iroq qilinsinmi?» deb so'raladi — tasodifiy qo'ng'iroq ketmaydi.",
                "При нажатии на кнопку звонка сначала появится вопрос «Позвонить?» — случайных звонков не будет.",
                "When you tap the call button, you're asked \"Call?\" first — no accidental calls.");
        t("Chat qulfi",
                "Блокировка чата",
                "Chat lock");
        t("Chat ichida ⋮ → «Chatni qulflash».",
                "В чате ⋮ → «Заблокировать чат».",
                "In a chat: ⋮ → \"Lock chat\".");
        t("Chat barmoq izi, yuz yoki telefon PIN'i bilan ochiladi. Ro'yxatda va bildirishnomada xabar matni ko'rinmaydi. Ilovadan chiqsangiz, yana qulflanadi.",
                "Чат открывается по отпечатку пальца, лицу или PIN-коду телефона. Текст сообщений не виден в списке и уведомлениях. Когда вы выходите из приложения, чат снова блокируется.",
                "The chat opens with your fingerprint, face or phone PIN. Message text is hidden in the list and notifications. It locks again when you leave the app.");
        t("Chat ichida ⋮ → «Qulfni olib tashlash».",
                "В чате ⋮ → «Снять блокировку».",
                "In a chat: ⋮ → \"Remove lock\".");
        t("Sozlamalar → AlfaGram → Maxfiylik → «Yashirin chatlar»: qulf turini (4 raqamli PIN yoki parol) tanlab kod qo'ying. Keyin chat ichida ⋮ → «Chatni yashirish».",
                "Настройки → AlfaGram → Приватность → «Скрытые чаты»: выберите тип блокировки (4-значный PIN или пароль) и задайте код. Затем в чате ⋮ → «Скрыть чат».",
                "Settings → AlfaGram → Privacy → \"Hidden chats\": choose a lock type (4-digit PIN or password) and set a code. Then in a chat: ⋮ → \"Hide chat\".");
        t("Yashirilgan chat ro'yxatdan va uzatish oynasidan yo'qoladi va ovozsiz bo'ladi. Ko'rish uchun «AlfaGram» sarlavhasini bosib turing va kodni kiriting; ilovadan chiqsangiz, yana yashirinadi.",
                "Скрытый чат пропадает из списка и окна пересылки, а уведомления от него отключаются. Чтобы увидеть его, нажмите и удерживайте заголовок «AlfaGram» и введите код; когда вы выходите из приложения, он снова скрывается.",
                "A hidden chat disappears from the list and the forward screen and is muted. To see it, long-press the \"AlfaGram\" title and enter the code; it hides again when you leave the app.");
        t("Chat ichida ⋮ → «Yashirishdan chiqarish», yoki sozlamada kod bilan «O'chirish».",
                "В чате ⋮ → «Вернуть из скрытых» или «Отключить» в настройках с вводом кода.",
                "In a chat: ⋮ → \"Unhide chat\", or \"Turn off\" in settings with your code.");
        t("Sozlamalar → AlfaGram → Ko'rinish va til → «Yangi alifbo» (ilova matnlari) va «Yangi alifboda yuborilsin» (xabarlar).",
                "Настройки → AlfaGram → Оформление и язык → «Новый алфавит» (тексты приложения) и «Отправлять новым алфавитом» (сообщения).",
                "Settings → AlfaGram → Appearance & language → \"New alphabet\" (app texts) and \"Send in new alphabet\" (messages).");
        t("sh, ch, o', g' → ş, ç, ö, ğ. Siz odatdagidek yozasiz, xabar yangi alifboda ketadi. Havola, @username va teglar o'zgarmaydi. Kirill bilan bir vaqtda faqat bittasi yoqiladi.",
                "sh, ch, o', g' → ş, ç, ö, ğ. Вы пишете как обычно, а сообщение уходит в новом алфавите. Ссылки, @username и теги не меняются. Вместе с кириллицей включить нельзя — только что-то одно.",
                "sh, ch, o', g' → ş, ç, ö, ğ. You type as usual and the message is sent in the new alphabet. Links, @usernames and tags stay unchanged. It can't be on together with Cyrillic.");
        t("O'sha bandlarni o'chiring.",
                "Отключите эти пункты.",
                "Turn those options off.");
        t("Maxfiy va to'xtatilgan akkauntlar",
                "Скрытые и приостановленные аккаунты",
                "Hidden and paused accounts");
        t("Sozlamalardagi akkauntni bosib turing → «Yashirish» yoki «Vaqtincha to'xtatish»; hammasi Sozlamalar → AlfaGram → Maxfiylik → «Maxfiy akkauntlar» yoki «Akkaunt qo'shish»ni bosib turing.",
                "Нажмите и удерживайте аккаунт в настройках → «Скрыть» или «Приостановить»; все аккаунты — в Настройки → AlfaGram → Приватность → «Скрытые аккаунты» или по долгому нажатию на «Добавить аккаунт».",
                "Long-press an account in Settings → \"Hide\" or \"Pause\"; see them all in Settings → AlfaGram → Privacy → \"Hidden accounts\", or long-press \"Add account\".");
        t("Yashirilgan akkaunt ro'yxatda va almashtirishda ko'rinmaydi, unga faqat «Maxfiy akkauntlar»dan kirasiz. To'xtatilgan akkauntdan bildirishnoma kelmaydi.",
                "Скрытый аккаунт не виден в списке и при переключении, открыть его можно только через «Скрытые аккаунты». От приостановленного аккаунта не приходят уведомления.",
                "A hidden account doesn't show in the list or the account switcher; you open it only from \"Hidden accounts\". A paused account sends no notifications.");
        t("Akkauntni yana bosib turing → «Qayta yoqish», yoki «Maxfiy akkauntlar»da o'chiring.",
                "Снова нажмите и удерживайте аккаунт → «Возобновить» или отключите в «Скрытых аккаунтах».",
                "Long-press the account again → \"Resume\", or turn it off in \"Hidden accounts\".");
        t("Chatni eksport qilish",
                "Экспорт чата",
                "Export chat");
        t("Chat ichida ⋮ → «Chatni eksport qilish».",
                "В чате ⋮ → «Экспорт чата».",
                "In a chat: ⋮ → \"Export chat\".");
        t("Oxirgi 5000 tagacha xabar HTML sahifa yoki TXT fayl bo'lib tayyorlanadi; uni saqlaysiz yoki istalgan joyga yuborasiz.",
                "До 5000 последних сообщений собираются в HTML-страницу или TXT-файл — его можно сохранить или отправить куда угодно.",
                "Up to the last 5000 messages are exported as an HTML page or TXT file that you can save or send anywhere.");
        t("Dumaloq video kamerasi",
                "Камера видеосообщений",
                "Video message camera");
        t("Sozlamalar → AlfaGram → Xabarlar → «Dumaloq video orqa kamerada» yoki «har safar kamerani so'rash».",
                "Настройки → AlfaGram → Сообщения → «Видеосообщение на основную камеру» или «каждый раз спрашивать камеру».",
                "Settings → AlfaGram → Messages → \"Video message with back camera\" or \"ask for camera each time\".");
        t("Doim old, doim orqa kamera yoki har safar so'rash: yozish tugmasini kamera rejimiga o'tkazganingizda «Old kamera / Orqa kamera» tanlanadi. Yozish paytida kamerani almashtirish tugmasi ham bor.",
                "Всегда фронтальная, всегда основная камера или спрашивать каждый раз: при переключении кнопки записи в режим камеры выбирается «Фронтальная / Основная». Во время записи есть и кнопка смены камеры.",
                "Always front, always back, or ask each time: when you switch the record button to camera mode, you pick \"Front camera / Back camera\". There's also a switch-camera button while recording.");
        t("Galereyadan dumaloq video",
                "Видеосообщение из галереи",
                "Video message from gallery");
        t("Chatda 📎 → «Dumaloq video» → videoni tanlang.",
                "В чате 📎 → «Видеосообщение» → выберите видео.",
                "In a chat: 📎 → \"Video message\" → pick a video.");
        t("1 daqiqagacha bo'lgan video o'rtasidan kvadrat qilib kesiladi va xuddi kamerada yozilgandek dumaloq video bo'lib ketadi. Uzunroq video qabul qilinmaydi.",
                "Видео до 1 минуты обрезается до квадрата по центру и отправляется как видеосообщение, будто записанное камерой. Более длинные видео не принимаются.",
                "Videos up to 1 minute are cropped to a square from the center and sent as a video message, just like one recorded with the camera. Longer videos aren't accepted.");
        t("Bot token bilan kirish",
                "Войти с токеном бота",
                "Log in with bot token");
        t("Kirish ekranida (telefon raqami so'raladigan joy) o'ng tepadagi robot tugmasini bosing: ochilgan oynaga @BotFather bergan tokenni kiriting va «Tizimga kirish»ni bosing.",
                "На экране входа (где запрашивается номер телефона) нажмите кнопку с роботом в правом верхнем углу, введите токен от @BotFather и нажмите «Войти».",
                "On the login screen (where your phone number is asked), tap the robot button in the top right, enter the token from @BotFather and tap \"Log in\".");
        t("Ilova bot nomidan ochiladi. Telegram botlarga eski chatlar va tarixni bermaydi — faqat kirgandan keyingi yangi xabarlar keladi.",
                "Приложение открывается от имени бота. Telegram не даёт ботам старые чаты и историю — приходят только новые сообщения после входа.",
                "The app opens as the bot. Telegram doesn't give bots old chats or history — only new messages arrive after you log in.");
        t("Bot akkauntidan chiqing.",
                "Выйдите из аккаунта бота.",
                "Log out of the bot account.");
        t("Kontaktlardan guruh va kanal",
                "Группа и канал из контактов",
                "Group and channel from Contacts");
        t("Kontaktlar tabining tepasidagi «Yangi guruh» yoki «Yangi kanal» qatorini bosing.",
                "Нажмите «Новая группа» или «Новый канал» вверху вкладки «Контакты».",
                "Tap \"New group\" or \"New channel\" at the top of the Contacts tab.");
        t("Guruh uchun a'zolarni tanlab nom berasiz, kanal uchun nom va tavsif kiritasiz.",
                "Для группы выберите участников и задайте название, для канала — название и описание.",
                "For a group, pick members and set a name; for a channel, enter a name and description.");
        t("Boshiga o'tish",
                "К первому сообщению",
                "Go to first message");
        t("Profil yoki kanal sahifasida ⋮ → «Boshiga o'tish».",
                "На странице профиля или канала ⋮ → «К первому сообщению».",
                "On a profile or channel page: ⋮ → \"Go to first message\".");
        t("Chat yoki kanal eng birinchi xabaridan ochiladi — uzoq tepaga surish shart emas.",
                "Чат или канал открывается с самого первого сообщения — не нужно долго листать вверх.",
                "The chat or channel opens at its very first message — no long scrolling up.");
        t("Yozdi, yubormadi",
                "Печатал, но не отправил",
                "Typed, not sent");
        t("Sozlamalar → AlfaGram → Xabarlar → «Yozdi-yubormadi jurnali»; profilda ⋮ → «Yozdi, yubormadi».",
                "Настройки → AlfaGram → Сообщения → «Журнал „Печатал, но не отправил“»; в профиле ⋮ → «Печатал, но не отправил».",
                "Settings → AlfaGram → Messages → \"Typed, not sent\" log; in a profile: ⋮ → \"Typed, not sent\".");
        t("Sizga yozishni boshlab, yubormagan paytlar yozib boriladi. Profilda faqat shu odamniki ko'rinadi.",
                "Записываются случаи, когда вам начали писать, но не отправили. В профиле видны только записи этого человека.",
                "Logs when someone started typing to you but didn't send. A profile shows only that person's entries.");
        t("10 ta akkaunt",
                "10 аккаунтов",
                "10 accounts");
        t("Sozlamalar → «Akkaunt qo'shish».",
                "Настройки → «Добавить аккаунт».",
                "Settings → \"Add account\".");
        t("Bitta ilovada 10 tagacha Telegram akkaunti.",
                "До 10 аккаунтов Telegram в одном приложении.",
                "Up to 10 Telegram accounts in one app.");
        t("Avtomatik papkalar",
                "Автоматические папки",
                "Automatic folders");
        t("Sukut bo'yicha yoqilgan: Sozlamalar → AlfaGram → Chatlar ro'yxati.",
                "Включено по умолчанию: Настройки → AlfaGram → Список чатов.",
                "On by default: Settings → AlfaGram → Chat list.");
        t("Chatlar shaxsiy, guruh, kanal, bot va o'qilmaganlarga ajraladi — faqat shu telefonda.",
                "Чаты делятся на личные, группы, каналы, ботов и непрочитанные — только на этом телефоне.",
                "Chats are split into private, groups, channels, bots and unread — only on this phone.");
        t("Havola va APK himoyasi",
                "Защита от ссылок и APK",
                "Link and APK protection");
        t("Sukut bo'yicha yoqilgan: Sozlamalar → AlfaGram.",
                "Включено по умолчанию: Настройки → AlfaGram.",
                "On by default: Settings → AlfaGram.");
        t("Soxta bank, Click, Payme yoki Telegram saytlari, «sovg'a» havolalari haqida ogohlantiradi; chatdagi virus APK o'rnatilmaydi.",
                "Предупреждает о поддельных сайтах банков, Click, Payme или Telegram и ссылках на «подарки»; вирусные APK из чатов не устанавливаются.",
                "Warns about fake bank, Click, Payme or Telegram sites and \"gift\" links; virus APKs from chats won't install.");
        t("O'zbekcha va kirillcha",
                "Узбекский и кириллица",
                "Uzbek and Cyrillic");
        t("Sozlamalar → Til → O'zbek tili; kirill uchun Sozlamalar → AlfaGram → Ko'rinish va til.",
                "Настройки → Язык → Узбекский; кириллица — в Настройки → AlfaGram → Оформление и язык.",
                "Settings → Language → Uzbek; for Cyrillic, Settings → AlfaGram → Appearance & language.");
        t("Ilova o'zbekcha, xohlasangiz kirillda. Lotincha yozganingiz kirillda yuborilishi ham mumkin.",
                "Приложение на узбекском, при желании — на кириллице. Текст на латинице можно отправлять кириллицей.",
                "The app is in Uzbek, or Uzbek Cyrillic if you like. Text you type in Latin can be sent in Cyrillic.");
        t("AlfaGram imkoniyatlari",
                "Возможности AlfaGram",
                "AlfaGram features");
        t("Tushunarli!",
                "Понятно!",
                "Got it!");
        t("Onlayn ko'rinmasdan o'qing: «yozyapti» va «o'qildi» yuborilmaydi. Chatlar ro'yxati tepasidagi 👻 tugmasi bilan bir bosishda yoqing.",
                "Читайте, не появляясь в сети: «печатает» и «прочитано» не отправляются. Включается одним нажатием на 👻 вверху списка чатов.",
                "Read without appearing online: typing and read status aren't sent. Turn it on with one tap on 👻 above the chat list.");
        t("O'chirilgan xabarlar qoladi",
                "Удалённые сообщения остаются",
                "Deleted messages stay");
        t("Suhbatdoshingiz xabarni o'chirsa ham, u sizda 🗑 belgisi bilan saqlanib qoladi.",
                "Даже если собеседник удалит сообщение, оно сохранится у вас с отметкой 🗑.",
                "Even if the other person deletes a message, you keep it, marked 🗑.");
        t("Tahrirlangan xabarni bosib turing → «Tahrir tarixi» — barcha oldingi matnlarni ko'rasiz.",
                "Нажмите и удерживайте изменённое сообщение → «История изменений» — увидите все прежние версии.",
                "Long-press an edited message → \"Edit history\" to see all previous versions.");
        t("Chat ichida ⋮ → «Chatni qulflash». U barmoq izi yoki PIN bilan ochiladi, xabarlari ro'yxatda va bildirishnomada ko'rinmaydi.",
                "В чате ⋮ → «Заблокировать чат». Он открывается по отпечатку пальца или PIN-коду, а его сообщения не видны в списке и уведомлениях.",
                "In a chat: ⋮ → \"Lock chat\". It opens with your fingerprint or PIN, and its messages are hidden in the list and notifications.");
        t("Story'larni muallifga bildirmasdan ko'ring.",
                "Смотрите истории так, чтобы автор не узнал.",
                "Watch stories without the author knowing.");
        t("10 tagacha akkauntni bitta ilovada ishlating. Keraklisini maxfiy qilib yashiring yoki vaqtincha to'xtating.",
                "Используйте до 10 аккаунтов в одном приложении. Любой из них можно скрыть или приостановить.",
                "Use up to 10 accounts in one app. Hide any of them or pause them for a while.");
        t("Tasodifan bosib, qo'ng'iroq qilib yubormaysiz — avval «Qo'ng'iroq qilinsinmi?» deb so'raladi.",
                "Вы не позвоните случайно — сначала появится вопрос «Позвонить?».",
                "No accidental calls — you're asked \"Call?\" first.");
        t("Havola himoyasi",
                "Защита от ссылок",
                "Link protection");
        t("Soxta bank, Click, Payme yoki Telegram saytlari va «sovg'a» va'da qiluvchi havolalarni ochishdan oldin ogohlantiramiz.",
                "Предупредим, прежде чем вы откроете поддельный сайт банка, Click, Payme или Telegram либо ссылку, обещающую «подарок».",
                "We warn you before you open fake bank, Click, Payme or Telegram sites or links promising a \"gift\".");
    }

    private static void part2() {
        t("Ilova o'zbek tilida, xohlasangiz kirillda. Lotincha yozganingizni kirillda yuborish ham mumkin.",
                "Приложение на узбекском, при желании — на кириллице. Текст на латинице можно отправлять кириллицей.",
                "The app is in Uzbek, or Uzbek Cyrillic if you like. You can also send Latin text in Cyrillic.");
        t("Hammasi bir joyda",
                "Всё в одном месте",
                "All in one place");
        t("Barcha imkoniyatlarni Sozlamalar → AlfaGram bo'limida yoqib-o'chirasiz.",
                "Все возможности включаются и отключаются в разделе Настройки → AlfaGram.",
                "Turn every feature on or off in Settings → AlfaGram.");
        t("AlfaGram'da yangi",
                "Новое в AlfaGram",
                "New in AlfaGram");
        t("Sozlamalarni ochish",
                "Открыть настройки",
                "Open settings");
        t("Keyinroq",
                "Позже",
                "Later");
        t("🔗 Shaxsiy havola — alfagram.uz/username: nusxalang, ulashing yoki QR kod (Sozlamalar → Mening AlfaGram havolam)\n\n✨ Kirill alifbosi — butun ilova o'zbek kirillida (AlfaGram sozlamalari → Til)\n\n🔤 Xabarni kirill ↔ lotinga o'girish — xabarni bosing → «Kirillga o'girish»\n\n✍️ Lotincha yozing — kirillda yuborilsin\n\n🟢 Onlayn tarixi — kontakt qachon onlayn bo'lgani (profil → ⋮)\n\n🆔 ID nusxalash — xabar menyusida va profil ⋮ menyusida\n\n👆 Ikki marta bosish — javob, nusxa yoki reaksiya (sozlamalardan tanlang)\n\n🌐 Tarjima tugmasi xabar menyusida",
                "🔗 Личная ссылка — alfagram.uz/username: копируйте, делитесь или используйте QR-код (Настройки → Моя ссылка AlfaGram)\n\n✨ Кириллица — всё приложение на узбекской кириллице (Настройки AlfaGram → Язык)\n\n🔤 Перевод сообщения кириллица ↔ латиница — нажмите на сообщение → «Показать кириллицей»\n\n✍️ Пишите латиницей — отправится кириллицей\n\n🟢 История онлайна — когда контакт был в сети (профиль → ⋮)\n\n🆔 Копирование ID — в меню сообщения и в меню ⋮ профиля\n\n👆 Двойное нажатие — ответ, копирование или реакция (выберите в настройках)\n\n🌐 Кнопка перевода в меню сообщения",
                "🔗 Personal link — alfagram.uz/username: copy, share or get a QR code (Settings → My AlfaGram link)\n\n✨ Uzbek Cyrillic — the whole app in Uzbek Cyrillic (AlfaGram Settings → Language)\n\n🔤 Convert messages Cyrillic ↔ Latin — tap a message → \"Show in Cyrillic\"\n\n✍️ Type in Latin — send in Cyrillic\n\n🟢 Online history — when a contact was online (profile → ⋮)\n\n🆔 Copy ID — in the message menu and the profile ⋮ menu\n\n👆 Double tap — reply, copy or react (choose in settings)\n\n🌐 Translate button in the message menu");
        t("Akkauntlarni boshqarish",
                "Управление аккаунтами",
                "Manage accounts");
        t("Joriy akkaunt",
                "Текущий аккаунт",
                "Current account");
        t("Boshqa akkauntlar",
                "Другие аккаунты",
                "Other accounts");
        t("Akkaunt qo'shish",
                "Добавить аккаунт",
                "Add account");
        t("AlfaGram'da %d tagacha akkaunt qo'shish mumkin.",
                "В AlfaGram можно добавить до %d аккаунтов.",
                "You can add up to %d accounts in AlfaGram.");
        t("Yashirish",
                "Скрыть",
                "Hide");
        t("Yoqilgan akkaunt «Hisoblar» ro'yxatida va akkaunt almashtirishda ko'rinmaydi, bildirishnomasi chiqmaydi. Unga faqat shu yerdan kirasiz.",
                "Отмеченный аккаунт не виден в списке «Аккаунты» и при переключении, уведомления от него не приходят. Войти в него можно только отсюда.",
                "A checked account doesn't appear in the Accounts list or the account switcher and shows no notifications. You can open it only from here.");
        t("Vaqtincha to'xtatish",
                "Приостановить",
                "Pause");
        t("To'xtatilgan akkauntdan qayta yoqilmaguncha hech qanday bildirishnoma kelmaydi.",
                "От приостановленного аккаунта не приходят уведомления, пока вы его не возобновите.",
                "A paused account sends no notifications until you resume it.");
        t("Maxfiy akkauntga kirish",
                "Вход в скрытый аккаунт",
                "Open hidden account");
        t("Kirish: %s",
                "Войти: %s",
                "Switch to %s");
        t("APK himoyasi",
                "Защита от APK",
                "APK protection");
        t("Himoya",
                "Защита",
                "Protection");
        t("APK o'rnatishni bloklash",
                "Блокировать установку APK",
                "Block APK installs");
        t("Shubhali havolalardan ogohlantirish",
                "Предупреждать о подозрительных ссылках",
                "Warn about suspicious links");
        t("Chatlardan kelgan dastur (APK) fayllar o'rnatilmaydi va avtomatik yuklanmaydi. Ochmoqchi bo'lsangiz, AlfaGram faylni tekshirib, xavfini ko'rsatadi.\n\nGuruhingiz uchun APK filtri: guruhga kiring → nomini bosing → ✏️ Tahrirlash (admin bo'lsangiz).\n\nHammasi faqat telefoningizda ishlaydi, hech qayerga yuborilmaydi.",
                "Файлы приложений (APK) из чатов не устанавливаются и не скачиваются автоматически. Если вы захотите открыть такой файл, AlfaGram проверит его и покажет, насколько он опасен.\n\nФильтр APK для вашей группы: откройте группу → нажмите на название → ✏️ Изменить (если вы администратор).\n\nВсё работает только на вашем телефоне, ничего никуда не отправляется.",
                "App files (APK) from chats aren't installed or downloaded automatically. If you want to open one, AlfaGram checks the file and shows how risky it is.\n\nAPK filter for your group: open the group → tap its name → ✏️ Edit (if you're an admin).\n\nEverything runs only on your phone; nothing is sent anywhere.");
        t("Hozircha o'zgarish yo'q",
                "Пока изменений нет",
                "No changes yet");
        t("Kontaktlaringiz yoki yozishgan odamlaringiz username'ini, profil rasmini yoki raqamini o'zgartirsa, shu yerda ko'rinadi. Ma'lumot faqat telefoningizda saqlanadi.",
                "Когда ваши контакты или собеседники меняют имя пользователя, фото профиля или номер, это появится здесь. Данные хранятся только на вашем телефоне.",
                "When your contacts or people you chat with change their username, profile photo or number, it shows up here. Data is stored only on your phone.");
        t("Bugun",
                "Сегодня",
                "Today");
        t("Kecha",
                "Вчера",
                "Yesterday");
        t("Tarixni tozalash",
                "Очистить историю",
                "Clear history");
        t("Barcha yozuvlar o'chiriladi. Yangi o'zgarishlar yozilishda davom etadi.",
                "Все записи будут удалены. Новые изменения продолжат записываться.",
                "All entries will be deleted. New changes will still be logged.");
        t("Tozalash",
                "Очистить",
                "Clear");
        t("Ochish",
                "Открыть",
                "Open");
        t("🚀 Yangi versiya",
                "🚀 Новая версия",
                "🚀 New version");
        t("Yangilash",
                "Обновить",
                "Update");
        t("o'rnatish bloklangan",
                "установка заблокирована",
                "installation blocked");
        t("virus bo'lishi mumkin",
                "может быть вирусом",
                "may be a virus");
        t("Fayl nomi niqoblangan («%s») — rasm yoki video ko'rinishida, aslida dastur",
                "Имя файла замаскировано («%s») — выглядит как фото или видео, а на самом деле это приложение",
                "The file name is disguised (“%s”) — it looks like a photo or video but is actually an app");
        t("Fayl nomi aldovchi: rasm, video, sovg'a yoki hujjat deb ko'rsatilgan — aslida dastur",
                "Обманчивое имя файла: выдаёт себя за фото, видео, подарок или документ — на самом деле это приложение",
                "Misleading file name: poses as a photo, video, gift or document — it's actually an app");
        t("O'zini «%s» deb ko'rsatyapti. Rasmiy ilovalar faqat Play Market'dan o'rnatiladi — Telegram orqali kelgan bunday fayl deyarli har doim soxta",
                "Выдаёт себя за «%s». Официальные приложения устанавливаются только из Play Маркета — такой файл из Telegram почти всегда поддельный",
                "Pretends to be “%s”. Official apps are installed only from Google Play — such a file received via Telegram is almost always fake");
        t("Faylni tekshirib bo'lmadi — buzilgan yoki ataylab yashirilgan dastur",
                "Не удалось проверить файл — повреждённое или намеренно скрытое приложение",
                "Couldn't check the file — a corrupted or deliberately disguised app");
        t("Bu fayl o'zini AlfaGram deb ko'rsatyapti. AlfaGram faqat Play Market orqali yangilanadi",
                "Этот файл выдаёт себя за AlfaGram. AlfaGram обновляется только через Play Маркет",
                "This file pretends to be AlfaGram. AlfaGram is updated only via Google Play");
        t("Dastur o'zini «%s» deb atagan. Rasmiy bank va Telegram ilovalari APK fayl qilib yuborilmaydi — bu soxta",
                "Приложение называет себя «%s». Официальные приложения банков и Telegram не присылают APK-файлом — это подделка",
                "The app calls itself “%s”. Official bank and Telegram apps are never sent as APK files — this is fake");
        t("SMS xabarlaringizni o'qiy oladi — bank va Telegram kodlarini o'g'irlaydi",
                "Может читать ваши SMS — крадёт коды банков и Telegram",
                "Can read your SMS — steals bank and Telegram codes");
        t("Sizning nomingizdan SMS yubora oladi",
                "Может отправлять SMS от вашего имени",
                "Can send SMS on your behalf");
        t("Boshqa ilovalar ustida oyna ocha oladi (soxta bank oynasi)",
                "Может показывать окна поверх других приложений (поддельное окно банка)",
                "Can display over other apps (fake bank screen)");
        t("O'zi boshqa dasturlarni o'rnata oladi",
                "Может сам устанавливать другие приложения",
                "Can install other apps on its own");
        t("Kontaktlaringizni o'qiy oladi (virusni ularga tarqatish uchun)",
                "Может читать ваши контакты (чтобы рассылать им вирус)",
                "Can read your contacts (to spread the virus to them)");
        t("Qo'ng'iroqlaringizni boshqara oladi",
                "Может управлять вашими звонками",
                "Can control your calls");
        t("Boshqa ilovalarni (antivirusni) o'chira oladi",
                "Может удалять другие приложения (антивирус)",
                "Can uninstall other apps (antivirus)");
        t("Telefoningizni masofadan boshqara oladi (maxsus imkoniyatlar xizmati)",
                "Может удалённо управлять телефоном (служба специальных возможностей)",
                "Can control your phone remotely (accessibility service)");
        t("Barcha bildirishnomalaringizni (kodlarni ham) o'qiy oladi",
                "Может читать все ваши уведомления (включая коды)",
                "Can read all your notifications (including codes)");
        t("Qurilma administratori bo'lishga urinadi — keyin o'chirib bo'lmaydi",
                "Пытается стать администратором устройства — после этого его не удалить",
                "Tries to become a device administrator — then it can't be uninstalled");
        t("Boshqa joydan forward qilingan",
                "Переслано из другого чата",
                "Forwarded from elsewhere");
        t("Bot yuborgan",
                "Отправлено ботом",
                "Sent by a bot");
        t("Yuboruvchi kontaktlaringizda yo'q",
                "Отправителя нет в ваших контактах",
                "The sender isn't in your contacts");
        t("Yuboruvchi akkaunti yaqinda ochilgan (%s)",
                "Аккаунт отправителя создан недавно (%s)",
                "The sender's account was created recently (%s)");
        t("Guruh yoki kanalda tarqatilgan",
                "Распространяется в группе или канале",
                "Spread in a group or channel");
        t("AlfaGram himoyasi yoqilgan: chatlardan kelgan dasturlar (APK) o'rnatilmaydi.\n\n",
                "Защита AlfaGram включена: приложения (APK) из чатов не устанавливаются.\n\n",
                "AlfaGram protection is on: apps (APK) from chats can't be installed.\n\n");
        t("Dastur: ",
                "Приложение: ",
                "App:");
        t("🔴 Xavf YUQORI — bu deyarli aniq virus:\n",
                "🔴 Риск ВЫСОКИЙ — это почти наверняка вирус:\n",
                "🔴 HIGH risk — this is almost certainly a virus:\n");
        t("🟠 Xavf o'rtacha:\n",
                "🟠 Средний риск:\n",
                "🟠 Medium risk:\n");
        t("🟢 Aniq xavfli belgi topilmadi. Lekin baribir faqat ishonchli odamdan kelgan dasturni o'rnating.\n",
                "🟢 Явных признаков опасности не найдено. Но всё равно устанавливайте приложения только от тех, кому доверяете.\n",
                "🟢 No clear signs of danger found. Still, only install apps from people you trust.\n");
        t("\nYuboruvchi:\n",
                "\nОтправитель:\n",
                "\nSender:\n");
        t("\nO'rnatish kerak bo'lsa: AlfaGram sozlamalari → Xavfsizlik → «APK o'rnatishni bloklash»ni o'chiring.",
                "\nЕсли нужно установить: Настройки AlfaGram → Безопасность → отключите «Блокировать установку APK».",
                "\nTo install it: AlfaGram Settings → Security → turn off “Block APK installation”.");
        t("\nVirus o'rnatilsa SMS kodlaringiz, bank kartangiz va Telegram akkauntingiz o'g'irlanadi.",
                "\nЕсли установить вирус, у вас украдут SMS-коды, банковскую карту и аккаунт Telegram.",
                "\nIf a virus gets installed, your SMS codes, bank card and Telegram account will be stolen.");
        t("🛡 APK o'rnatish bloklandi",
                "🛡 Установка APK заблокирована",
                "🛡 APK installation blocked");
        t("⚠️ Dastur fayli (APK)",
                "⚠️ Файл приложения (APK)",
                "⚠️ App file (APK)");
        t("Tushunarli",
                "Понятно",
                "Got it");
        t("Sozlamalar",
                "Настройки",
                "Settings");
        t("Baribir o'rnatish",
                "Всё равно установить",
                "Install anyway");
        t("Faylni o'chirish",
                "Удалить файл",
                "Delete file");
        t("Fayl telefondan o'chirildi",
                "Файл удалён с телефона",
                "File deleted from the phone");
        t("Himoyani o'chirasizmi?",
                "Отключить защиту?",
                "Turn off protection?");
        t("O'chirsangiz, chatlardan kelgan APK fayllarni o'rnatish mumkin bo'ladi.\n\nHozir Telegram'da «Rasmlar.apk», «Video.apk», «Taklifnoma.apk» kabi viruslar tarqalmoqda: ular SMS kodlaringizni, bank kartangizni va Telegram akkauntingizni o'g'irlaydi, keyin kontaktlaringizga o'zini yuboradi.\n\nFaqat kerakli dasturni o'rnatib bo'lgach, himoyani qayta yoqing.",
                "Если отключить, APK-файлы из чатов можно будет устанавливать.\n\nСейчас в Telegram распространяются вирусы вроде «Rasmlar.apk», «Video.apk», «Taklifnoma.apk»: они крадут SMS-коды, банковскую карту и аккаунт Telegram, а затем рассылают себя вашим контактам.\n\nУстановите нужное приложение и сразу включите защиту снова.",
                "If you turn it off, APK files from chats can be installed.\n\nViruses like “Rasmlar.apk”, “Video.apk” and “Taklifnoma.apk” are spreading on Telegram right now: they steal your SMS codes, bank card and Telegram account, then send themselves to your contacts.\n\nOnce you've installed the app you need, turn protection back on.");
        t("Yoqiq qolsin",
                "Оставить включённой",
                "Keep on");
        t("off::O'chirish",
                "Отключить",
                "Turn off");
        t("⚠️ Shubhali havola",
                "⚠️ Подозрительная ссылка",
                "⚠️ Suspicious link");
        t("Ochmaslik",
                "Не открывать",
                "Don't open");
        t("Baribir ochish",
                "Всё равно открыть",
                "Open anyway");
        t("Havola dastur (APK) yuklab beradi",
                "Ссылка скачивает приложение (APK)",
                "The link downloads an app (APK)");
        t("Manzil boshqa alifbo harflari bilan niqoblangan (%s)",
                "Адрес замаскирован буквами другого алфавита (%s)",
                "The address is disguised with letters from another alphabet (%s)");
        t("Manzil sayt nomi emas, raqamli IP",
                "Вместо имени сайта — числовой IP-адрес",
                "The address is a numeric IP, not a site name");
        t("Sayt o'zini «%1$s» qilib ko'rsatyapti, lekin rasmiy manzil emas (%2$s)",
                "Сайт выдаёт себя за «%1$s», но это не официальный адрес (%2$s)",
                "The site pretends to be “%1$s” but this isn't its official address (%2$s)");
        t("Sovg'a/bonus/premium va'da qiluvchi shubhali sayt",
                "Подозрительный сайт, обещающий подарки/бонусы/премиум",
                "Suspicious site promising gifts/bonuses/premium");
        t("\nBunday saytlar Telegram akkauntingiz, karta ma'lumotlaringiz yoki SMS kodingizni o'g'irlashi mumkin. Kodni hech kimga bermang.",
                "\nТакие сайты могут украсть ваш аккаунт Telegram, данные карты или SMS-код. Никому не сообщайте код.",
                "\nSuch sites can steal your Telegram account, card details or SMS code. Never give your code to anyone.");
        t("~ %1$s-yil %2$s",
                "~ %2$s %1$s",
                "~ %2$s %1$s");
        t("month::yanvar",
                "январь",
                "Jan");
        t("month::fevral",
                "февраль",
                "Feb");
        t("month::mart",
                "март",
                "Mar");
        t("month::aprel",
                "апрель",
                "Apr");
        t("month::may",
                "май",
                "May");
        t("month::iyun",
                "июнь",
                "Jun");
        t("month::iyul",
                "июль",
                "Jul");
        t("month::avgust",
                "август",
                "Aug");
        t("month::sentabr",
                "сентябрь",
                "Sep");
        t("month::oktabr",
                "октябрь",
                "Oct");
        t("month::noyabr",
                "ноябрь",
                "Nov");
        t("month::dekabr",
                "декабрь",
                "Dec");
        t("Urinishlar juda ko'p. Birozdan keyin qayta urinib ko'ring.",
                "Слишком много попыток. Попробуйте позже.",
                "Too many attempts. Please try again later.");
        t("Token noto'g'ri yoki bekor qilingan.",
                "Токен неверный или отозван.",
                "The token is invalid or has been revoked.");
        t("Kirib bo'lmadi",
                "Не удалось войти",
                "Couldn't log in");
        t("Himoyalangan kontent yuklab olinmoqda, so'ng yuboriladi…",
                "Защищённый контент загружается, затем будет отправлен…",
                "Downloading protected content, it will be sent afterwards…");
        t("Ba'zi xabarlarni yuborib bo'lmadi (%d)",
                "Не удалось отправить некоторые сообщения (%d)",
                "Some messages couldn't be sent (%d)");
        t("Reaksiya (standart)",
                "Реакция (по умолчанию)",
                "Reaction (default)");
        t("Javob berish",
                "Ответить",
                "Reply");
        t("Matnni nusxalash",
                "Копировать текст",
                "Copy text");
        t("Xayrli tong",
                "Доброе утро",
                "Good morning");
        t("Xayrli kun",
                "Добрый день",
                "Good afternoon");
        t("Xayrli kech",
                "Добрый вечер",
                "Good evening");
        t("Xayrli tun",
                "Доброй ночи",
                "Good night");
        t("Mening AlfaGram havolam",
                "Моя ссылка AlfaGram",
                "My AlfaGram link");
        t("Shaxsiy havola uchun avval foydalanuvchi nomi (@username) o'rnating.",
                "Чтобы получить личную ссылку, сначала задайте имя пользователя (@username).",
                "To get a personal link, set a username (@username) first.");
        t("set::O'rnatish",
                "Задать",
                "Set");
        t("Nusxalash",
                "Копировать",
                "Copy");
        t("Ulashish",
                "Поделиться",
                "Share");
        t("QR kod",
                "QR-код",
                "QR code");
        t("Havola nusxalandi",
                "Ссылка скопирована",
                "Link copied");
        t("Menga AlfaGram'da yozing: ",
                "Напишите мне в AlfaGram: ",
                "Message me on AlfaGram:");
        t("Bu QR kodni skanerlagan odam sizning AlfaGram sahifangizni ochadi.",
                "Тот, кто отсканирует этот QR-код, откроет вашу страницу в AlfaGram.",
                "Anyone who scans this QR code will open your AlfaGram page.");
        t("Ismini o'zgartirdi: ",
                "Изменил(а) имя: ",
                "Changed name:");
        t("Username qo'ydi: ",
                "Установил(а) имя пользователя: ",
                "Set username:");
        t("Username'ni olib tashladi: ",
                "Удалил(а) имя пользователя: ",
                "Removed username:");
        t("Profil rasmini o'zgartirdi",
                "Изменил(а) фото профиля",
                "Changed profile photo");
        t("Profil rasmini olib tashladi yoki yashirdi",
                "Удалил(а) или скрыл(а) фото профиля",
                "Removed or hid profile photo");
        t("Telefon raqami o'zgardi: ",
                "Номер телефона изменён: ",
                "Phone number changed:");
        t("Arxivlash",
                "Архивировать",
                "Archive");
        t("O'qildi / o'qilmagan",
                "Прочитано / непрочитано",
                "Read / unread");
        t("Ovozsiz qilish",
                "Без звука",
                "Mute");
        t("O'chirish",
                "Удалить",
                "Delete");
        t("Qadash",
                "Закрепить",
                "Pin");
        t("O'ngga surish",
                "Свайп вправо",
                "Swipe right");
        t("Chapga surish",
                "Свайп влево",
                "Swipe left");
        t("Avto papkalar yaratish",
                "Создать автопапки",
                "Create auto folders");
        t("Shaxsiy",
                "Личные",
                "Personal");
        t("Guruhlar",
                "Группы",
                "Groups");
        t("Kanallar",
                "Каналы",
                "Channels");
        t("Botlar",
                "Боты",
                "Bots");
        t("O'qilmagan",
                "Непрочитанные",
                "Unread");
        t("Hammasi",
                "Все",
                "All");
        t("Chatlar ro'yxati tepasida 5 ta papka paydo bo'ladi: Shaxsiy, Guruhlar, Kanallar, Botlar va O'qilmagan. Ular faqat AlfaGram'da ko'rinadi — boshqa Telegram ilovalarida chiqmaydi. Bor papkalaringizga tegilmaydi.",
                "Над списком чатов появятся 5 папок: Личные, Группы, Каналы, Боты и Непрочитанные. Они видны только в AlfaGram — в других приложениях Telegram их не будет. Ваши папки останутся без изменений.",
                "5 folders will appear above the chat list: Personal, Groups, Channels, Bots and Unread. They're visible only in AlfaGram — other Telegram apps won't show them. Your existing folders stay as they are.");
        t("Yaratish",
                "Создать",
                "Create");
        t("Avto papkalar tayyor",
                "Автопапки готовы",
                "Auto folders are ready");
        t("Bu papkalar allaqachon bor",
                "Эти папки уже есть",
                "These folders already exist");
        t("Parol",
                "Пароль",
                "Password");
        t("4 raqamli PIN kodni kiriting",
                "Введите 4-значный PIN-код",
                "Enter a 4-digit PIN");
        t("Parolni kiriting",
                "Введите пароль",
                "Enter password");
        t("Kod noto'g'ri",
                "Неверный код",
                "Wrong code");
        t("Qulf turi",
                "Тип блокировки",
                "Lock type");
        t("4 raqamli PIN",
                "4-значный PIN",
                "4-digit PIN");
        t("Harfli-raqamli parol",
                "Буквенно-цифровой пароль",
                "Alphanumeric password");
        t("Yangi kod",
                "Новый код",
                "New code");
        t("Qaytadan",
                "Повторите код",
                "Repeat code");
        t("Kodni ikki marta kiriting. Uni unutmang: tiklab bo'lmaydi.",
                "Введите код дважды. Не забудьте его: восстановить код нельзя.",
                "Enter the code twice. Don't forget it: it can't be recovered.");
        t("Saqlash",
                "Сохранить",
                "Save");
        t("Kodlar bir xil emas",
                "Коды не совпадают",
                "Codes don't match");
        t("PIN aynan 4 ta raqam bo'lishi kerak",
                "PIN должен состоять ровно из 4 цифр",
                "PIN must be exactly 4 digits");
        t("Parol kamida 4 belgidan iborat bo'lsin",
                "Пароль должен быть не короче 4 символов",
                "Password must be at least 4 characters");
        t("Yashirin chatlar yoqildi. Chat ichida ⋮ → «Chatni yashirish»",
                "Скрытые чаты включены. В чате: ⋮ → «Скрыть чат»",
                "Hidden chats enabled. In a chat: ⋮ → “Hide chat”");
        t("Kodni o'zgartirish",
                "Изменить код",
                "Change code");
        t("O'chirish (hamma chat ko'rinadi)",
                "Отключить (все чаты станут видны)",
                "Turn off (all chats become visible)");
        t("Yashirin chatlar yoqilmagan: Sozlamalar → AlfaGram → «Yashirin chatlar»",
                "Скрытые чаты не включены: Настройки → AlfaGram → «Скрытые чаты»",
                "Hidden chats are off: Settings → AlfaGram → “Hidden chats”");
        t("Yashirin chatlar yana yashirildi",
                "Скрытые чаты снова скрыты",
                "Hidden chats hidden again");
        t("Yashirin chatlar ko'rsatildi. Yana yashirish uchun sarlavhani bosib turing",
                "Скрытые чаты показаны. Чтобы снова скрыть, нажмите и удерживайте заголовок",
                "Hidden chats are shown. Press and hold the title to hide them again");
        t("Hisoblanmoqda…",
                "Подсчёт…",
                "Calculating…");
        t("%s yakuni",
                "%s: итоги",
                "%s recap");
        t("Yanvar",
                "Январь",
                "January");
        t("Fevral",
                "Февраль",
                "February");
        t("Mart",
                "Март",
                "March");
        t("Aprel",
                "Апрель",
                "April");
        t("May",
                "Май",
                "May");
        t("Iyun",
                "Июнь",
                "June");
        t("Iyul",
                "Июль",
                "July");
        t("Avgust",
                "Август",
                "August");
        t("Sentabr",
                "Сентябрь",
                "September");
        t("Oktabr",
                "Октябрь",
                "October");
        t("Noyabr",
                "Ноябрь",
                "November");
        t("Dekabr",
                "Декабрь",
                "December");
        t("AlfaGram Wrapped · shu qurilmadagi xabarlar bo'yicha",
                "AlfaGram Wrapped · по сообщениям на этом устройстве",
                "AlfaGram Wrapped · based on messages on this device");
        t("Bu oy uchun hali ma'lumot yo'q. Ko'proq yozishing — oy oxirida yakun shu yerda chiqadi.",
                "За этот месяц пока нет данных. Пишите больше — в конце месяца здесь появятся итоги.",
                "No data for this month yet. Keep chatting — your recap will appear here at the end of the month.");
        t("ta xabar yubordingiz",
                "отправлено сообщений",
                "messages sent");
        t("%d ta xabar oldingiz · %d kishi bilan yozishdingiz",
                "Получено: %d · собеседников: %d",
                "%d received · chatted with %d people");
        t("Eng ko'p yozishganlar",
                "Самые частые собеседники",
                "Top contacts");
        t("Eng faol soatingiz",
                "Ваш самый активный час",
                "Your most active hour");
        t("Sevimli emojilaringiz",
                "Ваши любимые эмодзи",
                "Your favorite emoji");
        t("%d ta",
                "%d сообщ.",
                "%d msgs");
        t("ta xabar yubordim",
                "отправлено сообщений",
                "messages sent");
        t("Eng ko'p yozishganlarim",
                "Мои самые частые собеседники",
                "My top contacts");
        t("Faol soatim",
                "Мой активный час",
                "My active hour");
        t("Sevimli emojilarim",
                "Мои любимые эмодзи",
                "My favorite emoji");
        t("Chat statistikasi",
                "Статистика чата",
                "Chat statistics");
        t("Rasmlar",
                "Фото",
                "Photos");
        t("Videolar",
                "Видео",
                "Videos");
        t("Ovozli xabarlar",
                "Голосовые сообщения",
                "Voice messages");
        t("Fayllar",
                "Файлы",
                "Files");
        t("Musiqa",
                "Музыка",
                "Music");
        t("Havolalar",
                "Ссылки",
                "Links");
        t("Hisoblanmoqda… %s",
                "Подсчёт… %s",
                "Calculating… %s");
        t("Tahlil qilingan xabarlar",
                "Проанализировано сообщений",
                "Messages analyzed");
        t("Siz",
                "Вы",
                "You");
        t("Suhbatdosh",
                "Собеседник",
                "Chat partner");
        t("Faollik",
                "Активность",
                "Activity");
        t("Birinchi xabar (taxminan)",
                "Первое сообщение (примерно)",
                "First message (approx.)");
        t("Eng faol vaqt",
                "Самое активное время",
                "Most active time");
        t("Dushanba",
                "Понедельник",
                "Monday");
        t("Seshanba",
                "Вторник",
                "Tuesday");
        t("Chorshanba",
                "Среда",
                "Wednesday");
        t("Payshanba",
                "Четверг",
                "Thursday");
        t("Juma",
                "Пятница",
                "Friday");
        t("Shanba",
                "Суббота",
                "Saturday");
        t("Yakshanba",
                "Воскресенье",
                "Sunday");
        t("Eng faol kun",
                "Самый активный день",
                "Most active day");
        t("Bir kundagi rekord",
                "Рекорд за день",
                "Daily record");
        t("%s ta xabar",
                "%s сообщ.",
                "%s messages");
        t("Eng ko'p ishlatilgan",
                "Чаще всего",
                "Most used");
        t("Eng faol a'zolar",
                "Самые активные участники",
                "Most active members");
        t("Media sonlari aniq. Qolgan ko'rsatkichlar oxirgi %s ta xabar bo'yicha. Hammasi telefoningizda hisoblandi.",
                "Количество медиа точное. Остальные показатели основаны на последних сообщениях: %s. Всё подсчитано на вашем телефоне.",
                "Media counts are exact. Other stats are based on the last %s messages. Everything was calculated on your phone.");
        t("HTML sahifa",
                "HTML-страница",
                "HTML page");
        t("Matn fayl (TXT)",
                "Текстовый файл (TXT)",
                "Text file (TXT)");
        t("Oxirgi 5000 tagacha xabar. Formatni tanlang.",
                "До 5000 последних сообщений. Выберите формат.",
                "Up to the last 5,000 messages. Choose a format.");
        t("Eksport qilish uchun xabar topilmadi.",
                "Нет сообщений для экспорта.",
                "No messages to export.");
        t("[Xizmat xabari]",
                "[Служебное сообщение]",
                "[Service message]");
        t("[Rasm]",
                "[Фото]",
                "[Photo]");
        t("[Ovozli xabar]",
                "[Голосовое сообщение]",
                "[Voice message]");
        t("[Dumaloq video]",
                "[Видеосообщение]",
                "[Video message]");
        t("[Stiker]",
                "[Стикер]",
                "[Sticker]");
        t("[Video]",
                "[Видео]",
                "[Video]");
        t("[Fayl]",
                "[Файл]",
                "[File]");
        t("[Media]",
                "[Медиа]",
                "[Media]");
        t("AlfaGram eksporti",
                "Экспорт AlfaGram",
                "AlfaGram export");
        t("Bu chatda hali o'chirilgan xabar yo'q.\nKimdir sizga yozib, keyin o'chirsa, xabar shu yerda saqlanadi.",
                "В этом чате пока нет удалённых сообщений.\nЕсли кто-то напишет вам и затем удалит сообщение, оно сохранится здесь.",
                "No deleted messages in this chat yet.\nIf someone writes to you and then deletes the message, it will be saved here.");
        t("Ushbu chat uchun saqlangan barcha o'chirilgan xabarlar o'chirilsinmi? Buni qaytarib bo'lmaydi.",
                "Удалить все сохранённые удалённые сообщения этого чата? Это действие нельзя отменить.",
                "Delete all saved deleted messages for this chat? This can't be undone.");
        t("Noma'lum",
                "Неизвестно",
                "Unknown");
        t("Foydalanuvchi %d",
                "Пользователь %d",
                "User %d");
        t("Chat %d",
                "Чат %d",
                "Chat %d");
        t("📷 Rasm",
                "📷 Фото",
                "📷 Photo");
        t("🎬 Video",
                "🎬 Видео",
                "🎬 Video");
        t("⏺ Video xabar",
                "⏺ Видеосообщение",
                "⏺ Video message");
        t("🎤 Ovozli xabar",
                "🎤 Голосовое сообщение",
                "🎤 Voice message");
        t("🎵 Musiqa",
                "🎵 Музыка",
                "🎵 Music");
        t("Stiker",
                "Стикер",
                "Sticker");
        t("📎 Fayl",
                "📎 Файл",
                "📎 File");
        t("[media]",
                "[медиа]",
                "[media]");
        t("Faylni ochib bo'lmadi",
                "Не удалось открыть файл",
                "Couldn't open the file");
        t(" (telefonga yuklanmagan edi)",
                " (не было загружено на телефон)",
                " (wasn't downloaded to the phone)");
        t(" — ochish uchun bosing",
                " — нажмите, чтобы открыть",
                " — tap to open");
        t("Bu chatda hali tahrirlangan xabar yo'q.\nKimdir xabarni tahrir qilsa, avvalgi matn shu yerda saqlanadi.",
                "В этом чате пока нет изменённых сообщений.\nЕсли кто-то изменит сообщение, прежний текст сохранится здесь.",
                "No edited messages in this chat yet.\nIf someone edits a message, the previous text will be saved here.");
        t("Ushbu chatning tahrir tarixi butunlay o'chirilsinmi?",
                "Полностью удалить историю изменений этого чата?",
                "Delete this chat's entire edit history?");
        t("Hozircha hech kim yozib turib to'xtatmagan.\nKimdir yozib, xabar yubormasdan to'xtatsa, shu yerda ko'rinadi.",
                "Пока никто не прерывал набор.\nЕсли кто-то начнёт печатать и остановится, не отправив сообщение, это появится здесь.",
                "Nobody has stopped typing yet.\nIf someone starts typing and stops without sending, it will appear here.");
        t("Ushbu chatning \"yozdi, yubormadi\" jurnali o'chirilsinmi?",
                "Удалить журнал «Печатал, но не отправил» для этого чата?",
                "Delete the \"Typed, not sent\" log for this chat?");
        t("~%d soniya yozdi, lekin xabar yubormadi",
                "Печатал ~%d с, но не отправил",
                "Typed for ~%d s, but didn't send");
        t("Videoni tanlang",
                "Выберите видео",
                "Choose a video");
    }

    private static void part3() {
        t("Galereyani ochib bo'lmadi",
                "Не удалось открыть галерею",
                "Couldn't open the gallery");
        t("Video 1 daqiqadan uzun. Dumaloq video faqat 1 daqiqagacha bo'lgan videodan qilinadi.",
                "Видео длиннее 1 минуты. Видеосообщение можно сделать только из видео до 1 минуты.",
                "The video is longer than 1 minute. Video messages can only be made from videos up to 1 minute long.");
        t("Bu videoni dumaloq qilib bo'lmadi",
                "Не удалось сделать видеосообщение из этого видео",
                "Couldn't turn this video into a video message");
        t("Matn nusxalandi",
                "Текст скопирован",
                "Text copied");
        t("Qulflangan chatni ochish",
                "Открыть заблокированный чат",
                "Unlock chat");
        t("Chat qulfdan chiqarildi",
                "Блокировка чата снята",
                "Chat unlocked");
        t("Chat qulflandi: ochish uchun barmoq izi yoki PIN so'raladi",
                "Чат заблокирован: для открытия потребуется отпечаток пальца или PIN",
                "Chat locked: fingerprint or PIN will be required to open it");
        t("Chat qulfini olib tashlash",
                "Снять блокировку чата",
                "Remove chat lock");
        t("Chat yana ro'yxatda",
                "Чат снова в списке",
                "Chat is back in the list");
        t("Chat yashirildi. Ko'rish uchun «AlfaGram» sarlavhasini bosib turing",
                "Чат скрыт. Чтобы увидеть его, зажмите заголовок «AlfaGram»",
                "Chat hidden. Press and hold the \"AlfaGram\" title to see it");
        t("Qulfni olib tashlash",
                "Снять блокировку",
                "Remove lock");
        t("Chatni qulflash",
                "Заблокировать чат",
                "Lock chat");
        t("Yashirishdan chiqarish",
                "Вернуть из скрытых",
                "Unhide chat");
        t("Chatni yashirish",
                "Скрыть чат",
                "Hide chat");
        t("Hammasini belgilash",
                "Выбрать все",
                "Select all");
        t("Hozirgi:\n",
                "Текущая версия:\n",
                "Current:\n");
        t("O'chirilgan xabar",
                "Удалённое сообщение",
                "Deleted message");
        t("Bu xabarni yuboruvchi o'chirgan. AlfaGram uni sizning chatingizda ushlab qoldi.\n\nBarcha o'chirilgan xabarlar ro'yxatini menyudan (\"O'chirilgan xabarlar\") ochsangiz ko'rasiz.",
                "Отправитель удалил это сообщение. AlfaGram сохранил его в вашем чате.\n\nСписок всех удалённых сообщений можно открыть в меню («Удалённые сообщения»).",
                "The sender deleted this message. AlfaGram kept it in your chat.\n\nYou can see all deleted messages from the menu (\"Deleted messages\").");
        t("Lotin alifbosida",
                "Латиницей",
                "In Latin script");
        t("Kirill alifbosida",
                "Кириллицей",
                "In Cyrillic script");
        t("Xabar ID nusxalandi: ",
                "ID сообщения скопирован: ",
                "Message ID copied:");
        t("Lotinga o'girish",
                "Показать латиницей",
                "Show in Latin");
        t("Kirillga o'girish",
                "Показать кириллицей",
                "Show in Cyrillic");
        t("O'chirilgan xabar (yuboruvchi o'chirgan)",
                "Удалённое сообщение (удалено отправителем)",
                "Deleted message (deleted by sender)");
        t("ID nusxalash",
                "Копировать ID",
                "Copy ID");
        t("APK filtri",
                "Фильтр APK",
                "APK filter");
        t("APK filtri yoqildi: a'zolar yuborgan APK hamma uchun avtomatik o'chiriladi",
                "Фильтр APK включён: APK от участников будут автоматически удаляться у всех",
                "APK filter on: APKs sent by members will be deleted for everyone automatically");
        t("APK filtri o'chirildi",
                "Фильтр APK выключен",
                "APK filter off");
        t("Ghost yoqildi — onlayn va «yozyapti» ko'rinmaydi",
                "Режим призрака включён — ваш онлайн и «печатает» не видны",
                "Ghost Mode on — your online status and \"typing\" are hidden");
        t("Ghost o'chirildi",
                "Режим призрака выключен",
                "Ghost Mode off");
        t("Kimni tabriklaysiz?",
                "Кого поздравить?",
                "Who do you want to congratulate?");
        t("\uD83C\uDF82 Bugun %sning tug'ilgan kuni",
                "\uD83C\uDF82 Сегодня день рождения у %s",
                "\uD83C\uDF82 Today is %s's birthday");
        t("\uD83C\uDF82 Bugun %d kishining tug'ilgan kuni",
                "\uD83C\uDF82 Дни рождения сегодня: %d",
                "\uD83C\uDF82 %d birthdays today");
        t("Bosing \u2014 tayyor tabrik bilan chat ochiladi",
                "Нажмите \u2014 откроется чат с готовым поздравлением",
                "Tap to open the chat with a ready-made greeting");
        t("\uD83D\uDCCA O'tgan oy yakuni tayyor",
                "\uD83D\uDCCA Итоги прошлого месяца готовы",
                "\uD83D\uDCCA Your last month recap is ready");
        t("Kim bilan ko'p yozishdingiz, faol soatingiz va sevimli emojilaringiz",
                "С кем вы больше всего переписывались, ваши активные часы и любимые эмодзи",
                "Who you chatted with most, your active hours and favorite emoji");
        t("\uD83C\uDF89 Tug'ilgan kuningiz muborak, %s! Sizga sog'lik, baxt va omad tilayman! \uD83C\uDF82",
                "\uD83C\uDF89 С днём рождения, %s! Желаю здоровья, счастья и удачи! \uD83C\uDF82",
                "\uD83C\uDF89 Happy birthday, %s! Wishing you health, happiness and good luck! \uD83C\uDF82");
        t("Barcha chatlar yuklanmoqda...",
                "Загрузка всех чатов...",
                "Loading all chats...");
        t("Belgilandi: ",
                "Выбрано: ",
                "Selected:");
        t("Bu papka faqat AlfaGram'da, shu telefonda ko'rinadi — boshqa Telegram ilovalarida chiqmaydi.",
                "Эта папка видна только в AlfaGram на этом телефоне — в других приложениях Telegram её не будет.",
                "This folder is visible only in AlfaGram on this phone — it won't appear in other Telegram apps.");
        t("Orqaga",
                "Назад",
                "Back");
        t("Bot rejimi cheklangan: chatlar ro'yxati va eski xabarlar ko'rinmaydi",
                "Режим бота ограничен: список чатов и старые сообщения недоступны",
                "Bot mode is limited: the chat list and old messages aren't available");
        t("Tizimga kirish",
                "Войти",
                "Log in");
        t("Tokenni @BotFather beradi: /newbot yoki /token buyrug'i.",
                "Токен выдаёт @BotFather: команда /newbot или /token.",
                "Get a token from @BotFather with the /newbot or /token command.");
        t("Token noto'g'ri. U 123456789:ABCdef... ko'rinishida bo'ladi.",
                "Неверный токен. Он выглядит так: 123456789:ABCdef...",
                "Invalid token. It looks like 123456789:ABCdef...");
        // QR kod bilan kirish (LoginActivity + AlfaQrLoginDialog). «Qayta urinish», «Orqaga» — yuqorida/pastda bor.
        t("QR kod bilan kirish",
                "Вход по QR-коду",
                "Log in with QR code");
        t("Telefoningizda Telegram'ni oching",
                "Откройте Telegram на телефоне",
                "Open Telegram on your phone");
        t("Sozlamalar → Qurilmalar → Qurilma ulash",
                "Настройки → Устройства → Подключить устройство",
                "Settings → Devices → Link Desktop Device");
        t("Shu QR kodni skanerlang",
                "Отсканируйте этот QR-код",
                "Scan this QR code");
        t("Kirilmoqda...",
                "Выполняется вход...",
                "Logging in...");
        t("Kirib bo'lmadi. Qaytadan urinib ko'ring.",
                "Не удалось войти. Попробуйте ещё раз.",
                "Couldn't log in. Please try again.");
        t("Juda ko'p urinish. Birozdan keyin qayta urining.",
                "Слишком много попыток. Повторите чуть позже.",
                "Too many attempts. Please try again a bit later.");
        t("QR kodni olib bo'lmadi. Internetni tekshirib, qayta urining.",
                "Не удалось получить QR-код. Проверьте интернет и повторите.",
                "Couldn't get a QR code. Check your connection and try again.");
        t("Telefon raqam bilan kirish",
                "Войти по номеру телефона",
                "Log in with phone number");
        t("Yoqiq",
                "Вкл.",
                "On");
        t("AlfaGram havolasi",
                "Ссылка AlfaGram",
                "AlfaGram link");
        t("ID nusxa olindi: ",
                "ID скопирован: ",
                "ID copied:");
        t("ID nusxalandi",
                "ID скопирован",
                "ID copied");
        t("Ro'yxatdan o'tgan sana",
                "Дата регистрации",
                "Registration date");
        t("Telegram bu ma'lumotni bermaydi. AlfaGram uni foydalanuvchi ID raqamiga qarab taxminan hisoblaydi: ID qancha katta bo'lsa, akkaunt shuncha yangi. Bir necha oy xato bo'lishi mumkin.",
                "Telegram не предоставляет эти данные. AlfaGram примерно вычисляет их по ID пользователя: чем больше ID, тем новее аккаунт. Погрешность может составлять несколько месяцев.",
                "Telegram doesn't provide this information. AlfaGram estimates it from the user ID: the higher the ID, the newer the account. It may be off by a few months.");
        t("Hali yozuv yo'q. AlfaGram bu kontakt qachon onlayn bo'lganini kuzatib boradi (ilova internetga ulangan paytlarda). Kontakt «oxirgi marta» vaqtini yashirgan bo'lsa, aniq vaqt ko'rinmaydi.",
                "Записей пока нет. AlfaGram отслеживает, когда этот контакт бывает в сети (пока приложение подключено к интернету). Если контакт скрыл время последнего визита, точное время не видно.",
                "No records yet. AlfaGram tracks when this contact is online (while the app is connected to the internet). If the contact hides their last seen time, the exact time isn't visible.");
        t("  — oxirgi marta onlayn",
                "  — был(а) в сети в последний раз",
                "  — last seen online");
        t("hozir onlayn",
                "сейчас в сети",
                "online now");
        t(" daq",
                " мин",
                " min");
        t("NUSXALASH",
                "КОПИРОВАТЬ",
                "COPY");
        t("Ro'yxatdan o'tgan (taxminan)",
                "Дата регистрации (примерно)",
                "Registered (approx.)");
        t("Ko'rish",
                "Посмотреть",
                "View");
        t("Fayl hali yuklanmagan",
                "Файл ещё не загружен",
                "File not downloaded yet");
        t("Galereyaga saqlandi",
                "Сохранено в галерею",
                "Saved to gallery");
        t("Galereyaga saqlash",
                "Сохранить в галерею",
                "Save to gallery");
        t("Yashirin funksiyalar va sozlamalar",
                "Скрытые функции и настройки",
                "Hidden features and settings");
        t("Nusxalash, ulashish va QR kod",
                "Копировать, поделиться и QR-код",
                "Copy, share and QR code");
        t("Yordam guruhi",
                "Группа поддержки",
                "Support group");
        t("Savol, taklif va xatolar — @alfagram_chat",
                "Вопросы, предложения и ошибки — @alfagram_chat",
                "Questions, suggestions and bugs — @alfagram_chat");
        t("Qayta yoqish",
                "Возобновить",
                "Resume");
        t("Akkaunt qayta yoqildi",
                "Аккаунт возобновлён",
                "Account resumed");
        t("Akkaunt to'xtatildi: bildirishnoma kelmaydi",
                "Аккаунт приостановлен: уведомления не будут приходить",
                "Account paused: no notifications will arrive");
        t("Akkaunt maxfiy akkauntlarga o'tdi",
                "Аккаунт перемещён в скрытые",
                "Account moved to hidden accounts");
        t("Chatni ekranning o'ng chetidan chapga yoki chap chetidan o'ngga torting. Papkalar bo'lsa, o'rtadan surish papkani almashtiradi.",
                "Потяните чат влево от правого края экрана или вправо от левого края. Если есть папки, свайп из середины переключает папку.",
                "Swipe a chat left from the right edge of the screen or right from the left edge. If you have folders, swiping from the middle switches folders.");
        t("🔒 Qulflangan chat",
                "🔒 Заблокированный чат",
                "🔒 Locked chat");
        t("Yuborilsinmi?",
                "Отправить?",
                "Send?");
        t("Xabar tayyor.",
                "Сообщение готово.",
                "Message is ready.");
        t("Old kamera",
                "Фронтальная камера",
                "Front camera");
        t("Orqa kamera",
                "Основная камера",
                "Back camera");
        t("Dumaloq video",
                "Видеосообщение",
                "Video message");
        t("Video qo'ng'iroq qilinsinmi?",
                "Начать видеозвонок?",
                "Start a video call?");
        t("Qo'ng'iroq qilinsinmi?",
                "Позвонить?",
                "Start a call?");
        t("Qo'ng'iroq qilish",
                "Позвонить",
                "Call");
        t("Bu story uchun hali saqlangan ko'ruvchi yo'q.\n\n\"Ko'ruvchilar\"ni ochsangiz, AlfaGram avtomatik saqlaydi — story o'chib ketsa ham qoladi.",
                "Для этой истории ещё нет сохранённых зрителей.\n\nОткройте «Зрители» — AlfaGram сохранит их автоматически, и они останутся даже после удаления истории.",
                "No saved viewers for this story yet.\n\nOpen \"Viewers\" and AlfaGram will save them automatically — they stay even after the story is gone.");
        t("%d ko'ruvchi saqlangan",
                "Сохранено зрителей: %d",
                "%d viewers saved");
        t("Bu story — ko'ruvchilar tarixi",
                "Зрители этой истории",
                "Viewers of this story");
        t("icon::Oq",
                "Белая",
                "White");
        t("icon::Qora",
                "Чёрная",
                "Black");
        t("icon::Oltin",
                "Золотая",
                "Gold");
        t("icon::Binafsha",
                "Фиолетовая",
                "Purple");
        t("icon::Neon",
                "Неон",
                "Neon");
        // 6994: Oy yakuni (server bo'yicha hisob)
        t("Xabarlaringiz sanalmoqda: %d%%",
                "Считаем ваши сообщения: %d%%",
                "Counting your messages: %d%%");
        t("Telegram serveridagi xabarlaringiz bo'yicha",
                "По вашим сообщениям на сервере Telegram",
                "Based on your messages on Telegram servers");
        t("Hisoblab bo'lmadi. Internetni tekshirib, qayta urinib ko'ring.",
                "Не удалось подсчитать. Проверьте интернет и попробуйте снова.",
                "Couldn't calculate. Check your connection and try again.");
        t("Qayta urinish",
                "Повторить",
                "Try again");
        t("Eng faol kuningiz",
                "Самый активный день",
                "Your most active day");
        t("%d ta faol chat tekshirildi · natija faqat telefoningizda",
                "Проверено активных чатов: %d · результат только на вашем телефоне",
                "%d active chats checked · results stay on your phone");
            // 6994: «AlfaGram haqida» yangi bandlari
        t("Sukut bo'yicha yoqilgan: Sozlamalar → Maxfiylik va xavfsizlik → APK himoyasi.",
                "Включено по умолчанию: Настройки → Конфиденциальность → Защита от APK.",
                "On by default: Settings → Privacy and Security → APK protection.");
        t("Sozlamalar → AlfaGram → «Oy yakuni (AlfaGram Wrapped)».",
                "Настройки → AlfaGram → «Итоги месяца (AlfaGram Wrapped)».",
                "Settings → AlfaGram → \"Monthly recap (AlfaGram Wrapped)\".");
        t("Oy davomida nechta xabar yozganingiz, eng ko'p yozishgan odamlaringiz, faol soat va kuningiz, sevimli emojilaringiz — Telegram serveridagi aniq sonlar bilan. Rasm qilib ulashish mumkin.",
                "Сколько сообщений вы написали за месяц, с кем переписывались больше всего, самые активные час и день, любимые эмодзи — точные цифры с сервера Telegram. Можно поделиться картинкой.",
                "How many messages you sent this month, who you chatted with most, your most active hour and day, favorite emoji — exact numbers from Telegram servers. Share it as an image.");
        t("Kirish ekranida «QR kod bilan kirish» tugmasini bosing.",
                "На экране входа нажмите «Войти по QR-коду».",
                "On the login screen, tap \"Log in with QR code\".");
        t("Boshqa telefondagi Telegram'da Sozlamalar → Qurilmalar → «Qurilma ulash» orqali QR kodni skanerlang — SMS kod kutish shart emas.",
                "В Telegram на другом телефоне откройте Настройки → Устройства → «Подключить устройство» и отсканируйте QR-код — ждать SMS не нужно.",
                "In Telegram on another phone open Settings → Devices → \"Link Desktop Device\" and scan the QR code — no need to wait for an SMS.");
            t("attach::Dumaloq",
                "Кружок",
                "Round");
            t("Barcha imkoniyatlar bepul",
                "Все функции бесплатны",
                "All features are free");
    }
}
