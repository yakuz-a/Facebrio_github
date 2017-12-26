package com.hackz.facebrio.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.hackz.facebrio.R;

import java.util.ArrayList;

public class OfflinePagesAdapter extends ArrayAdapter<String> {

    public OfflinePagesAdapter(Context context, ArrayList<String> items) {
        super(context, 0, items);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        String item = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_offline_page, parent, false);

        TextView page = (TextView) convertView.findViewById(R.id.page);

        @SuppressLint("DefaultLocale") String formattedId = "" + String.format("%02d", position + 1);
        String stringToSet = formattedId + ":  " + item;

        page.setText(stringToSet);

        return convertView;
    }

}
