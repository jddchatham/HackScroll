package com.hackscroll.app;

import android.util.Xml;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;
/* loaded from: classes.dex */
public final class RssParser {
    private static final String NS_CONTENT = "http://purl.org/rss/1.0/modules/content/";
    private static final String NS_MEDIA = "http://search.yahoo.com/mrss/";
    private static final Pattern IMG = Pattern.compile("<img[^>]+?src=[\"']([^\"']+)[\"']", 2);
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern NUM_ENTITY = Pattern.compile("&#(\\d+);|&#x([0-9a-fA-F]+);|&(\\w+);");
    private static final Map<String, String> ENTITIES = new HashMap();

    private RssParser() {
    }

    static {
        ENTITIES.put("amp", "&");
        ENTITIES.put("lt", "<");
        ENTITIES.put("gt", ">");
        ENTITIES.put("quot", "\"");
        ENTITIES.put("apos", "'");
        ENTITIES.put("nbsp", " ");
        ENTITIES.put("hellip", "…");
        ENTITIES.put("mdash", "—");
        ENTITIES.put("ndash", "–");
        ENTITIES.put("lsquo", "‘");
        ENTITIES.put("rsquo", "’");
        ENTITIES.put("ldquo", "“");
        ENTITIES.put("rdquo", "”");
    }

    public static ArrayList<Article> parse(String str) {
        ArrayList<Article> arrayList = new ArrayList<>();
        if (str != null) {
            try {
                XmlPullParser newPullParser = Xml.newPullParser();
                newPullParser.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces", true);
                newPullParser.setInput(new StringReader(str));
                Article article = null;
                for (int eventType = newPullParser.getEventType(); eventType != 1; eventType = newPullParser.next()) {
                    if (eventType == 2) {
                        String name = newPullParser.getName();
                        if ("item".equals(name)) {
                            article = new Article();
                        } else if (article != null) {
                            readField(newPullParser, article, name);
                        }
                    } else if (eventType == 3 && article != null && "item".equals(newPullParser.getName())) {
                        finish(article);
                        if (article.title != null && !article.title.isEmpty() && article.link != null && !article.link.isEmpty()) {
                            arrayList.add(article);
                        }
                        article = null;
                    }
                }
            } catch (Exception e) {
            }
        }
        return arrayList;
    }

    private static void readField(XmlPullParser xmlPullParser, Article article, String str) throws Exception {
        String namespace = xmlPullParser.getNamespace();
        if ("title".equals(str) && namespace.isEmpty()) {
            article.title = xmlPullParser.nextText();
        } else if ("link".equals(str) && namespace.isEmpty()) {
            article.link = xmlPullParser.nextText().trim();
        } else if ("pubDate".equals(str) && namespace.isEmpty()) {
            article.pubDate = xmlPullParser.nextText().trim();
        } else if ("description".equals(str) && namespace.isEmpty()) {
            article.descHtml = xmlPullParser.nextText();
        } else if ("encoded".equals(str) && NS_CONTENT.equals(namespace)) {
            article.contentHtml = xmlPullParser.nextText();
        } else if ("content".equals(str) && NS_MEDIA.equals(namespace)) {
            String attributeValue = xmlPullParser.getAttributeValue(null, "url");
            if (article.mediaUrl == null && attributeValue != null && !attributeValue.isEmpty()) {
                article.mediaUrl = attributeValue.trim();
            }
            skipElement(xmlPullParser);
        } else {
            skipElement(xmlPullParser);
        }
    }

    private static void skipElement(XmlPullParser xmlPullParser) throws Exception {
        int i = 1;
        while (i > 0) {
            int next = xmlPullParser.next();
            if (next == 2) {
                i++;
            } else if (next == 3) {
                i--;
            }
        }
    }

    private static void finish(Article article) {
        article.title = decode(stripTags(article.title)).trim();
        String firstImage = firstImage(article.contentHtml);
        if (firstImage == null) {
            firstImage = article.mediaUrl;
        }
        if (firstImage == null) {
            firstImage = firstImage(article.descHtml);
        }
        article.imageUrl = firstImage == null ? "" : firstImage.trim();
        String trim = decode(stripTags(article.descHtml == null ? "" : article.descHtml)).trim();
        int indexOf = trim.toLowerCase(Locale.US).indexOf("read more");
        if (indexOf > 0) {
            trim = trim.substring(0, indexOf);
        }
        String trim2 = trim.replaceAll("[\\s…]+$", "").trim();
        if (trim2.length() > 240) {
            String substring = trim2.substring(0, 240);
            int lastIndexOf = substring.lastIndexOf(32);
            if (lastIndexOf > 120) {
                substring = substring.substring(0, lastIndexOf);
            }
            trim2 = String.valueOf(substring) + "…";
        }
        article.excerpt = trim2;
        article.dateText = formatDate(article.pubDate);
    }

    private static String firstImage(String str) {
        if (str == null) {
            return null;
        }
        Matcher matcher = IMG.matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String stripTags(String str) {
        return str == null ? "" : TAG.matcher(str).replaceAll(" ").replaceAll("\\s+", " ");
    }

    static String decode(String str) {
        String str2;
        int parseInt;
        if (str == null || str.indexOf(38) < 0) {
            return str == null ? "" : str;
        }
        Matcher matcher = NUM_ENTITY.matcher(str);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            try {
            } catch (Exception e) {
                str2 = " ";
            }
            if (matcher.group(1) != null) {
                parseInt = Integer.parseInt(matcher.group(1));
            } else if (matcher.group(2) != null) {
                parseInt = Integer.parseInt(matcher.group(2), 16);
            } else {
                String str3 = ENTITIES.get(matcher.group(3).toLowerCase(Locale.US));
                if (str3 == null) {
                    str3 = matcher.group();
                }
                matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(str3));
            }
            str2 = new String(Character.toChars(parseInt));
            matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(str2));
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    private static String formatDate(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        try {
            return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).parse(str.trim()));
        } catch (Exception e) {
            return str;
        }
    }
}
