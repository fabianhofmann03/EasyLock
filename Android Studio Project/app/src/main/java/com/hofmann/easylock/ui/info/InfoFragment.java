package com.hofmann.easylock.ui.info;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.hofmann.easylock.R;
import com.hofmann.easylock.databinding.FragmentInfoBinding;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfoFragment extends Fragment {

    private FragmentInfoBinding binding;
    TextView infotext;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        InfoViewModel galleryViewModel =
                new ViewModelProvider(this).get(InfoViewModel.class);

        binding = FragmentInfoBinding.inflate(inflater, container, false);

        infotext = binding.infoText;
        String formattedText = getResources().getString(R.string.info).replaceAll("\n", "\n\n");
        SpannableString spannableString = new SpannableString(formattedText);

        Matcher matcher = Pattern.compile("\n\n").matcher(formattedText);
        while (matcher.find()) {
            spannableString.setSpan(new AbsoluteSizeSpan(15, true), matcher.start() + 1, matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        infotext.setText(spannableString);
        infotext.setMovementMethod(new ScrollingMovementMethod());

        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}