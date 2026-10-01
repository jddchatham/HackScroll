package com.hackscroll.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/* loaded from: classes.dex */
public class ImageLoader {
    private final Context appContext;
    private File diskDir;
    private int memBytes;
    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final int memMaxBytes = (int) Math.max(4194304L, Runtime.getRuntime().maxMemory() / 8);
    private final LinkedHashMap<String, Bitmap> mem = new LinkedHashMap<>(16, 0.75f, true);

    public ImageLoader(Context context) {
        this.appContext = context.getApplicationContext();
        this.diskDir = new File(this.appContext.getCacheDir(), "img");
        if (!this.diskDir.exists()) {
            this.diskDir.mkdirs();
        }
    }

    private synchronized Bitmap memGet(String str) {
        return this.mem.get(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void memPut(String str, Bitmap bitmap) {
        Bitmap put = this.mem.put(str, bitmap);
        if (put != null && !put.isRecycled()) {
            this.memBytes -= put.getByteCount();
        }
        this.memBytes += bitmap.getByteCount();
        Iterator<Map.Entry<String, Bitmap>> it = this.mem.entrySet().iterator();
        while (this.memBytes > this.memMaxBytes && it.hasNext()) {
            it.remove();
            this.memBytes -= it.next().getValue().getByteCount();
        }
    }

    public void load(final ImageView imageView, final String str) {
        if (str == null || str.isEmpty()) {
            imageView.setImageDrawable(null);
            imageView.setBackgroundColor(-15460324);
            return;
        }
        Bitmap memGet = memGet(str);
        if (memGet != null) {
            imageView.setImageBitmap(memGet);
            imageView.setAlpha(1.0f);
            return;
        }
        imageView.setImageDrawable(null);
        imageView.setBackgroundColor(-15460324);
        imageView.setTag(str);
        this.io.execute(new Runnable() { // from class: com.hackscroll.app.ImageLoader.1
            @Override // java.lang.Runnable
            public void run() {
                final Bitmap fromDisk = ImageLoader.this.fromDisk(str);
                if (fromDisk == null) {
                    fromDisk = ImageLoader.this.fromNet(str);
                }
                if (fromDisk != null) {
                    ImageLoader.this.memPut(str, fromDisk);
                }
                Handler handler = ImageLoader.this.main;
                final ImageView imageView2 = imageView;
                final String str2 = str;
                handler.post(new Runnable() { // from class: com.hackscroll.app.ImageLoader.1.1
                    @Override // java.lang.Runnable
                    public void run() {
                        Object tag = imageView2.getTag();
                        if ((tag instanceof String) && tag.equals(str2)) {
                            if (fromDisk != null) {
                                imageView2.setImageBitmap(fromDisk);
                                imageView2.setAlpha(0.0f);
                                imageView2.animate().alpha(1.0f).setDuration(220L).start();
                                return;
                            }
                            imageView2.setImageDrawable(null);
                            imageView2.setBackgroundColor(-15460324);
                        }
                    }
                });
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Bitmap fromDisk(String str) {
        File file = new File(this.diskDir, key(str));
        if (file.exists()) {
            Bitmap decode = decode(file.getAbsolutePath());
            if (decode == null) {
                file.delete();
                return decode;
            }
            return decode;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Bitmap fromNet(String str) {
        Throwable th;
        HttpURLConnection httpURLConnection;
        HttpURLConnection httpURLConnection2 = null;
        try {
            httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
            try {
                httpURLConnection.setRequestProperty("User-Agent", "HackScroll/1.0 Android RSS Reader");
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
                File createTempFile = File.createTempFile("img", ".tmp", this.diskDir);
                InputStream inputStream = httpURLConnection.getInputStream();
                FileOutputStream fileOutputStream = new FileOutputStream(createTempFile);
                byte[] bArr = new byte[8192];
                while (true) {
                    int read = inputStream.read(bArr);
                    if (read <= 0) {
                        break;
                    }
                    fileOutputStream.write(bArr, 0, read);
                }
                fileOutputStream.close();
                inputStream.close();
                Bitmap decode = decode(createTempFile.getAbsolutePath());
                if (decode != null) {
                    File file = new File(this.diskDir, key(str));
                    if (file.exists()) {
                        file.delete();
                    }
                    createTempFile.renameTo(file);
                } else {
                    createTempFile.delete();
                }
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                return decode;
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

    private static Bitmap decode(String str) {
        int i = 1;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            FileInputStream fileInputStream = new FileInputStream(str);
            BitmapFactory.decodeStream(fileInputStream, null, options);
            fileInputStream.close();
            while (Math.max(options.outWidth, options.outHeight) / (i * 2) >= 1400) {
                i *= 2;
            }
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inSampleSize = i;
            FileInputStream fileInputStream2 = new FileInputStream(str);
            Bitmap decodeStream = BitmapFactory.decodeStream(fileInputStream2, null, options2);
            fileInputStream2.close();
            return decodeStream;
        } catch (Exception e) {
            return null;
        }
    }

    private static String key(String str) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(str.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            int length = digest.length;
            for (int i = 0; i < length; i++) {
                sb.append(String.format("%02x", Byte.valueOf(digest[i])));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(str.hashCode());
        }
    }
}
