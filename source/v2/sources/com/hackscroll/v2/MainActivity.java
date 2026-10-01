package com.hackscroll.v2;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.hackscroll.v2.ArticlePager;
import com.hackscroll.v2.Fetch;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/* loaded from: classes.dex */
public class MainActivity extends Activity {
    private static final String FEED_URL = "https://hackaday.com/feed/";
    private static final int PAGE_LIMIT = 100;
    private static final int SORT_DATE = 0;
    private static final int SORT_RND = 2;
    private static final int SORT_TOP = 1;
    private Button errorButton;
    private TextView errorTitle;
    private LinearLayout errorView;
    private View hintView;
    private ImageLoader images;
    private TextView loadingPill;
    private TextView matchInfo;
    private TextView offlinePill;
    private ArticlePager pager;
    private SharedPreferences prefs;
    private TextView refreshPill;
    private FrameLayout root;
    private LinearLayout searchExpanded;
    private EditText searchInput;
    private TextView searchPill;
    private TextView sortDatePill;
    private TextView sortRndPill;
    private TextView sortTopPill;
    private LinearLayout topBar;
    private final ExecutorService io = Executors.newFixedThreadPool(SORT_RND);
    private final Random random = new Random();
    private ArrayList<Article> shuffled = null;
    private ArrayList<Article> shuffledBase = null;
    private int pendingStartPage = -1;
    private boolean firstPageEvent = true;
    private final ArrayList<Article> articles = new ArrayList<>();
    private final HashSet<String> loadedLinks = new HashSet<>();
    private int nextBrowsePage = SORT_TOP;
    private boolean noMoreBrowse = false;
    private final ArrayList<Article> searchResults = new ArrayList<>();
    private final HashSet<String> searchLinks = new HashSet<>();
    private int nextSearchPage = SORT_TOP;
    private boolean noMoreSearch = false;
    private String searchTotal = "";
    private final ArrayList<PageRef> pageRefs = new ArrayList<>();
    private ArrayList<Article> visible = new ArrayList<>();
    private int sortMode = SORT_DATE;
    private String searchQuery = "";
    private boolean loading = false;
    private boolean renderedOnce = false;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class PageRef {
        Article article;
        ImageView image;

        private PageRef() {
        }

        /* synthetic */ PageRef(PageRef pageRef) {
            this();
        }
    }

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.prefs = getSharedPreferences("hackscroll", SORT_DATE);
        this.images = new ImageLoader(this);
        this.root = new FrameLayout(this);
        this.root.setBackgroundColor(-16052719);
        this.pager = new ArticlePager(this);
        this.pager.setDescendantFocusability(393216);
        this.pager.setOnPageChangeListener(new ArticlePager.OnPageChangeListener() { // from class: com.hackscroll.v2.MainActivity.1
            @Override // com.hackscroll.v2.ArticlePager.OnPageChangeListener
            public void onPageChanged(int i) {
                if (MainActivity.this.firstPageEvent) {
                    MainActivity.this.firstPageEvent = false;
                    return;
                }
                MainActivity.this.hideHint();
                MainActivity.this.updateVisibleImages(i);
                MainActivity.this.maybeLoadMore(i);
            }
        });
        this.root.addView(this.pager, new FrameLayout.LayoutParams(-1, -1));
        buildTopBar();
        this.root.addView(buildOfflinePill());
        this.loadingPill = buildLoadingPill();
        this.root.addView(this.loadingPill);
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
            Iterator<Article> it = RssParser.parse(load).iterator();
            while (it.hasNext()) {
                Article next = it.next();
                if (next.link != null && this.loadedLinks.add(next.link)) {
                    this.articles.add(next);
                }
            }
            rebuild(false);
        }
        loadPage(SORT_TOP);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadPage(int i) {
        if (!this.loading) {
            this.loading = true;
            setLoadingPill(true, "LOADING MORE…");
            this.io.execute(new AnonymousClass2("https://hackaday.com/feed/?paged=" + i, i));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.hackscroll.v2.MainActivity$2  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 implements Runnable {
        private final /* synthetic */ int val$page;
        private final /* synthetic */ String val$url;

        AnonymousClass2(String str, int i) {
            this.val$url = str;
            this.val$page = i;
        }

        @Override // java.lang.Runnable
        public void run() {
            final String str = Fetch.get(this.val$url);
            final ArrayList<Article> parse = str == null ? null : RssParser.parse(str);
            MainActivity mainActivity = MainActivity.this;
            final int i = this.val$page;
            mainActivity.runOnUiThread(new Runnable() { // from class: com.hackscroll.v2.MainActivity.2.1
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.loading = false;
                    MainActivity.this.setLoadingPill(false, null);
                    if (parse != null) {
                        if (!parse.isEmpty()) {
                            MainActivity.this.offlinePill.setVisibility(8);
                            if (i == MainActivity.SORT_TOP) {
                                if (MainActivity.this.searchQuery.isEmpty()) {
                                    FeedStore.save(MainActivity.this, str);
                                }
                                MainActivity.this.articles.clear();
                                MainActivity.this.loadedLinks.clear();
                                MainActivity.this.shuffled = null;
                                MainActivity.this.shuffledBase = null;
                            }
                            Iterator it = parse.iterator();
                            int i2 = MainActivity.SORT_DATE;
                            while (it.hasNext()) {
                                Article article = (Article) it.next();
                                if (article.link != null && !MainActivity.this.loadedLinks.contains(article.link)) {
                                    MainActivity.this.loadedLinks.add(article.link);
                                    MainActivity.this.articles.add(article);
                                    MainActivity.this.shuffleInsert(MainActivity.this.articles, article);
                                    i2 += MainActivity.SORT_TOP;
                                }
                            }
                            if (i2 == 0 && i > MainActivity.SORT_TOP) {
                                MainActivity.this.noMoreBrowse = true;
                            }
                            MainActivity.this.nextBrowsePage = i + MainActivity.SORT_TOP;
                            if (i >= MainActivity.PAGE_LIMIT) {
                                MainActivity.this.noMoreBrowse = true;
                            }
                            MainActivity.this.rebuild(i != MainActivity.SORT_TOP ? MainActivity.SORT_TOP : false);
                            return;
                        }
                        MainActivity.this.noMoreBrowse = true;
                        if (!MainActivity.this.renderedOnce) {
                            MainActivity mainActivity2 = MainActivity.this;
                            final int i3 = i;
                            mainActivity2.showError("Can’t reach Hackaday", "RETRY", new Runnable() { // from class: com.hackscroll.v2.MainActivity.2.1.1
                                @Override // java.lang.Runnable
                                public void run() {
                                    MainActivity.this.loadPage(i3);
                                }
                            });
                        }
                        MainActivity.this.updateMatchInfo();
                        return;
                    }
                    MainActivity.this.onFailure(null);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void runSearch(int i) {
        if (!this.loading) {
            this.loading = true;
            setLoadingPill(this.renderedOnce, "SEARCHING…");
            this.io.execute(new AnonymousClass3(this.searchQuery, i));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.hackscroll.v2.MainActivity$3  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass3 implements Runnable {
        private final /* synthetic */ int val$page;
        private final /* synthetic */ String val$query;

        AnonymousClass3(String str, int i) {
            this.val$query = str;
            this.val$page = i;
        }

        @Override // java.lang.Runnable
        public void run() {
            Fetch.Result ex = Fetch.getEx(WpJsonSearch.urlFor(this.val$query, this.val$page));
            final ArrayList<Article> parse = ex == null ? null : WpJsonSearch.parse(ex.body);
            final String str = ex != null ? ex.total : null;
            MainActivity mainActivity = MainActivity.this;
            final String str2 = this.val$query;
            final int i = this.val$page;
            mainActivity.runOnUiThread(new Runnable() { // from class: com.hackscroll.v2.MainActivity.3.1
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.loading = false;
                    MainActivity.this.setLoadingPill(false, null);
                    if (str2.equals(MainActivity.this.searchQuery)) {
                        if (parse == null) {
                            MainActivity mainActivity2 = MainActivity.this;
                            final int i2 = i;
                            mainActivity2.onFailure(new Runnable() { // from class: com.hackscroll.v2.MainActivity.3.1.1
                                @Override // java.lang.Runnable
                                public void run() {
                                    MainActivity.this.runSearch(i2);
                                }
                            });
                            return;
                        }
                        if (str != null) {
                            MainActivity.this.searchTotal = str;
                        }
                        if (i == MainActivity.SORT_TOP) {
                            MainActivity.this.searchResults.clear();
                            MainActivity.this.searchLinks.clear();
                            MainActivity.this.shuffled = null;
                            MainActivity.this.shuffledBase = null;
                        }
                        Iterator it = parse.iterator();
                        while (it.hasNext()) {
                            Article article = (Article) it.next();
                            if (article.link != null && !MainActivity.this.searchLinks.contains(article.link)) {
                                MainActivity.this.searchLinks.add(article.link);
                                MainActivity.this.searchResults.add(article);
                                MainActivity.this.shuffleInsert(MainActivity.this.searchResults, article);
                            }
                        }
                        MainActivity.this.nextSearchPage = i + MainActivity.SORT_TOP;
                        if (parse.isEmpty() || MainActivity.this.searchResults.size() >= 1000) {
                            MainActivity.this.noMoreSearch = true;
                        }
                        if (MainActivity.this.searchResults.isEmpty()) {
                            MainActivity.this.showError("No results for “" + str2 + "”", "CLEAR", new Runnable() { // from class: com.hackscroll.v2.MainActivity.3.1.2
                                @Override // java.lang.Runnable
                                public void run() {
                                    MainActivity.this.clearSearch();
                                }
                            });
                        }
                        MainActivity.this.rebuild(i != MainActivity.SORT_TOP ? MainActivity.SORT_TOP : MainActivity.SORT_DATE);
                    }
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onFailure(Runnable runnable) {
        if (!this.renderedOnce || this.visible.isEmpty()) {
            showError("Can’t reach Hackaday", "RETRY", runnable);
        } else {
            flashOfflinePill();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onRefreshTap() {
        if (!this.loading) {
            this.refreshPill.animate().rotationBy(360.0f).setDuration(500L).start();
            if (this.searchQuery.isEmpty()) {
                loadPage(SORT_TOP);
            } else {
                runSearch(SORT_TOP);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeLoadMore(int i) {
        if (this.renderedOnce && !this.loading && this.pager.getChildCount() != 0 && i == this.pager.getChildCount() - 1) {
            if (this.searchQuery.isEmpty()) {
                if (!this.noMoreBrowse) {
                    loadPage(this.nextBrowsePage);
                }
            } else if (!this.noMoreSearch) {
                runSearch(this.nextSearchPage);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void rebuild(boolean z) {
        String str;
        int i;
        if (!z || this.visible.size() <= 0) {
            str = null;
        } else {
            str = this.visible.get(Math.min(this.pager.getCurrentPage(), this.visible.size() - 1)).link;
        }
        this.visible = computeVisible();
        this.pager.removeAllViews();
        this.pageRefs.clear();
        Iterator<Article> it = this.visible.iterator();
        while (it.hasNext()) {
            this.pager.addView(buildPage(it.next()));
        }
        if (this.pendingStartPage >= 0) {
            i = Math.min(this.pendingStartPage, Math.max((int) SORT_DATE, this.visible.size() - 1));
            this.pendingStartPage = -1;
        } else {
            if (str != null) {
                i = SORT_DATE;
                while (i < this.visible.size()) {
                    if (str.equals(this.visible.get(i).link)) {
                        break;
                    }
                    i += SORT_TOP;
                }
            }
            i = SORT_DATE;
        }
        this.pager.setPage(i, false);
        this.pager.requestLayout();
        updateVisibleImages(i);
        updateMatchInfo();
        this.renderedOnce = true;
        this.errorView.setVisibility(8);
        this.root.bringChildToFront(this.errorView);
        this.root.bringChildToFront(this.topBar);
        if (this.hintView != null) {
            this.root.bringChildToFront(this.hintView);
        }
    }

    private ArrayList<Article> computeVisible() {
        boolean z = true;
        ArrayList<Article> arrayList = this.searchQuery.isEmpty() ? this.articles : this.searchResults;
        if (this.sortMode == SORT_RND) {
            if (this.shuffled == null || this.shuffledBase != arrayList) {
                this.shuffled = Article.shuffledCopy(arrayList, this.random);
                this.shuffledBase = arrayList;
            }
            return new ArrayList<>(this.shuffled);
        }
        return Article.sortCopy(arrayList, (this.sortMode == SORT_TOP && this.searchQuery.isEmpty()) ? false : false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void shuffleInsert(ArrayList<Article> arrayList, Article article) {
        if (this.sortMode == SORT_RND && this.shuffled != null && this.shuffledBase == arrayList) {
            this.shuffled.add(this.random.nextInt(this.shuffled.size() + SORT_TOP), article);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateVisibleImages(int i) {
        int i2 = SORT_DATE;
        while (true) {
            int i3 = i2;
            if (i3 < this.pageRefs.size()) {
                PageRef pageRef = this.pageRefs.get(i3);
                if (Math.abs(i3 - i) <= SORT_TOP) {
                    this.images.load(pageRef.image, pageRef.article.imageUrl);
                } else {
                    pageRef.image.setImageDrawable(null);
                }
                i2 = i3 + SORT_TOP;
            } else {
                return;
            }
        }
    }

    private View buildPage(final Article article) {
        FrameLayout frameLayout = new FrameLayout(this);
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundColor(-15460324);
        frameLayout.addView(imageView, new FrameLayout.LayoutParams(-1, -1));
        View view = new View(this);
        view.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{1493172224, 0, 1426063360, -234881024}));
        frameLayout.addView(view, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(SORT_TOP);
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
        layoutParams.setMargins(SORT_DATE, dp(10), SORT_DATE, SORT_DATE);
        linearLayout.addView(textView2, layoutParams);
        String str = article.dateText;
        if (article.comments > 0) {
            str = String.valueOf(str) + "   ·   " + article.comments + " comments";
        }
        TextView textView3 = new TextView(this);
        textView3.setText(str);
        textView3.setTextColor(-5654849);
        textView3.setTextSize(12.0f);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.setMargins(SORT_DATE, dp(6), SORT_DATE, SORT_DATE);
        linearLayout.addView(textView3, layoutParams2);
        if (article.excerpt != null && !article.excerpt.isEmpty()) {
            TextView textView4 = new TextView(this);
            textView4.setText(article.excerpt);
            textView4.setTextColor(-2695452);
            textView4.setTextSize(14.0f);
            textView4.setMaxLines(3);
            textView4.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, -2);
            layoutParams3.setMargins(SORT_DATE, dp(8), SORT_DATE, SORT_DATE);
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
        button.setPadding(dp(18), SORT_DATE, dp(18), SORT_DATE);
        button.setMinHeight(SORT_DATE);
        button.setMinimumHeight(SORT_DATE);
        button.setOnClickListener(new View.OnClickListener() { // from class: com.hackscroll.v2.MainActivity.4
            @Override // android.view.View.OnClickListener
            public void onClick(View view2) {
                MainActivity.this.openLink(article.link);
            }
        });
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(-2, dp(46));
        layoutParams4.setMargins(SORT_DATE, dp(16), SORT_DATE, SORT_DATE);
        linearLayout.addView(button, layoutParams4);
        frameLayout.addView(linearLayout, new FrameLayout.LayoutParams(-1, -1));
        PageRef pageRef = new PageRef(null);
        pageRef.article = article;
        pageRef.image = imageView;
        this.pageRefs.add(pageRef);
        return frameLayout;
    }

    private void buildTopBar() {
        LinearLayout linearLayout = new LinearLayout(this);
        this.topBar = linearLayout;
        linearLayout.setOrientation(SORT_TOP);
        int dp = dp(10);
        linearLayout.setPadding(dp, dp(12), dp, SORT_DATE);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(SORT_DATE);
        linearLayout2.setGravity(16);
        this.sortDatePill = makePill("NEW");
        this.sortDatePill.setOnClickListener(new View.OnClickListener() { // from class: com.hackscroll.v2.MainActivity.5
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.setSort(MainActivity.SORT_DATE);
            }
        });
        linearLayout2.addView(this.sortDatePill);
        this.sortTopPill = makePill("TOP");
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, dp(34));
        layoutParams.setMargins(dp(6), SORT_DATE, SORT_DATE, SORT_DATE);
        this.sortTopPill.setLayoutParams(layoutParams);
        this.sortTopPill.setOnClickListener(new View.OnClickListener() { // from class: com.hackscroll.v2.MainActivity.6
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.setSort(MainActivity.SORT_TOP);
            }
        });
        linearLayout2.addView(this.sortTopPill);
        this.sortRndPill = makePill("RND");
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, dp(34));
        layoutParams2.setMargins(dp(6), SORT_DATE, SORT_DATE, SORT_DATE);
        this.sortRndPill.setLayoutParams(layoutParams2);
        this.sortRndPill.setOnClickListener(new View.OnClickListener() { // from class: com.hackscroll.v2.MainActivity.7
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.setSort(MainActivity.SORT_RND);
            }
        });
        linearLayout2.addView(this.sortRndPill);
        linearLayout2.addView(new View(this), new LinearLayout.LayoutParams(SORT_DATE, SORT_TOP, 1.0f));
        this.refreshPill = makePill("⟳");
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-2, dp(34));
        layoutParams3.setMargins(dp(6), SORT_DATE, dp(6), SORT_DATE);
        this.refreshPill.setLayoutParams(layoutParams3);
        this.refreshPill.setOnClickListener(new View.OnClickListener() { // from class: com.hackscroll.v2.MainActivity.8
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.onRefreshTap();
            }
        });
        linearLayout2.addView(this.refreshPill);
        this.searchPill = makePill("⌕");
        this.searchPill.setOnClickListener(new View.OnClickListener() { // from class: com.hackscroll.v2.MainActivity.9
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.expandSearch();
            }
        });
        linearLayout2.addView(this.searchPill);
        linearLayout.addView(linearLayout2);
        this.searchExpanded = buildSearchExpanded();
        this.searchExpanded.setVisibility(8);
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(-1, dp(38));
        layoutParams4.setMargins(SORT_DATE, dp(8), SORT_DATE, SORT_DATE);
        this.searchExpanded.setLayoutParams(layoutParams4);
        linearLayout.addView(this.searchExpanded);
        this.matchInfo = new TextView(this);
        this.matchInfo.setTextColor(-5654849);
        this.matchInfo.setTextSize(11.0f);
        this.matchInfo.setPadding(dp(4), dp(6), dp(4), SORT_DATE);
        this.matchInfo.setVisibility(8);
        linearLayout.addView(this.matchInfo);
        this.root.addView(linearLayout, new FrameLayout.LayoutParams(-1, -2, 48));
        styleSortPills();
    }

    private TextView makePill(String str) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(12.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(17);
        textView.setPadding(dp(10), SORT_DATE, dp(10), SORT_DATE);
        textView.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(34)));
        return textView;
    }

    private void stylePill(TextView textView, boolean z) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        if (z) {
            gradientDrawable.setColor(-16726171);
            textView.setTextColor(-16052719);
        } else {
            gradientDrawable.setColor(-1725551821);
            textView.setTextColor(-2695452);
        }
        gradientDrawable.setCornerRadius(dp(17));
        textView.setBackground(gradientDrawable);
    }

    private void styleSortPills() {
        stylePill(this.sortDatePill, this.sortMode == 0 ? SORT_TOP : SORT_DATE);
        stylePill(this.sortTopPill, this.sortMode == SORT_TOP ? SORT_TOP : SORT_DATE);
        stylePill(this.sortRndPill, this.sortMode == SORT_RND ? SORT_TOP : SORT_DATE);
        boolean z = this.searchQuery.isEmpty() ? SORT_DATE : SORT_TOP;
        this.sortTopPill.setEnabled(z ? false : SORT_TOP);
        this.sortTopPill.setAlpha(z ? 0.4f : 1.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSort(int i) {
        if (i != SORT_TOP || this.searchQuery.isEmpty()) {
            if (this.sortMode != i || i == SORT_RND) {
                this.sortMode = i;
                this.shuffled = null;
                this.shuffledBase = null;
                if (i == SORT_RND) {
                    ArrayList<Article> arrayList = this.searchQuery.isEmpty() ? this.articles : this.searchResults;
                    if (!arrayList.isEmpty()) {
                        this.pendingStartPage = this.random.nextInt(arrayList.size());
                    }
                }
                styleSortPills();
                rebuild(false);
            }
        }
    }

    private LinearLayout buildSearchExpanded() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(SORT_DATE);
        linearLayout.setGravity(16);
        this.searchInput = new EditText(this);
        this.searchInput.setHint("Search all of Hackaday…");
        this.searchInput.setHintTextColor(-8615276);
        this.searchInput.setTextColor(-1);
        this.searchInput.setTextSize(13.0f);
        this.searchInput.setSingleLine(true);
        this.searchInput.setImeOptions(3);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-14472396);
        gradientDrawable.setCornerRadius(dp(17));
        gradientDrawable.setStroke(dp(SORT_TOP), 1308622847);
        this.searchInput.setBackground(gradientDrawable);
        this.searchInput.setPadding(dp(14), SORT_DATE, dp(14), SORT_DATE);
        linearLayout.addView(this.searchInput, new LinearLayout.LayoutParams(SORT_DATE, dp(38), 1.0f));
        TextView makePill = makePill("GO");
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, dp(38));
        layoutParams.setMargins(dp(8), SORT_DATE, SORT_DATE, SORT_DATE);
        makePill.setLayoutParams(layoutParams);
        stylePill(makePill, true);
        makePill.setOnClickListener(new View.OnClickListener() { // from class: com.hackscroll.v2.MainActivity.10
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.submitSearch();
            }
        });
        linearLayout.addView(makePill);
        TextView makePill2 = makePill("✕");
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, dp(38));
        layoutParams2.setMargins(dp(8), SORT_DATE, SORT_DATE, SORT_DATE);
        makePill2.setLayoutParams(layoutParams2);
        stylePill(makePill2, false);
        makePill2.setOnClickListener(new View.OnClickListener() { // from class: com.hackscroll.v2.MainActivity.11
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.clearSearch();
            }
        });
        linearLayout.addView(makePill2);
        this.searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: com.hackscroll.v2.MainActivity.12
            @Override // android.widget.TextView.OnEditorActionListener
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                MainActivity.this.submitSearch();
                return true;
            }
        });
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void expandSearch() {
        this.searchPill.setVisibility(8);
        this.searchExpanded.setVisibility(SORT_DATE);
        this.searchInput.requestFocus();
        this.searchInput.post(new Runnable() { // from class: com.hackscroll.v2.MainActivity.13
            @Override // java.lang.Runnable
            public void run() {
                InputMethodManager inputMethodManager = (InputMethodManager) MainActivity.this.getSystemService("input_method");
                if (inputMethodManager != null) {
                    inputMethodManager.showSoftInput(MainActivity.this.searchInput, MainActivity.SORT_TOP);
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void submitSearch() {
        String trim = this.searchInput.getText().toString().trim();
        hideKeyboard();
        if (!trim.isEmpty() && !trim.equals(this.searchQuery)) {
            this.searchQuery = trim;
            this.searchTotal = "";
            this.nextSearchPage = SORT_TOP;
            this.noMoreSearch = false;
            styleSortPills();
            updateMatchInfo();
            runSearch(SORT_TOP);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearSearch() {
        this.searchQuery = "";
        this.searchResults.clear();
        this.searchLinks.clear();
        this.searchTotal = "";
        this.searchInput.setText("");
        hideKeyboard();
        this.searchExpanded.setVisibility(8);
        this.searchPill.setVisibility(SORT_DATE);
        styleSortPills();
        rebuild(false);
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService("input_method");
        if (inputMethodManager != null && this.searchInput != null) {
            inputMethodManager.hideSoftInputFromWindow(this.searchInput.getWindowToken(), SORT_DATE);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMatchInfo() {
        int i = SORT_DATE;
        boolean z = this.searchQuery.isEmpty() ? SORT_DATE : SORT_TOP;
        TextView textView = this.matchInfo;
        if (!z) {
            i = 8;
        }
        textView.setVisibility(i);
        if (z) {
            StringBuilder sb = new StringBuilder();
            if (!this.searchTotal.isEmpty()) {
                sb.append(this.searchTotal).append(" matches");
            } else {
                sb.append(this.searchResults.size()).append(this.searchResults.size() == SORT_TOP ? " match" : " matches");
            }
            sb.append(" · ").append(this.searchResults.size()).append(" loaded");
            if (this.noMoreSearch) {
                sb.append(" · end of results");
            } else {
                sb.append(" · swipe to the last page for more");
            }
            this.matchInfo.setText(sb.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openLink(String str) {
        try {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse(str)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No browser found", (int) SORT_DATE).show();
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
        layoutParams.setMargins(SORT_DATE, dp(96), SORT_DATE, SORT_DATE);
        this.offlinePill.setLayoutParams(layoutParams);
        return this.offlinePill;
    }

    private void flashOfflinePill() {
        this.offlinePill.setVisibility(SORT_DATE);
        this.offlinePill.animate().alpha(1.0f).setDuration(200L).start();
        this.offlinePill.postDelayed(new Runnable() { // from class: com.hackscroll.v2.MainActivity.14
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.offlinePill.animate().alpha(0.0f).setDuration(600L).start();
            }
        }, 2500L);
    }

    private TextView buildLoadingPill() {
        TextView textView = new TextView(this);
        textView.setText("LOADING MORE…");
        textView.setTextColor(-16052719);
        textView.setTextSize(11.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-16726171);
        gradientDrawable.setCornerRadius(dp(14));
        textView.setBackground(gradientDrawable);
        textView.setPadding(dp(12), dp(6), dp(12), dp(6));
        textView.setVisibility(8);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2, 81);
        layoutParams.setMargins(SORT_DATE, SORT_DATE, SORT_DATE, dp(24));
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setLoadingPill(boolean z, String str) {
        if (str != null) {
            this.loadingPill.setText(str);
        }
        this.loadingPill.setVisibility(z ? SORT_DATE : 8);
    }

    private View buildHint() {
        TextView textView = new TextView(this);
        textView.setText("Swipe up for the next hack — more load as you go ↑");
        textView.setTextColor(-1);
        textView.setTextSize(14.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-870571221);
        gradientDrawable.setCornerRadius(dp(18));
        textView.setBackground(gradientDrawable);
        textView.setPadding(dp(20), dp(10), dp(20), dp(10));
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2, 49);
        layoutParams.setMargins(SORT_DATE, dp(140), SORT_DATE, SORT_DATE);
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
            view.postDelayed(new Runnable() { // from class: com.hackscroll.v2.MainActivity.15
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.root.removeView(view);
                }
            }, 350L);
        }
    }

    private LinearLayout buildErrorView() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(SORT_TOP);
        linearLayout.setGravity(17);
        linearLayout.setBackgroundColor(-16052719);
        int dp = dp(32);
        linearLayout.setPadding(dp, dp, dp, dp);
        this.errorTitle = new TextView(this);
        this.errorTitle.setText("Can’t reach Hackaday");
        this.errorTitle.setTextColor(-1);
        this.errorTitle.setTextSize(20.0f);
        this.errorTitle.setTypeface(Typeface.DEFAULT_BOLD);
        this.errorTitle.setGravity(17);
        linearLayout.addView(this.errorTitle);
        TextView textView = new TextView(this);
        textView.setText("Check your connection and try again.");
        textView.setTextColor(-5654849);
        textView.setTextSize(14.0f);
        textView.setGravity(17);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.setMargins(SORT_DATE, dp(8), SORT_DATE, SORT_DATE);
        linearLayout.addView(textView, layoutParams);
        this.errorButton = new Button(this);
        this.errorButton.setText("RETRY");
        this.errorButton.setTextColor(-16052719);
        this.errorButton.setTextSize(13.0f);
        this.errorButton.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-16726171);
        gradientDrawable.setCornerRadius(dp(8));
        this.errorButton.setBackground(gradientDrawable);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(dp(160), dp(46));
        layoutParams2.setMargins(SORT_DATE, dp(24), SORT_DATE, SORT_DATE);
        linearLayout.addView(this.errorButton, layoutParams2);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showError(String str, String str2, final Runnable runnable) {
        this.errorTitle.setText(str);
        this.errorButton.setText(str2);
        this.errorButton.setOnClickListener(new View.OnClickListener() { // from class: com.hackscroll.v2.MainActivity.16
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.errorView.setVisibility(8);
                runnable.run();
            }
        });
        this.errorView.setVisibility(SORT_DATE);
        this.root.bringChildToFront(this.topBar);
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
