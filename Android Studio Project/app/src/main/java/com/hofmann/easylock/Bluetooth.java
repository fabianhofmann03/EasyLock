package com.hofmann.easylock;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.companion.CompanionDeviceManager;
import android.companion.CompanionDeviceService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.hofmann.easylock.R;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import io.reactivex.exceptions.Exceptions;

public class Bluetooth {
    public static enum BLUETOOTH_CONNECTION {CONNECTED, NO_PERMISSION, NO_BLUETOOTH, ERROR}
    private static BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static BluetoothSocket sock;
    private static BluetoothSocket sockFallback;
    private static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static boolean connection_status = false;

    private static BluetoothDevice bluetoothDevice = null;
    private static InputStream inputStream = null;
    private static OutputStream outputStream = null;
    private static ExecutorService executorService = null;
    private static android.os.Handler handler = null;
    private static View view = null;
    private static Context context = null;

    public static void init(View inp, android.os.Handler inp2, ExecutorService inp3) {
        view = inp;
        context = view.getContext();
        handler = inp2;
        executorService = inp3;
    }

    public static void setBluetoothDevice(BluetoothDevice inp) {
        bluetoothDevice = inp;
    }
    public static BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public static boolean socket_available() {
        return sock != null;
    }

    public static boolean outputstream_available() {
        return outputStream != null;
    }

    public static boolean inputstream_available() {
        return inputStream != null;
    }

    public static int available() {
        try {
            return inputStream.available();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static byte read() {
        if(inputStream != null) {
            try {
                return (byte) inputStream.read();
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }
        }else return 0;
    }

    public static void write(byte[] inp) {
        if(outputStream != null) {
            try {
                outputStream.write(inp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void write(byte inp) {
        if(outputStream != null) {
            try {
                outputStream.write(new byte[]{inp}, 0, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setConnection_status(boolean newStatus) {
        connection_status = newStatus;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (connection_status) {
                    MainActivity.btn_connect.setTitle(context.getResources().getString(R.string.toolbox_disconnect));
                } else {
                    MainActivity.btn_connect.setTitle(context.getResources().getString(R.string.toolbox_connect));
                }
            }
        });
    }

    public static boolean is_connected() {
        return connection_status;
    }

    public static void closeConnection() {
        if (sock != null) {
            try {
                sock.close();
                outputStream = null;
                inputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
            MainActivity.btn_connect.setEnabled(true);
        }
    }

    public static ArrayList<BluetoothDevice> refresh_devices() {
        if (bluetoothAdapter.isEnabled()) {
            final ArrayList devices = new ArrayList();
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    devices.add(device);
                }
            }
            return devices;
        } else {
            return null;
        }
    }

    private static void connection_callback(BLUETOOTH_CONNECTION result) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (result == BLUETOOTH_CONNECTION.CONNECTED) {
                    Snackbar.make(view, context.getResources().getString(R.string.connected) + " " + bluetoothDevice.getName(), context.getResources().getInteger(R.integer.snackbar_time)).show();
                    try {
                        outputStream = sock.getOutputStream();
                        inputStream = sock.getInputStream();
                        connection_status = true;
                        Background_Listener.start_listener(executorService, handler);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else {
                    Snackbar.make(view, context.getResources().getString(R.string.not_connected_1) + " " + bluetoothDevice.getName() + " " + context.getResources().getString(R.string.not_connected_2), context.getResources().getInteger(R.integer.snackbar_time)).show();
                    outputStream = null;
                    inputStream = null;
                }
                if(MainActivity.btn_connect != null) MainActivity.btn_connect.setEnabled(true);
            }
        });
    }
    public static void startConnection() {
        if (executorService != null) {
            Log.d("Bluetooth", "Starting Connection");
            if(MainActivity.btn_connect != null) MainActivity.btn_connect.setEnabled(false);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    /* * Establish Bluetooth connection * */
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        Log.d("Bluetooth", "No Permission");
                        if(MainActivity.btn_connect != null) MainActivity.btn_connect.setEnabled(true);
                        return;
                    }
                    bluetoothAdapter.cancelDiscovery();
                    try {
                        sock = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                        Log.d("Bluetooth", "First Try Connecting");
                        sock.connect();
                        Log.d("Bluetooth", "First Try Worked");
                        connection_callback(BLUETOOTH_CONNECTION.CONNECTED);
                    } catch (Exception e1) {
                        Class<?> clazz = sock.getRemoteDevice().getClass();
                        Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                        try {

                            /************Fallback method 1*********************/
                            Method m = clazz.getMethod(
                                    "createRfcommSocket"
                                    , paramTypes
                            );
                            Object[] params = new Object[]{Integer.valueOf(1)};
                            sockFallback = (BluetoothSocket) m.invoke(
                                    sock.getRemoteDevice()
                                    , params
                            );
                            Log.d("Bluetooth", "Second Try Connecting");
                            sockFallback.connect();
                            Log.d("Bluetooth", "Second Try Worked");
                            sock = sockFallback;
                            connection_callback(BLUETOOTH_CONNECTION.CONNECTED);
                        } catch (Exception e2) {
                            Log.d("Bluetooth", "Didnt work");
                            connection_callback(BLUETOOTH_CONNECTION.ERROR);
                        }
                    }
                }
            });
        }
    }
}
