package org.telegram.messenger;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.HashSet;
import java.util.Set;

/**
 * AlfaGram — chat qulfi (iOS bilan bir xil): qulflangan chat barmoq izi / yuz / telefon PIN'i bilan ochiladi,
 * ro'yxatda, oldindan ko'rishda va bildirishnomada matni ko'rinmaydi, ilova fonga o'tganda yana qulflanadi.
 * Saqlash: "alfa_features" → "alfaLockedChats" = {"<userId>:<dialogId>"}.
 */
public final class AlfaChatLock {

    private static final String KEY = "alfaLockedChats";
    /** Shu ochiq sessiyada ochib qo'yilganlar (fonga o'tganda tozalanadi). */
    private static final HashSet<String> unlocked = new HashSet<>();

    private AlfaChatLock() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("alfa_features", 0);
    }

    private static String entry(int account, long dialogId) {
        return UserConfig.getInstance(account).getClientUserId() + ":" + dialogId;
    }

    public static boolean isLocked(int account, long dialogId) {
        if (dialogId == 0) {
            return false;
        }
        Set<String> set = prefs().getStringSet(KEY, null);
        return set != null && set.contains(entry(account, dialogId));
    }

    public static boolean needsUnlock(int account, long dialogId) {
        return isLocked(account, dialogId) && !unlocked.contains(entry(account, dialogId));
    }

    public static void setLocked(int account, long dialogId, boolean locked) {
        String value = entry(account, dialogId);
        HashSet<String> set = new HashSet<>(prefs().getStringSet(KEY, new HashSet<>()));
        if (locked) {
            set.add(value);
        } else {
            set.remove(value);
        }
        prefs().edit().putStringSet(KEY, set).apply();
        unlocked.remove(value);
    }

    public static void markUnlocked(int account, long dialogId) {
        unlocked.add(entry(account, dialogId));
    }

    /** Ilova fonga o'tdi — hamma qulflangan chat yana so'raydi. */
    public static void relockAll() {
        unlocked.clear();
    }

    /**
     * Barmoq izi / yuz yoki telefon PIN'i. Telefonda umuman ekran qulfi bo'lmasa, tekshiradigan narsa yo'q —
     * o'tkaziladi (iOS'dagi kabi).
     */
    public static void authenticate(FragmentActivity activity, String title, Runnable onSuccess) {
        if (activity == null) {
            return;
        }
        final int authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        try {
            if (BiometricManager.from(activity).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
                // Telefonda ekran qulfi yo'q (yoki tizim tekshira olmaydi): chat o'z egasidan qulflanib qolmasin.
                onSuccess.run();
                return;
            }
            BiometricPrompt prompt = new BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    onSuccess.run();
                }
            });
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setAllowedAuthenticators(authenticators)
                    .build();
            prompt.authenticate(info);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
