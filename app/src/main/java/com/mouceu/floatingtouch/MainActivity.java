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
import java.util.List;
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

    private int selectedOpacity;
    private int selectedTouchAreaSize;
    private TextView opacityPickerValue;
    private TextView touchAreaSizePickerValue;
    private LocalBroadcastManager broadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        broadcastManager = LocalBroadcastManager.getInstance(this);
        setContentView(R.layout.activity_main);

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            // TODO: message/instruction
            startActivityForResult(intent, DRAW_OVER_OTHER_APP_PERMISSION_CODE);
        }
        if (!Util.isAccessibilityServiceEnabled(this, FloatingViewService.class)) {
            // TODO: message/instruction
            Intent accessibilityIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(accessibilityIntent, ENABLE_ACCESSIBILITY_CODE);
        }

        initSettingList();
        initOpacityPicker();
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
                        "Enable FloatingView service in accessibility settings",
                        Toast.LENGTH_SHORT
                ).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void initOpacityPicker() {
        SeekBar opacityPicker = findViewById(R.id.opacity_picker);
        opacityPickerValue = findViewById(R.id.opacity_picker_title);
        final String opacityPickerTitle = getString(R.string.opacity_picker_title) + ": %d";
        selectedOpacity = Util.getSetting(OPACITY.name(), DEFAULT_OPACITY, MainActivity.this);
        opacityPickerValue.setText(String.format(opacityPickerTitle, selectedOpacity));
        opacityPicker.setProgress(selectedOpacity);

        opacityPicker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedOpacity = seekBar.getProgress();

                opacityPickerValue.setText(String.format(opacityPickerTitle, selectedOpacity));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Util.saveSetting(OPACITY.name(), selectedOpacity, MainActivity.this);
                Bundle bundle = new Bundle();
                bundle.putInt(OPACITY.name(), selectedOpacity);
                Intent intent = new Intent(SERVICE_PARAMS);
                intent.putExtras(bundle);
                broadcastManager.sendBroadcast(intent);
            }
        });
    }

    private void initTouchAreaPicker() {
        SeekBar areaPicker = findViewById(R.id.touch_area_size_picker);
        touchAreaSizePickerValue = findViewById(R.id.touch_area_size_picker_title);

        final String title = getString(R.string.touch_area_size) + ": %d";
        selectedTouchAreaSize = Util.getSetting(TOUCH_AREA.name(), DEFAULT_TOUCH_AREA_SIZE, MainActivity.this);
        touchAreaSizePickerValue.setText(String.format(title, selectedTouchAreaSize));
        areaPicker.setProgress(selectedTouchAreaSize);

        areaPicker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedTouchAreaSize = seekBar.getProgress();

                touchAreaSizePickerValue.setText(String.format(title, selectedTouchAreaSize));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Util.saveSetting(TOUCH_AREA.name(), selectedTouchAreaSize, MainActivity.this);
                Bundle bundle = new Bundle();
                bundle.putInt(TOUCH_AREA.name(), selectedTouchAreaSize);
                Intent intent = new Intent(SERVICE_PARAMS);
                intent.putExtras(bundle);
                broadcastManager.sendBroadcast(intent);
            }
        });
    }

    private void initSettingList() {
        Drawable arrowIcon = getDrawable(R.drawable.ic_arrow_downward_24dp);
        Drawable sensitivityIcon = getDrawable(R.drawable.ic_sensitivity_angle_24dp);
        Drawable touchIcon = getDrawable(R.drawable.ic_touch);
        Objects.requireNonNull(arrowIcon);
        final List<SettingItem> settingItems = Arrays.asList(
                new SettingItem(ACTION_LEFT,
                        Util.getStoredAction(ACTION_LEFT, OPEN_RECENT_APPS, this).resolve(this),
                        getRotateDrawable(arrowIcon, 90)),
                new SettingItem(ACTION_UP,
                        Util.getStoredAction(ACTION_UP, OPEN_HOME_SCREEN, this).resolve(this),
                        getRotateDrawable(arrowIcon, 180)),
                new SettingItem(ACTION_RIGHT,
                        Util.getStoredAction(ACTION_RIGHT, OPEN_PREVIOUS_APP, this).resolve(this),
                        getRotateDrawable(arrowIcon, 270)),
                new SettingItem(ACTION_DOWN,
                        Util.getStoredAction(ACTION_DOWN, OPEN_NOTIFICATIONS, this).resolve(this),
                        arrowIcon),
                new SettingItem(ACTION_TOUCH,
                        Util.getStoredAction(ACTION_TOUCH, GO_BACK, this).resolve(this),
                        touchIcon),
                new SettingItem(SENSITIVITY_ANGLE,
                        Util.getSetting(SENSITIVITY_ANGLE.name(), String.valueOf(DEFAULT_ANGLE), this),
                        sensitivityIcon)
        );

        final SettingListAdapter adapter = new SettingListAdapter(this, settingItems);
        final ListView settingList = findViewById(R.id.setting_list);
        settingList.setAdapter(adapter);

        final String[] angleValues = getResources().getStringArray(R.array.sensitivity_angle_values);
        final String[] actions = SlideAction.getResolvedValues(this);
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
                                .setTitle(clickedItem.getName().resolve(MainActivity.this))
                                .setItems(actions, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final SlideAction action = SlideAction.values()[which];
                                        Util.saveSetting(clickedItem.getName().name(), action.name(), MainActivity.this);
                                        clickedItem.setValue(actions[which]);
                                        adapter.notifyDataSetChanged();

                                        Bundle bundle = new Bundle();
                                        bundle.putString(clickedItem.getName().name(), action.name());
                                        Intent intent = new Intent(SERVICE_PARAMS);
                                        intent.putExtras(bundle);
                                        broadcastManager.sendBroadcast(intent);
                                    }
                                });
                        break;

                    case SENSITIVITY_ANGLE:
                        builder
                                .setTitle(R.string.angle_spinner_title)
                                .setItems(angleValues, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final String angleValue = angleValues[which];
                                        Util.saveSetting(SENSITIVITY_ANGLE.name(), angleValue, MainActivity.this);
                                        clickedItem.setValue(angleValue);
                                        adapter.notifyDataSetChanged();

                                        Bundle bundle = new Bundle();
                                        bundle.putString(SENSITIVITY_ANGLE.name(), angleValue);
                                        Intent intent = new Intent(SERVICE_PARAMS);
                                        intent.putExtras(bundle);
                                        broadcastManager.sendBroadcast(intent);
                                    }
                                });
                        break;
                }
                builder.show();
            }
        });
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
