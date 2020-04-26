package com.mouceu.floatingtouch;

import android.graphics.drawable.Drawable;

public class SettingItem {
    private final KnownSetting name;
    private final Drawable image;
    private String value;

    public SettingItem(KnownSetting name, Drawable image) {
        this.name = name;
        this.image = image;
    }

    public SettingItem(KnownSetting name, String value, Drawable image) {
        this.name = name;
        this.value = value;
        this.image = image;
    }

    public KnownSetting getName() {
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
