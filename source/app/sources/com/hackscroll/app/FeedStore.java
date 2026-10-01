package com.hackscroll.app;

import android.content.Context;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
/* loaded from: classes.dex */
public final class FeedStore {
    private static final String FILE = "feed.xml";

    private FeedStore() {
    }

    public static String load(Context context) {
        String str = null;
        File fileStreamPath = context.getFileStreamPath(FILE);
        if (fileStreamPath.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(fileStreamPath);
                String readAll = Fetch.readAll(new BufferedInputStream(fileInputStream));
                fileInputStream.close();
                str = readAll;
                return str;
            } catch (Exception e) {
                return str;
            }
        }
        return null;
    }

    public static void save(Context context, String str) {
        if (str != null) {
            File file = new File(context.getFilesDir(), "feed.xml.tmp");
            File fileStreamPath = context.getFileStreamPath(FILE);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(str.getBytes("UTF-8"));
                fileOutputStream.close();
                if (fileStreamPath.exists()) {
                    fileStreamPath.delete();
                }
                if (!file.renameTo(fileStreamPath)) {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    FileOutputStream fileOutputStream2 = new FileOutputStream(fileStreamPath);
                    byte[] bArr = new byte[8192];
                    while (true) {
                        int read = fileInputStream.read(bArr);
                        if (read > 0) {
                            fileOutputStream2.write(bArr, 0, read);
                        } else {
                            fileInputStream.close();
                            fileOutputStream2.close();
                            file.delete();
                            return;
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }
}
