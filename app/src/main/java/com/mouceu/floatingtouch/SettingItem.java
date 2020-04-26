package com.mouceu.floatingtouch;

import android.graphics.drawable.Drawable;

class SettingItem {
    private final AppSetting name;
    private final Drawable image;
    private String value;

    public SettingItem(AppSetting name, Drawable image) {
        this.name = name;
        this.image = image;
    }

    public SettingItem(AppSetting name, String value, Drawable image) {
        this.name = name;
        this.value = value;
        this.image = image;
    }

    public AppSetting getName() {
        return name;
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
