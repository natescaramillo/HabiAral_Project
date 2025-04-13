package com.example.habiaral;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class HomeFragment extends Fragment {

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        LinearLayout btnLesson1 = view.findViewById(R.id.bahagi);
        LinearLayout btnLesson2 = view.findViewById(R.id.komprehensyon);
        LinearLayout btnLesson3 = view.findViewById(R.id.kayarian);
        LinearLayout btnLesson4 = view.findViewById(R.id.palaro);

        btnLesson1.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), Bahagi_ng_kayarian.class)));
        btnLesson2.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), Komprehensyon.class)));
        btnLesson3.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), Kayarian_ng_pangungusap.class)));
        btnLesson4.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), Palaro.class)));

        return view;
    }
}
