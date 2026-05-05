package org.foodApp.Manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.foodApp.Client.Filter;
import org.foodApp.Product;
import org.foodApp.Request;
import org.foodApp.Store;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.List;

import static org.foodApp.RequestType.*;


public class Manager implements Runnable {
    private Scanner scanner;
    private Store selected_store;


    public Manager() {
        this.scanner = new Scanner(System.in);
    }

    public void run() { // το ui του manager
        while (true) {
            System.out.println("\nManager Menu:");
            System.out.println("1. Add Store");
            System.out.println("2. Change Availability of Product(s)");
            System.out.println("3. Add / Remove Product(s)");
            System.out.println("4. Show total Sales by Store Category");
            System.out.println("5. Show total Sales by Product");
            System.out.println("6. Exit");
            System.out.print("Choose: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    addStore();
                    break;
                case 2:
                    modifyProductAvailability();
                    break;
                case 3:
                    modifyProducts();
                    break;
                case 4:
                    TotalSalesPerStores();
                    break;
                case 5:
                    TotalSalesPerFood();
                    break;
                case 6:
                    System.out.println("Exiting...");
                    return;
                default:
                    System.out.println("Not a valid choice. Try again.");
            }
        }
    }

    // προσθήκη καταστήματος
    private void addStore() {
        System.out.print("Insert folder's path that includes JSON and logo: ");
        String folderPath = scanner.nextLine();

        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Folder doesn't exist or isn't valid.");
            return;
        }

        File jsonFile = new File(folder, "store.json");
        if (!jsonFile.exists()) {
            System.out.println("File store.json was not found in folder.");
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Store store = objectMapper.readValue(jsonFile, Store.class);
            store.setLogo(store.getAddedStoreLogo());
            System.out.println("Store was added successfully: " + store.getStoreName());
            Request request = new Request(store, ADD_STORE);
            sendObjectToServer(request);
        } catch (IOException e) {
            System.out.println("Error while reading json file.");
            e.printStackTrace();
        }
    }

    // βοηθητική μέθοδος για την επιλογή καταστήματος
    private Store ChooseStore() {
        try (Socket requestSocket = new Socket("127.0.0.1", 6000);
             ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream())) {

            Request request = new Request( null, DISPLAY_ALL_STORES);
            out.writeObject(request);
            out.flush();

            List<Store> stores = null;

            stores = (List<Store>) in.readObject(); // Directly read the response

            if (stores == null || stores.isEmpty()) {
                System.out.println("No available stores.");
                return null;
            }

            System.out.println("Available stores:");
            for (int i = 0; i < stores.size(); i++) {
                Store store = stores.get(i);
                System.out.println((i + 1) + ". " + store.getStoreName());
            }

            System.out.print("Select a store by number: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice < 1 || choice > stores.size()) {
                System.out.println("Invalid selection.");
                return null;
            }

            return stores.get(choice - 1);

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error communicating with the server.");
            e.printStackTrace();
        }

        return null;
    }

    // βοηθητική μέθοδος για την επιλογή προιοντος
    public Product ChooseProduct(Store selected_store) {
        int i = 1;
        System.out.print("Choose Product: ");
        List<Product> products = selected_store.getProducts();
        if (products == null || products.isEmpty()) {
            System.out.println("No products exist.");
            return null;
        }

        for (Product product : products) {
            System.out.println(i++ + ". " + product.toString());
        }

        int choice = scanner.nextInt();
        scanner.nextLine();

        if (choice < 1 || choice > products.size()) {
            System.out.println("Not a valid choice.");
            return null;
        }

        return products.get(choice - 1);
    }

    // βοηθητική μέθοδος για την αποστολή request αντικειμένων στον master
    private Object sendObjectToServer(Request request) {
        try (Socket requestSocket = new Socket("127.0.0.1", 6000);
             ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream())) {

            out.writeObject(request);
            out.flush();

            Object response = in.readObject();

            if (response instanceof List<?> list) {
                return (List<Store>) list;
            }else if (response instanceof String string) {
                return string;
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // μέθοδος για την εκτέλεση της λειτουργίας αλλαγής διαθεσιμότητας προιόντος
    private void modifyProductAvailability() {
        selected_store = ChooseStore();

        if (selected_store == null) {
            System.out.println("No Store is chosen. Return to menu.");
            return;
        }

        Product product = ChooseProduct(selected_store);

        if (product == null) {
            System.out.println("No product chosen. Return to menu.");
            return;
        }

        System.out.print("Change the available amount to: ");
        int amount = scanner.nextInt();
        scanner.nextLine();

        if (amount >= 0) {
            product.setAvailableAmount(amount);
            System.out.println("The product amount updated successfully!");
            Request request = new Request( selected_store, UPDATE_PRODUCT);
            request.setData2(product);
            sendObjectToServer(request);
        } else {
            System.out.println("The amount you have entered is invalid.");
        }
    }

    // μέθοδος για την εκτέλεση της λειτουργίας προσθήκης/αφαίρεσης προιόντος
    private void modifyProducts() {
        System.out.print("Do you want to add or delete products?\n1. Add\n2. Delete\n");
        int choice = scanner.nextInt();
        scanner.nextLine();
        selected_store = ChooseStore();

        if (choice == 1) {
            System.out.println("Enter product Name: ");
            String productName = scanner.nextLine();
            System.out.println("Enter product Type");
            String productType = scanner.nextLine();
            System.out.println("Enter Available Amount");
            int available_amount = scanner.nextInt();
            scanner.nextLine();
            System.out.println("Enter Price");
            double price = scanner.nextDouble();
            scanner.nextLine();

            Product newProduct = new Product(productName, productType, available_amount, price);
            selected_store.getProducts().add(newProduct);
            Request request = new Request(selected_store , ADD_PRODUCT);
            request.setData2(newProduct);
            sendObjectToServer(request);
            System.out.println("Product added successfully!");

        } else if (choice == 2) {
            Product removeproduct = ChooseProduct(selected_store);
            Request request = new Request(selected_store, REMOVE_PRODUCT);
            request.setData2(removeproduct);
            sendObjectToServer(request);
            System.out.println("Product deleted successfully!");

        } else {
            System.out.println("Invalid selection.");
        }
    }

    // βοηθητική μέθοδος για το φιλτράρισμα κατηγορίας καταστημάτων
    private List<Store> SearchByCategoryFood(String category) {
        Filter filter = new Filter();
        filter.setFoodCategory(category);
        Request request = new Request(null, SALES_BY_STORE_TYPE);
        request.setData2(filter);

        Object result = sendObjectToServer(request);

        if (result instanceof List<?> stores) {
            if (stores.isEmpty()) {
                System.out.println("No available stores in this category.");
                return null;
            }
            System.out.println("Stores filtered by category: " + category);
            return (List<Store>) stores;
        } else {
            System.out.println("Unexpected server response.");
            return null;
        }
    }

    // βοηθητική μέθοδος για το φιλτράρισμα κατηγορίας προίοντος
    private List<Store> SearchByProduct(String category) {
        Filter filter = new Filter();
        filter.setProductType(category);
        Request request = new Request(null, SALES_BY_PRODUCT_CATEGORY);
        request.setData2(filter);

        Object result = sendObjectToServer(request);

        if (result instanceof List<?> stores) {
            if (stores.isEmpty()) {
                System.out.println("No available stores in this category.");
                return null;
            }
            System.out.println("Stores filtered by product: " + category);
            return (List<Store>) stores;
        } else {
            System.out.println("Unexpected server response.");
            return null;
        }
    }

    // μέθοδος για την εμφάνιση πωλήσεων ανά κατηγορία καταστημάτων
    private void TotalSalesPerStores() {
        System.out.println("Enter Food Category: ");
        String category = scanner.nextLine();
        List<Store> stores = SearchByCategoryFood(category);
        if (stores == null) return;

        int complete_total = 0;
            for (Store store : stores) {
                int total = 0;
                for (Product product : store.getProducts()) {
                    total += store.getSalesPerProduct(product);
                }
                complete_total += total;
                System.out.println(store.getStoreName() + ": " + total);

            }

        System.out.println("total : " + complete_total + "\n");

    }

    // μέθοδος για την εμφάνιση πωλήσεων ανά κατηγορία προιόντων
    private void TotalSalesPerFood() {
        System.out.println("Enter Product Category: ");
        String category = scanner.nextLine();
        List<Store> stores = SearchByProduct(category);
        if (stores == null) return;
        int complete_total = 0;
        for (Store store : stores) {
            int total = 0;
            for (Product product : store.getProducts()) {
                if (product.getProductType().equals(category)) {
                    total += store.getSalesPerProduct(product);
                }
            }
            complete_total += total;
            System.out.println("\"" + store.getStoreName() + "\"" + ": " + total + ",");
        }
        System.out.println("total : " + complete_total + ",\n");
    }

    // αρχικοποίηση νήματος για τον manager
    public static void main(String args[]) {
        new Thread(new Manager()).start();
    }
}
