package com.hackscroll.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String FEED_URL = "https://hackaday.com/feed/";

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private ArticlePager pager;
    private FrameLayout root;
    private View hintView;
    private View errorView;
    private TextView offlinePill;
    private ImageLoader images;
    private SharedPreferences prefs;
    private boolean renderedOnce = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("hackscroll", MODE_PRIVATE);
        images = new ImageLoader(this);

        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0B0E11);

        pager = new ArticlePager(this);
        pager.setOnPageChangeListener(new ArticlePager.OnPageChangeListener() {
            @Override
            public void onPageChanged(int page) {
                hideHint();
            }
        });
        root.addView(pager, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(buildOfflinePill());
        errorView = buildErrorView();
        errorView.setVisibility(View.GONE);
        root.addView(errorView);

        if (!prefs.getBoolean("hintShown", false)) {
            hintView = buildHint();
            root.addView(hintView);
        }

        setContentView(root);

        // 1. Show cached stories immediately if available.
        String cached = FeedStore.load(this);
        if (cached != null) {
            render(RssParser.parse(cached), false);
        }
        // 2. Fetch the newest feed; replaces the content on success.
        refresh();
    }

    private void refresh() {
        errorView.setVisibility(View.GONE);
        io.execute(new Runnable() {
            @Override
            public void run() {
                final String xml = Fetch.get(FEED_URL);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (xml != null) {
                            FeedStore.save(MainActivity.this, xml);
                            List<Article> articles = RssParser.parse(xml);
                            if (!articles.isEmpty()) {
                                offlinePill.setVisibility(View.GONE);
                                render(articles, true);
                                return;
                            }
                        }
                        // Networking failed or feed was unparseable.
                        if (!renderedOnce) {
                            showError();
                        } else {
                            offlinePill.setVisibility(View.VISIBLE);
                            offlinePill.animate().alpha(1f).setDuration(200).start();
                            offlinePill.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    offlinePill.animate().alpha(0f).setDuration(600).start();
                                }
                            }, 2500);
                        }
                    }
                });
            }
        });
    }

    private void render(List<Article> articles, boolean keepPosition) {
        if (articles == null || articles.isEmpty()) return;
        int keep = pager.getCurrentPage();
        pager.removeAllViews();
        for (Article a : articles) {
            pager.addView(buildPage(a));
        }
        int target = (keepPosition && keep < articles.size()) ? keep : 0;
        pager.setPage(target, false);
        pager.requestLayout();
        renderedOnce = true;
        root.bringChildToFront(errorView);
        if (hintView != null) root.bringChildToFront(hintView);
    }

    private View buildPage(final Article a) {
        FrameLayout page = new FrameLayout(this);

        // Article image, cropped to fill.
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackgroundColor(0xFF14181C);
        page.addView(img, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        images.load(img, a.imageUrl);

        // Dark gradient so text stays readable over any image.
        View shade = new View(this);
        shade.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x59000000, 0x00000000, 0x55000000, 0xF2000000}));
        page.addView(shade, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Bottom text block.
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.BOTTOM);
        int pad = dp(22);
        box.setPadding(pad, pad, pad, dp(30));

        TextView brand = new TextView(this);
        brand.setText("HACKADAY  ·  HACKSCROLL");
        brand.setTextColor(0xFF00C765);
        brand.setTextSize(11);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setLetterSpacing(0.18f);
        box.addView(brand);

        TextView title = new TextView(this);
        title.setText(a.title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(21);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setMaxLines(4);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.setMargins(0, dp(10), 0, 0);
        box.addView(title, tlp);

        if (a.dateText != null && !a.dateText.isEmpty()) {
            TextView date = new TextView(this);
            date.setText(a.dateText);
            date.setTextColor(0xFFA9B6BF);
            date.setTextSize(12);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dlp.setMargins(0, dp(6), 0, 0);
            box.addView(date, dlp);
        }

        if (a.excerpt != null && !a.excerpt.isEmpty()) {
            TextView excerpt = new TextView(this);
            excerpt.setText(a.excerpt);
            excerpt.setTextColor(0xFFD6DEE4);
            excerpt.setTextSize(14);
            excerpt.setMaxLines(3);
            excerpt.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            elp.setMargins(0, dp(8), 0, 0);
            box.addView(excerpt, elp);
        }

        Button read = new Button(this);
        read.setText("READ ON HACKADAY  \u2192");
        read.setTextColor(0xFF0B0E11);
        read.setTextSize(13);
        read.setTypeface(Typeface.DEFAULT_BOLD);
        read.setAllCaps(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF00C765);
        bg.setCornerRadius(dp(8));
        read.setBackground(bg);
        read.setPadding(dp(18), 0, dp(18), 0);
        read.setMinHeight(0);
        read.setMinimumHeight(0);
        read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLink(a.link);
            }
        });
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(46));
        rlp.setMargins(0, dp(16), 0, 0);
        box.addView(read, rlp);

        page.addView(box, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return page;
    }

    private void openLink(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show();
        }
    }

    private TextView buildOfflinePill() {
        offlinePill = new TextView(this);
        offlinePill.setText("OFFLINE \u00B7 SHOWING CACHED");
        offlinePill.setTextColor(0xFFD6DEE4);
        offlinePill.setTextSize(11);
        offlinePill.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xB3262B33);
        bg.setCornerRadius(dp(14));
        offlinePill.setBackground(bg);
        int p = dp(10);
        offlinePill.setPadding(p, dp(6), p, dp(6));
        offlinePill.setAlpha(0f);
        offlinePill.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lp.setMargins(0, dp(52), 0, 0);
        offlinePill.setLayoutParams(lp);
        return offlinePill;
    }

    private View buildHint() {
        TextView hint = new TextView(this);
        hint.setText("Swipe up for the next hack \u2191");
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(14);
        hint.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC1C232B);
        bg.setCornerRadius(dp(18));
        hint.setBackground(bg);
        hint.setPadding(dp(20), dp(10), dp(20), dp(10));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lp.setMargins(0, dp(96), 0, 0);
        hint.setLayoutParams(lp);
        return hint;
    }

    private void hideHint() {
        if (hintView == null) return;
        prefs.edit().putBoolean("hintShown", true).apply();
        final View h = hintView;
        hintView = null;
        h.animate().alpha(0f).setDuration(300).start();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                root.removeView(h);
            }
        }, 350);
    }

    private View buildErrorView() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(0xFF0B0E11);
        int pad = dp(32);
        box.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Can\u2019t reach Hackaday");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Check your connection and try again.");
        sub.setTextColor(0xFFA9B6BF);
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.setMargins(0, dp(8), 0, 0);
        box.addView(sub, slp);

        Button retry = new Button(this);
        retry.setText("RETRY");
        retry.setTextColor(0xFF0B0E11);
        retry.setTextSize(13);
        retry.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF00C765);
        bg.setCornerRadius(dp(8));
        retry.setBackground(bg);
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(dp(160), dp(46));
        rlp.setMargins(0, dp(24), 0, 0);
        box.addView(retry, rlp);
        return box;
    }

    private void showError() {
        errorView.setVisibility(View.VISIBLE);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
