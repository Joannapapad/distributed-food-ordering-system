package org.foodApp.Worker;

import java.io.Serializable;

public class Filter implements Serializable {

    private String storeName;
    private Double latitude;
    private Double longitude;
    private String foodCategory;   // e.g., "pizzeria"
    private Integer minStars;      // e.g., minimum star rating
    private Integer minVotes;      // e.g., minimum number of votes
    private String productType;    // e.g., "pizza", "salad"
    private Double maxPrice;       // max price for product filtering

    public Filter() {}

    public Filter(String storeName, Double latitude, Double longitude,
                  String foodCategory, Integer minStars, Integer minVotes,
                  String productType, Double maxPrice) {
        this.storeName = storeName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.foodCategory = foodCategory;
        this.minStars = minStars;
        this.minVotes = minVotes;
        this.productType = productType;
        this.maxPrice = maxPrice;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
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

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    @Override
    public String toString() {
        return "Filter{" +
                "storeName='" + storeName + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", foodCategory='" + foodCategory + '\'' +
                ", minStars=" + minStars +
                ", minVotes=" + minVotes +
                ", productType='" + productType + '\'' +
                ", maxPrice=" + maxPrice +
                '}';
    }
}
