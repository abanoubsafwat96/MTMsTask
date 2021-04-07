package com.example.mtmstask.maps;

import android.content.Context;

import com.example.mtmstask.model.Driver;
import com.example.mtmstask.model.Source;

import java.util.ArrayList;


public interface MapsContract {

    interface View {

        void onSourcesRetrieved(ArrayList<Source> list);

        Context getContext();

        void onDetectNearestDriver(Driver driver);
    }

    interface Presenter {

    }
}