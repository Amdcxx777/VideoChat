package com.amdc.videochat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Objects;

public class CallingActivity extends AppCompatActivity {
    private TextView nameContact;
    private ImageView profileImage, cancelCallBtn, acceptCallBtn;
    private String receiverUserId = "", receiverUserImage = "", receiverUserName = "", senderUserId = "",
            senderUserImage = "", senderUserName = "", checker = "", callingID = "", ringingID = "";
    private DatabaseReference userRef;
    private MediaPlayer mediaPlayer;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        senderUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        receiverUserId = (String) Objects.requireNonNull(getIntent().getExtras()).get("visit_user_id");
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        mediaPlayer = MediaPlayer.create(this, R.raw.ringing);
        nameContact = findViewById(R.id.name_calling);
        profileImage = findViewById(R.id.profile_image_calling);
        acceptCallBtn = findViewById(R.id.make_call);
        cancelCallBtn = findViewById(R.id.cancel_call);
        cancelCallBtn.setOnClickListener(v -> {
            mediaPlayer.stop();
            checker = "clicked";
            cancelCallingUser();
        });
        acceptCallBtn.setOnClickListener(v -> {
            mediaPlayer.stop();
            final HashMap<String, Object> callingPicUpMap = new HashMap<>();
            callingPicUpMap.put("picked", "picked");
            userRef.child(senderUserId).child("Ringing").updateChildren(callingPicUpMap).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Intent intent = new Intent(CallingActivity.this, VideoChatActivity.class);
                    startActivity(intent);
                }
            });
        });
        getAndSetUserProfileInfo();
    }

    private void getAndSetUserProfileInfo() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(receiverUserId).exists()) {
                    receiverUserImage = (String) dataSnapshot.child(receiverUserId).child("image").getValue();
                    receiverUserName = (String) dataSnapshot.child(receiverUserId).child("name").getValue();
                    nameContact.setText(receiverUserName);
                    Picasso.get().load(receiverUserImage).placeholder(R.drawable.profile_image).into(profileImage);
                }
                if (dataSnapshot.child(senderUserId).exists()) {
                    senderUserImage = (String) dataSnapshot.child(senderUserId).child("image").getValue();
                    senderUserName = (String) dataSnapshot.child(senderUserId).child("name").getValue();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mediaPlayer.start();
        userRef.child(receiverUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!checker.equals("clicked") && !dataSnapshot.hasChild("Calling") && !dataSnapshot.hasChild("Ringing")) {
//                    mediaPlayer.start();
                    final HashMap<String, Object> callingInfo = new HashMap<>();
//                    callingInfo.put("uid", senderUserId);
//                    callingInfo.put("name", senderUserName);
//                    callingInfo.put("image", senderUserImage);
                    callingInfo.put("calling", receiverUserId);
                    userRef.child(senderUserId).child("Calling").updateChildren(callingInfo).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            final HashMap<String, Object> ringingInfo = new HashMap<>();
//                                  ringingInfo.put("uid", receiverUserId);
//                                  ringingInfo.put("name", receiverUserName);
//                                  ringingInfo.put("image", receiverUserImage);
                            ringingInfo.put("ringing", senderUserId);
                            userRef.child(receiverUserId).child("Ringing").updateChildren(ringingInfo).addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) Toast.makeText(CallingActivity.this, "Call", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(senderUserId).child("Ringing").exists() && !dataSnapshot.child(senderUserId).child("Calling").exists()) {
                    acceptCallBtn.setVisibility(View.VISIBLE);
                }
                if (dataSnapshot.child(receiverUserId).child("Ringing").hasChild("picked")) {
                    mediaPlayer.stop();
                    Intent intent = new Intent(CallingActivity.this, VideoChatActivity.class);
                    startActivity(intent);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void cancelCallingUser() {
        userRef.child(senderUserId).child("Calling").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (checker.equals("clicked") && dataSnapshot.exists() && dataSnapshot.hasChild("calling")) {
                    callingID = (String) dataSnapshot.child("calling").getValue();
                    userRef.child(Objects.requireNonNull(callingID)).child("Ringing").removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(CallingActivity.this, "Task is Successful", Toast.LENGTH_SHORT).show();
                            userRef.child(senderUserId).child("Calling").removeValue().addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    startActivity(new Intent(CallingActivity.this, RegistrationActivity.class));
                                    finish();
                                }
                            });
                        }
                    });
                } else {
                    startActivity(new Intent(CallingActivity.this, RegistrationActivity.class));
                    finish();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
        userRef.child(senderUserId).child("Ringing").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("ringing")) {
                    ringingID = (String) dataSnapshot.child("ringing").getValue();
                    userRef.child(Objects.requireNonNull(ringingID)).child("Calling").removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            userRef.child(senderUserId).child("Ringing").removeValue().addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    startActivity(new Intent(CallingActivity.this, RegistrationActivity.class));
                                    finish();
                                }
                            });
                        }
                    });
                } else {
                    startActivity(new Intent(CallingActivity.this, RegistrationActivity.class));
                    finish();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }
}
