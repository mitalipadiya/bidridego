package com.bidridego.user;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.bidridego.R;
import com.bidridego.driver.BidingDialog;
import com.bidridego.models.Trip;
import com.bidridego.viewadapter.UserUpcomingTripAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 */
public class FragmentUpcomingTrips extends Fragment {

    private RecyclerView recyclerView;
    private UserUpcomingTripAdapter adapter;
    public ArrayList<Trip> tripArrayList;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReferenceToTrips;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.user_fragment_upcoming_trips_list, container, false);
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReferenceToTrips = firebaseDatabase.getReference("trips");
        // Initialize RecyclerView
        recyclerView = rootView.findViewById(R.id.user_upcoming_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        tripArrayList = new ArrayList <>();
        // Initialize Adapter
        adapter = new UserUpcomingTripAdapter(R.layout.user_fragment_upcoming_trips, tripArrayList, getContext());
        recyclerView.setAdapter(adapter);
        databaseReferenceToTrips.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                snapshot.getChildren().forEach(e->{
                    tripArrayList.add(e.getValue(Trip.class));
                });
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        // Populate your dataset and update the adapter as needed

        adapter.setOnItemClickListener(new UserUpcomingTripAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Trip trip = tripArrayList.get(position);
                BidingDialog biddingDialog = new BidingDialog();
                biddingDialog.setTrip(trip);
                biddingDialog.show(getActivity().getSupportFragmentManager(), "BidingDialogTag");
//                startActivity(new Intent(getActivity(), BidDetails.class).putExtras(bundle));
            }
        });
        return rootView;
    }

}