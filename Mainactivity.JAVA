package com.kasrasolar.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.*;

public class MainActivity extends Activity {

    private WebView  webView;
    private ProgressBar progressBar;
    private LinearLayout errorLayout;
    private SharedPreferences prefs;
    private String currentUrl = "";

    private static final String PREF_FILE = "SmartSolar";
    private static final String PREF_URL  = "server_url";

    // ─────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen, dark status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.parseColor("#060e1f"));
        }

        prefs = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        buildUI();

        String saved = prefs.getString(PREF_URL, null);
        if (saved == null || saved.isEmpty()) {
            showServerDialog(false);
        } else {
            loadApp(saved);
        }
    }

    // ─── Build UI ─────────────────────────────────────────────────
    @SuppressLint("SetJavaScriptEnabled")
    private void buildUI() {
        float dp = getResources().getDisplayMetrics().density;

        // Root
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#060e1f"));
        setContentView(root);

        // Blue progress bar (top)
        progressBar = new ProgressBar(this, null,
            android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setIndeterminate(false);
        if (Build.VERSION.SDK_INT >= 21) {
            progressBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#3b82f6")));
        }
        progressBar.setVisibility(LinearLayout.GONE);
        root.addView(progressBar, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (int)(4 * dp)));

        // WebView
        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#060e1f"));
        configWebView();
        root.addView(webView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // ── Error layout ──────────────────────────────────────────
        errorLayout = new LinearLayout(this);
        errorLayout.setOrientation(LinearLayout.VERTICAL);
        errorLayout.setGravity(Gravity.CENTER);
        errorLayout.setBackgroundColor(Color.parseColor("#060e1f"));
        errorLayout.setVisibility(LinearLayout.GONE);
        errorLayout.setPadding((int)(24*dp), 0, (int)(24*dp), 0);

        TextView errIcon = new TextView(this);
        errIcon.setText("📡");
        errIcon.setTextSize(52);
        errIcon.setGravity(Gravity.CENTER);

        TextView errTitle = new TextView(this);
        errTitle.setText("سرور در دسترس نیست");
        errTitle.setTextSize(17);
        errTitle.setTextColor(Color.parseColor("#e8edf5"));
        errTitle.setGravity(Gravity.CENTER);
        errTitle.setTypeface(null, Typeface.BOLD);
        errTitle.setPadding(0, (int)(16*dp), 0, (int)(8*dp));

        TextView errSub = new TextView(this);
        errSub.setText("مطمئن شوید:\n• سرور SmartSolar روشن است\n• گوشی و سرور در یک شبکه هستند\n• آدرس IP صحیح است");
        errSub.setTextSize(13);
        errSub.setTextColor(Color.parseColor("#8b97b0"));
        errSub.setGravity(Gravity.CENTER);
        errSub.setLineSpacing(4 * dp, 1f);
        errSub.setPadding(0, 0, 0, (int)(24*dp));

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setGravity(Gravity.CENTER);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button retryBtn = makeBtn("🔄  تلاش مجدد", "#3b82f6", "#ffffff");
        retryBtn.setOnClickListener(v -> loadApp(currentUrl));

        Button changeBtn = makeBtn("⚙️  تغییر آدرس", "#132237", "#8b97b0");
        changeBtn.setOnClickListener(v -> showServerDialog(true));

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.setMargins((int)(8*dp), 0, (int)(8*dp), 0);
        btnRow.addView(retryBtn, bp);
        btnRow.addView(changeBtn, bp);

        errorLayout.addView(errIcon);
        errorLayout.addView(errTitle);
        errorLayout.addView(errSub);
        errorLayout.addView(btnRow);

        root.addView(errorLayout, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT));
    }

    // ─── Configure WebView ────────────────────────────────────────
    @SuppressLint("SetJavaScriptEnabled")
    private void configWebView() {
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);          // localStorage
        ws.setDatabaseEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);
        ws.setSupportZoom(false);
        ws.setAllowFileAccess(true);
        ws.setGeolocationEnabled(false);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= 21) {
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(LinearLayout.GONE);
                webView.setVisibility(LinearLayout.VISIBLE);
                errorLayout.setVisibility(LinearLayout.GONE);
            }

            @Override
            public void onReceivedError(WebView view, int code,
                    String desc, String failingUrl) {
                progressBar.setVisibility(LinearLayout.GONE);
                webView.setVisibility(LinearLayout.GONE);
                errorLayout.setVisibility(LinearLayout.VISIBLE);
            }

            // Android 6+ error handler
            @Override
            public void onReceivedError(WebView view,
                    WebResourceRequest request, WebResourceError error) {
                if (Build.VERSION.SDK_INT >= 23
                        && request.isForMainFrame()) {
                    progressBar.setVisibility(LinearLayout.GONE);
                    webView.setVisibility(LinearLayout.GONE);
                    errorLayout.setVisibility(LinearLayout.VISIBLE);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress < 100) {
                    progressBar.setVisibility(LinearLayout.VISIBLE);
                    progressBar.setProgress(progress);
                } else {
                    progressBar.setVisibility(LinearLayout.GONE);
                }
            }
        });
    }

    // ─── Load the app ─────────────────────────────────────────────
    private void loadApp(String url) {
        currentUrl = url;
        errorLayout.setVisibility(LinearLayout.GONE);
        webView.setVisibility(LinearLayout.VISIBLE);
        progressBar.setVisibility(LinearLayout.VISIBLE);
        webView.loadUrl(url);
    }

    // ─── Server URL dialog ────────────────────────────────────────
    private void showServerDialog(boolean cancelable) {
        float dp = getResources().getDisplayMetrics().density;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = (int)(20 * dp);
        layout.setPadding(p, (int)(12*dp), p, (int)(4*dp));

        TextView hint = new TextView(this);
        hint.setText("آدرس IP سرور SmartSolar را وارد کنید:");
        hint.setTextColor(Color.parseColor("#8b97b0"));
        hint.setTextSize(13);
        hint.setPadding(0, 0, 0, (int)(8*dp));

        TextView example = new TextView(this);
        example.setText("مثال:  http://192.168.1.100");
        example.setTextColor(Color.parseColor("#3b82f6"));
        example.setTextSize(12);
        example.setPadding(0, 0, 0, (int)(10*dp));

        EditText input = new EditText(this);
        String prev = prefs.getString(PREF_URL, "http://");
        input.setText(prev);
        input.setTextColor(Color.parseColor("#e8edf5"));
        input.setHintTextColor(Color.parseColor("#4b5a72"));
        input.setHint("http://192.168.1.100");
        input.setTextSize(14);
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        input.setPadding((int)(12*dp), (int)(10*dp),
                         (int)(12*dp), (int)(10*dp));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#132237"));
        bg.setCornerRadius(10 * dp);
        bg.setStroke((int)(1.5f*dp), Color.parseColor("#1a2d45"));
        input.setBackground(bg);

        layout.addView(hint);
        layout.addView(example);
        layout.addView(input);

        AlertDialog dlg = new AlertDialog.Builder(this,
                android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("☀️  کسری سولار")
            .setView(layout)
            .setPositiveButton("🔌  اتصال", (d, w) -> {
                String url = input.getText().toString().trim();
                if (url.isEmpty()) return;
                if (!url.startsWith("http")) url = "http://" + url;
                url = url.replaceAll("/+$", "");
                prefs.edit().putString(PREF_URL, url).apply();
                loadApp(url);
            })
            .setCancelable(cancelable)
            .create();

        if (cancelable) {
            dlg.setButton(AlertDialog.BUTTON_NEGATIVE,
                "انصراف", (d, w) -> d.dismiss());
        }

        dlg.show();
    }

    // ─── Button helper ────────────────────────────────────────────
    private Button makeBtn(String text, String bgHex, String fgHex) {
        float dp = getResources().getDisplayMetrics().density;
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor(fgHex));
        btn.setTextSize(14);
        btn.setAllCaps(false);
        btn.setPadding((int)(20*dp), (int)(12*dp),
                       (int)(20*dp), (int)(12*dp));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(bgHex));
        bg.setCornerRadius(24 * dp);
        btn.setBackground(bg);
        return btn;
    }

    // ─── Back button ──────────────────────────────────────────────
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            new AlertDialog.Builder(this,
                    android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("خروج از اپلیکیشن")
                .setMessage("آیا می‌خواهید از کسری سولار خارج شوید؟")
                .setPositiveButton("بله، خروج", (d, w) -> finish())
                .setNegativeButton("خیر", null)
                .show();
        }
    }

    // ─── Pause / Resume WebView ───────────────────────────────────
    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}