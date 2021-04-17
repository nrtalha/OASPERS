package com.example.firstapp2;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.Provider;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProviderMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Location mLastLocation;
    private LatLng SeekerLocation;
    private DatabaseReference databaseReference;

    LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationClient;


    private LocationListener locationListener;
    private LocationManager locationManager;
    private final long MIN_TIME = 1000;
    private final long MIN_DIST = 5000;

    private EditText editTextLatitude;
    private EditText editTextLongitude;

    private Button mLogout, mRequest, mSettings;

    private Switch mAvailableSwitch;

    private String seekerId= "";

    private Boolean isLoggingOut= false;
    private SupportMapFragment mapFragment;

    private LinearLayout mSeekerInfo;

    private ImageView mSeekerProfileImage;

    private TextView mSeekerName, mSeekerPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_map);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.


        mFusedLocationClient= LocationServices.getFusedLocationProviderClient(this);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);



        mSeekerInfo= (LinearLayout) findViewById(R.id.seekerInfo);
        mSeekerProfileImage= (ImageView) findViewById(R.id.seekerProfileImage);

        mSeekerName= (TextView) findViewById(R.id.seekerName);
        mSeekerPhone= (TextView) findViewById(R.id.seekerPhone);
        mAvailableSwitch= (Switch) findViewById(R.id.availableSwitch);

        mAvailableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    connectProvider();
                }else{
                    disconnectProvider();
                }
            }
        });

        mLogout = (Button) findViewById(R.id.logout);
        mRequest = (Button) findViewById(R.id.mRequest);
        mSettings = (Button) findViewById(R.id.settings);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(ProviderMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProviderMapActivity.this, ProviderSettingsActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
        mRequest.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String userId= FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Provider_Available");
                GeoFire geoFire= new GeoFire(ref);
                geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                SeekerLocation= new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                mMap.addMarker(new MarkerOptions().position(SeekerLocation).title("Seeker is here!").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_seeker)));
                mRequest.setText("Searching for Seekers, Please wait!");
                mSettings.setOnClickListener(v1-> {
                    Intent intent = new Intent(ProviderMapActivity.this, ProviderSettingsActivity.class);
                    startActivity(intent);
                    return;
                });
            }

        });




        databaseReference = FirebaseDatabase.getInstance().getReference("Location_Provider");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    String databaseLatitudeString = dataSnapshot.child("latitude").getValue().toString().substring(1, dataSnapshot.child("latitude").getValue().toString().length()-1);
                    String databaseLongitudeString = dataSnapshot.child("longitude").getValue().toString().substring(1, Objects.requireNonNull(dataSnapshot.child("longitude").getValue()).toString().length()-1);

                    String[] stringLat = databaseLatitudeString.split(", ");
                    Arrays.sort(stringLat);
                    String latitude = stringLat[stringLat.length-1].split("=")[1];

                    String[] stringLong = databaseLongitudeString.split(", ");
                    Arrays.sort(stringLong);
                    String longitude = stringLong[stringLong.length-1].split("=")[1];


                    LatLng latLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));

                    mMap.addMarker(new MarkerOptions().position(latLng).title(latitude + " , " + longitude));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));


                }
                catch (Exception e){
                    e.printStackTrace();
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        getAssignedSeeker();
    }

    private void getAssignedSeeker()
    {
        String providerId= FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedSeekerRef= FirebaseDatabase.getInstance().getReference().child("Users").child("Providers").child(providerId).child("seekerSeekID");

        assignedSeekerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists())
                {
                        seekerId= snapshot.getValue().toString();
                        getAssignedSeekerLocation();
                        getAssignedSeekerInfo();
                }
                else
                {
                    seekerId= "";
                    if(pickupMarker!= null)
                    {
                        pickupMarker.remove();
                    }
                    if (assignedSeekerLocationRefListener!=null)
                    {
                        assignedSeekerLocationRef.removeEventListener(assignedSeekerLocationRefListener);
                    }
                    mSeekerInfo.setVisibility(View.GONE);

                    mSeekerName.setText(" ");
                    mSeekerPhone.setText(" ");
                    mSeekerProfileImage.setImageResource(R.mipmap.ic_default_user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    Marker pickupMarker;
    private  DatabaseReference assignedSeekerLocationRef;
    private ValueEventListener assignedSeekerLocationRefListener;
    private void getAssignedSeekerLocation()
    {

        assignedSeekerLocationRef= FirebaseDatabase.getInstance().getReference().child("Seekers_Request").child("seekerId").child("1");
        assignedSeekerLocationRefListener= assignedSeekerLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && !seekerId.equals(""))
                {
                    List<Object> map= (List<Object>) snapshot.getValue();
                    double locationLat= 0;
                    double locationLng= 1;

                    mRequest.setText("Seeker Found!");

                    if(map.get(0)!=null){
                        locationLat= Double.parseDouble(map.get(1).toString());
                    }
                    if(map.get(1)!=null){
                        locationLat= Double.parseDouble(map.get(1).toString());
                    }

                    LatLng providerLatLng= new LatLng(locationLat,locationLng);

                    pickupMarker= mMap.addMarker(new MarkerOptions().position(providerLatLng).title("Seeker Location!").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_seeker)));

                }
            }



            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getAssignedSeekerInfo(){

        mSeekerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mSeekersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Seekers").child(seekerId);
        mSeekersDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount()>0) {
                    Map<String, Object> map= (Map<String, Object>) snapshot.getValue();

                    if(map.get("name")!=null)
                    {
                        mSeekerName.setText(map.get("name").toString());
                    }

                    if(map.get("phone")!=null)
                    {
                        mSeekerPhone.setText(map.get("phone").toString());

                    }

                    if(map.get("profileImageUrl")!=null)
                    {

                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mSeekerProfileImage);

                    }

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }

        });
    }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            }else{
                checkLocationPermission();
            }
            mMap.setMyLocationEnabled(true);
        }
    }



        LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                for (Location location : locationResult.getLocations()) {
                    mLastLocation = location;

                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("Provider_Available");
                    DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("Provider_Working");
                    GeoFire geoFireAvailable = new GeoFire(refAvailable);
                    GeoFire geoFireWorking = new GeoFire(refWorking);
                    switch (seekerId) {
                        case "":
                            geoFireWorking.removeLocation(userId);
                            geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));

                            break;

                        default:
                            geoFireAvailable.removeLocation(userId);
                            geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));


                            break;

                    }


                }
            }


        };
        private void checkLocationPermission() {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    new AlertDialog.Builder(this)
                            .setTitle("give permission")
                            .setMessage("give permission message")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    ActivityCompat.requestPermissions(ProviderMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                                }
                            })
                            .create()
                            .show();
                }
                else{
                    ActivityCompat.requestPermissions(ProviderMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            switch(requestCode){
                case 1:{
                    if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                            mMap.setMyLocationEnabled(true);
                        }
                    } else{
                        Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                    }
                    break;
                }
            }
        }
    private void connectProvider(){
        checkLocationPermission();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }

    private void disconnectProvider(){
        if(mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
    }



    public void updateButtonOnclick(View view){

        databaseReference.child("latitude").push().setValue(editTextLatitude.getText().toString());
        databaseReference.child("longitude").push().setValue(editTextLongitude.getText().toString());

    }
}
