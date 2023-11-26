package com.bidridego.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bidridego.BidDetails;
import com.bidridego.R;
import com.bidridego.models.BidRideLocation;
import com.bidridego.models.Trip;
import com.bidridego.viewadapter.ArrayTripAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class DriverTripsListFragment  extends Fragment {

    private RecyclerView recyclerView;
    private ArrayTripAdapter adapter;
    public ArrayList<Trip> tripArrayList;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReferenceToTrips;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recyclerview_list, container, false);
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReferenceToTrips = firebaseDatabase.getReference("trips");
        // Initialize RecyclerView
        recyclerView = rootView.findViewById(R.id.recycler_id);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        tripArrayList = new ArrayList <>();
        // Initialize Adapter
        adapter = new ArrayTripAdapter(R.layout.trip_list_item, tripArrayList, getContext());
        recyclerView.setAdapter(adapter);
        tripArrayList.add(new Trip("id", 0, null, null, 0, "postedBy", 1, "date", "time", true, "rideType"));
        adapter.notifyDataSetChanged();

        adapter.setOnItemClickListener(new ArrayTripAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // You can use the 'position' parameter to get the clicked item position

//                Bundle bundle

                startActivity(new Intent(getActivity(), BidDetails.class));
            }
        });
        return rootView;
    }
}