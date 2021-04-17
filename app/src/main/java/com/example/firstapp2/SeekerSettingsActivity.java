package com.example.firstapp2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SeekerSettingsActivity extends AppCompatActivity {
private EditText mNameField, mPhoneField, mAddressField;

private Button mBack, mConfirm;

private ImageView mProfileImage;

private FirebaseAuth mAuth;
private DatabaseReference mSeekersDatabase;

private String userID;
private String mName;
private String mPhone;
private String mAddress;
private String mProfileUrl;

private Uri resultUri;
    private Object Bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seeker_settings);

        mNameField = (EditText) findViewById(R.id.name);
        mPhoneField = (EditText) findViewById(R.id.phone);
        mAddressField = (EditText) findViewById(R.id.address);

        mProfileImage= (ImageView) findViewById (R.id.profileimage);

        mBack = (Button) findViewById(R.id.back);
        mConfirm = (Button) findViewById(R.id.confirm);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        mSeekersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Seekers").child(userID);

        getUserInfo();

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(Intent.ACTION_PICK);
                intent.setType("Image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent,1);

            }
        });

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

               saveUserInformation();
                finish();
                return;
            }

        });

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });
    }

        private void getUserInfo(){
            mSeekersDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.getChildrenCount()>0) {
                        Map<String, Object> map= (Map<String, Object>) snapshot.getValue();

                        if(map.get("name")!=null)
                        {
                            mName= map.get("name").toString();
                            mNameField.setText(mName);
                        }

                        if(map.get("phone")!=null)
                        {
                            mPhone= map.get("phone").toString();
                            mPhoneField.setText(mPhone);

                        }

                        if(map.get("address")!=null)
                        {
                            mAddress= map.get("address").toString();
                            mAddressField.setText(mAddress);

                        }
                        if(map.get("profileImageUrl")!=null)
                        {
                            mProfileUrl= map.get("profileImageUrl").toString();
                            Glide.with(getApplication()).load(mProfileUrl).into(mProfileImage);

                        }

                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }

            });
        }

    private void saveUserInformation()  {

        mName= mNameField.getText().toString();
        mPhone= mPhoneField.getText().toString();
        mAddress= mAddressField.getText().toString();

        Map userInfo= new HashMap();
        userInfo.put("name", mName);
        userInfo.put("phone", mPhone);
        userInfo.put("address", mAddress);

        mSeekersDatabase.updateChildren(userInfo);

        if(resultUri!= null)
        {
            StorageReference filePath= FirebaseStorage.getInstance().getReference().child("profile_image").child(userID);
            Bitmap bitmap= null;

            try {
                bitmap= MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            ByteArrayOutputStream baos= new ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 20,baos);

            byte[] data= baos.toByteArray();
            UploadTask uploadTask= filePath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    finish();
                    return;
                }
            });
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> downloadUrl = taskSnapshot.getStorage().getDownloadUrl();
                    Map newImage= new HashMap();
                    newImage.put("profileImageUrl", downloadUrl.toString());
                    mSeekersDatabase.updateChildren(newImage);

                    finish();
                    return;
                }
            });
        } else {

            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1 && resultCode== Activity.RESULT_OK)
        {
            final Uri imageUri= data.getData();
            resultUri= imageUri;
            mProfileImage.setImageURI(resultUri);
        }
    }
}