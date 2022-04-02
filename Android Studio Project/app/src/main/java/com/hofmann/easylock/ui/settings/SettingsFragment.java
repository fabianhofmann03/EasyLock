package com.hofmann.easylock.ui.settings;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.hofmann.easylock.Background_Listener;
import com.hofmann.easylock.Bluetooth;
import com.example.easylock.R;
import com.hofmann.easylock.MainActivity;
import com.hofmann.easylock.Singleton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static View view;
    private static Context context;
    private static BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

    ListPreference settings_start_automatic_device;
    ListPreference settings_start_specific_bluetooth_device;
    SwitchPreference settings_start_automatic_connection;
    Preference change_password_btn;
    Preference config_btn;

    private static boolean last_device = false;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    CharSequence[] bluetoothAdress;

    static boolean dismissBlocker = false;

    private static LayoutInflater layoutInflater;
    PopupWindow change_password_window;
    View change_password_view;
    static PopupWindow config_window;
    static View config_view_password;
    static View config_view;

    static EditText config_password;
    static Button config_password_continue;

    static TextView config_text;
    static Button config_continue;

    EditText text_password_changepw;
    EditText text_new_password_changepw;
    Button changepw;

    static float scale;

    private static int config_num = 0;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(androidx.appcompat.R.attr.background, typedValue, true);
        view.setBackgroundColor(typedValue.data);

        settings_start_automatic_device = (ListPreference) findPreference("settings_start_automatic_device");
        settings_start_specific_bluetooth_device = (ListPreference) findPreference("settings_start_specific_bluetooth_device");
        change_password_btn = (Preference) findPreference("settings_device_change_password");
        config_btn = (Preference) findPreference("settings_device_config_device");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        editor = sharedPreferences.edit();

        change_password_view = layoutInflater.inflate(R.layout.change_password_popup, null);
        config_view_password = layoutInflater.inflate(R.layout.configuration_popup_password, null);
        config_view = layoutInflater.inflate(R.layout.configuration_popup, null);

        scale = context.getResources().getDisplayMetrics().density;
        change_password_window = new PopupWindow(change_password_view, (int) (270 * scale + 0.5f), (int) (350 * scale + 0.5f), true);
        config_window = new PopupWindow(config_view_password, (int) (270 * scale + 0.5f), (int) (350 * scale + 0.5f), true);

        change_password_window.setElevation(10);
        config_window.setElevation(10);

        text_password_changepw = change_password_view.findViewById(R.id.text_password_changepw);
        text_new_password_changepw = change_password_view.findViewById(R.id.text_new_password_changepw);
        changepw = change_password_view.findViewById(R.id.changepw);

        config_password = config_view_password.findViewById(R.id.configuration_password);
        config_password_continue = config_view_password.findViewById(R.id.start_config_button);

        config_text = config_view.findViewById(R.id.configuration_text);
        config_continue = config_view.findViewById(R.id.config_button);

        config_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Background_Listener.send_stat((byte) Background_Listener.status_message.CONTINUE.num);
            }
        });
        config_password_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d("config pw", config_password.getText().toString());
                if (config_password.getText().toString().length() > 127) {
                    Snackbar.make(view, getResources().getString(R.string.pw_too_long), getResources().getInteger(R.integer.snackbar_time)).show();
                } else {
                    if (MainActivity.bluetooth_check_without_consequenz()) {
                        if (Bluetooth.is_connected()) {
                            Background_Listener.set_send_data_storage(Background_Listener.variable.PASSWORD.num, config_password.getText().toString());
                            while (true) {
                                if (Background_Listener.send_cmd((byte) Background_Listener.command.START_CONFIG.num) != Background_Listener.sending_response.WAIT)
                                    break;
                            }
                        } else {
                            Snackbar.make(view, getResources().getString(R.string.bluetooth_not_connected), getResources().getInteger(R.integer.snackbar_time)).show();
                        }
                    }
                }
            }
        });
        config_btn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                config_window.showAtLocation(view, Gravity.CENTER, 0, 0);
                dimBackground((View) config_window.getContentView().getParent());
                return true;
            }
        });
        changepw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (text_password_changepw.getText().toString().length() > 127) {
                    Snackbar.make(view, getResources().getString(R.string.pw_too_long), getResources().getInteger(R.integer.snackbar_time)).show();
                } else {
                    if (text_new_password_changepw.getText().toString().length() > 127) {
                        Snackbar.make(view, getResources().getString(R.string.new_pw_too_long), getResources().getInteger(R.integer.snackbar_time)).show();
                    } else {
                        if (MainActivity.bluetooth_check_without_consequenz()) {
                            if (Bluetooth.is_connected()) {
                                Background_Listener.set_send_data_storage(Background_Listener.variable.PASSWORD.num, text_password_changepw.getText().toString());
                                Background_Listener.set_send_data_storage(Background_Listener.variable.NEW_PASSWORD.num, text_new_password_changepw.getText().toString());
                                while (true) {
                                    if (Background_Listener.send_cmd((byte) Background_Listener.command.CHANGE_PASSWORD.num) != Background_Listener.sending_response.WAIT)
                                        break;
                                }
                            } else {
                                Snackbar.make(view, getResources().getString(R.string.bluetooth_not_connected), getResources().getInteger(R.integer.snackbar_time)).show();
                            }
                        }
                        change_password_window.dismiss();
                    }
                }
            }
        });

        switch ((String) settings_start_automatic_device.getValue()) {
            case "settings_start_last_device":
                settings_start_specific_bluetooth_device.setEnabled(false);
                break;
            case "settings_start_specific_device":
                settings_start_specific_bluetooth_device.setEnabled(true);
                refresh_list();
                break;
        }

        settings_start_specific_bluetooth_device.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                if(MainActivity.bluetooth_check_without_consequenz()) {
                    refresh_list();
                    return true;
                }else {
                    return false;
                }
            }
        });

        settings_start_specific_bluetooth_device.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                editor.putString(getResources().getString(R.string.auto_select_device), (String) newValue);
                editor.apply();
                return true;
            }
        });

        settings_start_automatic_device.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                switch((String) newValue) {
                    case "settings_start_last_device":
                        settings_start_specific_bluetooth_device.setEnabled(false);
                        if(Bluetooth.getBluetoothDevice() != null) editor.putString(getResources().getString(R.string.auto_select_device), Bluetooth.getBluetoothDevice().getAddress());
                        else editor.putString(getResources().getString(R.string.auto_select_device), null);
                        editor.apply();
                        settings_start_specific_bluetooth_device.setValue(null);
                        break;
                    case "settings_start_specific_device":
                        settings_start_specific_bluetooth_device.setEnabled(true);
                        editor.putString(getResources().getString(R.string.auto_select_device), null);
                        editor.apply();
                        refresh_list();
                        break;
                }
                return true;
            }
        });

        change_password_btn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                change_password_window.showAtLocation(view, Gravity.CENTER, 0, 0);
                dimBackground((View) change_password_window.getContentView().getParent());
                return true;
            }
        });

        change_password_window.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                text_password_changepw.setText("");
                text_new_password_changepw.setText("");
            }
        });

        config_window.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if(!dismissBlocker) {
                    Background_Listener.send_stat((byte) Background_Listener.status_message.CANCELD.num);
                    config_window.setContentView(config_view_password);
                    config_password.setText("");
                    config_num = 0;
                }
                dismissBlocker = false;
            }
        });
    }

    public static void close_config() {
        config_window.dismiss();
    }

    public static void continue_config() {
        Log.d("Continue", "Continued with " + Integer.toString(config_num));
        switch (config_num) {
            case 0:
                Log.d("Continue", "In 0");
                dismissBlocker = true;
                config_window.dismiss();
                config_window.setContentView(config_view);
                config_window.showAtLocation(view, Gravity.CENTER, 0, 0);

                dimBackground((View) config_window.getContentView().getParent());
                config_text.setText(context.getResources().getString(R.string.configuration_message_1));
                config_continue.setText(R.string.continue_config);
                config_num++;
                break;
            case 1:
                config_text.setText(context.getResources().getString(R.string.configuration_message_2));
                config_continue.setText(R.string.finish_config);
                config_window.update();
                break;
        }
    }

    private static void dimBackground(View container) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        // add flag
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 1 - (95f/255f);
        wm.updateViewLayout(container, p);
    }

    public static void init(View inp) {
        view = inp;
        context = view.getContext();
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    private void refresh_list() {
        if (MainActivity.bluetooth_check_without_consequenz()) {
            ArrayList<BluetoothDevice> bluetoothDevices = Bluetooth.refresh_devices();
            CharSequence[] bluetoothNames = new CharSequence[bluetoothDevices.size()];
            bluetoothAdress = new CharSequence[bluetoothDevices.size()];
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            for (int x = 0; x < bluetoothDevices.size(); x++) {
                bluetoothNames[x] = bluetoothDevices.get(x).getName();
                bluetoothAdress[x] = bluetoothDevices.get(x).getAddress();
            }
            settings_start_specific_bluetooth_device.setEntries(bluetoothNames);
            settings_start_specific_bluetooth_device.setEntryValues(bluetoothAdress);
            if(settings_start_specific_bluetooth_device.getValue() != null) {
                editor.putString(getResources().getString(R.string.auto_select_device), settings_start_specific_bluetooth_device.getValue());
                editor.apply();
            }
        }
    }

    ActivityResultLauncher<Intent> mGetContent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() != Activity.RESULT_OK) {
                Snackbar snackbar = Snackbar.make(view, Singleton.get_bluetooth_message(), getResources().getInteger(R.integer.snackbar_time));
                snackbar.show();
            }
        }
    });
}