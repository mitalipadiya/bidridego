package com.bidridego.user;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bidridego.R;
import com.bidridego.models.BidRideLocation;
import com.bidridego.models.Trip;
import com.bidridego.services.TripService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    SupportMapFragment mapFragment;
    Context mThis;
    Activity mActivity;
    private GoogleMap googleMap;
    private MapView mapView;
    private Marker currentLocationMarker = null;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private AlertDialog locationSettingsDialog;
    private Geocoder geocoder;
    private static boolean isFirstLocationUpdate = true;
    private AutoCompleteTextView sourceEditText, destinationEditText;
    private static final String  OPENCAGE_API_KEY  = "ce0b8aa59a7d44ebb30298a06a04cbdc";
    static ArrayAdapter<String> sourceAdapter;
    static ArrayAdapter<String> destinationAdapter;

    private Marker destinationMarker;
    private Polyline routePolyline;
    private EditText date,time, cost, passengers;
    private Switch isCarPool;
    private Button rideNow;
    private RadioGroup rideTypeRadioGroup;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mThis = getContext();
        mActivity = getActivity();
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        sourceEditText = rootView.findViewById(R.id.sourceEditText);
        destinationEditText = rootView.findViewById(R.id.destinationEditText);

        // Set up the adapter for autocomplete suggestions
        sourceAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line);
        destinationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line);

        sourceEditText.setAdapter(sourceAdapter);
        destinationEditText.setAdapter(destinationAdapter);
        date = rootView.findViewById(R.id.date);
        time = rootView.findViewById(R.id.time);
        cost = rootView.findViewById(R.id.cost);
        passengers = rootView.findViewById(R.id.seats);
        isCarPool = rootView.findViewById(R.id.is_car_pool);
        rideTypeRadioGroup = rootView.findViewById(R.id.ride_type_radio_group);
        rideNow = rootView.findViewById(R.id.ride_now);
        rideNow.setEnabled(false);
        Trip trip = new Trip();
        trip.setCarPool(false);
        trip.setPostedBy(FirebaseAuth.getInstance().getCurrentUser().getUid());
        AtomicBoolean isToSet = new AtomicBoolean(false);
        AtomicBoolean isFromSet = new AtomicBoolean(false);

        rideNow.setOnClickListener(v -> {
            TripService.getInstance().saveOrUpdate(trip);
        });
        rideTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton radioButton = rootView.findViewById(checkedId);
            trip.setRideType(String.valueOf(radioButton.getText()));
            isValidTrip(trip);
        });

        date.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(mThis, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    date.setText(String.valueOf(dayOfMonth)+'/'+String.valueOf(month)+'/'+String.valueOf(year));
                    trip.setDate(date.toString());
                    isValidTrip(trip);
                }
            }, 2023, 11, 11);
            dialog.show();
        });

        time.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(mThis, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    String hr="", min="";
                    if(hourOfDay<10)
                        hr = "0";
                    if(minute<10)
                        min = "0";
                    hr += String.valueOf(hourOfDay);
                    min += String.valueOf(minute);
                    time.setText(hr+":"+min);
                    trip.setDate(time.toString());
                    isValidTrip(trip);
                }
            }, 0, 0, true);
            dialog.show();
        });
        isCarPool.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                trip.setCarPool(true);
            } else {
                trip.setCarPool(false);
            }
        });

        // Add a TextChangedListener to fetch suggestions as the user types
        sourceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Fetch suggestions as the user types
                if(charSequence.toString().length() >= 3) {
                    fetchLocationSuggestions(charSequence.toString(), true);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        // Add a TextChangedListener to fetch suggestions as the user types
        destinationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Fetch suggestions as the user types
                if(charSequence.toString().length() >= 3) {
                    fetchLocationSuggestions(charSequence.toString(), false);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        sourceEditText.setOnItemClickListener((parent, view, position, id) -> {
            // Get the selected item from the adapter
            String selectedItem = (String) parent.getItemAtPosition(position);

            // Use the geocoder to get the details (latitude, longitude) for the selected item
            try {
                List<Address> addresses = geocoder.getFromLocationName(selectedItem, 1);
                if (addresses != null && addresses.size() > 0) {
                    Address selectedAddress = addresses.get(0);
                    double selectedLatitude = selectedAddress.getLatitude();
                    double selectedLongitude = selectedAddress.getLongitude();

                    // Now you have the name (selectedItem), latitude, and longitude
                    showToast("Selected Location: " + selectedItem +
                            "\nLatitude: " + selectedLatitude +
                            "\nLongitude: " + selectedLongitude);
                    BidRideLocation from = new BidRideLocation(selectedLatitude, selectedLongitude, selectedItem);
                    trip.setFrom(from);
                    isFromSet.set(true);
                    if(isToSet.get()) trip.setDistance(distance(trip.getTo(), from));
                    isValidTrip(trip);
                    // Update the marker on the map with the selected location
                    updateMarker(new LatLng(selectedLatitude, selectedLongitude), selectedItem, true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        destinationEditText.setOnItemClickListener((parent, view, position, id) -> {
            // Get the selected item from the adapter
            String selectedItem = (String) parent.getItemAtPosition(position);

            // Use the geocoder to get the details (latitude, longitude) for the selected item
            try {
                List<Address> addresses = geocoder.getFromLocationName(selectedItem, 1);
                if (addresses != null && addresses.size() > 0) {
                    Address selectedAddress = addresses.get(0);
                    double selectedLatitude = selectedAddress.getLatitude();
                    double selectedLongitude = selectedAddress.getLongitude();

                    // Now you have the name (selectedItem), latitude, and longitude
                    BidRideLocation to = new BidRideLocation(selectedLatitude, selectedLongitude, selectedItem);
                    trip.setTo(to);
                    isToSet.set(true);
                    if(isFromSet.get()) trip.setDistance(distance(trip.getFrom(), to));
                    isValidTrip(trip);
                    showToast("Selected Location: " + selectedItem +
                            "\nLatitude: " + selectedLatitude +
                            "\nLongitude: " + selectedLongitude);
                    // Update the marker on the map with the selected location
                    updateMarker(new LatLng(selectedLatitude, selectedLongitude), selectedItem, false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

//        cost
        cost.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                trip.setCost(Double.parseDouble(s.toString()));
                isValidTrip(trip);
            }
        });

        passengers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                trip.setPassengers(Integer.parseInt(s.toString()));
                isValidTrip(trip);
            }
        });

        // Add a TextChangedListener to fetch suggestions as the user types
        sourceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Fetch suggestions as the user types
                if(charSequence.toString().length() >= 3) {
                    fetchLocationSuggestions(charSequence.toString(), true);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        // Add a TextChangedListener to fetch suggestions as the user types
        destinationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Fetch suggestions as the user types
                if(charSequence.toString().length() >= 3) {
                    fetchLocationSuggestions(charSequence.toString(), false);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        sourceEditText.setOnItemClickListener((parent, view, position, id) -> {
            // Get the selected item from the adapter
            String selectedItem = (String) parent.getItemAtPosition(position);

            // Use the geocoder to get the details (latitude, longitude) for the selected item
            try {
                List<Address> addresses = geocoder.getFromLocationName(selectedItem, 1);
                if (addresses != null && addresses.size() > 0) {
                    Address selectedAddress = addresses.get(0);
                    double selectedLatitude = selectedAddress.getLatitude();
                    double selectedLongitude = selectedAddress.getLongitude();

                    // Now you have the name (selectedItem), latitude, and longitude
                    showToast("Selected Location: " + selectedItem +
                            "\nLatitude: " + selectedLatitude +
                            "\nLongitude: " + selectedLongitude);
                    // Update the marker on the map with the selected location
                    updateMarker(new LatLng(selectedLatitude, selectedLongitude), selectedItem, true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        destinationEditText.setOnItemClickListener((parent, view, position, id) -> {
            // Get the selected item from the adapter
            String selectedItem = (String) parent.getItemAtPosition(position);

            // Use the geocoder to get the details (latitude, longitude) for the selected item
            try {
                List<Address> addresses = geocoder.getFromLocationName(selectedItem, 1);
                if (addresses != null && addresses.size() > 0) {
                    Address selectedAddress = addresses.get(0);
                    double selectedLatitude = selectedAddress.getLatitude();
                    double selectedLongitude = selectedAddress.getLongitude();

                    // Now you have the name (selectedItem), latitude, and longitude
                    showToast("Selected Location: " + selectedItem +
                            "\nLatitude: " + selectedLatitude +
                            "\nLongitude: " + selectedLongitude);
                    // Update the marker on the map with the selected location
                    updateMarker(new LatLng(selectedLatitude, selectedLongitude), selectedItem, false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        // Check and request location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestLocationPermissions();
        } else {
            // Permissions are already granted for devices below Android 6.0
            initMap();
        }

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return rootView;
    }
    private double distance(BidRideLocation to, BidRideLocation from) {
        double lon1 = Math.toRadians(to.getLng());
        double lon2 = Math.toRadians(from.getLng());

        double lat1 = Math.toRadians(to.getLat());
        double lat2 = Math.toRadians(to.getLng());

        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2),2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Radius of earth in kilometers. Use 3956
        // for miles
        double r = 6371;

        // calculate the result
        return(c * r);
    }

    private boolean isValidTrip(Trip trip){
        boolean result = true;
        if(
                (trip.getCost() < 1) &&
                        (trip.getPassengers() < 1)&&
                        (trip.getDate() == null || trip.getDate().trim().isEmpty())&&
                        (trip.getTime() == null || trip.getTime().trim().isEmpty())&&
                        (trip.getFrom() == null)&&
                        (trip.getTo() == null)&&
                        (trip.getPostedBy() == null || trip.getPostedBy().trim().isEmpty())&&
                        (trip.getRideType() == null || trip.getRideType().trim().isEmpty())

        ) result = false;
//        if(trip.getPassengers() < 1) result = result & false;
//        if(trip.getDate() == null || trip.getDate().trim().isEmpty()) result = result & false;
//        if(trip.getTime() == null || trip.getTime().trim().isEmpty()) result = result & false;
//        if(trip.getFrom() == null) result = result & false;
//        if(trip.getTo() == null) result = result & false;
//        if(trip.getPostedBy() == null || trip.getPostedBy().trim().isEmpty()) result = result & false;
//        if(trip.getRideType() == null || trip.getRideType().trim().isEmpty()) result = result & false;

        if(result){
            rideNow.setEnabled(true);
            rideNow.setAlpha(1f);
        }

        return result;
    }

    private void updateMarker(LatLng latLng, String locationName, boolean isSource) {
        // Remove the previous marker if exists
        if (isSource) {
            if (currentLocationMarker != null) {
                currentLocationMarker.remove();
            }
            // Add a marker for the source (current location)
            currentLocationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(locationName)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        } else {
            if (destinationMarker != null) {
                destinationMarker.remove();
            }
            // Add a marker for the destination
            destinationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(locationName)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
        // Move the camera to the marker's position
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        // Update source marker
        if (currentLocationMarker != null && destinationMarker != null) {
            drawPolyline();
        }
    }
    private void drawPolyline() {
        if (routePolyline != null) {
            routePolyline.remove(); // Remove previous polyline
        }

        // Draw a polyline between source and destination
        LatLng sourceLatLng = currentLocationMarker.getPosition();
        LatLng destinationLatLng = destinationMarker.getPosition();

        PolylineOptions polylineOptions = new PolylineOptions()
                .add(sourceLatLng, destinationLatLng)
                .width(5) // Set the width of the polyline
                .color(ContextCompat.getColor(requireContext(), R.color.teal_200)); // Set the color

        routePolyline = googleMap.addPolyline(polylineOptions);
    }
    private void fetchLocationSuggestions(String input, boolean isSource) {
        // Perform the Geocoding API request in a background thread
        new FetchLocationSuggestionsTask(isSource).execute(input);
    }

    private static class FetchLocationSuggestionsTask extends AsyncTask<String, Void, ArrayList<String>> {
        boolean isSource;
        FetchLocationSuggestionsTask(boolean isSource) {
            this.isSource = isSource;
        }
        @Override
        protected ArrayList<String> doInBackground(String... params) {
            ArrayList<String> suggestions = new ArrayList<>();

            try {
                // Construct the URL for the Geocoding API request
                String apiUrl = "https://api.opencagedata.com/geocode/v1/json" +
                        "?q=" + URLEncoder.encode(params[0], "UTF-8") +
                        "&key=" + OPENCAGE_API_KEY + "&countrycode=ca";

                URL url = new URL(apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    String response = readStream(in);

                    // Parse the response and extract suggestions
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray results = jsonResponse.getJSONArray("results");

                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        String address = result.getString("formatted");
                        Log.i("Mapdata ==>", address);
                        suggestions.add(address);
                    }
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return suggestions;
        }

        @Override
        protected void onPostExecute(ArrayList<String> suggestions) {
            // Update the adapter with the fetched suggestions
            if(isSource) {
                sourceAdapter.clear();
                sourceAdapter.addAll(suggestions);
                sourceAdapter.notifyDataSetChanged();
            } else {
                destinationAdapter.clear();
                destinationAdapter.addAll(suggestions);
                destinationAdapter.notifyDataSetChanged();
            }
        }
    }

    private static String readStream(InputStream is) {
        try (Scanner s = new Scanner(is).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(mThis, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, get the location
            initMap();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get the location
                initMap();
            } else {
                // Permission denied, handle accordingly
            }
        }
    }
    private void initMap() {
        if (mapView != null) {
            mapView.onCreate(null);
            mapView.onResume();
            mapView.getMapAsync(this);
        }
    }
    private void showLocationSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Location Services Disabled")
                .setMessage("Please enable location services to use this feature.")
                .setPositiveButton("Settings", (dialog, which) -> openLocationSettings())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        locationSettingsDialog = builder.create();

        // Show the dialog
        locationSettingsDialog.show();
    }

    private void openLocationSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }
    private void dismissLocationSettingsDialog() {
        // Dismiss the location settings dialog if it is currently showing
        if (locationSettingsDialog != null && locationSettingsDialog.isShowing()) {
            locationSettingsDialog.dismiss();
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Enable location layer (blue dot)
        if (ActivityCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
        // Initialize the geocoder
        geocoder = new Geocoder(getContext(), Locale.getDefault());

        // Set up location updates
        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (isFirstLocationUpdate && currentLocationMarker == null) {
                        // Handle the location change
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        LatLng latLng = new LatLng(latitude, longitude);

                        // Remove the previous marker if exists
                        if (currentLocationMarker != null) {
                            currentLocationMarker.remove();
                        }

                        // Add a marker at the current location
                        currentLocationMarker = googleMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title("Current Location"));

                        // Move the camera to the current location
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        // Display the name of the current location in a toast
                        showLocationName(latitude, longitude);
                        // Remove location updates after the first successful update
                        locationManager.removeUpdates(this);
                        isFirstLocationUpdate = false;
                    }

                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                    dismissLocationSettingsDialog();
                }

                @Override
                public void onProviderDisabled(String provider) {
                    // Location services are disabled, prompt the user to enable them
                    showLocationSettingsDialog();
                }
            });
        }
    }
    private void showLocationName(double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String locationName = address.getAddressLine(0); // You can customize this based on your needs
                showToast("Current Location: " + locationName);
                sourceEditText.setText(locationName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}