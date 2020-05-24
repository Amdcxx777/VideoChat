package com.amdc.videochat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Objects;


public class NotificationsActivity extends AppCompatActivity {
    private RecyclerView notifications_list;
    private DatabaseReference friendRequestRef, contactsRef, usersRef;
    private String currentUserId;
    private String listUserId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        friendRequestRef = FirebaseDatabase.getInstance().getReference().child("Friend Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        notifications_list = findViewById(R.id.notifications_list);
        notifications_list.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseRecyclerOptions options = new FirebaseRecyclerOptions.Builder<Contacts>().setQuery(friendRequestRef.child(currentUserId), Contacts.class).build();
        FirebaseRecyclerAdapter<Contacts, NotificationViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Contacts, NotificationViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull NotificationViewHolder holder, int i, @NonNull Contacts contacts) {
                holder.acceptBtn.setVisibility(View.VISIBLE);
                holder.cancelBtn.setVisibility(View.VISIBLE);
                listUserId = getRef(i).getKey();
                DatabaseReference requestTypeRef = getRef(i).child("request_type").getRef();
                requestTypeRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String type = (String) dataSnapshot.getValue();
                            if (Objects.equals(type, "received")) {
                                holder.cardView.setVisibility(View.VISIBLE);
                                usersRef.child(Objects.requireNonNull(listUserId)).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.hasChild("image")) {
                                            final String imageStr = (String) dataSnapshot.child("image").getValue();
                                            Picasso.get().load(imageStr).resize(300, 300).into(holder.profileImageView);
                                        }
                                        final String nameStr = (String) dataSnapshot.child("name").getValue();
                                        holder.userNameTxt.setText(nameStr);
                                        holder.acceptBtn.setOnClickListener(v -> AcceptFriendRequest());
                                        holder.cancelBtn.setOnClickListener(v -> CancelFriendRequest());
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                                });
                            } else {
                                holder.cardView.setVisibility(View.GONE);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                });
            }
            @NonNull
            @Override
            public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.find_friend_design, parent, false);
                return new NotificationViewHolder(view);
            }
        };
        notifications_list.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTxt;
        Button acceptBtn, cancelBtn;
        ImageView profileImageView;
        RelativeLayout cardView;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameTxt = itemView.findViewById(R.id.name_notification);
            acceptBtn = itemView.findViewById(R.id.request_accept_btn);
            cancelBtn = itemView.findViewById(R.id.request_decline_btn);
            profileImageView = itemView.findViewById(R.id.image_notification);
            cardView = itemView.findViewById(R.id.card_view);
        }
    }

    private void AcceptFriendRequest() {
        contactsRef.child(currentUserId).child(listUserId).child("Contact").setValue("Saved").addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                contactsRef.child(listUserId).child(currentUserId).child("Contact").setValue("Saved").addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        friendRequestRef.child(currentUserId).child(listUserId).removeValue().addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful()) {
                                friendRequestRef.child(listUserId).child(currentUserId).removeValue().addOnCompleteListener(task3 -> {
                                    if (task3.isSuccessful()) {
                                        Toast.makeText(NotificationsActivity.this, "New Contact Saved.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void CancelFriendRequest() {
        friendRequestRef.child(currentUserId).child(listUserId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                friendRequestRef.child(listUserId).child(currentUserId).removeValue().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Toast.makeText(NotificationsActivity.this, "Friend Request Cancelled", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
