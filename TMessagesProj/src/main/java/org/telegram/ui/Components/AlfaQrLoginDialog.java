package org.telegram.ui.Components;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.ui.ActionBar.Theme;

import java.util.Arrays;
import java.util.HashMap;

/**
 * AlfaGram: «QR kod bilan kirish» (https://core.telegram.org/api/qr-login).
 * <p>
 * auth.exportLoginToken → QR (tg://login?token=base64url) ko'rsatiladi; boshqa qurilmadagi Telegram uni
 * «Sozlamalar → Qurilmalar → Qurilma ulash» orqali skanerlaydi (auth.acceptLoginToken).
 * updateLoginToken kirmagan klientda MessagesController'da qayta ishlanmaydi, shuning uchun oyna ochiq turganda
 * exportLoginToken har POLL_INTERVAL_MS da qayta so'raladi: qabul qilingach server loginTokenSuccess (shu DC) yoki
 * loginTokenMigrateTo (akkaunt boshqa DC'da) qaytaradi. MigrateTo'da — Telegram Desktop'dagi kabi — asosiy DC
 * o'sha DC'ga o'tkaziladi (ConnectionsManager.setDefaultDatacenterId, rasmiy PasskeysController ham shunday qiladi)
 * va auth.importLoginToken aynan o'sha DC'ga yuboriladi. 2FA (SESSION_PASSWORD_NEEDED) — account.getPassword
 * olinadi va LoginActivity'ning parol sahifasiga uzatiladi.
 */
public class AlfaQrLoginDialog extends Dialog {

    public interface Delegate {
        /** Muvaffaqiyatli kirildi (UI oqimida, oyna yopilgandan keyin). */
        void onAuthorized(TLRPC.TL_auth_authorization authorization);

        /** Akkauntda ikki bosqichli parol bor — parol sahifasini ochish kerak (UI oqimida, oyna yopilgandan keyin). */
        void onPasswordNeeded(TL_account.Password password);
    }

    private static final long POLL_INTERVAL_MS = 6000; // tasdiq updateLoginToken orqali darhol keladi, bu — zaxira
    private static final long MAX_RETRY_DELAY_MS = 15000;
    /** QR har doim oq kartada qora — qorong'i mavzuda ham skaner yaxshi o'qisin. */
    private static final int CARD_COLOR = 0xffffffff;
    private static final int QR_COLOR = 0xff000000;
    private static final int CARD_TEXT_COLOR = 0xff222222;

    private final int currentAccount;
    private final Delegate delegate;

    private FrameLayout qrCard;
    private ImageView qrImageView;
    private RLottieImageView qrLogoView;
    private LinearLayout overlayView;
    private RadialProgressView overlayProgress;
    private TextView overlayText;
    private TextView retryButton;

    private Bitmap qrBitmap;
    private int qrLogoSize;
    private int qrPadding;

    private byte[] shownToken;
    private int tokenExpires;
    private int requestId;
    private int requestGeneration;
    private int renderGeneration;
    private int failCount;
    private long floodUntil;
    /** true — import/parol bosqichi: so'rov (polling) to'xtatilgan. */
    private boolean authorizing;
    private int importDcId;
    private byte[] importTokenBytes;
    private boolean closed;
    private Runnable retryAction;

    private final Runnable pollRunnable = this::requestToken;
    private final Runnable expiryRunnable = this::onTokenExpired;

    public AlfaQrLoginDialog(Context context, int account, Delegate delegate) {
        super(context, android.R.style.Theme_DeviceDefault_NoActionBar);
        this.currentAccount = account;
        this.delegate = delegate;

        final int bg = Theme.getColor(Theme.key_windowBackgroundWhite);
        final int textColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
        final int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);
        final int accentPressedColor = Theme.getColor(Theme.key_featuredStickers_addButtonPressed);
        final int accentTextColor = Theme.getColor(Theme.key_featuredStickers_buttonText);
        final int linkColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4);
        final boolean lightBackground = AndroidUtilities.computePerceivedBrightness(bg) > 0.721f;

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(bg));
            window.setStatusBarColor(bg);
            window.setNavigationBarColor(bg);
            int flags = 0;
            if (lightBackground) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(bg);
        // Android 15+ (targetSdk 35+) oyna status bar ostidan chiziladi — tugmalar ostida qolmasin.
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });

        TextView back = new TextView(context);
        back.setText(AlfaCyrillic.ui("Orqaga"));
        back.setTextColor(textColor);
        back.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        back.setGravity(Gravity.CENTER);
        back.setPadding(AndroidUtilities.dp(18), 0, AndroidUtilities.dp(18), 0);
        back.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(22), Theme.getColor(Theme.key_windowBackgroundGray), Theme.getColor(Theme.key_listSelector)));
        back.setOnClickListener(v -> dismiss());
        root.addView(back, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 44, Gravity.LEFT | Gravity.TOP, 16, 12, 0, 0));

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        root.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, 64, 0, 0));
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(4), AndroidUtilities.dp(24), AndroidUtilities.dp(16));
        scroll.addView(column, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(context);
        title.setText(AlfaCyrillic.ui("QR kod bilan kirish"));
        title.setTextColor(textColor);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setTypeface(AndroidUtilities.bold());
        title.setGravity(Gravity.CENTER);
        column.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // QR kartasi: oq, yumaloq burchakli; markazda Telegram'ning animatsiyali logotipi (QRCodeBottomSheet'dagi kabi).
        qrPadding = AndroidUtilities.dp(16);
        int cardSize = Math.max(AndroidUtilities.dp(180), Math.min(AndroidUtilities.dp(280), AndroidUtilities.displaySize.x - AndroidUtilities.dp(80)));
        qrCard = new FrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int inner = MeasureSpec.getSize(widthMeasureSpec) - qrPadding * 2;
                if (qrBitmap != null && qrLogoSize > 0 && inner > 0 && qrLogoView != null) {
                    int logo = Math.max(1, (int) (qrLogoSize * inner / (float) qrBitmap.getWidth()));
                    ViewGroup.LayoutParams lp = qrLogoView.getLayoutParams();
                    if (lp != null && lp.width != logo) {
                        lp.width = lp.height = logo;
                    }
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(CARD_COLOR);
        cardBg.setCornerRadius(AndroidUtilities.dp(28));
        if (lightBackground) {
            cardBg.setStroke(Math.max(1, AndroidUtilities.dp(1)), 0x1f000000);
        }
        qrCard.setBackground(cardBg);
        qrCard.setClipToOutline(true);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(cardSize, cardSize);
        cardParams.gravity = Gravity.CENTER_HORIZONTAL;
        cardParams.topMargin = AndroidUtilities.dp(24);
        column.addView(qrCard, cardParams);

        qrImageView = new ImageView(context);
        qrImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qrImageView.setAlpha(0f);
        qrCard.addView(qrImageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER, 16, 16, 16, 16));

        qrLogoView = new RLottieImageView(context);
        qrLogoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qrLogoView.setBackgroundColor(CARD_COLOR);
        qrLogoView.setAutoRepeat(true);
        qrLogoView.setAnimation(R.raw.qr_code_logo, 60, 60);
        qrLogoView.setVisibility(View.GONE);
        qrCard.addView(qrLogoView, LayoutHelper.createFrame(60, 60, Gravity.CENTER));

        overlayView = new LinearLayout(context);
        overlayView.setOrientation(LinearLayout.VERTICAL);
        overlayView.setGravity(Gravity.CENTER);
        overlayView.setPadding(AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20), 0);
        overlayView.setBackgroundColor(0xf2ffffff);
        overlayView.setClickable(true);
        qrCard.addView(overlayView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        overlayProgress = new RadialProgressView(context);
        overlayProgress.setSize(AndroidUtilities.dp(32));
        overlayProgress.setProgressColor(accentColor);
        overlayView.addView(overlayProgress, LayoutHelper.createLinear(44, 44, Gravity.CENTER_HORIZONTAL));

        overlayText = new TextView(context);
        overlayText.setTextColor(CARD_TEXT_COLOR);
        overlayText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        overlayText.setGravity(Gravity.CENTER);
        overlayText.setVisibility(View.GONE);
        overlayView.addView(overlayText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 0));

        retryButton = new TextView(context);
        retryButton.setText(AlfaCyrillic.ui("Qayta urinish"));
        retryButton.setTextColor(accentTextColor);
        retryButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        retryButton.setTypeface(AndroidUtilities.bold());
        retryButton.setGravity(Gravity.CENTER);
        retryButton.setPadding(AndroidUtilities.dp(22), 0, AndroidUtilities.dp(22), 0);
        retryButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(20), accentColor, accentPressedColor));
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(v -> {
            Runnable action = retryAction;
            retryAction = null;
            if (action != null && !closed) {
                action.run();
            }
        });
        overlayView.addView(retryButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 40, Gravity.CENTER_HORIZONTAL, 0, 16, 0, 0));

        // Qadamlar: 1) Telegram'ni oching 2) Sozlamalar → Qurilmalar → Qurilma ulash 3) Skanerlang
        LinearLayout steps = new LinearLayout(context);
        steps.setOrientation(LinearLayout.VERTICAL);
        addStep(context, steps, 1, AlfaCyrillic.ui("Telefoningizda Telegram'ni oching"), textColor, accentColor, accentTextColor);
        addStep(context, steps, 2, AlfaCyrillic.ui("Sozlamalar → Qurilmalar → Qurilma ulash"), textColor, accentColor, accentTextColor);
        addStep(context, steps, 3, AlfaCyrillic.ui("Shu QR kodni skanerlang"), textColor, accentColor, accentTextColor);
        column.addView(steps, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 28, 0, 0));

        column.addView(new View(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f));

        TextView phoneLogin = new TextView(context);
        phoneLogin.setText(AlfaCyrillic.ui("Telefon raqam bilan kirish"));
        phoneLogin.setTextColor(linkColor);
        phoneLogin.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        phoneLogin.setGravity(Gravity.CENTER);
        phoneLogin.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
        phoneLogin.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(20), 0, Theme.getColor(Theme.key_listSelector)));
        phoneLogin.setOnClickListener(v -> dismiss());
        column.addView(phoneLogin, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 40, Gravity.CENTER_HORIZONTAL, 0, 24, 0, 0));

        setContentView(root);
        showProgressOverlay(null);
    }

    private static void addStep(Context context, LinearLayout steps, int number, CharSequence text, int textColor, int accentColor, int accentTextColor) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = new TextView(context);
        badge.setText(String.valueOf(number));
        badge.setTextColor(accentTextColor);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        badge.setTypeface(AndroidUtilities.bold());
        badge.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(accentColor);
        badge.setBackground(circle);
        row.addView(badge, LayoutHelper.createLinear(24, 24));

        TextView label = new TextView(context);
        label.setText(text);
        label.setTextColor(textColor);
        label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        row.addView(label, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 12, 0, 0, 0));

        steps.addView(row, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, steps.getChildCount() == 0 ? 0 : 14, 0, 0));
    }

    private final org.telegram.messenger.NotificationCenter.NotificationCenterDelegate loginTokenObserver = (id, account, args) -> {
        if (id == org.telegram.messenger.NotificationCenter.alfaLoginTokenUpdated && !closed && !authorizing) {
            // server: token tasdiqlandi — darhol qayta so'raymiz (loginTokenSuccess / migrateTo keladi)
            AndroidUtilities.cancelRunOnUIThread(pollRunnable);
            requestToken();
        }
    };

    @Override
    public void show() {
        super.show();
        org.telegram.messenger.NotificationCenter.getGlobalInstance().addObserver(loginTokenObserver, org.telegram.messenger.NotificationCenter.alfaLoginTokenUpdated);
        requestToken();
    }

    @Override
    public void dismiss() {
        stop();
        try {
            super.dismiss();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void stop() {
        if (closed) {
            return;
        }
        closed = true;
        org.telegram.messenger.NotificationCenter.getGlobalInstance().removeObserver(loginTokenObserver, org.telegram.messenger.NotificationCenter.alfaLoginTokenUpdated);
        retryAction = null;
        AndroidUtilities.cancelRunOnUIThread(pollRunnable);
        AndroidUtilities.cancelRunOnUIThread(expiryRunnable);
        requestGeneration++;
        renderGeneration++;
        if (requestId != 0) {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(requestId, true);
            requestId = 0;
        }
        if (qrLogoView != null) {
            qrLogoView.stopAnimation();
        }
    }

    // ───────────────────────── Tarmoq ─────────────────────────

    private void requestToken() {
        AndroidUtilities.cancelRunOnUIThread(pollRunnable);
        if (closed || authorizing || requestId != 0) {
            return;
        }
        long floodLeft = floodUntil - SystemClock.elapsedRealtime();
        if (floodLeft > 0) {
            schedulePoll(floodLeft);
            return;
        }
        TLRPC.TL_auth_exportLoginToken req = new TLRPC.TL_auth_exportLoginToken();
        req.api_id = BuildVars.APP_ID;
        req.api_hash = BuildVars.APP_HASH;
        // Shu qurilmada allaqachon ochiq akkauntlar — ikkinchi marta kirilmasin.
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (a == currentAccount) {
                continue;
            }
            UserConfig userConfig = UserConfig.getInstance(a);
            if (userConfig.isClientActivated()) {
                long userId = userConfig.getClientUserId();
                if (userId != 0 && !req.except_ids.contains(userId)) {
                    req.except_ids.add(userId);
                }
            }
        }
        final int generation = ++requestGeneration;
        requestId = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (closed || generation != requestGeneration) {
                return;
            }
            requestId = 0;
            onTokenResponse(response, error, false);
        }), ConnectionsManager.RequestFlagWithoutLogin | ConnectionsManager.RequestFlagFailOnServerErrors);
    }

    private void importLoginToken(int dcId, byte[] token) {
        AndroidUtilities.cancelRunOnUIThread(pollRunnable);
        AndroidUtilities.cancelRunOnUIThread(expiryRunnable);
        if (closed) {
            return;
        }
        authorizing = true;
        importDcId = dcId;
        importTokenBytes = token;
        showProgressOverlay(AlfaCyrillic.ui("Kirilmoqda..."));

        ConnectionsManager connectionsManager = ConnectionsManager.getInstance(currentAccount);
        if (dcId != connectionsManager.getCurrentDatacenterId()) {
            // Akkaunt boshqa DC'da (loginTokenMigrateTo). Telegram Desktop'dagi setMainDcId kabi asosiy DC'ni o'tkazamiz:
            // native moveToDatacenter → kirilmagan klientda darhol currentDatacenterId = dcId (+ saveConfig).
            // Keyingi barcha so'rovlar (2FA: account.getPassword / auth.checkPassword, onAuthSuccess'dan keyingilar) shu DC'ga ketadi.
            connectionsManager.setDefaultDatacenterId(dcId);
        }
        TLRPC.TL_auth_importLoginToken req = new TLRPC.TL_auth_importLoginToken();
        req.token = token;
        final int generation = ++requestGeneration;
        requestId = connectionsManager.sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (closed || generation != requestGeneration) {
                return;
            }
            requestId = 0;
            onTokenResponse(response, error, true);
        }), null, null, ConnectionsManager.RequestFlagWithoutLogin | ConnectionsManager.RequestFlagFailOnServerErrors | ConnectionsManager.RequestFlagEnableUnauthorized, dcId, ConnectionsManager.ConnectionTypeGeneric, true);
    }

    private void requestPassword() {
        AndroidUtilities.cancelRunOnUIThread(pollRunnable);
        AndroidUtilities.cancelRunOnUIThread(expiryRunnable);
        if (closed) {
            return;
        }
        authorizing = true;
        showProgressOverlay(AlfaCyrillic.ui("Kirilmoqda..."));
        TL_account.getPassword req = new TL_account.getPassword();
        final int generation = ++requestGeneration;
        requestId = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (closed || generation != requestGeneration) {
                return;
            }
            requestId = 0;
            if (response instanceof TL_account.Password) {
                final TL_account.Password password = (TL_account.Password) response;
                finishWith(() -> delegate.onPasswordNeeded(password));
            } else {
                showError(AlfaCyrillic.ui("Kirib bo'lmadi. Qaytadan urinib ko'ring."), this::requestPassword);
            }
        }), ConnectionsManager.RequestFlagFailOnServerErrors | ConnectionsManager.RequestFlagWithoutLogin);
    }

    private void onTokenResponse(TLObject response, TLRPC.TL_error error, boolean fromImport) {
        if (response instanceof TLRPC.TL_auth_loginToken) {
            TLRPC.TL_auth_loginToken loginToken = (TLRPC.TL_auth_loginToken) response;
            authorizing = false;
            importTokenBytes = null;
            failCount = 0;
            tokenExpires = loginToken.expires;
            if (shownToken == null || !Arrays.equals(shownToken, loginToken.token)) {
                shownToken = loginToken.token;
                // tg://login?token=<base64url, '=' to'ldiruvchisiz>
                renderQr("tg://login?token=" + Base64.encodeToString(loginToken.token, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING));
            } else if (qrBitmap != null) {
                hideOverlay();
            }
            scheduleExpiryCheck();
            schedulePoll(POLL_INTERVAL_MS);
        } else if (response instanceof TLRPC.TL_auth_loginTokenMigrateTo) {
            TLRPC.TL_auth_loginTokenMigrateTo migrateTo = (TLRPC.TL_auth_loginTokenMigrateTo) response;
            importLoginToken(migrateTo.dc_id, migrateTo.token);
        } else if (response instanceof TLRPC.TL_auth_loginTokenSuccess) {
            TLRPC.auth_Authorization authorization = ((TLRPC.TL_auth_loginTokenSuccess) response).authorization;
            if (authorization instanceof TLRPC.TL_auth_authorization) {
                final TLRPC.TL_auth_authorization auth = (TLRPC.TL_auth_authorization) authorization;
                finishWith(() -> delegate.onAuthorized(auth));
            } else {
                // auth.authorizationSignUpRequired — QR orqali bo'lmaydi (akkaunt mavjud bo'lishi kerak).
                showError(AlfaCyrillic.ui("Kirib bo'lmadi. Qaytadan urinib ko'ring."), this::restart);
            }
        } else if (error != null && error.text != null && error.text.contains("SESSION_PASSWORD_NEEDED")) {
            requestPassword();
        } else {
            onRequestError(error, fromImport);
        }
    }

    private void onRequestError(TLRPC.TL_error error, boolean fromImport) {
        final String text = error != null ? error.text : null;
        final boolean flood = text != null && text.startsWith("FLOOD_WAIT");
        if (flood) {
            Integer seconds = Utilities.parseInt(text);
            floodUntil = SystemClock.elapsedRealtime() + Math.max(1, seconds == null ? 5 : seconds) * 1000L;
        }
        if (fromImport) {
            if (text != null && text.startsWith("AUTH_TOKEN")) {
                // AUTH_TOKEN_EXPIRED / _INVALID / _ALREADY_ACCEPTED — yangi QR ko'rsatamiz.
                restart();
            } else {
                final int dcId = importDcId;
                final byte[] token = importTokenBytes;
                showError(flood ? AlfaCyrillic.ui("Juda ko'p urinish. Birozdan keyin qayta urining.") : AlfaCyrillic.ui("Kirib bo'lmadi. Qaytadan urinib ko'ring."),
                        token != null ? () -> importLoginToken(dcId, token) : (Runnable) this::restart);
            }
            return;
        }
        failCount++;
        long delay = Math.min(POLL_INTERVAL_MS * failCount, MAX_RETRY_DELAY_MS);
        if (flood) {
            delay = Math.max(delay, floodUntil - SystemClock.elapsedRealtime());
        }
        boolean tokenStillValid = qrBitmap != null && shownToken != null && tokenExpires > ConnectionsManager.getInstance(currentAccount).getCurrentTime() + 1;
        if (tokenStillValid) {
            // Ko'rsatilgan QR hali yaroqli — jim qayta urinamiz.
            schedulePoll(delay);
        } else {
            showError(flood ? AlfaCyrillic.ui("Juda ko'p urinish. Birozdan keyin qayta urining.") : AlfaCyrillic.ui("QR kodni olib bo'lmadi. Internetni tekshirib, qayta urining."), this::restart);
        }
    }

    private void restart() {
        if (closed) {
            return;
        }
        authorizing = false;
        importTokenBytes = null;
        shownToken = null;
        failCount = 0;
        showProgressOverlay(null);
        requestToken();
    }

    private void schedulePoll(long delay) {
        AndroidUtilities.cancelRunOnUIThread(pollRunnable);
        if (!closed && !authorizing) {
            AndroidUtilities.runOnUIThread(pollRunnable, Math.max(500, delay));
        }
    }

    private void scheduleExpiryCheck() {
        AndroidUtilities.cancelRunOnUIThread(expiryRunnable);
        if (closed || authorizing) {
            return;
        }
        int left = tokenExpires - ConnectionsManager.getInstance(currentAccount).getCurrentTime();
        AndroidUtilities.runOnUIThread(expiryRunnable, Math.max(1, left) * 1000L);
    }

    private void onTokenExpired() {
        if (closed || authorizing) {
            return;
        }
        if (tokenExpires > ConnectionsManager.getInstance(currentAccount).getCurrentTime()) {
            scheduleExpiryCheck();
            return;
        }
        // Yangi token hali kelmadi (masalan, internet yo'q) — eskirgan QR'ni yopib turamiz.
        if (retryAction == null) {
            showProgressOverlay(null);
        }
        if (requestId == 0 && retryAction == null) {
            requestToken();
        }
    }

    private void finishWith(Runnable action) {
        if (closed) {
            return;
        }
        dismiss();
        action.run();
    }

    // ───────────────────────── QR chizish ─────────────────────────

    private void renderQr(String link) {
        final int generation = ++renderGeneration;
        Utilities.globalQueue.postRunnable(() -> {
            Bitmap bitmap = null;
            int logoSize = 0;
            try {
                HashMap<EncodeHintType, Object> hints = new HashMap<>();
                // Q (25%) — markazdagi logotip o'rnini bemalol tiklaydi (Telegram Desktop ham shunday).
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
                hints.put(EncodeHintType.MARGIN, 0);
                QRCodeWriter writer = new QRCodeWriter();
                bitmap = writer.encode(link, 768, 768, hints, null, 0.75f, CARD_COLOR, QR_COLOR);
                logoSize = writer.getImageSize();
            } catch (Throwable e) {
                FileLog.e(e);
            }
            final Bitmap result = bitmap;
            final int resultLogoSize = logoSize;
            AndroidUtilities.runOnUIThread(() -> {
                if (closed || generation != renderGeneration) {
                    return;
                }
                if (result == null) {
                    showError(AlfaCyrillic.ui("QR kodni olib bo'lmadi. Internetni tekshirib, qayta urining."), this::restart);
                    return;
                }
                boolean first = qrBitmap == null;
                qrBitmap = result;
                qrLogoSize = resultLogoSize;
                qrImageView.setImageBitmap(result);
                if (first) {
                    qrImageView.animate().alpha(1f).setDuration(220).start();
                } else {
                    qrImageView.setAlpha(1f);
                }
                if (qrLogoView.getVisibility() != View.VISIBLE) {
                    qrLogoView.setVisibility(View.VISIBLE);
                    qrLogoView.playAnimation();
                }
                qrCard.requestLayout();
                if (!authorizing && retryAction == null) {
                    hideOverlay();
                }
            });
        });
    }

    // ───────────────────────── Holatlar ─────────────────────────

    private void showProgressOverlay(CharSequence text) {
        retryAction = null;
        overlayView.setVisibility(View.VISIBLE);
        overlayProgress.setVisibility(View.VISIBLE);
        overlayText.setText(text);
        overlayText.setVisibility(text == null ? View.GONE : View.VISIBLE);
        retryButton.setVisibility(View.GONE);
    }

    private void showError(CharSequence text, Runnable retry) {
        AndroidUtilities.cancelRunOnUIThread(pollRunnable);
        AndroidUtilities.cancelRunOnUIThread(expiryRunnable);
        if (closed) {
            return;
        }
        authorizing = false;
        retryAction = retry;
        overlayView.setVisibility(View.VISIBLE);
        overlayProgress.setVisibility(View.GONE);
        overlayText.setText(text);
        overlayText.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
    }

    private void hideOverlay() {
        retryAction = null;
        overlayView.setVisibility(View.GONE);
    }
}
