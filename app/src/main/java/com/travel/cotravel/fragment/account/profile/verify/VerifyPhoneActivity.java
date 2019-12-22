package com.travel.cotravel.fragment.account.profile.verify;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.chaos.view.PinView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.travel.cotravel.BaseActivity;
import com.travel.cotravel.R;
import com.travel.cotravel.fragment.trip.module.User;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.travel.cotravel.Constants.ENABLE_ADMOB;
import static com.travel.cotravel.Constants.UsersInstance;


public class VerifyPhoneActivity extends BaseActivity {


    Button buttonSignIn;
    @BindView(R.id.cl_verify)
    ConstraintLayout clVerify;
    @BindView(R.id.pinView)
    PinView pinView;
    @BindView(R.id.resendCode)
    TextView resendCode;
    @BindView(R.id.tvcountDown)
    TextView tvcountDown;
    private FirebaseAuth mAuth;
    private FirebaseUser fuser;
    private String mVerificationId;

    String mobile, mobileCode;

    AdView mAdmobView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_phone);
        ButterKnife.bind(this);
        //initializing objects
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        mAuth = FirebaseAuth.getInstance();

        Intent intent = getIntent();
        mobile = intent.getStringExtra("mobile");
        mobileCode = intent.getStringExtra("mobileCode");
        sendVerificationCode(mobile, mobileCode);
        startCounter();
        initAdmob();
    }

    private void startCounter() {
        new CountDownTimer(21000, 1000) {

            public void onTick(long millisUntilFinished) {
                tvcountDown.setVisibility(View.VISIBLE);
                resendCode.setVisibility(View.GONE);
                tvcountDown.setText("seconds remaining: " + millisUntilFinished / 1000);
                //here you can have your logic to set text to edittext
            }

            public void onFinish() {
                tvcountDown.setVisibility(View.GONE);
                resendCode.setVisibility(View.VISIBLE);
            }

        }.start();
    }


    protected void initAdmob() {
        MobileAds.initialize(this, getString(R.string.app_id));
        mAdmobView = (AdView) findViewById(R.id.home_admob);
        if (ENABLE_ADMOB) {

            AdRequest.Builder builder = new AdRequest.Builder();
            AdRequest adRequest = builder.build();
            // Start loading the ad in the background.
            mAdmobView.loadAd(adRequest);
            mAdmobView.setVisibility(View.VISIBLE);
        } else {
            mAdmobView.setVisibility(View.GONE);
        }
    }

    private void sendVerificationCode(String mobile, String mobileCode) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                mobileCode + mobile,
                60,
                TimeUnit.SECONDS,
                TaskExecutors.MAIN_THREAD,
                mCallbacks);
    }


    //the callback to detect the verification status
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {

            //Getting the code sent by SMS
            String code = phoneAuthCredential.getSmsCode();

            //sometime the code is not detected automatically
            //in this case the code will be null
            //so user has to manually enter the code
            if (code != null) {
                pinView.setText(code);
                //verifying the code
                verifyVerificationCode(code);
            }
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            Toast.makeText(VerifyPhoneActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);

            //storing the verification id that is sent to the user
            mVerificationId = s;
        }
    };


    private void verifyVerificationCode(String code) {
        //creating the credential
        showProgressDialog();
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);

        signInWithPhoneAuthCredential(credential);
    }

    private void updateUI(FirebaseUser currentUser) {
        if (currentUser != null) {
            retrieveUserDetail(currentUser);

        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        if (fuser != null) {
            Log.i(TAG, "signInWithPhoneAuthCredential: if");
            fuser.linkWithCredential(credential)
                    .addOnCompleteListener(VerifyPhoneActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                //verification successful we will start the profile activity
                                FirebaseUser user = task.getResult().getUser();
                                snackBar(clVerify, user.getPhoneNumber() + " " + user.getUid());
                                setPhoneNumber(fuser.getUid(), mobile, mobileCode);
                                updateUI(user);

                            } else {
                                //verification unsuccessful.. display an error message
                                Log.i(TAG, "signInWithPhoneAuthCredential: if else");
                                String message = "Somthing is wrong, we will fix it soon...";

                                if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                    message = "Invalid code entered...";
                                }
                                snackBar(clVerify, task.getException().getMessage());
                                Log.i(TAG, "onComplete: " + fuser.getUid());
                                updateUI(null);

                            }
                        }
                    });
        } else {
            Log.i(TAG, "signInWithPhoneAuthCredential: else");
            phoneLogin(credential);
        }

    }

    private void registerPhoneNumber(FirebaseUser user) {
        ArrayList<String> travel_with = new ArrayList<>();
        ArrayList<String> looking_for = new ArrayList<>();
        ArrayList<String> range_age = new ArrayList<>();

        travel_with.add("Female");
        travel_with.add("Male");

        range_age.add("18");
        range_age.add("55");

        Log.i(TAG, "registerPhoneNumber: " + user.getUid() + " " + user.getDisplayName() + " " + user.getEmail() + " " + user.getProviderId() + " " + user.getPhoneNumber());

        User userClass = new User(Objects.requireNonNull(user).getUid(), user.getPhoneNumber(), "offline", user.getPhoneNumber(), "", "18", "", user.getProviderId(), "", "", "", "", "", "", travel_with, looking_for, range_age, "", user.getPhoneNumber(), mobile, mobileCode, "", "", 1, false, "");
        UsersInstance.child(Objects.requireNonNull(mAuth.getCurrentUser()).getUid()).setValue(userClass);
    }

    private void phoneLogin(PhoneAuthCredential credential) {
        Log.i(TAG, "phoneLogin: ");
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(VerifyPhoneActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = task.getResult().getUser();
//                                snackBar(clVerify,user.getPhoneNumber()+" "+user.getUid());
                            dismissProgressDialog();

                            UsersInstance.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.exists()) {
                                        registerPhoneNumber(Objects.requireNonNull(user));
                                        updateUI(user);
                                    } else {
                                        updateUI(user);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {


                                }
                            });


                        } else {
                            //verification unsuccessful.. display an error message
                            String message = "Somthing is wrong, we will fix it soon...";

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                message = "Invalid code entered...";
                            }
                            snackBar(clVerify, task.getException().getMessage());
                            dismissProgressDialog();
                            updateUI(null);

                        }
                    }
                });
    }

    @OnClick({R.id.buttonSignIn, R.id.resendCode})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.buttonSignIn:
                String code = pinView.getText().toString().trim();
                if (code.isEmpty() || code.length() < 6) {
                    pinView.setError("Enter valid code");
                    pinView.requestFocus();
                    return;
                }
                //verifying the code entered manually
                verifyVerificationCode(code);
                break;

            case R.id.resendCode:
                sendVerificationCode(mobile, mobileCode);
                startCounter();
                break;
        }

    }

}