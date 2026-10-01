package com.hackscroll.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
/* loaded from: classes.dex */
public class Article {
    public int comments;
    public String contentHtml;
    public long dateMillis;
    public String dateText;
    public String descHtml;
    public String excerpt;
    public String imageUrl;
    public String link;
    public String mediaUrl;
    public String pubDate;
    public String title;

    public static ArrayList<Article> sortCopy(List<Article> list, final boolean z) {
        ArrayList<Article> arrayList = new ArrayList<>(list);
        Collections.sort(arrayList, new Comparator<Article>() { // from class: com.hackscroll.v2.Article.1
            @Override // java.util.Comparator
            public int compare(Article article, Article article2) {
                if (z && article2.comments != article.comments) {
                    return article2.comments - article.comments;
                }
                if (article2.dateMillis != article.dateMillis) {
                    return article2.dateMillis > article.dateMillis ? 1 : -1;
                }
                return 0;
            }
        });
        return arrayList;
    }

    public static ArrayList<Article> shuffledCopy(List<Article> list, Random random) {
        ArrayList<Article> arrayList = new ArrayList<>(list);
        Collections.shuffle(arrayList, random);
        return arrayList;
    }
}
