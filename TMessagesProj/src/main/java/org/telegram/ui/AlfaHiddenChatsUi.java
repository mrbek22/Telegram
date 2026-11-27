package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.AlfaHiddenChats;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

/** AlfaGram — yashirin chatlar kodi: o'rnatish (4 raqamli PIN yoki parol), so'rash, ko'rsatish / yashirish. */
public class AlfaHiddenChatsUi {

    private static EditTextBoldCursor field(Context context, boolean numeric, String hint) {
        EditTextBoldCursor field = new EditTextBoldCursor(context);
        field.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        field.setGravity(Gravity.CENTER);
        field.setHint(hint);
        field.setSingleLine(true);
        field.setInputType(numeric
                ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        field.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        field.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        field.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
        return field;
    }

    /** Kodni so'raydi; to'g'ri bo'lsa onSuccess. */
    public static void askCode(BaseFragment fragment, String title, Runnable onSuccess) {
        Context context = fragment == null ? null : fragment.getParentActivity();
        if (context == null) {
            return;
        }
        boolean numeric = AlfaHiddenChats.isNumericCode();
        EditTextBoldCursor field = field(context, numeric, numeric ? "••••" : AlfaCyrillic.ui("Parol"));
        FrameLayout container = new FrameLayout(context);
        container.addView(field, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 40, Gravity.TOP, 24, 6, 24, 0));
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(AlfaCyrillic.ui(numeric ? "4 raqamli PIN kodni kiriting" : "Parolni kiriting"))
                .setView(container)
                .setPositiveButton(AlfaCyrillic.ui("Ochish"), (d, w) -> {
                    String code = field.getText() == null ? "" : field.getText().toString();
                    if (AlfaHiddenChats.checkCode(code)) {
                        onSuccess.run();
                    } else {
                        BulletinFactory.of(fragment).createErrorBulletin(AlfaCyrillic.ui("Kod noto'g'ri")).show();
                    }
                })
                .setNegativeButton(AlfaCyrillic.ui("Bekor qilish"), null)
                .create();
        fragment.showDialog(dialog);
        field.requestFocus();
        AndroidUtilities.runOnUIThread(() -> AndroidUtilities.showKeyboard(field), 200);
    }

    /** Qulf turi → kod ikki marta. */
    public static void setup(BaseFragment fragment, Runnable onDone) {
        Context context = fragment == null ? null : fragment.getParentActivity();
        if (context == null) {
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle(AlfaCyrillic.ui("Qulf turi"))
                .setItems(new CharSequence[]{AlfaCyrillic.ui("4 raqamli PIN"), AlfaCyrillic.ui("Harfli-raqamli parol")}, (d, which) -> enterNewCode(fragment, which == 0, onDone))
                .setNegativeButton(AlfaCyrillic.ui("Bekor qilish"), null)
                .show();
    }

    private static void enterNewCode(BaseFragment fragment, boolean numeric, Runnable onDone) {
        Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        EditTextBoldCursor first = field(context, numeric, AlfaCyrillic.ui("Yangi kod"));
        EditTextBoldCursor second = field(context, numeric, AlfaCyrillic.ui("Qaytadan"));
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(first, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40, 24, 6, 24, 0));
        column.addView(second, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40, 24, 10, 24, 0));
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(AlfaCyrillic.ui(numeric ? "4 raqamli PIN" : "Harfli-raqamli parol"))
                .setMessage(AlfaCyrillic.ui("Kodni ikki marta kiriting. Uni unutmang: tiklab bo'lmaydi."))
                .setView(column)
                .setPositiveButton(AlfaCyrillic.ui("Saqlash"), (d, w) -> {
                    String a = first.getText() == null ? "" : first.getText().toString();
                    String b = second.getText() == null ? "" : second.getText().toString();
                    String problem = null;
                    if (!a.equals(b)) {
                        problem = "Kodlar bir xil emas";
                    } else if (numeric && (a.length() != 4 || !a.matches("[0-9]{4}"))) {
                        problem = "PIN aynan 4 ta raqam bo'lishi kerak";
                    } else if (!numeric && a.length() < 4) {
                        problem = "Parol kamida 4 belgidan iborat bo'lsin";
                    }
                    if (problem != null) {
                        BulletinFactory.of(fragment).createErrorBulletin(AlfaCyrillic.ui(problem)).show();
                        return;
                    }
                    AlfaHiddenChats.setCode(a, numeric);
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.passcode_lock_close, AlfaCyrillic.ui("Yashirin chatlar yoqildi. Chat ichida ⋮ → «Chatni yashirish»")).show();
                    if (onDone != null) {
                        onDone.run();
                    }
                })
                .setNegativeButton(AlfaCyrillic.ui("Bekor qilish"), null)
                .create();
        fragment.showDialog(dialog);
        first.requestFocus();
        AndroidUtilities.runOnUIThread(() -> AndroidUtilities.showKeyboard(first), 200);
    }

    /** Sozlamalardan: yoqish, yoki (kod bilan) kodni o'zgartirish / o'chirish. */
    public static void manage(BaseFragment fragment, Runnable onDone) {
        if (!AlfaHiddenChats.isEnabled()) {
            setup(fragment, onDone);
            return;
        }
        askCode(fragment, AlfaCyrillic.ui("Yashirin chatlar"), () -> {
            Context context = fragment.getParentActivity();
            if (context == null) {
                return;
            }
            new AlertDialog.Builder(context)
                    .setTitle(AlfaCyrillic.ui("Yashirin chatlar"))
                    .setItems(new CharSequence[]{AlfaCyrillic.ui("Kodni o'zgartirish"), AlfaCyrillic.ui("O'chirish (hamma chat ko'rinadi)")}, (d, which) -> {
                        if (which == 0) {
                            setup(fragment, onDone);
                        } else {
                            AlfaHiddenChats.disable();
                            if (onDone != null) {
                                onDone.run();
                            }
                        }
                    })
                    .setNegativeButton(AlfaCyrillic.ui("Bekor qilish"), null)
                    .show();
        });
    }

    /** Chatlar sarlavhasini bosib turish: yashirin chatlarni ko'rsatadi (kod bilan) yoki yana yashiradi. */
    public static void toggleReveal(BaseFragment fragment) {
        if (!AlfaHiddenChats.isEnabled()) {
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.info, AlfaCyrillic.ui("Yashirin chatlar yoqilmagan: Sozlamalar → AlfaGram → «Yashirin chatlar»")).show();
            return;
        }
        if (AlfaHiddenChats.isRevealed()) {
            AlfaHiddenChats.setRevealed(false);
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.passcode_lock_close, AlfaCyrillic.ui("Yashirin chatlar yana yashirildi")).show();
            return;
        }
        askCode(fragment, AlfaCyrillic.ui("Yashirin chatlar"), () -> {
            AlfaHiddenChats.setRevealed(true);
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.info, AlfaCyrillic.ui("Yashirin chatlar ko'rsatildi. Yana yashirish uchun sarlavhani bosib turing")).show();
        });
    }
}
