package com.mouceu.floatingtouch;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.mouceu.floatingtouch.KnownAction.*;
import static com.mouceu.floatingtouch.KnownSetting.*;

public class MainActivity extends AppCompatActivity {
    private static final int DRAW_OVER_OTHER_APP_PERMISSION_CODE = 2084;
    private static final int ENABLE_ACCESSIBILITY_CODE = 2085;
    static final String SERVICE_PARAMS = "com.mouceu.floatingtouch.SERVICE_PARAMS";
    static final String OPACITY_SETTING_NAME = "OPACITY";
    static final String ANGLE_SETTING_NAME = "ANGLE";
    static final int DEFAULT_ANGLE = 90;

    private int selectedOpacity = 0;
    private TextView opacityPickerValue;
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

//        initSettingList();
        initAngleSpinner();
        initOpacityPicker();
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

    private void initAngleSpinner() {
        Spinner angleSpinner = findViewById(R.id.angle_spinner);
        Integer[] values = {15, 30, 45, 60, 75, DEFAULT_ANGLE};
        final ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        angleSpinner.setAdapter(adapter);
        angleSpinner.setSelection(values.length - 1);

        angleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Integer selectedAngle = adapter.getItem(position);
                if (selectedAngle == null) {
                    selectedAngle = DEFAULT_ANGLE;
                }
                Util.saveSetting(ANGLE_SETTING_NAME, selectedAngle, MainActivity.this);
                Bundle bundle = new Bundle();
                bundle.putInt(ANGLE_SETTING_NAME, selectedAngle);
                Intent intent = new Intent(SERVICE_PARAMS);
                intent.putExtras(bundle);
                broadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void initOpacityPicker() {
        SeekBar opacityPicker = findViewById(R.id.opacity_picker);
        opacityPickerValue = findViewById(R.id.opacity_picker_value);
        selectedOpacity = Util.getSetting(OPACITY_SETTING_NAME, 0, MainActivity.this);
        opacityPickerValue.setText(String.valueOf(selectedOpacity));
        opacityPicker.setProgress(selectedOpacity);

        opacityPicker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedOpacity = seekBar.getProgress();
                opacityPickerValue.setText(String.valueOf(selectedOpacity));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Util.saveSetting(OPACITY_SETTING_NAME, selectedOpacity, MainActivity.this);
                Bundle bundle = new Bundle();
                bundle.putInt(OPACITY_SETTING_NAME, selectedOpacity);
                Intent intent = new Intent(SERVICE_PARAMS);
                intent.putExtras(bundle);
                broadcastManager.sendBroadcast(intent);
            }
        });
    }



    private final Map<KnownSetting, Drawable> settingImagesMapping = new HashMap<>();
    private final Map<KnownSetting, KnownAction> settings = new HashMap<>();

    private void initSettingList() {
        Drawable arrow = getDrawable(R.drawable.ic_arrow_downward_24dp);
        Objects.requireNonNull(arrow);
        List<SettingItem> settingItems = Arrays.asList(
                new SettingItem(ACTION_LEFT, getStoredAction(ACTION_LEFT, OPEN_RECENT_APPS),
                        getRotateDrawable(arrow, 90)),
                new SettingItem(ACTION_UP, getStoredAction(ACTION_UP, OPEN_HOME_SCREEN),
                        getRotateDrawable(arrow, 180)),
                new SettingItem(ACTION_RIGHT, getStoredAction(ACTION_RIGHT, OPEN_PREVIOUS_APP),
                        getRotateDrawable(arrow, 270)),
                new SettingItem(ACTION_DOWN, getStoredAction(ACTION_DOWN, OPEN_NOTIFICATIONS), arrow)
        );
//        settingImagesMapping.put(ACTION_LEFT, getRotateDrawable(arrow, 90));
//        settingImagesMapping.put(ACTION_UP, getRotateDrawable(arrow, 180));
//        settingImagesMapping.put(ACTION_RIGHT, getRotateDrawable(arrow, 270));
//        settingImagesMapping.put(ACTION_DOWN, arrow);

//        settingImagesMapping.put(ACTION_LEFT, arrow);
//        settingImagesMapping.put(ACTION_UP, arrow);
//        settingImagesMapping.put(ACTION_RIGHT, arrow);
//        settingImagesMapping.put(ACTION_DOWN, arrow);

//        settings.put(ACTION_LEFT, getStoredAction(ACTION_LEFT, OPEN_RECENT_APPS));
//        settings.put(ACTION_UP, getStoredAction(ACTION_UP, OPEN_HOME_SCREEN));
//        settings.put(ACTION_RIGHT, getStoredAction(ACTION_RIGHT, OPEN_PREVIOUS_APP));
//        settings.put(ACTION_DOWN, getStoredAction(ACTION_DOWN, OPEN_NOTIFICATIONS));

//        SettingListAdapter adapter = new SettingListAdapter(this, settings, settingImagesMapping);
        SettingListAdapter adapter = new SettingListAdapter(this, settingItems);
        ListView settingList = findViewById(R.id.setting_list);
        settingList.setAdapter(adapter);
//        adapter.notifyDataSetChanged();

        Log.d("Adapter", String.valueOf(adapter.getCount()));
//        settingList.setOnClickListener();
    }

    @NonNull
    private KnownAction getStoredAction(KnownSetting actionLeft, KnownAction openRecentApps) {
        return KnownAction.valueOf(Util.getSetting(actionLeft, openRecentApps.name(), this));
    }

    private Drawable getRotateDrawable(@NonNull final Drawable d, final float angle) {
        final Drawable[] layers = { d };
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
