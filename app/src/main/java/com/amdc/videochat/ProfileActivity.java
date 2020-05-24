package com.amdc.videochat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {
    private String receiverUserID = "", receiverUserImage = "", receiverUserName = "";
    private ImageView background_profile_view;
    private TextView name_profile;
    private Button add_friend, decline_friend_request;
    private FirebaseAuth mAuth;
    private String senderUserId, currentState = "new";
    private DatabaseReference friendRequestRef, contactsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        senderUserId = mAuth.getCurrentUser().getUid();
        friendRequestRef = FirebaseDatabase.getInstance().getReference().child("Friend Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        receiverUserID = (String) getIntent().getExtras().get("visit_user_id");
        receiverUserImage = (String) getIntent().getExtras().get("profile_image");
        receiverUserName = (String) getIntent().getExtras().get("profile_name");
        background_profile_view = findViewById(R.id.background_profile_view);
        name_profile = findViewById(R.id.name_profile);
        add_friend = findViewById(R.id.add_friend);
        decline_friend_request = findViewById(R.id.decline_friend_request);

        Picasso.get().load(receiverUserImage).into(background_profile_view);
        name_profile.setText(receiverUserName);
        manageClickEvents();
    }

    private void manageClickEvents() {
        friendRequestRef.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(receiverUserID)) {
                    String requestType = (String) dataSnapshot.child(receiverUserID).child("request_type").getValue();
                    if (Objects.equals(requestType, "sent")) {
                        currentState = "request_sent";
                        add_friend.setText("Cancel Friend Request");
                    }
                    else if (Objects.equals(requestType, "received")) {
                        currentState = "request_received";
                        add_friend.setText("Accept Friend Request");
                        decline_friend_request.setVisibility(View.VISIBLE);
                        decline_friend_request.setOnClickListener(v -> CancelFriendRequest());
                    }
                } else {
                    contactsRef.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild(receiverUserID)) {
                                currentState = "friends";
                                add_friend.setText("Delete contact");
                            } else {
                                currentState = "new";
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
        if (senderUserId.equals(receiverUserID)) add_friend.setVisibility(View.GONE);
        else {
            add_friend.setOnClickListener(v -> {
                if (currentState.equals("new")) SendFriendRequest();
                if (currentState.equals("request_sent")) CancelFriendRequest();
                if (currentState.equals("request_received")) AcceptFriendRequest();
//                if (currentState.equals("friends")) {
//                    CancelFriendRequest();
//                }
            });
        }
    }

    @SuppressLint("SetTextI18n")
    private void AcceptFriendRequest() {
        contactsRef.child(senderUserId).child(receiverUserID).child("Contact").setValue("Saved").addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                contactsRef.child(receiverUserID).child(senderUserId).child("Contact").setValue("Saved").addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        friendRequestRef.child(senderUserId).child(receiverUserID).removeValue().addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful()) {
                                friendRequestRef.child(receiverUserID).child(senderUserId).removeValue().addOnCompleteListener(task3 -> {
                                    if (task3.isSuccessful()) {
                                        currentState = "friends";
                                        add_friend.setText("Delete Contact");
                                        decline_friend_request.setVisibility(View.GONE);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void CancelFriendRequest() {
        friendRequestRef.child(senderUserId).child(receiverUserID).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                friendRequestRef.child(receiverUserID).child(senderUserId).removeValue().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        currentState = "new";
                        add_friend.setText("Add Friend");
                    }
                });
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void SendFriendRequest() {
        friendRequestRef.child(senderUserId).child(receiverUserID).child("request_type").setValue("sent").addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                friendRequestRef.child(receiverUserID).child(senderUserId).child("request_type").setValue("received").addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        currentState = "request_sent";
                        add_friend.setText("Cancel Friend Request");
                        Toast.makeText(ProfileActivity.this, "Friend Request Sent.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
