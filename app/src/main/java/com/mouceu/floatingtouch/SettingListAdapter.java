package com.mouceu.floatingtouch;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

class SettingListAdapter extends ArrayAdapter<SettingItem> {
    private Activity context;

    public SettingListAdapter(Activity context, List<SettingItem> items) {
        super(context, R.layout.setting_list, items);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            rowView = LayoutInflater.from(context).inflate(R.layout.setting_list, null);
        }

        SettingItem item = getItem(position);
        if (item != null) {

            ImageView settingIconView = rowView.findViewById(R.id.setting_icon);
            TextView settingNameView = rowView.findViewById(R.id.setting_name);
            TextView settingValueView = rowView.findViewById(R.id.setting_value);

            settingIconView.setImageDrawable(item.getImage());
            settingNameView.setText(item.getTitle());
            settingValueView.setText(item.getValue());
        }
        return rowView;
    }
}
