package com.hackscroll.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.hackscroll.app.ArticlePager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/* loaded from: classes.dex */
public class MainActivity extends Activity {
    private static final String FEED_URL = "https://hackaday.com/feed/";
    private View errorView;
    private View hintView;
    private ImageLoader images;
    private TextView offlinePill;
    private ArticlePager pager;
    private SharedPreferences prefs;
    private FrameLayout root;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean renderedOnce = false;

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.prefs = getSharedPreferences("hackscroll", 0);
        this.images = new ImageLoader(this);
        this.root = new FrameLayout(this);
        this.root.setBackgroundColor(-16052719);
        this.pager = new ArticlePager(this);
        this.pager.setOnPageChangeListener(new ArticlePager.OnPageChangeListener() { // from class: com.hackscroll.app.MainActivity.1
            @Override // com.hackscroll.app.ArticlePager.OnPageChangeListener
            public void onPageChanged(int i) {
                MainActivity.this.hideHint();
            }
        });
        this.root.addView(this.pager, new FrameLayout.LayoutParams(-1, -1));
        this.root.addView(buildOfflinePill());
        this.errorView = buildErrorView();
        this.errorView.setVisibility(8);
        this.root.addView(this.errorView);
        if (!this.prefs.getBoolean("hintShown", false)) {
            this.hintView = buildHint();
            this.root.addView(this.hintView);
        }
        setContentView(this.root);
        String load = FeedStore.load(this);
        if (load != null) {
            render(RssParser.parse(load), false);
        }
        refresh();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.hackscroll.app.MainActivity$2  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 implements Runnable {
        AnonymousClass2() {
        }

        @Override // java.lang.Runnable
        public void run() {
            final String str = Fetch.get(MainActivity.FEED_URL);
            MainActivity.this.runOnUiThread(new Runnable() { // from class: com.hackscroll.app.MainActivity.2.1
                @Override // java.lang.Runnable
                public void run() {
                    if (str != null) {
                        FeedStore.save(MainActivity.this, str);
                        ArrayList<Article> parse = RssParser.parse(str);
                        if (!parse.isEmpty()) {
                            MainActivity.this.offlinePill.setVisibility(8);
                            MainActivity.this.render(parse, true);
                            return;
                        }
                    }
                    if (MainActivity.this.renderedOnce) {
                        MainActivity.this.offlinePill.setVisibility(0);
                        MainActivity.this.offlinePill.animate().alpha(1.0f).setDuration(200L).start();
                        MainActivity.this.offlinePill.postDelayed(new Runnable() { // from class: com.hackscroll.app.MainActivity.2.1.1
                            @Override // java.lang.Runnable
                            public void run() {
                                MainActivity.this.offlinePill.animate().alpha(0.0f).setDuration(600L).start();
                            }
                        }, 2500L);
                        return;
                    }
                    MainActivity.this.showError();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refresh() {
        this.errorView.setVisibility(8);
        this.io.execute(new AnonymousClass2());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void render(List<Article> list, boolean z) {
        if (list != null && !list.isEmpty()) {
            int currentPage = this.pager.getCurrentPage();
            this.pager.removeAllViews();
            for (Article article : list) {
                this.pager.addView(buildPage(article));
            }
            this.pager.setPage((!z || currentPage >= list.size()) ? 0 : currentPage, false);
            this.pager.requestLayout();
            this.renderedOnce = true;
            this.root.bringChildToFront(this.errorView);
            if (this.hintView != null) {
                this.root.bringChildToFront(this.hintView);
            }
        }
    }

    private View buildPage(final Article article) {
        FrameLayout frameLayout = new FrameLayout(this);
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundColor(-15460324);
        frameLayout.addView(imageView, new FrameLayout.LayoutParams(-1, -1));
        this.images.load(imageView, article.imageUrl);
        View view = new View(this);
        view.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{1493172224, 0, 1426063360, -234881024}));
        frameLayout.addView(view, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(80);
        int dp = dp(22);
        linearLayout.setPadding(dp, dp, dp, dp(30));
        TextView textView = new TextView(this);
        textView.setText("HACKADAY  ·  HACKSCROLL");
        textView.setTextColor(-16726171);
        textView.setTextSize(11.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLetterSpacing(0.18f);
        linearLayout.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(article.title);
        textView2.setTextColor(-1);
        textView2.setTextSize(21.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setMaxLines(4);
        textView2.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.setMargins(0, dp(10), 0, 0);
        linearLayout.addView(textView2, layoutParams);
        if (article.dateText != null && !article.dateText.isEmpty()) {
            TextView textView3 = new TextView(this);
            textView3.setText(article.dateText);
            textView3.setTextColor(-5654849);
            textView3.setTextSize(12.0f);
            LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
            layoutParams2.setMargins(0, dp(6), 0, 0);
            linearLayout.addView(textView3, layoutParams2);
        }
        if (article.excerpt != null && !article.excerpt.isEmpty()) {
            TextView textView4 = new TextView(this);
            textView4.setText(article.excerpt);
            textView4.setTextColor(-2695452);
            textView4.setTextSize(14.0f);
            textView4.setMaxLines(3);
            textView4.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, -2);
            layoutParams3.setMargins(0, dp(8), 0, 0);
            linearLayout.addView(textView4, layoutParams3);
        }
        Button button = new Button(this);
        button.setText("READ ON HACKADAY  →");
        button.setTextColor(-16052719);
        button.setTextSize(13.0f);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(true);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-16726171);
        gradientDrawable.setCornerRadius(dp(8));
        button.setBackground(gradientDrawable);
        button.setPadding(dp(18), 0, dp(18), 0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setOnClickListener(new View.OnClickListener() { // from class: com.hackscroll.app.MainActivity.3
            @Override // android.view.View.OnClickListener
            public void onClick(View view2) {
                MainActivity.this.openLink(article.link);
            }
        });
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(-2, dp(46));
        layoutParams4.setMargins(0, dp(16), 0, 0);
        linearLayout.addView(button, layoutParams4);
        frameLayout.addView(linearLayout, new FrameLayout.LayoutParams(-1, -1));
        return frameLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openLink(String str) {
        try {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse(str)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No browser found", 0).show();
        }
    }

    private TextView buildOfflinePill() {
        this.offlinePill = new TextView(this);
        this.offlinePill.setText("OFFLINE · SHOWING CACHED");
        this.offlinePill.setTextColor(-2695452);
        this.offlinePill.setTextSize(11.0f);
        this.offlinePill.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1289344205);
        gradientDrawable.setCornerRadius(dp(14));
        this.offlinePill.setBackground(gradientDrawable);
        int dp = dp(10);
        this.offlinePill.setPadding(dp, dp(6), dp, dp(6));
        this.offlinePill.setAlpha(0.0f);
        this.offlinePill.setVisibility(8);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2, 49);
        layoutParams.setMargins(0, dp(52), 0, 0);
        this.offlinePill.setLayoutParams(layoutParams);
        return this.offlinePill;
    }

    private View buildHint() {
        TextView textView = new TextView(this);
        textView.setText("Swipe up for the next hack ↑");
        textView.setTextColor(-1);
        textView.setTextSize(14.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-870571221);
        gradientDrawable.setCornerRadius(dp(18));
        textView.setBackground(gradientDrawable);
        textView.setPadding(dp(20), dp(10), dp(20), dp(10));
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2, 49);
        layoutParams.setMargins(0, dp(96), 0, 0);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideHint() {
        if (this.hintView != null) {
            this.prefs.edit().putBoolean("hintShown", true).apply();
            final View view = this.hintView;
            this.hintView = null;
            view.animate().alpha(0.0f).setDuration(300L).start();
            view.postDelayed(new Runnable() { // from class: com.hackscroll.app.MainActivity.4
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.root.removeView(view);
                }
            }, 350L);
        }
    }

    private View buildErrorView() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        linearLayout.setBackgroundColor(-16052719);
        int dp = dp(32);
        linearLayout.setPadding(dp, dp, dp, dp);
        TextView textView = new TextView(this);
        textView.setText("Can’t reach Hackaday");
        textView.setTextColor(-1);
        textView.setTextSize(20.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(17);
        linearLayout.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText("Check your connection and try again.");
        textView2.setTextColor(-5654849);
        textView2.setTextSize(14.0f);
        textView2.setGravity(17);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.setMargins(0, dp(8), 0, 0);
        linearLayout.addView(textView2, layoutParams);
        Button button = new Button(this);
        button.setText("RETRY");
        button.setTextColor(-16052719);
        button.setTextSize(13.0f);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-16726171);
        gradientDrawable.setCornerRadius(dp(8));
        button.setBackground(gradientDrawable);
        button.setOnClickListener(new View.OnClickListener() { // from class: com.hackscroll.app.MainActivity.5
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.refresh();
            }
        });
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(dp(160), dp(46));
        layoutParams2.setMargins(0, dp(24), 0, 0);
        linearLayout.addView(button, layoutParams2);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showError() {
        this.errorView.setVisibility(0);
    }

    private int dp(int i) {
        return (int) ((i * getResources().getDisplayMetrics().density) + 0.5f);
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.io.shutdownNow();
        super.onDestroy();
    }
}
