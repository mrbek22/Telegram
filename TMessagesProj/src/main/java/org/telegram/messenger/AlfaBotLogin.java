package org.telegram.messenger;

import org.telegram.tgnet.InputSerializedData;
import org.telegram.tgnet.OutputSerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

/**
 * AlfaGram — bot token bilan kirish (iOS'dagi kabi). Telegram'ning auth.importBotAuthorization so'rovi;
 * javobi oddiy auth.Authorization, shuning uchun LoginActivity.onAuthSuccess bilan yakunlanadi.
 * Bot rejimi cheklangan: Telegram botlarga eski chatlar ro'yxati va tarixni bermaydi.
 */
public final class AlfaBotLogin {

    private AlfaBotLogin() {
    }

    /** auth.importBotAuthorization#67a3ff2c flags:int api_id:int api_hash:string bot_auth_token:string = auth.Authorization */
    public static class TL_auth_importBotAuthorization extends TLObject {
        public static final int constructor = 0x67a3ff2c;

        public int flags;
        public int api_id;
        public String api_hash;
        public String bot_auth_token;

        @Override
        public TLObject deserializeResponse(InputSerializedData stream, int constructor, boolean exception) {
            return TLRPC.auth_Authorization.TLdeserialize(stream, constructor, exception);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt32(api_id);
            stream.writeString(api_hash);
            stream.writeString(bot_auth_token);
        }
    }

    /** Token ko'rinishi: «123456789:ABCdef…». */
    public static boolean looksLikeToken(String token) {
        return token != null && token.contains(":") && token.length() > 20;
    }

    /** Server xatosini foydalanuvchiga tushunarli matnga. */
    public static String errorText(TLRPC.TL_error error) {
        String text = error != null && error.text != null ? error.text : "";
        if (text.startsWith("FLOOD_WAIT")) {
            return AlfaCyrillic.ui("Urinishlar juda ko'p. Birozdan keyin qayta urinib ko'ring.");
        } else if (text.contains("TOKEN")) {
            return AlfaCyrillic.ui("Token noto'g'ri yoki bekor qilingan.");
        }
        return AlfaCyrillic.ui("Kirib bo'lmadi") + (text.isEmpty() ? "" : ": " + text);
    }
}
