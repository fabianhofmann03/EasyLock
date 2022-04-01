package com.example.easylock.ui.home;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.easylock.Background_Listener;
import com.example.easylock.Bluetooth;
import com.example.easylock.R;
import com.example.easylock.Singleton;
import com.example.easylock.databinding.FragmentHomeBinding;
import com.google.android.material.snackbar.Snackbar;

public class HomeFragment extends Fragment {
    public Button btn_open;
    private static ImageView lock_view;
    private static AnimationDrawable lock_gif;
    private FragmentHomeBinding binding;

    public static void btn_animation(boolean new_status) {
        //lock_gif.setSpeed(30);
        if(Singleton.get_door_status() != new_status) {
            Drawable cur_frame = lock_gif.getCurrent();
            int frame = 0;
            for(int x = 0; x < 30; x++) {
                if(lock_gif.getFrame(x) == cur_frame)  {
                    frame = x;
                    break;
                }
            }
            if (Singleton.get_door_status()) {
                lock_view.setBackgroundResource(R.drawable.lock_animation_2);
            } else {
                lock_view.setBackgroundResource(R.drawable.lock_animation_1);
            }
            Singleton.set_door_status(new_status);
            lock_gif = (AnimationDrawable) lock_view.getBackground();
            lock_gif.setOneShot(true);
            lock_gif.stop();
            lock_gif.selectDrawable(15);
            lock_gif.start();
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d("HomeFragment", "Starting standard protocol");
        super.onCreate(savedInstanceState);
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        Log.d("HomeFragment", "Starting personal protocol");

        EditText pwd = (EditText) binding.passwordInputHome;
        btn_open = (Button) binding.btnOpen;
        lock_view = (ImageView) binding.lockView;

        if (Singleton.get_door_status()) {
            lock_view.setBackgroundResource(R.drawable.lock_animation_2);
        } else {
            lock_view.setBackgroundResource(R.drawable.lock_animation_1);
        }
        lock_gif = (AnimationDrawable) lock_view.getBackground();
        lock_gif.setOneShot(true);

        btn_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pwd.getText().toString().length() > 127) {
                    Snackbar.make(view, getResources().getString(R.string.pw_too_long), getResources().getInteger(R.integer.snackbar_time)).show();
                } else {
                    if (bluetooth_check()) {
                        if (Bluetooth.is_connected()) {
                            Background_Listener.set_send_data_storage(Background_Listener.variable.PASSWORD.num, pwd.getText().toString());
                            while (true) {
                                if (Background_Listener.send_cmd((byte) Background_Listener.command.OPEN_LOCK.num) != Background_Listener.sending_response.WAIT)
                                    break;
                            }
                        } else {
                            Snackbar.make(view, getResources().getString(R.string.bluetooth_not_connected), getResources().getInteger(R.integer.snackbar_time)).show();
                        }
                    }
                }
            }
        });

        View root = binding.getRoot();
        return root;
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
            if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}