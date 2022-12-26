package com.atiksoftware.tuhafeyler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.mancj.materialsearchbar.MaterialSearchBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity  implements MaterialSearchBar.OnSearchActionListener{

    File storageContainer;
    MaterialSearchBar search_bar;
    LinearLayout video_container;
    StyledPlayerView video_player;
    ExoPlayer player;
    TextView title;
    View share_button;
    ListViewAdapter listviewAdapter;
    ExpandableListView listview;
    View status_container;
    TextView status_text;
    ProgressBar status_progress_bar;


    ArrayList<Category> categories;
    int last_item_id = 0;
    Item current_item = null;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        search_bar = findViewById(R.id.search_bar);
        video_container = findViewById(R.id.video_container);
        video_player = findViewById(R.id.video_player);
        title = findViewById(R.id.title);
        share_button = findViewById(R.id.share_button);
        listview = findViewById(R.id.listview);
        status_container = findViewById(R.id.status_container);
        status_text = findViewById(R.id.status_text);
        status_progress_bar = findViewById(R.id.status_progress_bar);

        search_bar.setOnSearchActionListener(this);

        player = new ExoPlayer.Builder(this).build();
        video_player = findViewById(R.id.video_player);

        share_button.setOnClickListener(v -> share());

        resetUI();


        checkStoragePermission();

    }

    void checkStoragePermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e("Storage", "Permission not granted");
                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                }, 11);
            }
        }
        onPermissionGranted();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 11) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted();
            }
        }
    }
    void onPermissionGranted(){
        checkStorageContainer();
        loadMedia();
        new Thread(this::checkUpdates).start();
    }
    void checkStorageContainer(){
        if(storageContainer == null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                storageContainer = getExternalMediaDirs()[0];
            }else{
                storageContainer = getExternalFilesDir(null);
            }

            File nomedia = new File(storageContainer, ".nomedia");
            if (!nomedia.exists())
                fileWriteBytes(nomedia, "".getBytes());
        }
    }
    public byte[] fileReadBytes(File file) {
        byte[] tempBuf = new byte[100];
        int byteRead;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            while ((byteRead = bufferedInputStream.read(tempBuf)) != -1) {
                byteArrayOutputStream.write(tempBuf, 0, byteRead);
            }
            bufferedInputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public void fileWriteBytes(File file, byte[] data) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("XXXX", "writeBytes: " + e.getMessage());
        }
    }
    public boolean fileDelete(File file) {
        try {
            return file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    void loadMedia(){
        File feedFile = new File(storageContainer, "database.json");
        if(!feedFile.exists()){
            return;
        }
        categories = new ArrayList<>();
        last_item_id = 0;
        String data = new String(fileReadBytes(feedFile));
        try{
            JSONObject database = new JSONObject(data);
            JSONArray categoryArray = database.getJSONArray("categories");
            JSONArray itemArray = database.getJSONArray("items");
            for(int i = 0; i < categoryArray.length(); i++){
                JSONObject categoryObject = categoryArray.getJSONObject(i);
                int id = categoryObject.getInt("id");
                String title = categoryObject.getString("title");
                Category category = new Category(id,title);
                categories.add(category);
            }
            for(int i = 0; i < itemArray.length(); i++){
                JSONObject itemObject = itemArray.getJSONObject(i);
                int id = itemObject.getInt("id");
                int category_id = itemObject.getInt("category_id");
                String title = itemObject.getString("title");
                String searchtext = itemObject.getString("searchtext");
                String media_type = itemObject.getString("media_type");
                String filename = itemObject.getString("filename");
                String thumbnail = itemObject.getString("thumbnail");
                Item item = new Item(id, category_id, title, searchtext, media_type, new File(storageContainer,filename), new File(storageContainer,thumbnail));
                for(Category category : categories){
                    if(category.id == category_id){
                        category.items.add(item);
                        break;
                    }
                }
                if(id > last_item_id){
                    last_item_id = id - 1;
                }
            }

            runOnUiThread(() -> {
                listviewAdapter = new ListViewAdapter(this,categories);
                listview.setAdapter(listviewAdapter);
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void play(Item item) {
        this.current_item = item;
        video_container.setVisibility(View.VISIBLE);
        if (item.type == Item.Type.VIDEO) {
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
            video_player.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px));
        } else {
            video_player.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        }

        MediaItem mediaItem = MediaItem.fromUri(item.file.getAbsolutePath());
        player.setMediaItem(mediaItem);
        player.setPlayWhenReady(true);
        player.prepare();
        player.play();

        title.setText(item.title);

        for( Category m_category : categories)
            for( Item m_item : m_category.items)
                m_item.selected = false;
        item.selected = true;

        try {
            runOnUiThread(()->listviewAdapter.notifyDataSetChanged());
        }
        catch (Exception ignored){}
    }

    public void resetUI(){
        if(video_player.getPlayer() == null){
            video_player.setPlayer(player);
            video_player.setShowFastForwardButton(false);
            video_player.setShowPreviousButton(false);
            video_player.setShowNextButton(false);
            video_player.setShowPreviousButton(false);
            video_player.setShowRewindButton(false);
            video_player.setShowSubtitleButton(false);
            video_player.setControllerAutoShow(false);
        }
        video_container.setVisibility(View.GONE);
        title.setText("");
    }


    @Override
    public void onSearchStateChanged(boolean enabled) {
        if (!enabled) {
            doFilter(null);
        }
    }

    @Override
    public void onSearchConfirmed(CharSequence text) {
        doFilter(slug(text.toString()));
    }

    @Override
    public void onButtonClicked(int buttonCode) {
        // destrop this activity and go back to the previous activity
        finish();
    }

    void doFilter(String query) {
        boolean is_searched = query != null && !query.equals("");
        for (Category category : categories) {
            for (Item item : category.items) {
                item.showable = !is_searched || item.searchtext.contains(query);
            }
        }
        try {
            runOnUiThread(()->{
                if(listviewAdapter.getGroupCount() > 0){
                    listview.expandGroup(0);
                }
                listviewAdapter.notifyDataSetChanged();
            });
        }
        catch (Exception ignored){}
    }

    void checkUpdates(){
        runOnUiThread(() ->{
            status_container.setVisibility(View.VISIBLE);
            status_text.setText(getTranslate("status_checking_updates"));
            status_progress_bar.setIndeterminate(true);
        });
        HttpAgent httpAgent = new HttpAgent();
        String url = httpAgent.url("/feed.php?method=check&last_item_id=" + last_item_id);
        try {
            String response = httpAgent.get(url);
            JSONObject jsonObject = new JSONObject(response);
            boolean has_update = jsonObject.getBoolean("has_update");
            if(has_update){
                int new_item_count = jsonObject.getInt("new_item_count");
                runOnUiThread(() -> status_text.setText(
                        getTranslate("status_downloading_updates", new String[]{String.valueOf(new_item_count)})
                ));
                new Thread(this::downloadUpdates).start();
            }else{
                runOnUiThread(() ->{
                    status_text.setText(getTranslate("status_no_updates"));
                    status_progress_bar.setVisibility(View.GONE);
                });
                Thread.sleep(1000);
                runOnUiThread(() -> status_container.setVisibility(View.GONE));
            }
        } catch (RuntimeException | InterruptedException | JSONException e) {
            Log.e("XXXX", "checkUpdates: " + e.getMessage());
            status_container.setVisibility(View.GONE);
            e.printStackTrace();
        }
    }
    @SuppressLint("DefaultLocale")
    void downloadUpdates(){
        HttpAgent httpAgent = new HttpAgent();
        String url = httpAgent.url("/feed.php?method=download&last_item_id=" + last_item_id);
        File zipFile = new File(storageContainer, "feed.zip");
        if(zipFile.exists()){
            fileDelete(zipFile);
        }
        runOnUiThread(() ->{
            status_container.setVisibility(View.VISIBLE);
            status_progress_bar.setIndeterminate(false);
            status_progress_bar.setVisibility(View.VISIBLE);
            status_progress_bar.setProgress(0);
        });
        httpAgent.download(
                url,
                zipFile,
                () -> runOnUiThread(() ->{
                    status_text.setText(getTranslate("status_downloading_progress", new String[]{
                            String.format("%.2f",httpAgent.download_percent),
                            httpAgent.toMb(httpAgent.download_downloaded),
                            httpAgent.toMb(httpAgent.download_total_size),
                    }));
                    status_progress_bar.setProgress((int) httpAgent.download_percent);
                }),
                this::onDownloadCompleted,
                this::onUpdateFailed
        );

    }
    void onDownloadCompleted(){
        runOnUiThread(() ->{
            status_text.setText(getTranslate("status_extracting"));
            status_progress_bar.setIndeterminate(true);
        });
        File zipFile = new File(storageContainer, "feed.zip");
        try {
            // extract zip file to storage container
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry zipEntry;
            byte[] buffer = new byte[1024];
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                File newFile = new File(storageContainer, zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    FileOutputStream fileOutputStream = new FileOutputStream(newFile);
                    int len;
                    while ((len = zipInputStream.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, len);
                    }
                    fileOutputStream.close();
                }
                zipInputStream.closeEntry();
            }
            zipInputStream.close();
            //fileDelete(zipFile);
            runOnUiThread(() ->{
                status_text.setText(getTranslate("status_extracting_completed"));
                status_progress_bar.setIndeterminate(false);
                status_progress_bar.setProgress(100);
            });
            Thread.sleep(1000);
            runOnUiThread(() -> status_container.setVisibility(View.GONE));
            loadMedia();
        } catch (Exception e) {
            e.printStackTrace();
            onUpdateFailed();
        }
    }
    void onUpdateFailed(){
        runOnUiThread(() -> status_container.setVisibility(View.GONE));
    }

    public void share() {
        Uri fileUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fileUri = FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()), BuildConfig.APPLICATION_ID + ".provider", current_item.file);
        } else {
            fileUri = Uri.fromFile(current_item.file);
        }
        Intent share = new Intent();
        share.setAction(Intent.ACTION_SEND);
        if (current_item.type.equals(Item.Type.VIDEO)) {
            share.setType("video/*");
        }
        if (current_item.type.equals(Item.Type.AUDIO)) {
            share.setType("audio/*");
        }
        share.putExtra(Intent.EXTRA_STREAM, fileUri);
        startActivity(Intent.createChooser(share, getResources().getString(R.string.share_this_file)));

    }

    @SuppressLint("DiscouragedApi")
    String getTranslate(String key){
        return getResources().getString(getResources().getIdentifier(key, "string", getPackageName()));
    }
    String getTranslate(String key, String[] args){
        String text = getTranslate(key);
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", args[i]);
        }
        return text;
    }
    public String slug(String s) {
        s = s.toLowerCase();
        // replace turkish characters with english characters
        s = s.replace("ç", "c");
        s = s.replace("ğ", "g");
        s = s.replace("ı", "i");
        s = s.replace("ö", "o");
        s = s.replace("ş", "s");
        s = s.replace("ü", "u");
        s = s.replace("â", "a");
        // remove all non-alphanumeric characters
        s = s.replaceAll("[^a-zA-Z0-9]", "");
        return s;
    }
}