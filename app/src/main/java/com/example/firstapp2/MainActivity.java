package com.example.firstapp2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {
    private Button mSeeker, mProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSeeker= (Button) findViewById(R.id.seeker);
        mProvider= (Button) findViewById(R.id.provider);

        startService(new Intent(MainActivity.this, onAppKilled.class));
        mSeeker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(MainActivity.this, SeekerLoginActivity.class);
                startActivity(intent);
                finish();
                return;

            }
        });
        mProvider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(MainActivity.this, ProviderLoginActivity.class);
                startActivity(intent);
                finish();
                return;

            }
        });
    }
}