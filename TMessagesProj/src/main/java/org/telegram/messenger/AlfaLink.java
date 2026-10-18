package org.telegram.messenger;

import android.content.Intent;
import android.text.TextUtils;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.QRCodeBottomSheet;

/**
 * AlfaGram — shaxsiy havola: https://alfagram.uz/&lt;username&gt; (sayt profil kartasini ko'rsatadi).
 */
public class AlfaLink {

    public static final String BASE = "https://alfagram.uz/";

    /** Foydalanuvchi uchun havola (username bo'lmasa null). */
    public static String linkFor(TLRPC.User user) {
        String username = UserObject.getPublicUsername(user);
        return TextUtils.isEmpty(username) ? null : BASE + username;
    }

    /** Joriy foydalanuvchining havolasi: nusxalash / ulashish / QR. */
    public static void showMine(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) return;
        TLRPC.User me = UserConfig.getInstance(fragment.getCurrentAccount()).getCurrentUser();
        String link = linkFor(me);
        if (link == null) {
            AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
            b.setTitle(AlfaCyrillic.ui("Mening AlfaGram havolam"));
            b.setMessage(AlfaCyrillic.ui("Shaxsiy havola uchun avval foydalanuvchi nomi (@username) o'rnating."));
            b.setPositiveButton(AlfaCyrillic.ui("O'rnatish"), (d, w) -> {
                try {
                    fragment.presentFragment(new org.telegram.ui.ChangeUsernameActivity());
                } catch (Throwable ignore) {
                }
            });
            b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            fragment.showDialog(b.create());
            return;
        }
        show(fragment, AlfaCyrillic.ui("Mening AlfaGram havolam"), link, true);
    }

    /** Istalgan havola uchun amallar oynasi. */
    public static void show(BaseFragment fragment, String title, String link, boolean mine) {
        if (fragment == null || fragment.getParentActivity() == null || link == null) return;
        CharSequence[] items = {
                AlfaCyrillic.ui("Nusxalash"),
                AlfaCyrillic.ui("Ulashish"),
                AlfaCyrillic.ui("QR kod"),
        };
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        b.setTitle(title);
        b.setMessage(link);
        b.setItems(items, (d, which) -> {
            try {
                if (which == 0) {
                    AndroidUtilities.addToClipboard(link);
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.copy, AlfaCyrillic.ui("Havola nusxalandi")).show();
                } else if (which == 1) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    String text = mine
                            ? AlfaCyrillic.ui("Menga AlfaGram'da yozing: ") + link
                            : link;
                    intent.putExtra(Intent.EXTRA_TEXT, text);
                    fragment.getParentActivity().startActivity(Intent.createChooser(intent, AlfaCyrillic.ui("Ulashish")));
                } else {
                    new QRCodeBottomSheet(fragment.getParentActivity(), title, link,
                            AlfaCyrillic.ui("Bu QR kodni skanerlagan odam sizning AlfaGram sahifangizni ochadi."), true,
                            fragment.getResourceProvider()).show();
                }
            } catch (Throwable ignore) {
            }
        });
        fragment.showDialog(b.create());
    }
}
