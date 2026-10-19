package com.hackscroll.app;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the Hackaday WordPress RSS 2.0 feed, including content:encoded. */
public final class RssParser {

    private RssParser() {}

    private static final String NS_CONTENT = "http://purl.org/rss/1.0/modules/content/";
    private static final String NS_MEDIA = "http://search.yahoo.com/mrss/";

    private static final Pattern IMG =
            Pattern.compile("<img[^>]+?src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern NUM_ENTITY = Pattern.compile("&#(\\d+);|&#x([0-9a-fA-F]+);|&(\\w+);");

    private static final Map<String, String> ENTITIES = new HashMap<String, String>();
    static {
        ENTITIES.put("amp", "&");
        ENTITIES.put("lt", "<");
        ENTITIES.put("gt", ">");
        ENTITIES.put("quot", "\"");
        ENTITIES.put("apos", "'");
        ENTITIES.put("nbsp", " ");
        ENTITIES.put("hellip", "\u2026");
        ENTITIES.put("mdash", "\u2014");
        ENTITIES.put("ndash", "\u2013");
        ENTITIES.put("lsquo", "\u2018");
        ENTITIES.put("rsquo", "\u2019");
        ENTITIES.put("ldquo", "\u201C");
        ENTITIES.put("rdquo", "\u201D");
    }

    public static ArrayList<Article> parse(String xml) {
        ArrayList<Article> out = new ArrayList<Article>();
        if (xml == null) return out;
        try {
            XmlPullParser p = Xml.newPullParser();
            p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            p.setInput(new StringReader(xml));
            Article cur = null;
            int ev = p.getEventType();
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    String name = p.getName();
                    if ("item".equals(name)) {
                        cur = new Article();
                    } else if (cur != null) {
                        readField(p, cur, name);
                    }
                } else if (ev == XmlPullParser.END_TAG) {
                    if (cur != null && "item".equals(p.getName())) {
                        finish(cur);
                        if (cur.title != null && !cur.title.isEmpty()
                                && cur.link != null && !cur.link.isEmpty()) {
                            out.add(cur);
                        }
                        cur = null;
                    }
                }
                ev = p.next();
            }
        } catch (Exception e) {
            // Malformed feed: return whatever was parsed before the failure.
        }
        return out;
    }

    private static void readField(XmlPullParser p, Article a, String name) throws Exception {
        String ns = p.getNamespace();
        if ("title".equals(name) && ns.isEmpty()) {
            a.title = p.nextText();
        } else if ("link".equals(name) && ns.isEmpty()) {
            a.link = p.nextText().trim();
        } else if ("pubDate".equals(name) && ns.isEmpty()) {
            a.pubDate = p.nextText().trim();
        } else if ("description".equals(name) && ns.isEmpty()) {
            a.descHtml = p.nextText();
        } else if ("encoded".equals(name) && NS_CONTENT.equals(ns)) {
            a.contentHtml = p.nextText();
        } else if ("content".equals(name) && NS_MEDIA.equals(ns)) {
            String url = p.getAttributeValue(null, "url");
            if (a.mediaUrl == null && url != null && !url.isEmpty()) a.mediaUrl = url.trim();
            skipElement(p);
        } else {
            // Unknown element inside <item> (category, guid, dc:creator, ...):
            // skip its subtree so child nodes cannot pollute the article fields.
            skipElement(p);
        }
    }

    /** Consume everything up to and including the end tag of the current element. */
    private static void skipElement(XmlPullParser p) throws Exception {
        int depth = 1;
        while (depth > 0) {
            int ev = p.next();
            if (ev == XmlPullParser.START_TAG) depth++;
            else if (ev == XmlPullParser.END_TAG) depth--;
        }
    }

    private static void finish(Article a) {
        a.title = decode(stripTags(a.title)).trim();

        String img = firstImage(a.contentHtml);
        if (img == null) img = a.mediaUrl;
        if (img == null) img = firstImage(a.descHtml);
        a.imageUrl = img == null ? "" : img.trim();

        String text = decode(stripTags(a.descHtml == null ? "" : a.descHtml)).trim();
        int cut = text.toLowerCase(Locale.US).indexOf("read more");
        if (cut > 0) text = text.substring(0, cut);
        text = text.replaceAll("[\\s\u2026]+$", "").trim();
        if (text.length() > 240) {
            text = text.substring(0, 240);
            int sp = text.lastIndexOf(' ');
            if (sp > 120) text = text.substring(0, sp);
            text = text + "\u2026";
        }
        a.excerpt = text;

        a.dateText = formatDate(a.pubDate);
    }

    private static String firstImage(String html) {
        if (html == null) return null;
        Matcher m = IMG.matcher(html);
        return m.find() ? m.group(1) : null;
    }

    private static String stripTags(String html) {
        if (html == null) return "";
        return TAG.matcher(html).replaceAll(" ").replaceAll("\\s+", " ");
    }

    static String decode(String s) {
        if (s == null || s.indexOf('&') < 0) return s == null ? "" : s;
        Matcher m = NUM_ENTITY.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String rep;
            try {
                int cp;
                if (m.group(1) != null) cp = Integer.parseInt(m.group(1));
                else if (m.group(2) != null) cp = Integer.parseInt(m.group(2), 16);
                else {
                    String named = ENTITIES.get(m.group(3).toLowerCase(Locale.US));
                    rep = named != null ? named : m.group();
                    m.appendReplacement(sb, Matcher.quoteReplacement(rep));
                    continue;
                }
                rep = new String(Character.toChars(cp));
            } catch (Exception e) {
                rep = " ";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String formatDate(String pub) {
        if (pub == null || pub.isEmpty()) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
            Date d = in.parse(pub.trim());
            return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(d);
        } catch (Exception e) {
            return pub;
        }
    }
}
