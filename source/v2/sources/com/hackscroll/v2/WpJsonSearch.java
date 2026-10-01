package com.hackscroll.v2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
/* loaded from: classes.dex */
final class WpJsonSearch {
    static final String ENDPOINT = "https://hackaday.com/wp-json/wp/v2/posts";

    private WpJsonSearch() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String urlFor(String str, int i) {
        try {
            str = URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        return "https://hackaday.com/wp-json/wp/v2/posts?search=" + str + "&per_page=20&page=" + i + "&_embed";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ArrayList<Article> parse(String str) {
        ArrayList<Article> arrayList = new ArrayList<>();
        if (str != null) {
            try {
                JSONArray jSONArray = new JSONArray(str);
                for (int i = 0; i < jSONArray.length(); i++) {
                    JSONObject optJSONObject = jSONArray.optJSONObject(i);
                    if (optJSONObject != null) {
                        Article article = new Article();
                        article.link = optJSONObject.optString("link", "").trim();
                        if (!article.link.isEmpty()) {
                            article.dateMillis = RssParser.parseIsoMillis(optJSONObject.optString("date", ""));
                            article.dateText = RssParser.formatMillis(article.dateMillis);
                            JSONObject optJSONObject2 = optJSONObject.optJSONObject("title");
                            article.title = RssParser.decode(RssParser.stripTags(optJSONObject2 == null ? "" : optJSONObject2.optString("rendered", ""))).trim();
                            JSONObject optJSONObject3 = optJSONObject.optJSONObject("excerpt");
                            article.excerpt = RssParser.makeExcerpt(optJSONObject3 == null ? "" : optJSONObject3.optString("rendered", ""));
                            article.imageUrl = findImage(optJSONObject);
                            arrayList.add(article);
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
        return arrayList;
    }

    private static String findImage(JSONObject jSONObject) {
        JSONArray optJSONArray;
        JSONObject optJSONObject;
        JSONObject optJSONObject2;
        try {
            JSONObject optJSONObject3 = jSONObject.optJSONObject("_embedded");
            if (optJSONObject3 == null || (optJSONArray = optJSONObject3.optJSONArray("wp:featuredmedia")) == null || optJSONArray.length() == 0 || (optJSONObject = optJSONArray.optJSONObject(0)) == null) {
                return "";
            }
            JSONObject optJSONObject4 = optJSONObject.optJSONObject("media_details");
            JSONObject optJSONObject5 = optJSONObject4 == null ? null : optJSONObject4.optJSONObject("sizes");
            if (optJSONObject5 != null && (optJSONObject2 = optJSONObject5.optJSONObject("medium")) != null && !optJSONObject2.optString("source_url", "").isEmpty()) {
                return optJSONObject2.optString("source_url");
            }
            return optJSONObject.optString("source_url", "");
        } catch (Exception e) {
            return "";
        }
    }
}
