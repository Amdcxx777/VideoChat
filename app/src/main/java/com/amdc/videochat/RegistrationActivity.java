package com.amdc.videochat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.hbb20.CountryCodePicker;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RegistrationActivity extends AppCompatActivity {
        private CountryCodePicker ccp;
        private EditText phoneText, codeText;
        private Button continueAndNextBtn;
        private String mVerificationId, checker = "", phoneNumber = "";
        private RelativeLayout relativeLayout;
        private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
        private PhoneAuthProvider.ForceResendingToken mResentToken;
        private FirebaseAuth mAuth;
        private ProgressDialog loadingBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);
        phoneText = findViewById(R.id.phoneText);
        codeText = findViewById(R.id.codeText);
        continueAndNextBtn = findViewById(R.id.continueNextButton);
        relativeLayout = findViewById(R.id.phoneAuth);
        ccp = (CountryCodePicker) findViewById(R.id.ccp);
        ccp.registerCarrierNumberEditText(phoneText);

        continueAndNextBtn.setOnClickListener(v -> {
            if (continueAndNextBtn.getText().equals("Submit") || checker.equals("Code Sent")) {
                String verificationCode = codeText.getText().toString();
                if (verificationCode.equals("")) {
                    Toast.makeText(RegistrationActivity.this, "Please write verification code first", Toast.LENGTH_SHORT).show();
                } else {
                    loadingBar.setTitle("Code verification");
                    loadingBar.setMessage("Please wait, while we are verifying you code.");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);
                    signInWithPhoneAuthCredential(credential);
                }
            } else {
                phoneNumber = ccp.getFullNumberWithPlus();
                if (!phoneNumber.equals("")) {
                    loadingBar.setTitle("Phone number verification");
                    loadingBar.setMessage("Please wait, while we are verifying you phone number.");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();
                    // Phone number to verify // Timeout duration // Unit of timeout // Activity (for callback binding) // OnVerificationStateChangedCallbacks
                    PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber, 60, TimeUnit.SECONDS, RegistrationActivity.this, mCallbacks);
                } else Toast.makeText(RegistrationActivity.this, "Please write valid phone number", Toast.LENGTH_SHORT).show();
            }
        });
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(RegistrationActivity.this, "Invalid phone number...", Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
                relativeLayout.setVisibility(View.VISIBLE);
                continueAndNextBtn.setText("Continue");
                codeText.setVisibility(View.GONE);
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                mVerificationId = s;
                mResentToken = forceResendingToken;
                relativeLayout.setVisibility(View.GONE);
                checker = "Code Sent";
                continueAndNextBtn.setText("Submit");
                codeText.setVisibility(View.VISIBLE);
                loadingBar.dismiss();
                Toast.makeText(RegistrationActivity.this, "Code has been sent, please check.", Toast.LENGTH_SHORT).show();
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            Intent homeIntent = new Intent(RegistrationActivity.this, ContactsActivity.class);
            startActivity(homeIntent);
            finish();
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                loadingBar.dismiss();
                Toast.makeText(RegistrationActivity.this, "Congratulation, you are logged in successfully.", Toast.LENGTH_SHORT).show();
                sendToMainActivity();
            } else {
                loadingBar.dismiss();
                String e = Objects.requireNonNull(task.getException()).toString();
                Toast.makeText(RegistrationActivity.this, "Error: " + e, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendToMainActivity() {
        Intent intent = new Intent(RegistrationActivity.this, ContactsActivity.class);
        startActivity(intent);
        finish();
    }
}
