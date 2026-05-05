package org.foodApp;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Product implements Serializable {
    private static final long serialVersionUID = 8225427490978088357L;

    @JsonProperty("ProductName")
    private String productName;
    @JsonProperty("ProductType")
    private String productType;
    @JsonProperty("Available Amount")
    private int availableAmount;
    @JsonProperty("Price")
    private double price;
    @JsonIgnore
    int sales = 0;

    @Override
    public String toString() {
        return "Product " +
                " " + productName + '\'' +
                ", Type  " + productType + '\'' +
                ", availableAmount " + availableAmount +
                ", price " + price + "\n"
                ;
    }
    private String productId;

    public Product() {
    }

    public Product(String productName, String productType, int availableAmount, double price) {
        this.productName = productName;
        this.productType = productType;
        this.availableAmount = availableAmount;
        this.price = price;
        this.productId = generateProductId(productName);
    }

    private String generateProductId(String productName) {
        return String.valueOf(productName.hashCode());
    }

    public String getProductId() {
        return productId;
    }

    public double getPrice() {
        return price;
    }

    public int getAvailableAmount() {
        return availableAmount;
    }

    public void setAvailableAmount(int availableAmount) {
        this.availableAmount = availableAmount;
    }
    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }
    public String getProductType() {
        return productType;
    }

    public int getSales() {
        return sales;
    }

    public void addSales(int amount) {
        this.sales += amount;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Product product = (Product) obj;
        return this.productName.equals(product.productName); // or whatever makes products "equal"
    }

    @Override
    public int hashCode() {
        return Objects.hash(productName); // or other fields
    }

}