package com.katanemimena.yum.view;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.katanemimena.yum.ClientConnection.MyThread;
import com.katanemimena.yum.R;
import com.katanemimena.yum.ReviewsStoreList;

import org.foodApp.RequestType;
import org.foodApp.Store;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ReviewsActivity extends AppCompatActivity {

    // Δήλωση μεταβλητών για την προβολή λίστας και το adapter
    ListView reviewListView;
    BaseAdapter adapter;
    List<Store> reviewedStores; // Καταστήματα που έχουν αξιολογηθεί
    Handler handler;
    ArrayList<Store> currentstore = new ArrayList<>(); // Καταστήματα που έχουν αξιολογηθεί

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        // Ορισμός toolbar ως action bar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.yumToolbar);
        setSupportActionBar(toolbar);

        // Απόκρυψη τίτλου και back button (το back button θα χειριστεί χειροκίνητα)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Χειρισμός κουμπιού επιστροφής
        ImageButton backBtn = findViewById(R.id.backButton);
        backBtn.setOnClickListener(v -> {
            finish(); // Κλείνει το activity και επιστρέφει στο προηγούμενο
        });
        //  Handler για ολοκλήρωση αγοράς
        handler = new Handler(getMainLooper(), message -> {
            if (message.what == 1) {
                Toast.makeText(this, "Purchase successful!", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        // Σύνδεση ListView με το layout
        reviewListView = findViewById(R.id.review_list_view);
        // Λήψη των αξιολογημένων καταστημάτων από στατική λίστα
        reviewedStores = ReviewsStoreList.reviewedStores;

        // Ορισμός adapter για εμφάνιση των καταστημάτων σε λίστα
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return reviewedStores.size(); // Πόσα στοιχεία έχει η λίστα
            }

            @Override
            public Object getItem(int position) {
                return reviewedStores.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Αν δεν υπάρχει ήδη view για ανακύκλωση, δημιουργείται νέο
                if (convertView == null) {
                    convertView = LayoutInflater.from(ReviewsActivity.this)
                            .inflate(R.layout.list_item, parent, false);
                }

                // Αντιστοίχιση των στοιχείων του layout με μεταβλητές
                TextView storeName = convertView.findViewById(R.id.title);
                TextView storeCategory = convertView.findViewById(R.id.text);
                ImageView logo = convertView.findViewById(R.id.store_logo);

                // Λήψη καταστήματος που θα εμφανιστεί στη λίστα
                Store store = reviewedStores.get(position);

                // Ορισμός κειμένων
                storeName.setText(store.getStoreName());
                storeCategory.setText(store.getFoodCategory());

                // Προσπάθεια φόρτωσης εικόνας από τα assets
                try{
                    InputStream inputStream = getAssets().open(store.getLogo());
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    logo.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();

                    // Αν δεν βρεθεί η εικόνα, ορίζεται εικόνα προεπιλογής
                    logo.setImageResource(R.drawable.yum);
                }

                return convertView;
            }
        };

        // Ορισμός του adapter στη λίστα
        reviewListView.setAdapter(adapter);

        // Όταν ο χρήστης πατήσει σε κάποιο κατάστημα
        reviewListView.setOnItemClickListener((parent, view, position, id) -> {
            Store selectedStore = reviewedStores.get(position);
            currentstore.add(selectedStore);

            showReviewDialog(selectedStore); // Εμφάνιση διαλόγου για αξιολόγηση
        });
    }

    // Μέθοδος για εμφάνιση διαλόγου αξιολόγησης καταστήματος
    private void showReviewDialog(Store store) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_write_review, null);

        // Σύνδεση στοιχείων UI με μεταβλητές
        TextView storeTitle = dialogView.findViewById(R.id.review_store_title);
        EditText reviewInput = dialogView.findViewById(R.id.review_input_text);
        RatingBar ratingBar = dialogView.findViewById(R.id.review_rating_bar);

        storeTitle.setText("Review for " + store.getStoreName());

        // Δημιουργία και εμφάνιση διαλόγου
        new android.app.AlertDialog.Builder(this)
                .setTitle("Write Review") // Τίτλος διαλόγου
                .setView(dialogView) // Το περιεχόμενο του διαλόγου
                .setPositiveButton("Submit", (dialog, which) -> {
                    // Τι γίνεται όταν πατηθεί το κουμπί "Submit"
                    String reviewText = reviewInput.getText().toString().trim();
                    float rating = ratingBar.getRating();

                    // Έλεγχος αν είναι κενή η κριτική ή μηδενική η βαθμολογία
                    if (reviewText.isEmpty() || rating == 0) {
                        Toast.makeText(this, "Please enter a review and rating", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //new MyThread(handler, currentstore, RequestType.UPDATE_STORE, null).start();
                    Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

}
