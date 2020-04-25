package com.mouceu.floatingtouch;

import android.graphics.drawable.Drawable;

public class SettingItem {
    private final KnownSetting name;
    private final Drawable image;
    private KnownAction value;

    public SettingItem(KnownSetting name, Drawable image) {
        this.name = name;
        this.image = image;
    }

    public SettingItem(KnownSetting name, KnownAction value, Drawable image) {
        this.name = name;
        this.value = value;
        this.image = image;
    }

    public KnownSetting getName() {
        return name;
    }

    public KnownAction getValue() {
        return value;
    }

    public void setValue(KnownAction value) {
        this.value = value;
    }

    public Drawable getImage() {
        return image;
    }
}
