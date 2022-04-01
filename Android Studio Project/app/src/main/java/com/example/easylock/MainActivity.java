package com.example.easylock;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.easylock.databinding.FragmentDevicesBinding;
import com.example.easylock.ui.devices.DevicesFragment;
import com.example.easylock.ui.settings.SettingsFragment;
import com.google.android.material.navigation.NavigationView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.easylock.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    public static Toolbar toolbar;
    public static MenuItem btn_connect;
    private Handler handler = new Handler();
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    private IntentFilter intentFilter = new IntentFilter();
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_info, R.id.nav_terminal, R.id.nav_devices, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        //Snackbar.make(binding.getRoot(), Integer.toString(menuItem.getItemId()), context.getResources().getInteger(R.integer.snackbar_time)).show();

        //INIT
        Singleton.get_resources(binding.getRoot().getContext());
        Bluetooth.init(binding.getRoot(), handler, executorService);
        Background_Listener.init(binding.getRoot());
        SettingsFragment.init(binding.getRoot());


        Singleton.set_bluetooth_permission_granted(true);
        if (ContextCompat.checkSelfPermission(binding.getRoot().getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        }
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetooth_status, intentFilter);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(binding.getRoot().getContext());
        DevicesFragment.setSharedPreferences(sharedPreferences);
        String wanted_adress = sharedPreferences.getString(getResources().getString(R.string.auto_select_device), null);
        if(wanted_adress != null && bluetooth_check()) {
            Log.d("Device", wanted_adress);
            ArrayList<BluetoothDevice> bluetoothDevices = Bluetooth.refresh_devices();
            for(int x = 0; x < bluetoothDevices.size(); x++) {
                if(bluetoothDevices.get(x).getAddress().equals(wanted_adress)) {
                    Bluetooth.setBluetoothDevice(bluetoothDevices.get(x));
                }
            }
        }

        //Log.d("last device", Boolean.toString(sharedPreferences.getString("settings_start_automatic_device", null).equals("settings_start_last_device")));
    }

    private final BroadcastReceiver bluetooth_status = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    //Bluetooth.setConnection_status(true);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Bluetooth.setConnection_status(false);
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        btn_connect = menu.findItem(R.id.connect_btn);
        if(sharedPreferences.getBoolean("settings_start_automatic_connection", false) && bluetooth_check_without_consequenz() && Bluetooth.getBluetoothDevice() != null) {
            if(Bluetooth.is_connected()) {
                btn_connect.setTitle(getResources().getString(R.string.toolbox_disconnect));
            }else {
                Bluetooth.startConnection();
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect_btn:
                Log.d("Connection Button","Pressed");
                if (bluetooth_check()) {
                    Log.d("Connection Button","Connection possible");
                    if (Bluetooth.getBluetoothDevice() == null) {
                        Snackbar.make(binding.getRoot(), getResources().getString(R.string.bluetooth_device_message), getResources().getInteger(R.integer.snackbar_time)).show();
                    } else {
                        if (Bluetooth.is_connected()) {
                            Bluetooth.closeConnection();
                            Log.d("Connection Button","Closing Connection");
                        } else {
                            Bluetooth.startConnection();
                            Log.d("Connection Button","Starting Connection");
                        }
                    }
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                } else {
                    Snackbar.make(binding.getRoot(), getResources().getString(R.string.bluetooth_permission_message), getResources().getInteger(R.integer.snackbar_time)).show();
                    Singleton.set_bluetooth_permission_granted(false);
                }
            });

    private boolean bluetooth_check() {
        if (ContextCompat.checkSelfPermission(binding.getRoot().getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            if (bluetoothAdapter.isEnabled()) {
                return true;
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mGetContent.launch(enableBtIntent);
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        }
        return false;
    }

    private boolean bluetooth_check_without_consequenz() {
        if (ContextCompat.checkSelfPermission(binding.getRoot().getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            if (bluetoothAdapter.isEnabled()) {
                return true;
            }
        }
        return false;
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

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}