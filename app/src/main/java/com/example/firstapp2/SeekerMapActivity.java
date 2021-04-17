package com.example.firstapp2;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SeekerMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Location mLastLocation;
    private DatabaseReference databaseReference;

    private LocationListener locationListener;
    private LocationManager locationManager;
    private final long MIN_TIME = 1000;
    private final long MIN_DIST = 5000;

    private EditText editTextLatitude;
    private EditText editTextLongitude;

    private FusedLocationProviderClient mFusedLocationClient;

    private Button mLogout, mRequest, mSettings;

    private LatLng pickupLocation;

    private Boolean requestBol = false;

    private Marker pickupMarker;

    private String requestService;

    private LinearLayout mProviderInfo;

    private ImageView mProviderProfileImage;

    private TextView mProviderName, mProviderPhone, mProviderLocation;

    private RadioGroup mRadioGroup;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seeker_map2);


         mFusedLocationClient= LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        mProviderInfo = (LinearLayout) findViewById(R.id.providerInfo);
        mProviderProfileImage = (ImageView) findViewById(R.id.providerProfileImage);

        mProviderName = (TextView) findViewById(R.id.providerName);
        mProviderPhone = (TextView) findViewById(R.id.providerPhone);
        mProviderLocation= (TextView)findViewById(R.id.location);

        mRadioGroup= (RadioGroup)findViewById(R.id.radiogroup);
        mRadioGroup.check(R.id.electrician);

        mRequest = (Button) findViewById(R.id.request);
        mLogout = (Button) findViewById(R.id.Logout);
        mSettings = (Button) findViewById(R.id.settings);

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(SeekerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mSettings.setOnClickListener(v1 -> {
            Intent intent = new Intent(SeekerMapActivity.this, SeekerSettingsActivity.class);
            startActivity(intent);
            return;

        });
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (requestBol) {
                    requestBol = false;
                    geoQuery.removeAllListeners();


                    if (providerFoundID != null) {
                        DatabaseReference providerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Providers").child(providerFoundID).child("Seekers_Requests");
                        providerRef.removeValue();
                        providerFoundID = null;
                    }
                    providerFound = false;
                    radius = 1;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Seekers_Request");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(userId);

                    if (pickupMarker != null) {
                        pickupMarker.remove();
                    }
                    mRequest.setText("Call Provider");
                    mProviderInfo.setVisibility(View.GONE);

                    mProviderName.setText(" ");
                    mProviderPhone.setText(" ");


                    mProviderProfileImage.setImageResource(R.mipmap.ic_default_user);
                } else {

                    int selectId= mRadioGroup.getCheckedRadioButtonId();

                    final RadioButton radioButton= (RadioButton) findViewById(selectId);

                    if(radioButton.getText()==null)
                    {
                        return;
                    }

                    requestService= radioButton.getText().toString();

                    requestBol = true;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Seekers_Request");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Provider is here!").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_worker)));

                    mRequest.setText("Finding your provider......");
                    getClosestProvider();
                }



            }


            private int radius = 1;
            private boolean providerFound = false;
            private String providerFoundID;

            GeoQuery geoQuery;

            private void getClosestProvider() {

                DatabaseReference providerLocation = FirebaseDatabase.getInstance().getReference().child("Provider_Available");
                GeoFire geoFire = new GeoFire((providerLocation));

                geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
                geoQuery.removeAllListeners();


                geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                    @Override
                    public void onKeyEntered(String key, GeoLocation location) {
                        if (!providerFound && requestBol) {

                            DatabaseReference mSeekersDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child("Providers").child(key);

                            mSeekersDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                    if(snapshot.exists() && snapshot.getChildrenCount()>0) {
                                        Map<String, Object> providerMap = (Map<String, Object>) snapshot.getValue();

                                        if (providerFound) {
                                            return;
                                        }


                                        if (providerMap != null && Objects.equals(providerMap.get("service"), requestService)) {

                                            providerFound = true;
                                            providerFoundID = key;

                                            DatabaseReference providerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Providers").child(providerFoundID).child("Seekers_Request");
                                            String seekerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                            HashMap map = new HashMap();
                                            map.put("seekerSeekID", seekerId);
                                            providerRef.updateChildren(map);

                                            getProviderLocation();
                                            getProviderInfo();
                                            mRequest.setText("Looking for Provider.....");
                                            mRequest.setText("Provider Found");
                                        }
                                    }


                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                        }
                    }


                    @Override
                    public void onKeyExited(String key) {

                    }

                    @Override
                    public void onKeyMoved(String key, GeoLocation location) {

                    }

                    @Override
                    public void onGeoQueryReady() {
                        if (!providerFound) {
                            radius++;
                            getClosestProvider();
                        }

                    }

                    @Override
                    public void onGeoQueryError(DatabaseError error) {

                    }
                });
            }

            private void getProviderInfo() {

                mProviderInfo.setVisibility(View.VISIBLE);
                DatabaseReference mSeekersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Providers").child(providerFoundID);
                mSeekersDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                            Map<String, Object> map = (Map<String, Object>) snapshot.getValue();

                            if (map.get("name") != null) {
                                mProviderName.setText(map.get("name").toString());
                            }

                            if (map.get("phone") != null) {
                                mProviderPhone.setText(map.get("phone").toString());

                            }

                            if (map.get("profileImageUrl") != null) {

                                Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mProviderProfileImage);

                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }

                });
            }

            private Marker mProviderMarker;
            private DatabaseReference providerLocationRef;
            private ValueEventListener providerLocationRefListener;

            private void getProviderLocation() {
                providerLocationRef = FirebaseDatabase.getInstance().getReference().child("Provider_Working").child(providerFoundID).child("1");
                providerLocationRefListener = providerLocationRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot datasnapshot) {

                        if (datasnapshot.exists() && requestBol) {

                            List<Object> map = (List<Object>) datasnapshot.getValue();

                            double locationLat = 0;
                            double locationLng = 0;

                            mRequest.setText("Provider Found!");

                            if (map.get(0) != null) {
                                locationLat = Double.parseDouble(map.get(0).toString());
                            }
                            if (map.get(1) != null) {
                                locationLat = Double.parseDouble(map.get(1).toString());
                            }

                            LatLng providerLatLng = new LatLng(locationLat, locationLng);
                            if (mProviderMarker != null) {
                                mProviderMarker.remove();
                            }
                            Location loc1 = new Location("");
                            loc1.setLatitude(pickupLocation.latitude);
                            loc1.setLongitude(pickupLocation.longitude);

                            Location loc2 = new Location("");
                            loc2.setLatitude(providerLatLng.latitude);
                            loc2.setLongitude(providerLatLng.longitude);


                            float distance = loc1.distanceTo(loc2);

                            if (distance < 100) {
                                mRequest.setText("Provider is here!");
                            } else {
                                mRequest.setText("Provider is here! : " + String.valueOf(distance));
                            }


                            mProviderMarker = mMap.addMarker((new MarkerOptions().position(providerLatLng).title("Your Provider is here!").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_worker))));

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

        });


        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PackageManager.PERMISSION_GRANTED);


        databaseReference = FirebaseDatabase.getInstance().getReference("Location_Seeker");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    String databaseLatitudeString = dataSnapshot.child("latitude").getValue().toString().substring(1, dataSnapshot.child("latitude").getValue().toString().length() - 1);
                    String databaseLongitudeString = dataSnapshot.child("longitude").getValue().toString().substring(1, dataSnapshot.child("longitude").getValue().toString().length() - 1);

                    String[] stringLat = databaseLatitudeString.split(", ");
                    Arrays.sort(stringLat);
                    String latitude = stringLat[stringLat.length - 1].split("=")[1];

                    String[] stringLong = databaseLongitudeString.split(", ");
                    Arrays.sort(stringLong);
                    String longitude = stringLong[stringLong.length - 1].split("=")[1];


                    LatLng latLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));

                    mMap.addMarker(new MarkerOptions().position(latLng).title(latitude + " , " + longitude));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));


                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        locationListener = new LocationListener()  {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                mLastLocation = location;
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
            }

        };

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions

            return;
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DIST, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, locationListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}




