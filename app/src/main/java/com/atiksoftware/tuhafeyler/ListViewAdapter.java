package com.atiksoftware.tuhafeyler;


import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class ListViewAdapter extends BaseExpandableListAdapter {

    Context context;
    ArrayList<Category> categories;

    public ListViewAdapter(Context context, ArrayList<Category> categories) {
        this.context = context;
        this.categories = categories;
    }

    @Override
    public int getGroupCount() {
        int i = 0;
        for (Category category : categories) {
            if (category.getShowableItemCount() > 0) {
                i++;
            }
        }
        return i;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        Category category = getGroup(groupPosition);
        return category.getShowableItemCount();

    }

    @Override
    public Category getGroup(int groupPosition) {
        int i = 0;
        for (Category category : categories) {
            if (category.getShowableItemCount() > 0) {
                if (i == groupPosition) {
                    return category;
                }
                i++;
            }
        }
        if(categories.size() > groupPosition){
            return categories.get(groupPosition);
        }
        return null;
    }

    @Override
    public Item getChild(int groupPosition, int childPosition) {
        Category category = getGroup(groupPosition);
        if(category == null){
            return null;
        }
        int i = 0;
        for (Item item : category.items) {
            if (item.showable) {
                if (i == childPosition) {
                    return item;
                }
                i++;
            }
        }
        if(category.items.size() > childPosition){
            return category.items.get(childPosition);
        }
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        Category category = getGroup(groupPosition);
        // create main items (groups)
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.category_layout, null);
        }

        TextView title = convertView.findViewById(R.id.title);
        TextView count = convertView.findViewById(R.id.count);

        title.setText(category.title);
        count.setText(String.valueOf(category.getShowableItemCount()));

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.item_layout, null);
        }


        Item item = getChild(groupPosition, childPosition);
        LinearLayout wrapper = convertView.findViewById(R.id.wrapper);
        TextView title = convertView.findViewById(R.id.title);
        ImageView thumbnail = convertView.findViewById(R.id.thumbnail);

        convertView.setOnClickListener(v -> {
            for( Category m_category : categories)
                for( Item m_item : m_category.items)
                    m_item.selected = false;
            item.selected = true;
            ((MainActivity) context).play(item);
            //App.mainActivity.play(item);
        });

        thumbnail.setImageURI(Uri.parse(item.thumbnail.getAbsolutePath()));
        title.setText(item.title);


        if(item.selected){
            wrapper.setBackgroundColor(convertView.getResources().getColor(R.color.rose_500));
        }else{
            wrapper.setBackgroundColor(Color.TRANSPARENT);
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
