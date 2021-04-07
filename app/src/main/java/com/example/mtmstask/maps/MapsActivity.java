package com.example.mtmstask.maps;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.mtmstask.R;
import com.example.mtmstask.helpers.NativeDrawerHelper;
import com.example.mtmstask.model.Destination;
import com.example.mtmstask.model.Driver;
import com.example.mtmstask.model.Source;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, MapsContract.View {

    private GoogleMap mMap;
    MapsPresenter presenter;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker marker;
    private Location currentLocation;

    @BindView(R.id.source)
    EditText source;
    @BindView(R.id.destination)
    EditText destination;
    @BindView(R.id.drawerRv)
    RecyclerView drawerRv;
    @BindView(R.id.request_rd_btn)
    Button request_rd_btn;
    private Source sourceLocation;
    private Destination destinationLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        ButterKnife.bind(this);

        isConnected();
        new NativeDrawerHelper(this);
        presenter = new MapsPresenter(this);
        getLastLocation();

        source.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showRecyclerView();
                    presenter.getSources();
                }
            }
        });
        destination.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    showRecyclerView();
            }
        });

        destination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                presenter.searchInGoogleMaps(destination.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        presenter.destinationLocations_liveData.observe(this, list -> {
            if (list == null) return;
            drawerRv.setAdapter(new DestinationAdapter(list, destinationLocation -> {
                onItemClicked();
                this.destinationLocation=new Destination(destinationLocation.getFeatureName(), destinationLocation.getLatitude(), destinationLocation.getLongitude());
                destination.setText(destinationLocation.getFeatureName());
                LatLng latLng = new LatLng(destinationLocation.getLatitude(), destinationLocation.getLongitude());
                addMarker(latLng, "destinationLocation");
                flyWithCameraTo(latLng);
            }));
        });

        request_rd_btn.setOnClickListener(v -> {
            if (sourceLocation == null)
                printMsg("You must select source first");
            else
                presenter.getNearestDriver(sourceLocation);
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        showMyLocation();
    }

    private void getLastLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //checking location settings on device
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(new LocationRequest());

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {

            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    requestPermissionsIfNotGranted();
                    return;
                }
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(MapsActivity.this, new OnSuccessListener<android.location.Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    flyWithCameraTo(latLng);

                                    currentLocation = location;
                                }
                            }
                        });
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().

                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MapsActivity.this, 1);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    private void showRecyclerView() {
        drawerRv.setVisibility(View.VISIBLE);
    }

    private void printMsg(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    private void showMyLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // CalendarEvent: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            requestPermissionsIfNotGranted();
            return;
        }
        mMap.setMyLocationEnabled(true); //set mark at your location on the map
    }

    private void flyWithCameraTo(LatLng target) {
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(target, 13, 30, 0))
                , 3000, null);
    }

    private void addMarker(LatLng latLng, String title) {
        removeOldMark();
        marker = mMap.addMarker(new MarkerOptions().position(latLng).title(title));
    }

    private void removeOldMark() {
        if (marker != null)
            marker.remove();
    }

    private void onItemClicked() {
        hideKeyboard(this);
        clearAndHideList();
        clearEditTextsFocus();
    }

    private void clearEditTextsFocus() {
        if (source.hasFocus())
            source.clearFocus();
        if (destination.hasFocus())
            destination.clearFocus();
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onDetectNearestDriver(Driver driver) {
        if (driver == null)
            printMsg("No Near drivers");
        else
            printMsg(driver.name);
    }

    @Override
    public void onSourcesRetrieved(ArrayList<Source> list) {
        drawerRv.setAdapter(new SourceAdapter(list, sourceLocation -> {
            onItemClicked();
            this.sourceLocation = sourceLocation;
            source.setText(sourceLocation.name);
            LatLng latLng = new LatLng(sourceLocation.latitude, sourceLocation.longitude);
            addMarker(latLng, "sourceLocation");
            flyWithCameraTo(latLng);
        }));
    }

    private void clearAndHideList() {
        drawerRv.setAdapter(null);
        drawerRv.setVisibility(View.GONE);
    }

    public void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    protected boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork;
        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();
            boolean conn = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            if (!conn) {
                printMsg("You are offline. app will not working without internet");
            }
            return conn;
        }
        return false;

    }

    private void requestPermissionsIfNotGranted() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Define Needed Permissions for android Marshmallow and higher
            // The request code used in ActivityCompat.requestPermissions()
            // and returned in the Activity's onRequestPermissionsResult()
            int PERMISSION_ALL = 1;
            String[] PERMISSIONS = {
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            };

            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (permissions.length > 0 && permissions[0].equals(android.Manifest.permission.ACCESS_FINE_LOCATION)) {

            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // user rejected the permission

                requestPermissionsIfNotGranted();

            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //user allowed the permission

                getLastLocation();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerRv.getVisibility() == View.VISIBLE) {
            clearAndHideList();
            clearEditTextsFocus();
        } else
            super.onBackPressed();
    }
}