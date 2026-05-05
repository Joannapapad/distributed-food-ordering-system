package com.katanemimena.yum.view;

import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.katanemimena.yum.R;

import org.foodApp.Store;

import java.util.ArrayList;

public class LocationActivity extends AppCompatActivity {

    private static final String TAG = "LocationActivity";

    // Ορισμός των views
    private EditText longitude;
    private EditText latitude;
    private Button finishButton;

    ArrayList<Store> stores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location);
        // Αντιμετώπιση περιθωρίων από status/ nav bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Αντιστοίχιση μεταβλητών με τα στοιχεία UI από το layout
        latitude = findViewById(R.id.latitude_txt);
        longitude = findViewById(R.id.longtitude_txt);
        finishButton = findViewById(R.id.lat_long_btn);

        // Ορισμός του click listener στο κουμπί
        finishButton.setOnClickListener(v-> {
            // Ανάγνωση των τιμών από τα πεδία εισαγωγής
            String latText = latitude.getText().toString();
            String lonText = longitude.getText().toString();

            Log.d(TAG, "Finish button clicked with lat: " + latText + ", lon: " + lonText);

            // Έλεγχος για κενά πεδία
            if(latText.isEmpty()|| lonText.isEmpty()) {
                Toast.makeText(this,"Please enter both" ,Toast.LENGTH_SHORT).show();
            }

            try {
                // Μετατροπή των τιμών από String σε double
                double lat = Double.parseDouble(latText);
                double lon = Double.parseDouble(lonText);

                Log.d(TAG, "Parsed latitude: " + lat + ", longitude: " + lon);

                // Δημιουργία Intent για να ξεκινήσει η MainActivity και μεταφορά των τιμών
                Intent intent = new Intent(LocationActivity.this, MainActivity.class);
                intent.putExtra("longitude", lon);
                intent.putExtra("latitude", lat);

                startActivity(intent); // Εκκίνηση της νέας δραστηριότητας

            }catch (NumberFormatException e) {
                // Αν αποτύχει η μετατροπή σε αριθμό, εμφάνιση μηνύματος λάθους
                Toast.makeText(this, "Invalid latitude or longitude format", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to parse latitude or longitude", e);
            }

        });
    }
}