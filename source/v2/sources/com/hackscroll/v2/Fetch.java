package com.hackscroll.v2;

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

    /* compiled from: FeedStore.java */
    /* loaded from: classes.dex */
    static final class Result {
        final String body;
        final String total;

        Result(String str, String str2) {
            this.body = str;
            this.total = str2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Result getEx(String str) {
        Throwable th;
        HttpURLConnection httpURLConnection;
        HttpURLConnection httpURLConnection2 = null;
        try {
            httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
            try {
                httpURLConnection.setRequestProperty("User-Agent", UA);
                httpURLConnection.setConnectTimeout(12000);
                httpURLConnection.setReadTimeout(15000);
                httpURLConnection.setInstanceFollowRedirects(true);
                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    return null;
                }
                Result result = new Result(readAll(new BufferedInputStream(httpURLConnection.getInputStream())), httpURLConnection.getHeaderField("X-WP-Total"));
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                return result;
            } catch (Exception e) {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                return null;
            } catch (Throwable th2) {
                th = th2;
                httpURLConnection2 = httpURLConnection;
                if (httpURLConnection2 != null) {
                    httpURLConnection2.disconnect();
                }
                throw th;
            }
        } catch (Exception e2) {
            httpURLConnection = null;
        } catch (Throwable th3) {
            th = th3;
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
