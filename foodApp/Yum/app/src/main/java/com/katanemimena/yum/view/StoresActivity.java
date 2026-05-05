package com.katanemimena.yum.view;

import static org.foodApp.RequestType.BUY_PRODUCT;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.katanemimena.yum.ClientConnection.MyThread;
import com.katanemimena.yum.R;
import com.katanemimena.yum.ReviewsStoreList;
import com.katanemimena.yum.adapter.ProductAdapter;

import org.foodApp.Product;
import org.foodApp.RequestType;
import org.foodApp.Store;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

//Activity προβολής ενός επιλεγμένου καταστήματος.
//Εμφανίζει πληροφορίες καταστήματος (λογότυπο, κατηγορία, αστέρια, τιμή).
//Φορτώνει τα προϊόντα σε RecyclerView και επιτρέπει προσθήκη στο καλάθι.
//Παρέχει δυνατότητα checkout με εμφάνιση καλαθιού.
public class StoresActivity extends AppCompatActivity {

    private ImageView storeLogo;
    private final Map<String, Integer> cart = new LinkedHashMap<>();
    private final ArrayList<Store> currentstore = new ArrayList<>();
    private Map<String, Product> productMap = new HashMap<>();
    Handler handler;
    private TextView storeName, storeInfo;
    private RecyclerView productsRecyclerView;

    private ProductAdapter productAdapter;
    private AlertDialog cartDialog;
    private LinearLayout cartLayout;
    private TextView totalTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_stores);

        Toolbar toolbar = findViewById(R.id.yumToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ImageButton backBtn = findViewById(R.id.backButton);
        backBtn.setOnClickListener(v -> finish());

        //  Handler για ολοκλήρωση αγοράς
        handler = new Handler(getMainLooper(), message -> {
            if (message.what == 1) {
                Toast.makeText(this, "Purchase successful!", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // Λήψη του καταστήματος από προηγούμενο Activity
        Store store = (Store) getIntent().getSerializableExtra("selectedStore");
        if (store != null) {
            currentstore.add(store);
            try {
                showStoreInfo(store);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Button checkoutButton = findViewById(R.id.checkoutButton);
        checkoutButton.setOnClickListener(v -> showCartDialog(store, checkoutButton));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Προβάλλει πληροφορίες καταστήματος και φορτώνει προϊόντα στον RecyclerView.
    private void showStoreInfo(Store store) throws IOException {
        storeName = findViewById(R.id.storeName);
        storeInfo = findViewById(R.id.storeInfo);
        storeLogo = findViewById(R.id.storeLogo);
        productsRecyclerView = findViewById(R.id.productsRecyclerView);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        storeName.setText(store.getStoreName());
        storeInfo.setText(store.getFoodCategory() + " | ★" + store.getStars() + " | " + store.calculatePriceCategory());

        // Λογότυπο από assets
        InputStream inputStream = getAssets().open(store.getLogo());
        Bitmap logo = BitmapFactory.decodeStream(inputStream);
        if (logo != null) storeLogo.setImageBitmap(logo);

        // Δημιουργία map προϊόντων για γρήγορο lookup
        for (Product product : store.getProducts()) {
            productMap.put(product.getProductName(), product);
        }

        // Ορισμός adapter ώστε να εμφανίζονται τα προϊόντα
        productAdapter = new ProductAdapter(store.getProducts(), this::showProductDetailsPopup);
        productsRecyclerView.setAdapter(productAdapter);
    }
    //Popup λεπτομερειών προϊόντος: ζητάει ποσότητα και προσθέτει στο καλάθι.
    private void showProductDetailsPopup(Product product) {
        // Πεδίο εισαγωγής ποσότητας
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Quantity");

        TextView titleView = new TextView(this);
        titleView.setText(product.getProductName());
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setPadding(32, 32, 32, 16);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextSize(20);

        new AlertDialog.Builder(this, R.style.YumAlertDialog)
                .setCustomTitle(titleView)
                .setView(input)
                .setPositiveButton("Add to Cart", (dialog, which) -> {
                    int qty;
                    try {
                        qty = Integer.parseInt(input.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Έλεγχοι ποσότητας
                    if (qty < 1) {
                        Toast.makeText(this, "Minimum quantity is 1", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int alreadyInCart = cart.getOrDefault(product.getProductName(), 0);
                    if (alreadyInCart + qty > product.getAvailableAmount()) {
                        Toast.makeText(this, "Not enough stock available!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Προσθήκη / ενημέρωση καλαθιού
                    cart.put(product.getProductName(), alreadyInCart + qty);
                    Toast.makeText(this, "Added " + qty + "x " + product.getProductName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Εμφανίζει dialog  καλαθιού και επιτρέπει την ολοκλήρωση αγοράς.
    private void showCartDialog(Store store, Button checkoutButton) {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Cart is empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Αν ο dialog είναι ήδη ανοιχτός, απλά ανανεώνουμε τα δεδομένα του
        if (cartDialog != null && cartDialog.isShowing()) {
            updateCartLayout();
            return;
        }
        // ScrollView που περιέχει το cartLayout (γραμμές προϊόντων)
        ScrollView scrollView = new ScrollView(this);
        cartLayout = new LinearLayout(this);
        cartLayout.setOrientation(LinearLayout.VERTICAL);
        cartLayout.setPadding(24, 24, 24, 24);
        scrollView.addView(cartLayout);

        // TextView για το σύνολο
        totalTextView = new TextView(this);
        totalTextView.setTextSize(16);
        totalTextView.setPadding(0, 0, 0, 16);
        cartLayout.addView(totalTextView);

        // Φόρτωση πρώτων γραμμών καλαθιού
        updateCartLayout();

        TextView titleView = new TextView(this);
        titleView.setText("Your Cart");
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setPadding(32, 32, 32, 16);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextSize(20);

        // Δημιουργία και εμφάνιση cartDialog
        cartDialog = new AlertDialog.Builder(this, R.style.YumAlertDialog)
                .setCustomTitle(titleView)
                .setView(scrollView)
                .setNegativeButton("Cancel", (dialog, which) -> {
                    cartDialog = null; // Reset dialog reference
                })
                .setPositiveButton("Buy", (d, w) -> {
                    // Στέλνουμε το αίτημα αγοράς
                    new MyThread(handler, currentstore, BUY_PRODUCT, new HashMap<>(cart)).start();
                    // Προσθέτουμε το κατάστημα στη λίστα για αξιολόγηση,αν δεν υπάρχει ήδη
                    if (!ReviewsStoreList.reviewedStores.contains(store)) {
                        ReviewsStoreList.reviewedStores.add(store);
                    }
                    // Μετάβαση σε PurchaseActivity
                    startActivity(new Intent(this, PurchaseActivity.class));
                    cart.clear();
                    cartDialog = null;
                })
                .create();

        cartDialog.show();
    }

    private void updateCartLayout() {
        if (cartLayout == null) return;

        cartLayout.removeViews(1, cartLayout.getChildCount() - 1); // Clear old cart rows but keep totalTextView

        // Υπολογισμός συνολικού κόστους
        double total = 0.0;
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Product p = productMap.get(entry.getKey());
            if (p != null) {
                total += p.getPrice() * entry.getValue();
            }
        }

        totalTextView.setText("Total: " + String.format("%.2f", total) + "$");

        // Δημιουργία γραμμών για κάθε προϊόν στο καλάθι
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String productId = entry.getKey();
            int quantity = entry.getValue();
            Product p = productMap.get(productId);
            if (p == null) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView name = new TextView(this);
            name.setText(quantity + "x " + p.getProductName());
            name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            ImageView delete = new ImageView(this);
            delete.setImageResource(android.R.drawable.ic_menu_delete);
            delete.setColorFilter(ContextCompat.getColor(this, R.color.pink));
            delete.setOnClickListener(v -> {
                // Αφαίρεση προϊόντος από καλάθι
                cart.remove(productId);
                Toast.makeText(this, "Removed " + p.getProductName(), Toast.LENGTH_SHORT).show();
                // Αν άδειασαν όλα, κλείνουμε τον διάλογο
                if (cart.isEmpty()) {
                    cartDialog.dismiss();
                    cartDialog = null;
                } else {
                    updateCartLayout(); // Just refresh the current layout
                }
            });

            row.addView(name);
            row.addView(delete);
            cartLayout.addView(row);
        }
    }

}
