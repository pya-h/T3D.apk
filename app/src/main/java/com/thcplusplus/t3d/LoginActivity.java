package com.thcplusplus.t3d;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {
    private Button mLoginButton;
    private EditText mUserNameEditText;
    public static String sUserName = "";
    private FirebaseDatabase mDatabase;
    private DatabaseReference mUsersReference;

    public static final String SHARED_PREFERENCES_TAG = "LOGIN_PREFS";
    public static final String USER_NAME_TAG = "username", ALL_USERS_TAG = "users";
 /****************************************** handle login failed error ********************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ui implement
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        setContentView(R.layout.activity_login);
        mUserNameEditText = findViewById(R.id.userName_editText);
        mLoginButton = findViewById(R.id.login_button);


        mDatabase = FirebaseDatabase.getInstance();
        // check if user exists in shared preferences to login automatically
        SharedPreferences preferences = getSharedPreferences( SHARED_PREFERENCES_TAG, 0);
        sUserName = preferences.getString(USER_NAME_TAG, "");
        if(!sUserName.equals("")){
            mUsersReference = mDatabase.getReference(ALL_USERS_TAG + "/" + sUserName);
            addEventListener();
            mUsersReference.setValue("");

        }
        // login in manually
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sUserName = mUserNameEditText.getText().toString();
                mUserNameEditText.setText("");
                if(!sUserName.equals("")){
                    mLoginButton.setText("Logging in...");
                    mLoginButton.setEnabled(false);
                    mUsersReference = mDatabase.getReference(ALL_USERS_TAG + "/" + sUserName);
                    addEventListener();
                    mUsersReference.setValue("");

                }
            }
        });
    }

    private void addEventListener() {
        mUsersReference.addValueEventListener(new ValueEventListener() {
            // login success
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!sUserName.equals("")){
                    SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_TAG, 0);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(USER_NAME_TAG, sUserName);
                    editor.apply();


                    startActivity(new Intent(getApplicationContext(), JoinRoomActivity.class));
                    finish();
                }
            }

            // on error
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                mLoginButton.setText("Log in");
                mLoginButton.setEnabled(true);
                Toast.makeText(LoginActivity.this,  error.getMessage().toString() , Toast.LENGTH_LONG).show();
            }
        });
    }
}