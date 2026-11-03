package org.telegram.messenger;

import java.util.ArrayList;

/**
 * AlfaGram — faqat shu qurilmada saqlanadigan papkalar (avto papkalar).
 * Telegram serveriga yuborilmaydi, shuning uchun boshqa Telegram ilovalarida ko'rinmaydi.
 * Server papka id'lari 2..255 oralig'ida — undan kattasi lokal papka hisoblanadi.
 */
public final class AlfaLocalFolders {

    public static final int BASE_ID = 1000;

    private AlfaLocalFolders() {
    }

    public static boolean isLocal(int filterId) {
        return filterId >= BASE_ID;
    }

    public static boolean isLocal(MessagesController.DialogFilter filter) {
        return filter != null && isLocal(filter.id);
    }

    /** Ro'yxatdagi lokal papkalar soni (server limitiga kirmaydi). */
    public static int count(ArrayList<MessagesController.DialogFilter> filters) {
        int n = 0;
        if (filters != null) {
            for (int i = 0, N = filters.size(); i < N; i++) {
                if (isLocal(filters.get(i))) {
                    n++;
                }
            }
        }
        return n;
    }
}
