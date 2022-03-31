package com.example.easylock;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import pl.droidsonroids.gif.InputSource;

public class Singleton  {
    private static String pwd = null;
    private static boolean door_status = false;                                             //false: Closed, true: Open
    private static String[] bluetooth_messages;
    private static String[] bluetooth_permission_messages;
    private static int bluetooth_messages_counter = 0;
    private static int bluetooth_permission_messages_counter = 0;

    private static boolean bluetooth_permission_granted = true;
    public static void get_resources(@NonNull Context context) {
        bluetooth_messages = context.getResources().getStringArray(R.array.bluetooth_messages);
    }
    public static synchronized String getPwd() {
        if (pwd == null) {
            pwd = new String();
        }
        return(pwd);
    }
    public static synchronized void setPwd(String inp) {
        pwd = inp;
    }

    public static synchronized boolean get_door_status() {
        return(door_status);
    }
    public static synchronized void set_door_status(boolean inp) {
        door_status = inp;
    }
    public static synchronized String get_bluetooth_message() {
        String res = bluetooth_messages[bluetooth_messages_counter];
        if(bluetooth_messages_counter < bluetooth_messages.length - 1) bluetooth_messages_counter++;
        return res;
    }
    public static synchronized void set_bluetooth_permission_granted(boolean inp) {bluetooth_permission_granted = inp;}
    public static synchronized boolean get_bluetooth_permission_granted() {return bluetooth_permission_granted;}
}