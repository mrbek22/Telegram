package org.telegram.ui.Components;

import android.content.Intent;
import android.net.Uri;

import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ChatActivity;

/**
 * AlfaGram — galereyadagi oddiy videoni dumaloq video xabar qilib yuborish (iOS'dagi 📎 → «Dumaloq video»).
 * Video o'rtasidan kvadrat qilib kesiladi (Telegram konvertorining cropState'i bilan), 1 daqiqagacha olinadi.
 */
public class AlfaRoundVideoFromGallery {

    public static final int REQUEST_CODE = 3911;
    /** Dumaloq video tomoni (Telegram kamerasi 360 yozadi; 384 — yaqin, 16 ga karrali). */
    private static final int SIDE = 384;
    private static final long MAX_MS = 60_000L;
    private static final int BITRATE = 1_000_000;

    public static void pick(ChatActivity fragment) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            fragment.startActivityForResult(Intent.createChooser(intent, AlfaCyrillic.ui("Videoni tanlang")), REQUEST_CODE);
        } catch (Throwable e) {
            FileLog.e(e);
            BulletinFactory.of(fragment).createErrorBulletin(AlfaCyrillic.ui("Galereyani ochib bo'lmadi")).show();
        }
    }

    public static void onResult(ChatActivity fragment, Intent data) {
        if (fragment == null || data == null || data.getData() == null || fragment.getParentActivity() == null) {
            return;
        }
        final Uri uri = data.getData();
        final AlertDialog progress = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.setCanCancel(false);
        progress.showDelayed(300);
        Utilities.globalQueue.postRunnable(() -> {
            String path = null;
            VideoEditedInfo info = null;
            try {
                path = MediaController.copyFileToCache(uri, "mp4");
                if (path != null) {
                    info = build(path);
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
            final String finalPath = path;
            final VideoEditedInfo finalInfo = info;
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    progress.dismiss();
                } catch (Throwable ignore) {
                }
                if (finalPath == null || finalInfo == null) {
                    BulletinFactory.of(fragment).createErrorBulletin(AlfaCyrillic.ui("Bu videoni dumaloq qilib bo'lmadi")).show();
                    return;
                }
                fragment.alfaSendRoundVideo(finalPath, finalInfo);
            });
        });
    }

    /** Kvadrat kesish: kadr «xom» (aylantirilmagan) holatda qayta ishlanadi, aylantirish belgisi faylda qoladi. */
    private static VideoEditedInfo build(String path) {
        int[] params = new int[AnimatedFileDrawable.PARAM_NUM_COUNT];
        AnimatedFileDrawable.getVideoInfo(path, params, 0);
        if (params[AnimatedFileDrawable.PARAM_NUM_SUPPORTED_VIDEO_CODEC] == 0) {
            return null;
        }
        final int width = params[AnimatedFileDrawable.PARAM_NUM_WIDTH];
        final int height = params[AnimatedFileDrawable.PARAM_NUM_HEIGHT];
        final float durationMs = params[AnimatedFileDrawable.PARAM_NUM_DURATION];
        if (width <= 0 || height <= 0 || durationMs <= 0) {
            return null;
        }
        final long duration = (long) Math.min(durationMs, MAX_MS);
        final int framerate = params[AnimatedFileDrawable.PARAM_NUM_FRAMERATE];

        VideoEditedInfo info = new VideoEditedInfo();
        info.roundVideo = true;
        info.originalPath = path;
        info.originalWidth = width;
        info.originalHeight = height;
        info.rotationValue = params[AnimatedFileDrawable.PARAM_NUM_ROTATION];
        info.resultWidth = SIDE;
        info.resultHeight = SIDE;
        info.framerate = framerate > 0 ? Math.min(framerate, 30) : 30;
        info.bitrate = BITRATE;
        info.startTime = -1;
        // konvertor vaqtni mikrosekundda oladi
        info.endTime = durationMs > MAX_MS ? MAX_MS * 1000L : -1;
        info.estimatedDuration = duration;
        info.originalDuration = (long) (durationMs * 1000);
        info.estimatedSize = Math.max(1, (long) (duration / 1000f * (BITRATE + 64_000) / 8f));

        // o'rtadan kvadrat: qisqa tomon to'liq, uzun tomon o'rtasidan shu uzunlikda
        final int side = Math.min(width, height);
        MediaController.CropState crop = new MediaController.CropState();
        crop.cropPx = 0;
        crop.cropPy = 0;
        crop.cropScale = 1f;
        crop.cropRotate = 0;
        crop.cropPw = side / (float) width;
        crop.cropPh = side / (float) height;
        crop.transformWidth = SIDE;
        crop.transformHeight = SIDE;
        crop.transformRotation = 0;
        crop.mirrored = false;
        info.cropState = crop;
        return info;
    }
}
