package org.foodApp.Client;

import org.foodApp.Product;
import org.foodApp.Store;

import java.io.Serializable;

// χρήση φίλτρων κατά τη χρήση της εφαρμογής
public class Filter implements Serializable{
    private static final long serialVersionUID = 6780596491576307888L;
    private String storeName;
    private Double latitude;
    private Double longitude;
    private String foodCategory;
    private Integer minStars;
    private Integer minVotes;
    private String productType;
    private String priceCategory;

    public Filter() {}

    public Filter(String storeName, Double latitude, Double longitude, String foodCategory, Integer minStars, Integer minVotes, String productType, String priceCategory) {
        this.storeName = storeName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.foodCategory = foodCategory;
        this.minStars = minStars;
        this.minVotes = minVotes;
        this.productType = productType;
        this.priceCategory = priceCategory;
    }

    public String getStoreName() {
        return storeName;
    }
    public void setStoreName(String storename) {
        this.storeName = storename;
    }
    public Double getLatitude() {
        return latitude;
    }
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    public Double getLongitude() {
        return longitude;
    }
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    public String getFoodCategory() {
        return foodCategory;
    }
    public void setFoodCategory(String foodCategory) {
        this.foodCategory = foodCategory;
    }
    public Integer getMinStars() {
        return minStars;
    }
    public void setMinStars(Integer minStars) {
        this.minStars = minStars;
    }
    public Integer getMinVotes() {
        return minVotes;
    }
    public void setMinVotes(Integer minVotes) {
        this.minVotes = minVotes;
    }
    public String getProductType() {
        return productType;
    }
    public void setProductType(String productType) {
        this.productType = productType;
    }
    public String getPriceCategory() {
        return priceCategory;
    }
    public void setPriceCategory(String priceCategory) {
        this.priceCategory = priceCategory;
    }

        // υπολογισμός απόστασης 2 σημείων (5 χλμ φίλτρο)
        public static double haversine(double lat1, double lon1, double lat2, double lon2) {
            final int R = 6371; // Radius of the Earth in km
            double latDistance = Math.toRadians(lat2 - lat1);
            double lonDistance = Math.toRadians(lon2 - lon1);
            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        }


    // έλεγχος αν ένα κατάστημα πληρεί τα κριτήρια των φίλτρων
    public static boolean storeMatchesFilter(Store store, Filter filter) {
        if (filter == null) return true; // Αν δεν υπάρχει φίλτρο επιστρέφεται πάντα true
        if (filter.getStoreName() != null && !store.getStoreName().equalsIgnoreCase(filter.getStoreName())) {
            return false;
        }
        if (filter.getLatitude() != null && filter.getLongitude() != null) {
            double distance = haversine(store.getLatitude(), store.getLongitude(), filter.getLatitude(), filter.getLongitude());
            System.out.println("Store coords: " + store.getLatitude() + "," + store.getLongitude());

            System.out.println("Distance to store: " + distance);
            if (distance > 5000.0){
                System.out.println("Distance too large");
                return false;
            }

        }
        if (filter.getFoodCategory() != null && !store.getFoodCategory().equalsIgnoreCase(filter.getFoodCategory())) {
            return false;
        }
        if (filter.getMinStars() != null && store.getStars() < filter.getMinStars()) {
            return false;
        }
        if (filter.getMinVotes() != null && store.getNoOfVotes() < filter.getMinVotes()) {
            return false;
        }
        if (filter.getPriceCategory() != null && !store.calculatePriceCategory().equals(filter.getPriceCategory())) {
            return false;
        }
        // ελέγχει αν υπάρχει τουλάχιστον ένα προϊόν τύπου που θέλει ο χρήστης
        if (filter.getProductType() != null) {
            boolean found = false;
            for (Product product : store.getProducts()) {
                if (filter.getProductType().equalsIgnoreCase(product.getProductType())){
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true; // γίνεται αποδεκτό το κατάστημα
    }
    @Override
    public String toString() { // μετατροπή του φίλτρου σε αναγνώσιμο string
        return "Filter{" + "storeName='" + storeName + '\'' + ", latitude=" + latitude + ", longitude=" + longitude + ", foodCategory='" + foodCategory + ", minStars=" + minStars + ", minVotes=" + minVotes + ", ProductType='" + productType + '\'' + ", priceCategory=" + priceCategory + '}';
    }
}