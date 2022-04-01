package com.example.easylock;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;

import com.example.easylock.ui.home.HomeFragment;
import com.example.easylock.ui.settings.SettingsFragment;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;


public class Background_Listener {
    private static Timer timer = new Timer();
    private static int counter = 0;
    private final static int data_storage_size = 127;

    private static View view;
    private static Context context;

    private static volatile int curCom = 0;
    private static int sending_state = 0;

    public static void init(View inp) {
        view = inp;
        context = inp.getContext();
    }

    public static enum sending_response {WAIT, CONTINUE, CANCEL}

    private static enum lockstates {IDLE, OPENLOCK, WAITFOROPEN, WAITFORCLOSE}


    private lockstates working_state = lockstates.IDLE;

    private static enum commandcontroll {
        WAITFORCMD, READCMD, READREQ, READDATLEN, READDATVAR, READDAT, READSTAT, COMP
    }

    private static commandcontroll cmd_state = commandcontroll.WAITFORCMD;

    private static enum commandexecution {
        WAITTOSTART, EXECMD
    }

    private static commandexecution cmd_exe = commandexecution.WAITTOSTART;

    public static enum command {
        CHANGE_PASSWORD(1), OPEN_LOCK(2), START_CONFIG(3);
        private static final Map<Integer, command> NUM = new HashMap<Integer, command>();

        static {
            for (command e : values()) {
                NUM.put(e.num, e);
            }
        }

        public int num;

        command(int num) {
            this.num = num;
        }
    }

    public static enum variable {
        PASSWORD(1), NEW_PASSWORD(2), LOCK_STATUS(3);
        private static final Map<Integer, variable> NUM = new HashMap<Integer, variable>();

        static {
            for (variable e : values()) {
                NUM.put(e.num, e);
            }
        }

        public int num;

        variable(int num) {
            this.num = num;
        }
    }

    public static enum status_message {
        COMMAND_EXECUTED(1), RECEIVED(2), DENY(3), TOO_MUCH_DATA(4), MESSAGE_CORRUPTED(5), LOCK_OPEND(6), LOCK_CLOSED(7), CANCELD(8), CONTINUE(9);
        private static final Map<Integer, status_message> NUM = new HashMap<Integer, status_message>();

        static {
            for (status_message e : values()) {
                NUM.put(e.num, e);
            }
        }

        public int num;

        status_message(int num) {
            this.num = num;
        }
    }

    private static byte cmd;
    private volatile static byte[] data_storage = new byte[data_storage_size + 2];      //First byte:   [xxxxxxxx]
    //              [x-------] 1: Data completly loaded, 0: Data still loading
    //              [-xxxxxxx] What variable is stored
    //Second byte:  [xxxxxxxx] Length of Data (max 127)

    private volatile static byte[][] send_data_storage = new byte[2][];

    private volatile static byte request_storage;                                       //  [x-------] 1: Request has come, 0: No request
    //  [-xxxxxxx] Requested Variable

    private volatile static byte status_storage;                                        //  [x-------] 1: State has come, 0: No state
    //  [-xxxxxxx] Last State

    static byte control_byte;
    static byte data_storage_counter = 0;
    static commandcontroll last_state = commandcontroll.WAITFORCMD;
    private static Handler mainThreadHandler = null;

    private static int cmd_exe_counter = 0;

    private static Executor executor;

    private static void del_array(byte[] array, int len) {
        array = new byte[len];
    }

    private static void send_msg_stopping(byte[] msg) {
        for (int x = 0; x < msg.length; x++) {
            Log.d("Output Message", Integer.toHexString(msg[x]));
        }
        Bluetooth.write(msg);
    }

    private static void send_msg_stopping(byte msg) {
        Log.d("Output Message", Integer.toHexString(msg));
        Bluetooth.write(msg);
    }

    public static void set_send_data_storage(int var, String data) {
        int y = data.length();
        send_data_storage[var - 1] = new byte[y];
        Log.d("set send data storage", "Start Writing");
        for (int x = 0; x < y; x++) send_data_storage[var - 1][x] = (byte) data.toCharArray()[x];
        Log.d("set send data storage", "Finish writing");
    }

    public static sending_response send_cmd(byte cmd) {
        curCom = (byte) cmd;
        switch (sending_state) {
            case 0:
                //byte[] command_protocol = {0b10000001, cmd, 0};
                byte[] command_protocol = {((byte) 0b10000001), cmd, 0};
                command_protocol[2] = (byte) (command_protocol[0] ^ command_protocol[1]);
                send_msg_stopping(command_protocol);
                status_storage = 0;
                sending_state = 1;
                timer.setTimer(context.getResources().getInteger(R.integer.response_timer));
                break;
            case 1:
                sending_state = seek_confirmation(1, 2, 0, 3);
                break;
            case 2:
                sending_state = 0;
                return sending_response.CONTINUE;
            case 3:
                sending_state = 0;
                return sending_response.CANCEL;
        }
        return sending_response.WAIT;

    }

    private static sending_response send_req(byte var) {
        switch (sending_state) {
            case 0:
                del_array(data_storage, data_storage_size + 2);
                byte[] request_protocol = {0b01000010, var, 0};
                request_protocol[2] = (byte) (request_protocol[0] ^ request_protocol[1]);
                send_msg_stopping(request_protocol);
                status_storage = 0;
                sending_state = 1;
                timer.setTimer(context.getResources().getInteger(R.integer.response_timer));
                break;
            case 1:
                sending_state = seek_confirmation(1, 2, 0, 3);
                break;
            case 2:
                sending_state = 0;
                return sending_response.CONTINUE;
            case 3:
                sending_state = 0;
                return sending_response.CANCEL;
        }
        return sending_response.WAIT;
    }

    private static sending_response send_dat(byte var) {
        byte len = (byte) send_data_storage[var - 1].length;
        switch (sending_state) {
            case 0:
                byte control_byte = 0b00100100;
                control_byte = (byte) ((control_byte ^ (byte) var) ^ (byte) len);
                for (int x = 0; x < len; x++) {
                    control_byte = (byte) (control_byte ^ send_data_storage[var - 1][x]);
                }
                byte[] data_protocol = {0b00100100, var, len};
                send_msg_stopping(data_protocol);
                String p = "";
                for (int x = 0; x < 3; x++)
                    p = p + " " + Integer.toHexString((int) data_protocol[x]);
                Log.d("Data sent", p);
                send_msg_stopping(send_data_storage[var - 1]);
                send_msg_stopping(control_byte);
                sending_state = 1;
                timer.setTimer(context.getResources().getInteger(R.integer.response_timer));
                break;
            case 1:
                sending_state = seek_confirmation(1, 2, 0, 3);
                break;
            case 3:
                Snackbar.make(view, view.getContext().getResources().getString(R.string.error_data), view.getResources().getInteger(R.integer.snackbar_time)).show();
                sending_state = 0;
                status_storage = 0;
                send_data_storage[var - 1] = new byte[1];
                return sending_response.CANCEL;
            case 2:
                sending_state = 0;
                status_storage = 0;
                send_data_storage[var - 1] = new byte[1];
                return sending_response.CONTINUE;
        }
        return sending_response.WAIT;
    }

    public static void send_stat(byte stat) {
        byte status_protocol[] = {0b00011000, stat};
        send_msg_stopping(status_protocol);
    }

    private static int seek_confirmation(int wait_num, int continue_num, int retry_num, int cancel_num) {
        if (Byte.toUnsignedInt((byte) (status_storage & (1 << 7))) > 0) {
            switch ((byte) (status_storage & (~(1 << 7)))) {
                case 0b00000010:
                    status_storage = (byte) (status_storage & (~(1 << 7)));
                    counter = 0;
                    timer.cancel();
                    return continue_num;
                case 0b00000100:
                    status_storage = (byte) (status_storage & (~(1 << 7)));
                    counter = 0;
                    timer.cancel();
                    return cancel_num;
                case 0b00000101:
                    status_storage = (byte) (status_storage & (~(1 << 7)));
                    counter = 0;
                    timer.cancel();
                    return retry_num;
            }
        }
        if (timer.finished()) {
            counter++;
            Log.d("Seek_Confirmation_Counter", Integer.toString(counter));
            if (counter >= context.getResources().getInteger(R.integer.response_retry)) {
                return cancel_num;
            } else {
                return retry_num;
            }
        }
        return wait_num;
    }

    private static boolean wait_for_requested(byte req) {
        if (((data_storage[0] & (1 << 7)) > 0) && (req == (data_storage[0] & (~(1 << 7))))) {
            return true;
        }
        return false;
    }

    private static void stop_cmd() {
        cmd_exe_counter = 0;
        cmd_exe = commandexecution.WAITTOSTART;
    }

    public static void start_listener(Executor inp, Handler inp2) {
        executor = inp;
        mainThreadHandler = inp2;
        if (executor != null) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (Bluetooth.socket_available()) {
                        byte b = 0;
                        while (Bluetooth.is_connected() && Bluetooth.inputstream_available() && Bluetooth.outputstream_available()) {
                            if (Bluetooth.available() > 0) {
                                byte input = Bluetooth.read();
                                Log.d("Input Message", Integer.toHexString(input));
                                Log.d("Message", cmd_state.toString());
                                switch (cmd_state) {
                                    case WAITFORCMD:
                                        last_state = cmd_state;
                                        if (input == 0b10000001)
                                            cmd_state = commandcontroll.READCMD;
                                        else if (input == 0b01000010)
                                            cmd_state = commandcontroll.READREQ;
                                        else if (input == 0b00100100)
                                            cmd_state = commandcontroll.READDATVAR;
                                        else if (input == 0b00011000)
                                            cmd_state = commandcontroll.READSTAT;
                                        control_byte = input;
                                        break;
                                    case READCMD:
                                        cmd = (byte) input;
                                        last_state = cmd_state;
                                        cmd_state = commandcontroll.COMP;
                                        break;
                                    case READREQ:
                                        request_storage = (byte) input;
                                        last_state = cmd_state;
                                        cmd_state = commandcontroll.COMP;
                                        break;
                                    case READDATVAR:
                                        data_storage[0] = (byte) (input & (~(1 << 7)));
                                        last_state = cmd_state;
                                        cmd_state = commandcontroll.READDATLEN;
                                        break;
                                    case READDATLEN:
                                        data_storage[1] = input;
                                        last_state = cmd_state;
                                        cmd_state = commandcontroll.READDAT;
                                        if (data_storage[1] == 0)
                                            cmd_state = commandcontroll.COMP;
                                        break;
                                    case READDAT:
                                        if (data_storage[1] <= data_storage_size)
                                            data_storage[data_storage_counter + 2] = input;
                                        data_storage_counter++;
                                        if (data_storage_counter == (data_storage[1])) {
                                            data_storage_counter = 0;
                                            last_state = cmd_state;
                                            cmd_state = commandcontroll.COMP;
                                        }
                                        break;
                                    case READSTAT:
                                        status_storage = (byte) (input | (1 << 7));
                                        last_state = cmd_state;
                                        cmd_state = commandcontroll.WAITFORCMD;
                                        break;
                                    case COMP:
                                        if (control_byte == input) {
                                            switch (last_state) {
                                                case READCMD:
                                                    if (control_byte != input) {
                                                        send_stat((byte) status_message.MESSAGE_CORRUPTED.num);
                                                    } else {
                                                        send_stat((byte) status_message.RECEIVED.num);
                                                        cmd_exe = commandexecution.EXECMD;
                                                    }
                                                    break;
                                                case READREQ:
                                                    if (control_byte != input) {
                                                        send_stat((byte) status_message.MESSAGE_CORRUPTED.num);
                                                    } else {
                                                        send_stat((byte) status_message.RECEIVED.num);
                                                        request_storage = (byte) (request_storage | (1 << 7));
                                                    }
                                                    break;
                                                case READDAT:
                                                case READDATVAR:
                                                    if (data_storage[1] > data_storage_size) {
                                                        send_stat((byte) status_message.TOO_MUCH_DATA.num);
                                                    } else if (control_byte != input) {
                                                        send_stat((byte) status_message.MESSAGE_CORRUPTED.num);
                                                    } else {
                                                        send_stat((byte) status_message.RECEIVED.num);
                                                        data_storage[0] = (byte) (data_storage[0] | (1 << 7));
                                                    }
                                                    break;
                                            }
                                        }
                                        last_state = cmd_state;
                                        cmd_state = commandcontroll.WAITFORCMD;
                                        break;
                                }
                                if (last_state != commandcontroll.WAITFORCMD)
                                    control_byte = (byte) (control_byte ^ input);
                            }

                            if (Byte.toUnsignedInt((byte) (status_storage & (1 << 7))) > 0) {
                                switch ((byte) (status_storage & (~(1 << 7)))) {
                                    case 6:
                                        Log.d("Door status", "Open");
                                        mainThreadHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (!Singleton.get_door_status())
                                                    HomeFragment.btn_animation(true);
                                            }
                                        });
                                        status_storage = (byte) (status_storage & (~(1 << 7)));
                                        break;
                                    case 7:
                                        Log.d("Door status", "Close");
                                        mainThreadHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (Singleton.get_door_status())
                                                    HomeFragment.btn_animation(false);
                                            }
                                        });
                                        status_storage = (byte) (status_storage & (~(1 << 7)));
                                        break;
                                    case 1:
                                        send_data_storage = new byte[2][];
                                        switch (curCom) {
                                            case 1:
                                                Snackbar.make(view, context.getResources().getString(R.string.password_changed), view.getResources().getInteger(R.integer.snackbar_time)).show();
                                                break;
                                            case 3:
                                                Snackbar.make(view, context.getResources().getString(R.string.config_complete), view.getResources().getInteger(R.integer.snackbar_time)).show();
                                                mainThreadHandler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        SettingsFragment.close_config();
                                                    }
                                                });
                                            default:
                                                break;
                                        }
                                        status_storage = (byte) (status_storage & (~(1 << 7)));
                                        curCom = 0;
                                        break;
                                    case 3:
                                        send_data_storage = new byte[2][];
                                        switch (curCom) {
                                            case 1:
                                            case 2:
                                                Snackbar.make(view, context.getResources().getString(R.string.wrong_password), view.getResources().getInteger(R.integer.snackbar_time)).show();
                                                break;
                                            case 3:
                                                Snackbar.make(view, context.getResources().getString(R.string.wrong_password), view.getResources().getInteger(R.integer.snackbar_time)).show();
                                                mainThreadHandler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        SettingsFragment.close_config();
                                                    }
                                                });
                                                break;
                                            default:
                                                break;
                                        }
                                        status_storage = (byte) (status_storage & (~(1 << 7)));
                                        curCom = 0;
                                        break;
                                    case 8:
                                        switch (curCom) {
                                            case 3:
                                                Snackbar.make(view, context.getResources().getString(R.string.inactivity), view.getResources().getInteger(R.integer.snackbar_time)).show();
                                                mainThreadHandler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        SettingsFragment.close_config();
                                                    }
                                                });
                                                break;
                                            default:
                                                break;
                                        }
                                        curCom = 0;
                                        status_storage = (byte) (status_storage & (~(1 << 7)));
                                        break;
                                    case 9:
                                        switch (curCom) {
                                            case 3:
                                                mainThreadHandler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        SettingsFragment.continue_config();
                                                    }
                                                });
                                                break;
                                        }
                                        status_storage = (byte) (status_storage & (~(1 << 7)));
                                        break;
                                }
                            }
                            if (Byte.toUnsignedInt((byte) (request_storage & (1 << 7))) > 0) {
                                if (send_dat((byte) (request_storage & (~(1 << 7)))) != sending_response.WAIT) {
                                    request_storage = (byte) (request_storage & (~(1 << 7)));
                                }
                            }
                        }
                        mainThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Bluetooth.closeConnection();
                                HomeFragment.btn_animation(false);

                                Snackbar.make(view, view.getResources().getString(R.string.device_disconnected), view.getResources().getInteger(R.integer.snackbar_time)).show();
                            }
                        });
                        Log.d("Connection", "Closed");
                    }
                }
            });
            boolean req_loop = true;
            while (req_loop) {
                switch (send_req((byte) variable.LOCK_STATUS.num)) {
                    case WAIT:
                        break;
                    case CONTINUE:
                        req_loop = false;
                        Bluetooth.setConnection_status(true);
                        break;
                    case CANCEL:
                        Bluetooth.closeConnection();
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        Snackbar.make(view, context.getResources().getString(R.string.not_connected_1) + " " + Bluetooth.getBluetoothDevice().getName() + " " + context.getResources().getString(R.string.not_connected_2) + " " + context.getResources().getString(R.string.not_connected_not_responding), context.getResources().getInteger(R.integer.snackbar_time)).show();
                        req_loop = false;
                        break;
                }
            }
        }
    }
}
