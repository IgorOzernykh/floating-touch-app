package com.mouceu.floatingtouch;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.support.annotation.NonNull;
import android.view.accessibility.AccessibilityManager;

import java.util.List;


class Util {
    private static final String SHARED_PREF_NAME = "FLOATING_TOUCH_PREFS";

    static boolean isAccessibilityServiceEnabled(Context context,
                                                 Class<? extends AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager)
                context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(context.getPackageName())
                    && enabledServiceInfo.name.equals(service.getName()))
                return true;
        }

        return false;
    }

    static  <T> void saveSetting(String key, T value, Context context) {
        final SharedPreferences preferences = getSharedPreferences(context);
        final SharedPreferences.Editor editor = preferences.edit();
        if (value == null)
            editor.remove(key);
        else if (value.getClass() == int.class || value.getClass() == Integer.class)
            editor.putInt(key, (Integer) value);
        else
            editor.putString(key, String.valueOf(value));
        editor.apply();
    }

    static <T> T getSetting(String key, T defaultValue, Context context) {
        final SharedPreferences preferences = getSharedPreferences(context);
        @SuppressWarnings("unchecked")
        final Class<T> cl = (Class<T>) defaultValue.getClass();
        T value;
        if (cl == int.class || cl == Integer.class)
            value = cl.cast(preferences.getInt(key, (Integer) defaultValue));
        else
            value = cl.cast(preferences.getString(key, (String) defaultValue));
        return value;
    }

    @NonNull
    static SlideAction getStoredAction(AppSetting setting, SlideAction action, Context context) {
        return SlideAction.valueOf(Util.getSetting(setting.name(), action.name(), context));
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }
}
