package com.hackscroll.app;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
/* compiled from: FeedStore.java */
/* loaded from: classes.dex */
final class Fetch {
    static final String UA = "HackScroll/1.0 Android RSS Reader";

    private Fetch() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String get(String str) {
        Throwable th;
        HttpURLConnection httpURLConnection;
        HttpURLConnection httpURLConnection2;
        try {
            httpURLConnection2 = (HttpURLConnection) new URL(str).openConnection();
        } catch (Exception e) {
            httpURLConnection2 = null;
        } catch (Throwable th2) {
            th = th2;
            httpURLConnection = null;
        }
        try {
            httpURLConnection2.setRequestProperty("User-Agent", UA);
            httpURLConnection2.setConnectTimeout(12000);
            httpURLConnection2.setReadTimeout(15000);
            httpURLConnection2.setInstanceFollowRedirects(true);
            int responseCode = httpURLConnection2.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                if (httpURLConnection2 != null) {
                    httpURLConnection2.disconnect();
                }
                return null;
            }
            String readAll = readAll(new BufferedInputStream(httpURLConnection2.getInputStream()));
            if (httpURLConnection2 != null) {
                httpURLConnection2.disconnect();
            }
            return readAll;
        } catch (Exception e2) {
            if (httpURLConnection2 != null) {
                httpURLConnection2.disconnect();
            }
            return null;
        } catch (Throwable th3) {
            th = th3;
            httpURLConnection = httpURLConnection2;
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String readAll(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[8192];
        while (true) {
            int read = inputStream.read(bArr);
            if (read > 0) {
                byteArrayOutputStream.write(bArr, 0, read);
            } else {
                return new String(byteArrayOutputStream.toByteArray(), "UTF-8");
            }
        }
    }
}
