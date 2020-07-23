package com.example.restauranthealthinspector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class ClusterPin implements ClusterItem {
    private final LatLng mPosition;
    private final String mTitle;
    private final String mSnippet;
    private final int mType;

    public ClusterPin(String title, String snippet, LatLng latLng, int type) {
        mPosition = latLng;
        mTitle = title;
        mSnippet = snippet;
        mType = type;
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Nullable
    @Override
    public String getTitle() {
        return mTitle;
    }

    @Nullable
    @Override
    public String getSnippet() {
        return mSnippet;
    }

    public int getType() {
        return mType;
    }
}