package com.hackscroll.app;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Persists the last successfully downloaded feed XML. */
public final class FeedStore {

    private static final String FILE = "feed.xml";

    private FeedStore() {}

    public static String load(Context c) {
        File f = c.getFileStreamPath(FILE);
        if (!f.exists()) return null;
        try {
            FileInputStream fis = new FileInputStream(f);
            try {
                return Fetch.readAll(new BufferedInputStream(fis));
            } finally {
                fis.close();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static void save(Context c, String xml) {
        if (xml == null) return;
        File tmp = new File(c.getFilesDir(), FILE + ".tmp");
        File out = c.getFileStreamPath(FILE);
        try {
            FileOutputStream fos = new FileOutputStream(tmp);
            try {
                fos.write(xml.getBytes("UTF-8"));
            } finally {
                fos.close();
            }
            if (out.exists()) out.delete();
            if (!tmp.renameTo(out)) {
                FileInputStream fis = new FileInputStream(tmp);
                FileOutputStream fos2 = new FileOutputStream(out);
                try {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = fis.read(buf)) > 0) fos2.write(buf, 0, n);
                } finally {
                    fis.close();
                    fos2.close();
                }
                tmp.delete();
            }
        } catch (Exception e) {
            // Cache write failure is non-fatal; the app keeps running.
        }
    }
}

/** Minimal HTTPS GET used by both the feed fetch and the image loader. */
final class Fetch {

    static final String UA = "HackScroll/1.0 Android RSS Reader";

    private Fetch() {}

    static String get(String urlString) {
        HttpURLConnection con = null;
        try {
            URL url = new URL(urlString);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", UA);
            con.setConnectTimeout(12000);
            con.setReadTimeout(15000);
            con.setInstanceFollowRedirects(true);
            int code = con.getResponseCode();
            if (code < 200 || code >= 300) return null;
            return readAll(new BufferedInputStream(con.getInputStream()));
        } catch (Exception e) {
            return null;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) > 0) buf.write(b, 0, n);
        return new String(buf.toByteArray(), "UTF-8");
    }
}
