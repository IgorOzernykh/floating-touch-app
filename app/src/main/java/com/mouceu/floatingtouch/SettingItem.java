package com.mouceu.floatingtouch;

import android.graphics.drawable.Drawable;

class SettingItem {
    private final AppSetting name;
    private final String title;
    private final Drawable image;
    private String value;

    public SettingItem(AppSetting name, String title, Drawable image) {
        this.name = name;
        this.title = title;
        this.image = image;
    }

    public SettingItem(AppSetting name, String title, String value, Drawable image) {
        this.name = name;
        this.title = title;
        this.value = value;
        this.image = image;
    }

    public AppSetting getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Drawable getImage() {
        return image;
    }
}
