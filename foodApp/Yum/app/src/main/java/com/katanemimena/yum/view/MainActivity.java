package com.katanemimena.yum.view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.katanemimena.yum.ClientConnection.MyThread;
import com.katanemimena.yum.R;

import org.foodApp.Client.Filter;
import org.foodApp.RequestType;
import org.foodApp.Store;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

// Κύρια Activity ησ εφαρμογής.
// Φορτώνει καταστήματα από τον server και τα εμφανίζει σε λίστα.
//Παρέχει αναζήτηση (SearchView) και φίλτρα (διάλογος).
// Διαχειρίζεται navigation προς άλλες οθόνες (StoresActivity, ReviewsActivity).
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    ListView listView;
    Button filterBtn;
    SearchView searchInput;
    ImageButton reviewsButton;

    Handler handler;
    ArrayList<Store> allStores = new ArrayList<>(); // master list from backend
    ArrayList<Store> filteredStores = new ArrayList<>(); // filtered list for adapter

    BaseAdapter adapter; // Adapter για το ListView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.yumToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        // Κουμπί επιστροφής (κλείνει την activity)
        ImageButton backBtn = findViewById(R.id.backButton);
        backBtn.setOnClickListener(v -> {
            finish();
        });

        listView = findViewById(R.id.listView);
        filterBtn = findViewById(R.id.filter_button);
        searchInput = findViewById(R.id.search_view);
        reviewsButton = findViewById(R.id.reviews_button);

        // Handler: λαμβάνει μηνύματα από MyThread και ενημερώνει UI
        handler = new Handler(Looper.getMainLooper(), message -> {
            if (message.what == 1) {
                // Νέα δεδομένα καταστημάτων: ανανέωση λίστας
                filteredStores.clear();
                filteredStores.addAll(allStores);
                adapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "Loaded " + filteredStores.size() + " stores", Toast.LENGTH_SHORT).show();

            }
            return true;
        });

        adapter = new BaseAdapter() {
            @Override
            public int getCount() { return filteredStores.size(); }

            @Override
            public Object getItem(int position) { return filteredStores.get(position); }

            @Override
            public long getItemId(int position) { return position; }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    // Φορτώνουμε το layout του item αν δεν υπάρχει
                    convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.list_item, parent, false);
                }
                TextView title = convertView.findViewById(R.id.title);
                TextView text = convertView.findViewById(R.id.text);
                ImageView logo = convertView.findViewById(R.id.store_logo);

                Store store = filteredStores.get(position);
                title.setText(store.getStoreName());
                text.setText(store.getFoodCategory());

                try{
                    // Φόρτωση logo από assets
                    InputStream inputStream = getAssets().open(store.getLogo());
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    logo.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    logo.setImageResource(R.drawable.yum);
                }

                return convertView;
            }
        };
        listView.setAdapter(adapter);

        // Πάτημα σε κατάστημα, μετάβαση σε StoresActivity
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Store s = filteredStores.get(position);
            Intent intent = new Intent(MainActivity.this, StoresActivity.class);
            intent.putExtra("selectedStore", s);
            startActivity(intent);
        });

        searchInput.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Optional: you can handle submit action here if needed
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                String query = newText.trim().toLowerCase();
                applyLocalFilter(query);
                return true;
            }
        });

        // Κουμπί φίλτρων
        filterBtn.setOnClickListener(v -> showFilterDialog());


        // Κουμπί αξιολογήσεων
        reviewsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReviewsActivity.class);
            startActivity(intent);
        });

        //  Αρχικό φόρτωμα καταστημάτων από backend
        loadAllStoresFromBackend();
    }
//    @Override
//    public boolean onSupportNavigateUp() {
//        finish();
//        return true;
//    }

    //Επικοινωνεί με τον server για να λάβει όλα τα καταστήματα.
    //Αν υπάρχουν συντεταγμένες (lat/lon) τις στέλνει ώστε ο server να επιστρέψει κοντινά.
    private void loadAllStoresFromBackend() {

        double lon = getIntent().getDoubleExtra("longitude", -1);
        double lat = getIntent().getDoubleExtra("latitude", -1);

        Log.d(TAG, "Button clicked. Received latitude: " + lat + ", longitude: " + lon);


        Filter filter = new Filter();

        if(lat!= -1 && lon != -1) {
            filter.setLatitude(lat);
            filter.setLongitude(lon);

            Log.d(TAG, "Filter updated with latitude and longitude");
        } else {
            Log.d(TAG, "No latitude/longitude provided, sending filter without location");
        }
        // Εκκίνηση νήματος για κλήση στον server
        new MyThread(handler,allStores,RequestType.DISPLAY_ALL_STORES,filter).start();


    }

    //Τοπικό φιλτράρισμα λίστας (αναζήτηση) χωρίς νέο αίτημα στον server.
    private void applyLocalFilter(String query) {
        filteredStores.clear();
        if (query.isEmpty()) {
            filteredStores.addAll(allStores);
        } else {
            for (Store store : allStores) {
                if (store.getStoreName().toLowerCase().contains(query)) {
                    filteredStores.add(store);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

   // Dialog φίλτρων όπου ο χρήστης θέτει κριτήρια και στέλνει αίτημα FILTER_STORES.
    private void showFilterDialog() {
        // Φόρτωση layout.
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_filter, null);

        EditText minStarsInput = dialogView.findViewById(R.id.min_stars);
        EditText minVotesInput = dialogView.findViewById(R.id.min_votes);
        EditText storeCategoryInput = dialogView.findViewById(R.id.store_category);
        Spinner priceCategorySpinner = dialogView.findViewById(R.id.price_category);

        // Spinner τιμές τιμολογιακής κατηγορίας
        String[] priceOptions = {"", "$", "$$", "$$$"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priceOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priceCategorySpinner.setAdapter(adapter);

        // Δημιουργία AlertDialog
        new android.app.AlertDialog.Builder(this,R.style.YumAlertDialog)
                .setTitle("Set Filters")
                .setView(dialogView)
                .setPositiveButton("Apply", (dialog, which) -> {
                    Filter filter = new Filter();

                    // Ελάχιστα αστέρια
                    String starsStr = minStarsInput.getText().toString().trim();
                    if (!starsStr.isEmpty()) {
                        try {
                            int stars = Integer.parseInt(starsStr);
                            filter.setMinStars(stars);
                        } catch (NumberFormatException e) {
                            filter.setMinStars(null);
                        }
                    } else {
                        filter.setMinStars(null);
                    }

                    // Ελάχιστες ψήφοι
                    String votesStr = minVotesInput.getText().toString().trim();
                    if (!votesStr.isEmpty()) {
                        try {
                            int votes = Integer.parseInt(votesStr);
                            filter.setMinVotes(votes);
                        } catch (NumberFormatException e) {
                            filter.setMinVotes(null);
                        }
                    } else {
                        filter.setMinVotes(null);
                    }

                    // Τύπος προϊόντος (κατηγορία καταστήματος)
                    String productType = storeCategoryInput.getText().toString().trim();
                    if (!productType.isEmpty()) {
                        filter.setProductType(productType);
                    } else {
                        filter.setProductType(null);
                    }

                    // Τιμολογιακή κατηγορία
                    String priceCat = (String) priceCategorySpinner.getSelectedItem();
                    if (priceCat != null && !priceCat.isEmpty()) {
                        filter.setPriceCategory(priceCat);
                    } else {
                        filter.setPriceCategory(null);
                    }
                    Log.d(TAG, "Applying Filter: " +
                            "minStars=" + filter.getMinStars() + ", " +
                            "minVotes=" + filter.getMinVotes() + ", " +
                            "productType=" + filter.getProductType() + ", " +
                            "priceCategory=" + filter.getPriceCategory());

                    // Αν το φίλτρο είναι κενό, φορτώνουμε ξανά όλα τα καταστήματα
                    boolean isEmptyFilter =
                            starsStr.isEmpty() && votesStr.isEmpty() && productType.isEmpty() &&
                                    (priceCat == null || priceCat.isEmpty());

                    if (isEmptyFilter) {
                        Log.d(TAG, "Filter is empty. Reloading all stores.");
                        new MyThread(handler, allStores, RequestType.DISPLAY_ALL_STORES, null).start();
                    } else {
                        new MyThread(handler, allStores, RequestType.FILTER_STORES, filter).start();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

}
