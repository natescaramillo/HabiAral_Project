package com.example.habiaral;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private final Map<Integer, Class<?>> lessonMap = new HashMap<>();

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        lessonMap.put(R.id.bahagi, Bahagi_ng_pananalita.class);
        lessonMap.put(R.id.komprehensyon, Komprehensyon.class);
        lessonMap.put(R.id.kayarian, Kayarian_ng_pangungusap.class);
        lessonMap.put(R.id.palaro, Palaro.class);

        for (Map.Entry<Integer, Class<?>> entry : lessonMap.entrySet()) {
            LinearLayout button = view.findViewById(entry.getKey());
            Class<?> activityClass = entry.getValue();
            if (button != null) {
                button.setOnClickListener(v ->
                        startActivity(new Intent(getActivity(), activityClass)));
            }
        }

        return view;
    }
}