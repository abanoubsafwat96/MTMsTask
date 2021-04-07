package com.example.mtmstask.maps;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;

import com.example.mtmstask.model.Driver;
import com.example.mtmstask.model.Source;
import com.example.mtmstask.utils.AppExecutors;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapsPresenter implements MapsContract.Presenter {

    private String TAG = "Tag";
    MapsContract.View view;
    MutableLiveData<List<Address>> destinationLocations_liveData;

    public MapsPresenter(MapsContract.View view) {
        this.view = view;
        destinationLocations_liveData = new MutableLiveData<>();
    }

    public void addDocument(String collectionName, Object data) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(collectionName)
                .add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }

    public void getSources() {
        final ArrayList<Source> list = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Source")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map<String, Object> data = document.getData();
                                Source source = new Source(data.get("name").toString(), (double) data.get("latitude"), (double) data.get("longitude"));
                                list.add(source);
                            }
                            view.onSourcesRetrieved(list);
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    public void getNearestDriver(Source sourceLocation) {
        final ArrayList<Driver> list = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Drivers")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map<String, Object> data = document.getData();
                                Driver driver = new Driver(data.get("name").toString(), (double) data.get("latitude"), (double) data.get("longitude"));
                                list.add(driver);
                            }
                            Driver nearestDriver=detectNearestDriver(sourceLocation, list);
                            view.onDetectNearestDriver(nearestDriver);
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private Driver detectNearestDriver(Source sourceLocation, ArrayList<Driver> list) {
        if (list.size() == 0) return null;
        int nearestDriverPos = 0;
        Driver firstDriver = list.get(0);
        double nearestDistance = getDistance(sourceLocation.latitude, sourceLocation.longitude, firstDriver.latitude, firstDriver.longitude);

        for (int i = 1; i < list.size(); i++) {
            Driver driver = list.get(i);
            float distance = getDistance(sourceLocation.latitude, sourceLocation.longitude, driver.latitude, driver.longitude);

            if (distance < nearestDistance) {
                nearestDriverPos = i;
                nearestDistance=distance;
            }
        }
        return list.get(nearestDriverPos);
    }

    public void searchInGoogleMaps(String txt) {
        Geocoder geocoder = new Geocoder(view.getContext());
        getExecutor(() -> {
            List<Address> locationNames = new ArrayList<>();
            try {
                locationNames = geocoder.getFromLocationName(txt, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            destinationLocations_liveData.postValue(locationNames);
        });
    }

    public AppExecutors getExecutor(Runnable runnable) {
        AppExecutors appExecutors = new AppExecutors();
        appExecutors.diskIO().execute(runnable);
        return appExecutors;
    }

    public float getDistance(double firstLatitude, double firstLongitude, double secondLatitude, double secondLongitude) {
        float[] results = new float[1];
        Location.distanceBetween(firstLatitude, firstLongitude, secondLatitude, secondLongitude, results);
        float distanceInMeters = results[0];

        return distanceInMeters;
    }
}
