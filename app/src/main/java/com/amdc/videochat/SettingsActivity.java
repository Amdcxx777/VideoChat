package com.amdc.videochat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
        private Button saveBtn;
        private EditText userNameET, userBioET;
        private ImageView profileImageView;
        private static int GalleryPick = 1;
        private Uri imageUri;
        private StorageReference userProfileImgRef;
        private String downloadUrl, currentUserID;
        private DatabaseReference userRef;
        private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        userProfileImgRef = FirebaseStorage.getInstance().getReference().child("Profile Images");
        currentUserID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        saveBtn = findViewById(R.id.save_settings_btn);
        userNameET = findViewById(R.id.username_settings);
        userBioET = findViewById(R.id.bio_settings);
        profileImageView = findViewById(R.id.settings_profile_image);
        progressDialog = new ProgressDialog(this);

        profileImageView.setOnClickListener(v -> {
            Intent galleryIntent = new Intent();
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, GalleryPick);
        });

        saveBtn.setOnClickListener(v -> saveUserData());
        retrieveUserInfo();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GalleryPick && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
        }
    }

    private void saveUserData() {
        final String getUserName = userNameET.getText().toString();
        final String getUserStatus = userBioET.getText().toString();
        if (imageUri == null) {
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).hasChild("image")) {
                        saveInfoOnlyWithoutImage();
                    } else Toast.makeText(SettingsActivity.this, "Please select image first", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        } else if (getUserName.equals("")) Toast.makeText(this, "userName is mandatory", Toast.LENGTH_SHORT).show();
        else if (getUserStatus.equals("")) Toast.makeText(this, "bio is mandatory", Toast.LENGTH_SHORT).show();
        else {
            progressDialog.setTitle("Account Settings");
            progressDialog.setMessage("Please wait...");
            progressDialog.show();
            final StorageReference filePath = userProfileImgRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            final UploadTask uploadTask = filePath.putFile(imageUri);
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
                downloadUrl = filePath.getDownloadUrl().toString();
                return filePath.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    downloadUrl = task.getResult().toString();
                    HashMap<String, Object> profileMap = new HashMap<>();
                    profileMap.put("uid", currentUserID);
                    profileMap.put("name", getUserName);
                    profileMap.put("status", getUserStatus);
                    profileMap.put("image", downloadUrl);
                    userRef.child(currentUserID).updateChildren(profileMap).addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    Intent intent = new Intent(SettingsActivity.this, ContactsActivity.class);
                                    startActivity(intent);
                                    finish();
                                    progressDialog.dismiss();
                                    Toast.makeText(SettingsActivity.this, "Profile settings has been updated.", Toast.LENGTH_SHORT).show();
                                }
                            });

                }
            });
        }

    }

    private void saveInfoOnlyWithoutImage() {
        final String getUserName = userNameET.getText().toString();
        final String getUserStatus = userBioET.getText().toString();

        if (getUserName.equals("")) Toast.makeText(this, "userName is mandatory", Toast.LENGTH_SHORT).show();
        else if (getUserStatus.equals("")) Toast.makeText(this, "bio is mandatory", Toast.LENGTH_SHORT).show();
        else {
            progressDialog.setTitle("Account Settings");
            progressDialog.setMessage("Please wait...");
            progressDialog.show();
            HashMap<String, Object> profileMap = new HashMap<>();
            profileMap.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
            profileMap.put("name", getUserName);
            profileMap.put("status", getUserStatus);

            userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .updateChildren(profileMap).addOnCompleteListener(task1 -> {
                if (task1.isSuccessful()) {
                    Intent intent = new Intent(SettingsActivity.this, ContactsActivity.class);
                    startActivity(intent);
                    finish();
                    progressDialog.dismiss();
                    Toast.makeText(SettingsActivity.this, "Profile settings has been updated.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void retrieveUserInfo() {
        userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String imageDb = (String) dataSnapshot.child("image").getValue();
                            String nameDb = (String) dataSnapshot.child("name").getValue();
                            String bioDb = (String) dataSnapshot.child("status").getValue();

                            userNameET.setText(nameDb);
                            userBioET.setText(bioDb);
                            try { Picasso.get().load(imageDb).resize(250, 300).placeholder(R.drawable.profile_image).into(profileImageView);
                            } catch (Exception e) { Toast.makeText(SettingsActivity.this, "Error download User-Image", Toast.LENGTH_SHORT).show(); }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }
}
