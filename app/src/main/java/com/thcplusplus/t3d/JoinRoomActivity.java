package com.thcplusplus.t3d;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class JoinRoomActivity extends AppCompatActivity {

    private ListView mRoomsListView;
    private List<String> mRoomsList;
    private Button mCreateRoomButton;

    public static String sUserName = "", sRoomName = "";
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRoomReference, mRoomsReference;

    public static final String ROOM_NAME_TAG = "roomname", ALL_ROOMS_TAG = "rooms", HOST_TAG = "host", GUEST_TAG = "guest";

    /****************************************** handle login failed error ********************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        setContentView(R.layout.activity_join_room);
        mCreateRoomButton = findViewById(R.id.create_room_button);
        mRoomsListView = findViewById(R.id.rooms_listView);

        mDatabase = FirebaseDatabase.getInstance();

        SharedPreferences preferences = getSharedPreferences(LoginActivity.SHARED_PREFERENCES_TAG, 0);
        sUserName = preferences.getString(LoginActivity.USER_NAME_TAG,"");
        sRoomName = sUserName;

        mRoomsList = new ArrayList<>();
        mCreateRoomButton.setOnClickListener(new View.OnClickListener() {
            // create a room and add yourself as user1
            @Override
            public void onClick(View view) {
                mCreateRoomButton.setText("Creating Room...");
                mCreateRoomButton.setEnabled(false);
                sRoomName = sUserName;
                mRoomReference = mDatabase.getReference(ALL_ROOMS_TAG + "/" + sRoomName + "/user1");
                addRoomEventListener();
                mRoomReference.setValue(sUserName);

            }
        });

        mRoomsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // join the selected room as user2
                sRoomName = mRoomsList.get(position);
                mRoomReference = mDatabase.getReference(ALL_ROOMS_TAG + "/" + sRoomName + "/user2");
                addRoomEventListener();
                mRoomReference.setValue(sUserName);

            }
        });

        addRoomsEventListener();
    }

    private void addRoomEventListener() {
        mRoomReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mCreateRoomButton.setText("Create Room");
                mCreateRoomButton.setEnabled(true);
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra(ROOM_NAME_TAG, sRoomName);
                startActivity(intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                mCreateRoomButton.setText("Create Room");
                mCreateRoomButton.setEnabled(true);
                Toast.makeText(JoinRoomActivity.this, error.getMessage().toString() , Toast.LENGTH_LONG).show();
            }
        });

    }

    private void addRoomsEventListener(){
        // show if new room is available:
        mRoomsReference = mDatabase.getReference(ALL_ROOMS_TAG);
        mRoomsReference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mRoomsList.clear();
                Iterable<DataSnapshot> rooms = dataSnapshot.getChildren();
                for(DataSnapshot snapshot : rooms){
                    mRoomsList.add(snapshot.getKey());

                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(JoinRoomActivity.this, android.R.layout.simple_list_item_1, mRoomsList);
                mRoomsListView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //nothing to do
            }
        });
    }
}