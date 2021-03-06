package com.example.restauranthealthinspector.activities;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.restauranthealthinspector.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

/**
 *  CustomInfoViewAdapter is a adapter for Google Map info view
 */
public class CustomInfoViewAdapter implements GoogleMap.InfoWindowAdapter {

    private final LayoutInflater mInflater;

    public CustomInfoViewAdapter(LayoutInflater inflater) {
        this.mInflater = inflater;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        @SuppressLint("InflateParams") View view = mInflater.inflate(R.layout.info_window_layout, null);

        TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
        tvTitle.setText(marker.getSnippet());

        return view;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

}