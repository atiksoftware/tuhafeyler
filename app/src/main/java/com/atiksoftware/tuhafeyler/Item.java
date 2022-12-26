package com.atiksoftware.tuhafeyler;

import java.io.File;
import java.util.Objects;

public class Item {

    public enum Type {
        VIDEO,
        AUDIO,
    }
    public int id;
    public int category_id;
    public String title;
    public String searchtext;
    public String media_type;
    public Type type;
    public File file;
    public File thumbnail;

    public boolean showable = true;
    public boolean selected = false;


    public Item( int id, int category_id, String title,String searchtext, String media_type, File file, File thumbnail  ) {
        this.id = id;
        this.category_id = category_id;
        this.title = title;
        this.searchtext = searchtext;
        this.media_type = media_type;
        this.type = Objects.equals(media_type, "video") ? Type.VIDEO : Type.AUDIO;
        this.file = file;
        this.thumbnail = thumbnail;
    }
}
