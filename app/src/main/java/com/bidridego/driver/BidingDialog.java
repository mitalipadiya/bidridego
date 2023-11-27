package com.bidridego.driver;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bidridego.R;
import com.bidridego.models.Bid;
import com.bidridego.models.Trip;
import com.bidridego.services.TripService;
import com.google.firebase.auth.FirebaseAuth;

public class BidingDialog extends DialogFragment {
    private Trip trip;

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = requireContext().getSharedPreferences("BidRigeGo", Context.MODE_PRIVATE);

        // Inflate the custom layout for the dialog
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_bidding, null);

        // Find views in the custom layout
        EditText priceEditText = view.findViewById(R.id.editTextPrice);
        Button sendButton = view.findViewById(R.id.buttonSend);
        priceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if(trip.getMinBid() != null && !s.toString().isEmpty()){
                    if(Double.parseDouble(s.toString()) > trip.getMinBid().getBidValue()) sendButton.setEnabled(true);
                    else sendButton.setEnabled(false);
                }else sendButton.setEnabled(true);
            }
        });

        sendButton.setEnabled(false);
        sendButton.setOnClickListener(v -> {
            Bid bid = new Bid(FirebaseAuth.getInstance().getCurrentUser().getUid(), Double.parseDouble(priceEditText.getText().toString()));
            trip.setMinBid(bid);
            trip.getBids().add(bid);
            TripService.getInstance().saveOrUpdate(trip);
        });

        // Set up the AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Place a Bid")
                .setView(view)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Set up the positive button click listener

        return builder.create();
    }
}