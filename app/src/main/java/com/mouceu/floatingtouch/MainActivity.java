package com.mouceu.floatingtouch;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.mouceu.floatingtouch.SlideAction.*;
import static com.mouceu.floatingtouch.AppSetting.*;

public class MainActivity extends AppCompatActivity {
    private static final int DRAW_OVER_OTHER_APP_PERMISSION_CODE = 2084;
    private static final int ENABLE_ACCESSIBILITY_CODE = 2085;
    static final String SERVICE_PARAMS = "com.mouceu.floatingtouch.SERVICE_PARAMS";
    static final int DEFAULT_ANGLE = 90;
    static final int DEFAULT_OPACITY = 0;
    static final int DEFAULT_TOUCH_AREA_SIZE = 25;
    static final int DEFAULT_FLOATING_TOUCH_SIZE = 24;

    private LocalBroadcastManager broadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        broadcastManager = LocalBroadcastManager.getInstance(this);
        setContentView(R.layout.activity_main);

        if (!Util.isAccessibilityServiceEnabled(this, FloatingViewService.class)) {
            // TODO: message/instruction
            Intent accessibilityIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(accessibilityIntent, ENABLE_ACCESSIBILITY_CODE);
        }

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            // TODO: message/instruction
            startActivityForResult(intent, DRAW_OVER_OTHER_APP_PERMISSION_CODE);
        }

        initSettingList();
        initOpacityPicker();
        initFloatingTouchSizePicker();
        initTouchAreaPicker();
        initManageAccessibilityButton();
        initManageOverlayButton();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DRAW_OVER_OTHER_APP_PERMISSION_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(
                        this,
                        R.string.permissions_required,
                        Toast.LENGTH_SHORT
                ).show();
            }
        } else if (requestCode == ENABLE_ACCESSIBILITY_CODE) {
            if (!Util.isAccessibilityServiceEnabled(this, FloatingViewService.class)) {
                Toast.makeText(
                        this,
                        R.string.enable_service_message,
                        Toast.LENGTH_SHORT
                ).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void initOpacityPicker() {
        final SeekBar opacityPicker = findViewById(R.id.opacity_picker);
        final TextView opacityPickerValue = findViewById(R.id.opacity_picker_title);
        final String opacityPickerTitle = getString(R.string.opacity_picker_title) + ": %d";
        int opacity = Util.getSetting(OPACITY.name(), DEFAULT_OPACITY, MainActivity.this);
        opacityPickerValue.setText(String.format(opacityPickerTitle, opacity));
        opacityPicker.setProgress(opacity);

        opacityPicker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int opacity = seekBar.getProgress();
                opacityPickerValue.setText(String.format(opacityPickerTitle, opacity));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int opacity = seekBar.getProgress();
                Util.saveSetting(OPACITY.name(), opacity, MainActivity.this);
                notifySettingChanged(OPACITY.name(), opacity);
            }
        });
    }

    private void initTouchAreaPicker() {
        SeekBar areaPicker = findViewById(R.id.touch_area_size_picker);
        final TextView touchAreaSizePickerValue = findViewById(R.id.touch_area_size_picker_title);
        final String title = getString(R.string.touch_area_size) + ": %d";
        int touchArea = Util.getSetting(TOUCH_AREA.name(), DEFAULT_TOUCH_AREA_SIZE, MainActivity.this);
        touchAreaSizePickerValue.setText(String.format(title, touchArea));
        areaPicker.setProgress(touchArea);

        areaPicker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                touchAreaSizePickerValue.setText(String.format(title, seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int touchArea = seekBar.getProgress();
                Util.saveSetting(TOUCH_AREA.name(), touchArea, MainActivity.this);
                notifySettingChanged(TOUCH_AREA.name(), touchArea);
            }
        });
    }

    private void initFloatingTouchSizePicker() {
        SeekBar sizePicker = findViewById(R.id.floating_touch_size_picker);
        final TextView floatingTouchSizePicker = findViewById(R.id.floating_touch_size_picker_title);
        final String title = getString(R.string.floating_touch_size) + ": %d";
        int floatingTouchSize = Util.getSetting(
                FLOATING_TOUCH_SIZE.name(), DEFAULT_FLOATING_TOUCH_SIZE, MainActivity.this);
        floatingTouchSizePicker.setText(String.format(title, floatingTouchSize));
        sizePicker.setProgress(floatingTouchSize);

        sizePicker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                floatingTouchSizePicker.setText(String.format(title, seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int floatingTouchSize = seekBar.getProgress();
                Util.saveSetting(FLOATING_TOUCH_SIZE.name(), floatingTouchSize, MainActivity.this);
                notifySettingChanged(FLOATING_TOUCH_SIZE.name(), floatingTouchSize);
            }
        });
    }

    private void initSettingList() {
        final Map<AppSetting, String> settingTitles = getSettingTitles();
        final Map<SlideAction, String> actionTitles = getActionTitles();
        Drawable arrowIcon = getDrawable(R.drawable.ic_arrow_downward_24dp);
        Drawable sensitivityIcon = getDrawable(R.drawable.ic_sensitivity_angle_24dp);
        Drawable touchIcon = getDrawable(R.drawable.ic_touch);
        Objects.requireNonNull(arrowIcon);
        final List<SettingItem> settingItems = Arrays.asList(
                new SettingItem(ACTION_LEFT,
                        settingTitles.get(ACTION_LEFT),
                        actionTitles.get(Util.getStoredAction(ACTION_LEFT, OPEN_RECENT_APPS, this)),
                        getRotateDrawable(arrowIcon, 90)),
                new SettingItem(ACTION_UP,
                        settingTitles.get(ACTION_UP),
                        actionTitles.get(Util.getStoredAction(ACTION_UP, OPEN_HOME_SCREEN, this)),
                        getRotateDrawable(arrowIcon, 180)),
                new SettingItem(ACTION_RIGHT,
                        settingTitles.get(ACTION_RIGHT),
                        actionTitles.get(Util.getStoredAction(ACTION_RIGHT, OPEN_PREVIOUS_APP, this)),
                        getRotateDrawable(arrowIcon, 270)),
                new SettingItem(ACTION_DOWN,
                        settingTitles.get(ACTION_DOWN),
                        actionTitles.get(Util.getStoredAction(ACTION_DOWN, OPEN_NOTIFICATIONS, this)),
                        arrowIcon),
                new SettingItem(ACTION_TOUCH,
                        settingTitles.get(ACTION_TOUCH),
                        actionTitles.get(Util.getStoredAction(ACTION_TOUCH, GO_BACK, this)),
                        touchIcon),
                new SettingItem(SENSITIVITY_ANGLE,
                        settingTitles.get(SENSITIVITY_ANGLE),
                        String.valueOf(Util.getSetting(SENSITIVITY_ANGLE.name(), DEFAULT_ANGLE, this)),
                        sensitivityIcon)
        );

        final SettingListAdapter adapter = new SettingListAdapter(this, settingItems);
        final ListView settingList = findViewById(R.id.setting_list);
        settingList.setAdapter(adapter);

        final int[] angleValues = getResources().getIntArray(R.array.sensitivity_angle_values);
        final String[] actions = actionTitles.values().toArray(new String[0]);
        settingList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
                final SettingItem clickedItem = adapter.getItem(position);
                if (clickedItem == null) return;
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                        .setCancelable(true);
                switch (clickedItem.getName()) {
                    case ACTION_UP:
                    case ACTION_DOWN:
                    case ACTION_LEFT:
                    case ACTION_RIGHT:
                    case ACTION_TOUCH:
                        builder
                                .setTitle(settingTitles.get(clickedItem.getName()))
                                .setItems(actions, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final SlideAction action = SlideAction.values()[which];
                                        Util.saveSetting(clickedItem.getName().name(),
                                                action.name(), MainActivity.this);
                                        clickedItem.setValue(actions[which]);
                                        adapter.notifyDataSetChanged();
                                        notifySettingChanged(clickedItem.getName().name(), action.name());
                                    }
                                });
                        break;

                    case SENSITIVITY_ANGLE:
                        builder
                                .setTitle(R.string.angle_spinner_title)
                                .setItems(R.array.sensitivity_angle_values, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final int angleValue = angleValues[which];
                                        Util.saveSetting(clickedItem.getName().name(),
                                                angleValue, MainActivity.this);
                                        clickedItem.setValue(String.valueOf(angleValue));
                                        adapter.notifyDataSetChanged();
                                        notifySettingChanged(clickedItem.getName().name(), angleValue);
                                    }
                                });
                        break;
                }
                builder.show();
            }
        });
    }

    @NonNull
    private Map<AppSetting, String> getSettingTitles() {
        final Map<AppSetting, String> settingTitles = new EnumMap<>(AppSetting.class);
        settingTitles.put(ACTION_LEFT, getString(R.string.setting_slide_left));
        settingTitles.put(ACTION_UP, getString(R.string.setting_slide_up));
        settingTitles.put(ACTION_RIGHT, getString(R.string.setting_slide_right));
        settingTitles.put(ACTION_DOWN, getString(R.string.setting_slide_down));
        settingTitles.put(ACTION_TOUCH, getString(R.string.setting_touch));
        settingTitles.put(SENSITIVITY_ANGLE, getString(R.string.angle_spinner_title));
        return settingTitles;
    }

    @NonNull
    private Map<SlideAction, String> getActionTitles() {
        final Map<SlideAction, String> actionTitles = new EnumMap<>(SlideAction.class);
        actionTitles.put(OPEN_RECENT_APPS, getString(R.string.action_open_recent_apps));
        actionTitles.put(OPEN_NOTIFICATIONS, getString(R.string.action_open_notifications));
        actionTitles.put(OPEN_HOME_SCREEN, getString(R.string.action_open_home_screen));
        actionTitles.put(OPEN_PREVIOUS_APP, getString(R.string.action_open_previous_app));
        actionTitles.put(GO_BACK, getString(R.string.action_go_back));
        actionTitles.put(NONE, getString(R.string.action_empty));
        actionTitles.put(OPEN_POWER_DIALOG, getString(R.string.action_open_power_dialog));
        actionTitles.put(OPEN_QUICK_SETTINGS, getString(R.string.action_open_quick_settings));
        actionTitles.put(TOGGLE_SPLIT_SCREEN, getString(R.string.action_toggle_split_screen));
        return actionTitles;
    }

    private Drawable getRotateDrawable(@NonNull final Drawable d, final float angle) {
        final Drawable[] layers = {d};
        return new LayerDrawable(layers) {
            @Override
            public void draw(final Canvas canvas) {
                canvas.save();
                canvas.rotate(angle, d.getBounds().width() / 2, d.getBounds().height() / 2);
                super.draw(canvas);
                canvas.restore();
            }
        };
    }

    private <T> void notifySettingChanged(String key, @NonNull T value) {
        Bundle bundle = new Bundle();
        @SuppressWarnings("unchecked")
        final Class<T> valueType = (Class<T>) value.getClass();
        if (valueType == int.class || valueType == Integer.class) {
            bundle.putInt(key, (Integer) value);
        } else {
            bundle.putString(key, value.toString());
        }
        Intent intent = new Intent(SERVICE_PARAMS);
        intent.putExtras(bundle);
        broadcastManager.sendBroadcast(intent);
    }

    private void initManageAccessibilityButton() {
        findViewById(R.id.button_manage_accessibility).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent accessibilityIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(accessibilityIntent);
            }
        });
    }

    private void initManageOverlayButton() {
        findViewById(R.id.button_manage_overlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())
                );
                startActivityForResult(intent, DRAW_OVER_OTHER_APP_PERMISSION_CODE);
            }
        });
    }
}
