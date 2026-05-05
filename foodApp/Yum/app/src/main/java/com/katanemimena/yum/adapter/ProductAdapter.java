package com.katanemimena.yum.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.katanemimena.yum.R;
import org.foodApp.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    // Δημιουργίασ interface για χειρισμό κλικ σε προίόντα
    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private List<Product> products; // Λίστα με προίοντα
    private OnProductClickListener listener; // Listener για το κλικ

    // Constructor για να περάσουμε τα προιόντα και τον listener
    public ProductAdapter(List<Product> allProducts, OnProductClickListener listener) {
        this.listener = listener;

        // Φτιάχνουμε copy της λίστας, βγάζοντας όσα έχουν amount == 0
        this.products = new ArrayList<>() {};
        for (Product p : allProducts) {
            if (p.getAvailableAmount() > 0) {
                this.products.add(p);
            }
        }
    }

    // ViewHolder για να περάσουμε τα προιοντα και τον listener
    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView nameText, typeText, priceText;

        // ViewHolder για την αναπαράσταση ενός προιόντος στη λίστα
        public ProductViewHolder(View itemView) {
            super(itemView);
            // αντιστοίχιση των views απο το layout
            icon = itemView.findViewById(R.id.productIcon);
            nameText = itemView.findViewById(R.id.productName);
            typeText = itemView.findViewById(R.id.productType);
            priceText = itemView.findViewById(R.id.productPrice);

        }

        // Συνάρτηση για να αντιστοιχίσουμε εικόνες με προιόντα
        public void bind(Product product, OnProductClickListener listener) {
            // Επιλογή εικονιδίου ανάλογα με τον τύπο του προιόντος
            switch (product.getProductType().toLowerCase()) {
                case "pizza":
                    icon.setImageResource(R.drawable.pizza);
                    break;
                case "salad":
                    icon.setImageResource(R.drawable.salad);
                    break;
                case "drink":
                    icon.setImageResource(R.drawable.soft_drink);
                    break;
                case "rice bowl":
                    icon.setImageResource(R.drawable.rice_bowl);
                    break;
                case "pasta":
                    icon.setImageResource(R.drawable.spaguetti);
                    break;
                case "burger":
                    icon.setImageResource(R.drawable.burger);
                    break;
                case "wrap":
                    icon.setImageResource(R.drawable.burrito);
                    break;
                case "bun":
                    icon.setImageResource(R.drawable.tau_sar_bao);
                    break;
                case "sushi":
                    icon.setImageResource(R.drawable.sushi);
                    break;
                case "smoothie":
                    icon.setImageResource(R.drawable.smoothie);
                    break;
                default:
                    icon.setImageResource(R.drawable.cutlery);
            }
            nameText.setText(product.getProductName());
            typeText.setText("Type: " + product.getProductType());
            priceText.setText("Price: " + product.getPrice() + "$");

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });

        }
    }

    // Δημιουργία ενός νέου ViewHolder (καλείται όταν χρειάζεται νέο item)
    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int ViewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    // Σύνδεση  των δεδομένων με το ViewHolder
    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(products.get(position), listener);
    }

    // Επιστροφή του αριθμού των προιόντων
    @Override
    public int getItemCount() {
        return products.size();
    }
}
