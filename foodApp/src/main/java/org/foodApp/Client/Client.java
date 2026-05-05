package org.foodApp.Client;

import org.foodApp.*;
import java.net.Socket;
import java.util.*;
import java.io.*;

// τύποι αιτημάτων που θα χρησιμοποιηθούν
import static org.foodApp.RequestType.DISPLAY_ALL_STORES;
import static org.foodApp.RequestType.FILTER_STORES;


public class Client implements Runnable { // για να μπορεί να τρέχει σε νήμα
    private static final String HOST = "127.0.0.1"; // IP του local host
    private static final int PORT = 5000; // port επικοινωνίας client με master
    private static Scanner scanner;

    public Client() {
        this.scanner = new Scanner(System.in);
    }
    public void run() {
        while(true) {

            Double longtitude = null;
            Double latitude = null;

            while(true) {
                System.out.println("Enter longtitude:");
                String lon = scanner.nextLine().trim();
                System.out.println("Enter latitude:");
                String lat = scanner.nextLine().trim();
                try {
                    longtitude = Double.parseDouble(lon);
                    latitude = Double.parseDouble(lat);
                    break;
                }
                catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter valid numbers.");
                }
            }

            System.out.println("-----------------------------------------------");
            System.out.println("Client Menu:");
            System.out.println("-----------------------------------------------");
            System.out.println("1. Search Stores");
            System.out.println("2. Buy Product(s)");
            System.out.println("3. Exit");
            System.out.println("-----------------------------------------------");
            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            System.out.println("-----------------------------------------------");
            switch(choice) {
                case 1:
                    searchStores();
                    break;
                case 2:
                    buyProduct();
                    break;
                case 3:
                    System.exit(0);
                    break;
                default:
            }
        
        }
    }

    // αναζήτηση καταστημάτων
    private List<Store> searchStores() {
        try(Socket socket = new Socket(HOST, PORT); // σύνδεση με master
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()); // ροή εξόδου
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) { // ροή εισόδου

            Request request;
            System.out.print("\nUse Filters? (true/false): ");
            Boolean useFilter = scanner.hasNextBoolean() ? scanner.nextBoolean() : false;
            scanner.nextLine();

            // αν ο χρήστης επιλέξει να χρησιμοποιήσει φίλτρα
            if (useFilter) {
                Filter filter = new Filter();
                System.out.print("Store Name (optional): ");
                String name = scanner.nextLine().trim();
                if (!name.isEmpty()) filter.setStoreName(name);

                System.out.print("Food Category (optional): ");
                String food = scanner.nextLine().trim();
                if (!food.isEmpty()) filter.setFoodCategory(food);

                System.out.print("Minimum Stars ⭐1-5 (optional): ");
                String starAns = scanner.nextLine().trim();
                if (!starAns.isEmpty()) filter.setMinStars(Integer.parseInt(starAns));

                System.out.print("Minimum Votes (optional): ");
                String votesStr = scanner.nextLine().trim();
                if (!votesStr.isEmpty()) filter.setMinVotes(Integer.parseInt(votesStr));

                System.out.print("Product Type (optional): ");
                String productType = scanner.nextLine().trim();
                if (!productType.isEmpty()) filter.setProductType(productType);

                System.out.print("Price Category $ / $$ / $$$ (optional): ");
                String priceCategory = scanner.nextLine().trim();
                if (!priceCategory.isEmpty()) filter.setPriceCategory(priceCategory);

                // δημιουργία του αιτήματος με χρήση φίλτρων
                request = new Request( null, FILTER_STORES);
                request.setData2(filter);
            }
            else {
                // προβολή όλων των καταστημάτων
                request = new Request(null, DISPLAY_ALL_STORES);
            }

            // αποστολή request στο master
            out.writeObject(request);
            out.flush();

            // λήψη λίστας καταστημάτων
            List<Store> stores = (List<Store>) in.readObject();

            displayStores(stores);

            return stores;
        }
        catch (Exception e) {
            System.err.println("Error during Search: " + e.getMessage());
            e.printStackTrace();
            return List.of(); // επιστρέφεται κενή λίστα σε περίπτωση σφάλματος
        }
    }

    public static void displayStores(List<Store> stores){
        for (int i = 0; i < stores.size(); i++) {
            Store store = stores.get(i);
            System.out.println((i+1) + ". " + store.getStoreName());
        }
    }

    // επιλογή καταστήματος από λίστα
    public static Store ChooseStore(List<Store> stores) {
        if (stores == null || stores.isEmpty()) {
            System.out.println("No Stores to choose from.");
            return null;
        }

        System.out.print("Select a store by number: ");
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        scanner.nextLine();

        if (choice < 1 || choice > stores.size()) {
            System.out.println("Invalid choice.");
            return null;
        }
        return stores.get(choice - 1);
    }

    // αγορά προϊόντων
    private void buyProduct() {
        List<Store> storeList = searchStores(); // πρώτα επιλέγεται κατάστημα
        Store selectedStore = ChooseStore(storeList);
        if (selectedStore == null) {
            System.out.println("No store selected.");
            return;
        }

        // Φιλτράρονται μόνο τα διαθέσιμα προϊόντα
        List<Product> availableProducts = selectedStore.getProducts().stream()
                .filter(p -> p.getAvailableAmount() > 0)
                .toList();
        if (availableProducts.isEmpty()) {
            System.out.println("No available products in this store.");
            return;
        }

        Map<Product, Integer> cart = new HashMap<>(); // καλάθι αγορών

        while(true) {
            // εμφάνιση προϊόντων
            System.out.println("Available Products: ");
            for (int i = 0; i < availableProducts.size(); i++) {
                Product p = availableProducts.get(i);
                System.out.printf("%d. %s (Type: %s, Price: %.2f, Available: %d)%n", i+1, p.getProductName(), p.getProductType(), p.getPrice(), p.getAvailableAmount());
            }
            System.out.print("Enter product number to add to cart (or '0' to view cart / proceed): ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 0) {
                // εμφάνιση καλαθιού & υπολογισμός συνολικού κόστους
                System.out.println("\nYour Cart: ");
                double total = 0;
                for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
                    Product p = entry.getKey();
                    int qty = entry.getValue();
                    double cost = p.getPrice() * qty;
                    System.out.printf("- %s x%d = %.2f\n", p.getProductName(), qty, cost);
                    total += cost;
                }
                System.out.printf("Total: %.2f\n", total);

                System.out.print("Would you like to proceed with the purchase? (yes / no): ");
                String confirm = scanner.nextLine().trim().toLowerCase();
                if (confirm.equals("yes")) {
                    // τοπική ενημέρωση προϊόντων
                    for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
                        Product p = entry.getKey();
                        int qty = entry.getValue();
                        p.addSales(qty);
                        p.setAvailableAmount(p.getAvailableAmount() - qty);
                    }

                    // αποστολή αιτήματος αγοράς στον master
                    try (Socket socket = new Socket(HOST, PORT);
                         ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                         ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                             Map<String, Integer> productMap = new HashMap<>();
                             for (Map.Entry<Product, Integer> entry: cart.entrySet()) {
                                 productMap.put(entry.getKey().getProductName(), entry.getValue());
                             }
                             Request request = new Request(selectedStore, RequestType.BUY_PRODUCT);
                             request.setData2(productMap);
                             out.writeObject(request);
                             out.flush();
                             System.out.println("Purchase completed!");
                             return;
                         }
                    catch (Exception e) {
                        System.err.println("Error during Purchase: " + e.getMessage());
                        e.printStackTrace();
                        return;
                    }
                }
                else {
                    System.out.println("Returning to product selection..");
                    continue;
                }
            }

            // επιβεβαίωση εγκυρότητας επιλογής
            if (choice < 1 || choice > availableProducts.size()) {
                System.out.println("Invalid product number.");
                continue;
            }

            Product selectedProduct = availableProducts.get(choice - 1);
            System.out.println("Enter quantity to buy: ");
            int qty = scanner.nextInt();
            scanner.nextLine();

            if (qty <= 0 || qty > selectedProduct.getAvailableAmount()) {
                System.out.println("Invalid quantity.");
            }
            else {
                cart.put(selectedProduct, cart.getOrDefault(selectedProduct, 0) + qty);
                System.out.println("Added to cart.");
            }
        }
    }

    // εκκίνηση του client σε ξεχωριστό νήμα
    public static void main(String[] args) {
        new Thread(new Client()).start();
    }
}