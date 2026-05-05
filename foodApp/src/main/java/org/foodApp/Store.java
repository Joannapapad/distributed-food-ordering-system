package org.foodApp;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Store implements Serializable {
    @Serial
    private static final long serialVersionUID = 8225427490978088356L;

    @JsonProperty("StoreName")
    private String storeName;

    @JsonProperty("Latitude")
    private double latitude;

    @JsonProperty("Longitude")
    private double longitude;

    @JsonProperty("FoodCategory")
    private String foodCategory;

    @JsonProperty("Stars")
    private double stars;

    @JsonProperty("NoOfVotes")
    private int noOfVotes;

    @JsonProperty("StoreLogo")
    private String storeLogo;

    @JsonProperty("Products")
    private List<Product> products;

    @JsonIgnore
   HashMap<Integer,Product> SalesPerProduct;

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Store() {

    }

    public Store(String storeName, double latitude, double longitude, String foodCategory, double stars, int noOfVotes, String storeLogo) {
        this.storeName = storeName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.foodCategory = foodCategory;
        this.stars = stars;
        this.noOfVotes =noOfVotes;
        this.storeLogo = storeLogo;
        this.products = products;
        this.SalesPerProduct = SalesPerProduct;

    }

    public String getLogo() {
        return "logos/" + storeLogo;
    }

    public String getAddedStoreLogo() {
        return storeLogo;
    }


    public void setLogo(String logo) {
        this.storeLogo = logo;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public void removeProduct(Product product) {
        Iterator<Product> iterator = products.iterator();
        while (iterator.hasNext()) {
            Product p = iterator.next();
            if (Objects.equals(p.getProductName(), product.getProductName())) {
                iterator.remove();
                break;
            }
        }
    }

    public String getStoreName() {
        return storeName;
    }

    public HashMap<Integer,Product > getSalesPerProduct() {
        return SalesPerProduct;
    }

    public void setSalesPerProduct(HashMap<Integer,Product > salesPerProduct) {
        SalesPerProduct = salesPerProduct;
    }

    public void Purchase(Product product, int quantity) {
        if (SalesPerProduct == null) {
            SalesPerProduct = new HashMap<>();
        }
    }
    public int getSalesPerProduct(Product product) {
        for (Product p : products) {
            if (p.equals(product)) {
                return p.getSales();
            }
        }
        return 0;
    }

    public int TotalSales() {
        int totalSales = 0;
        for (Product product : products) {
            totalSales += product.getSales();
        }
        return totalSales;
    }


        public double getStars() {
        return stars;
    }

    public String calculatePriceCategory() {
        double totalPrice = 0;
        for (Product product : products) {
            totalPrice += product.getPrice();
        }
        double avgPrice = products.isEmpty() ? 0 : totalPrice / products.size();

        if (avgPrice <= 5) return "$";
        else if (avgPrice <= 15) return "$$";
        else return "$$$";
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getFoodCategory() {
        return foodCategory;
    }

    public void setFoodCategory(String foodCategory) {
        this.foodCategory = foodCategory;
    }

    public Integer getNoOfVotes() {
        return noOfVotes;
    }

    public void setNoOfVotes(Integer noOfVotes) { this.noOfVotes = noOfVotes;}

}
