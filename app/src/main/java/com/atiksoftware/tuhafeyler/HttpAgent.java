package com.atiksoftware.tuhafeyler;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpAgent {

    private static final String BASE_URL = "https://tuhafseyler.atiksoftware.com/";


    public String url(String path){
        if (path.startsWith("/")) path = path.substring(1);
        return BASE_URL +   path;
    }

    public String get(String path) {
        try {
            URL u = new URL(path);
            Log.d("XXX", "get: " + u );
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            //conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }
            InputStream is = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int length;
            StringBuilder sb = new StringBuilder();
            while ((length = is.read(buffer)) > 0) {
                sb.append(new String(buffer, 0, length));
            }
            is.close();
            conn.disconnect();
            return sb.toString();
        } catch (MalformedURLException mue) {
            Log.e("SYNC getUpdate", "malformed url error", mue);
        } catch (IOException ioe) {
            Log.e("SYNC getUpdate", "io error", ioe);
        } catch (SecurityException se) {
            Log.e("SYNC getUpdate", "security error", se);
        }
        return null;
    }

    public float download_percent = 0.0f;
    public int download_total_size = 0;
    public int download_downloaded = 0;
    // download zip file
    public void download(String url, File file, Runnable onProgress, Runnable onCompleted, Runnable onFailed){
        download_percent = 0.0f;
        new Thread(() -> {
            try {
                URL u = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                if (conn.getResponseCode() != 200) {
                    throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
                }
                InputStream is = conn.getInputStream();
                byte[] buffer = new byte[1024];
                int length;
                FileOutputStream fos = new FileOutputStream(file);
                download_total_size = conn.getContentLength();
                download_downloaded = 0;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                    download_downloaded += length;
                    download_percent = (float)download_downloaded / (float)download_total_size * 100.0f;
                    onProgress.run();
                }
                fos.close();
                is.close();
                conn.disconnect();
                onCompleted.run();
            } catch (MalformedURLException mue) {
                Log.e("SYNC getUpdate", "malformed url error", mue);
                onFailed.run();
            } catch (IOException ioe) {
                Log.e("SYNC getUpdate", "io error", ioe);
                onFailed.run();
            } catch (SecurityException se) {
                Log.e("SYNC getUpdate", "security error", se);
                onFailed.run();
            }
        }).start();

    }

    @SuppressLint("DefaultLocale")
    public String toMb(int bytes){
        return String.format("%.2f", (float)bytes / 1024.0f / 1024.0f) + "MB";
    }

}
