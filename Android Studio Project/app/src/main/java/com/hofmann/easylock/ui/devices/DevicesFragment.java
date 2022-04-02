package com.hofmann.easylock.ui.devices;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.hofmann.easylock.Bluetooth;
import com.example.easylock.R;
import com.hofmann.easylock.MainActivity;
import com.hofmann.easylock.Singleton;
import com.example.easylock.databinding.FragmentDevicesBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;


public class DevicesFragment extends Fragment {
    private FragmentDevicesBinding binding;
    private ListView listView;
    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private int REQUEST_ENABLE_BT = 1;
    private ArrayAdapter arrayAdapter;
    ArrayList<BluetoothDevice> bluetooth_devices;

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DevicesViewModel devicesViewModel =
                new ViewModelProvider(this).get(DevicesViewModel.class);

        binding = FragmentDevicesBinding.inflate(inflater, container, false);
        binding.swiper.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh_list();
            }
        });

        listView = binding.list;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Bluetooth.setBluetoothDevice(bluetooth_devices.get(i));
                Bluetooth.closeConnection();
                if(sharedPreferences.getString("settings_start_automatic_device", null).equals("settings_start_last_device")) {
                    Log.d("Devices","Device saved in pref");
                    editor.putString(getResources().getString(R.string.auto_select_device), bluetooth_devices.get(i).getAddress());
                    editor.apply();
                }
            }
        });
        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Bluetooth.setBluetoothDevice(bluetooth_devices.get(i));
                Bluetooth.closeConnection();
                if(sharedPreferences.getString("settings_start_automatic_device", null).equals("settings_start_last_device")) {
                    Log.d("Devices","Device saved in pref");
                    editor.putString(getResources().getString(R.string.auto_select_device), bluetooth_devices.get(i).getAddress());
                    editor.apply();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Bluetooth.setBluetoothDevice(null);
            }
        });
        View root = binding.getRoot();
        refresh_list();
        if(Bluetooth.getBluetoothDevice() != null) {
            for (int x = 0; x < bluetooth_devices.size(); x++) {
                Log.d("Find Devices", bluetooth_devices.get(x).getAddress() + " | " + Bluetooth.getBluetoothDevice().getAddress());
                if (bluetooth_devices.get(x).equals(Bluetooth.getBluetoothDevice())) {
                    listView.setItemChecked(x, true);
                    break;
                }
            }
        }

        return root;
    }

    public static void setSharedPreferences(SharedPreferences inp) {
        sharedPreferences = inp;
        editor = sharedPreferences.edit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    ActivityResultLauncher<Intent> mGetContent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() != Activity.RESULT_OK) {
                Snackbar snackbar = Snackbar.make(binding.getRoot(), Singleton.get_bluetooth_message(), getResources().getInteger(R.integer.snackbar_time));
                snackbar.show();
            }
        }
    });

    private void refresh_list() {
        binding.swiper.setRefreshing(false);
        if (MainActivity.bluetooth_check_without_consequenz()) {
            bluetooth_devices = Bluetooth.refresh_devices();
            ArrayList<String> device_names = new ArrayList<>(0);
            if (ActivityCompat.checkSelfPermission(binding.getRoot().getContext(), permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            for (int x = 0; x < bluetooth_devices.size(); x++) {
                device_names.add(bluetooth_devices.get(x).getName());
            }
            arrayAdapter = new ArrayAdapter(binding.getRoot().getContext(), android.R.layout.simple_list_item_single_choice, device_names);
            listView.setAdapter(arrayAdapter);
            if(Bluetooth.getBluetoothDevice() != null) {
                for (int x = 0; x < bluetooth_devices.size(); x++) {
                    Log.d("Find Devices", bluetooth_devices.get(x).getAddress() + " | " + Bluetooth.getBluetoothDevice().getAddress());
                    if (bluetooth_devices.get(x).equals(Bluetooth.getBluetoothDevice())) {
                        listView.setItemChecked(x, true);
                        break;
                    }
                }
            }
        }else {
            final ArrayList<String> device_names = new ArrayList<>();
            arrayAdapter = new ArrayAdapter(binding.getRoot().getContext(), android.R.layout.simple_list_item_1, device_names);
            listView.setAdapter(arrayAdapter);
        }
    }
}