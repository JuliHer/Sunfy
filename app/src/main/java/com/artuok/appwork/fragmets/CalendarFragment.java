package com.artuok.appwork.fragmets;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.artuok.appwork.CreateActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.library.WeekView;

public class CalendarFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_calendar, container, false);

        WeekView weekView = root.findViewById(R.id.weekly);

        weekView.setSelectListener((d, h, duration) -> startCreateActivity());

        return root;
    }

    public void startCreateActivity() {
        Intent intent = new Intent(requireActivity(), CreateActivity.class);
        startActivity(intent);
    }

    public void NotifyChanged() {

    }
}