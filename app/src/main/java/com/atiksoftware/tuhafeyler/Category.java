package com.atiksoftware.tuhafeyler;

import java.util.ArrayList;

public class Category {

    public int id;
    public String title;
    public ArrayList<Item> items = new ArrayList<>();

    public Category(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getShowableItemCount() {
        int i = 0;
        for (Item item : items) {
            if (item.showable) {
                i++;
            }
        }
        return i;
    }
}
