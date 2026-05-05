package com.katanemimena.yum.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.katanemimena.yum.R;

public class PurchaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Σύνδεση του activity με το αντίστοιχο layout XML
        setContentView(R.layout.activity_purchase);

        // Εύρεση του κουμπιού από το layout
        Button button = findViewById(R.id.button);

        // Ορισμός λειτουργίας όταν πατηθεί το κουμπί
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Δημιουργία Intent για επιστροφή στο MainActivity
                Intent intent = new Intent(PurchaseActivity.this, MainActivity.class);
                // Εκκίνηση του MainActivity
                startActivity(intent);
                finish();
            }
        });
    }
}
