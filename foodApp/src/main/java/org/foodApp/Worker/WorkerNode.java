package org.foodApp.Worker;

import org.foodApp.Client.Filter;
import org.foodApp.Product;
import org.foodApp.Request;
import org.foodApp.RequestType;
import org.foodApp.Store;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

//Η κλάση αυτή αναπαριστά έναν worker που δέχεται και επεξεργάζεται αιτήματα από τον Master Server.
public class WorkerNode implements Runnable{
    private ObjectInputStream in; // Ροή εισόδου από το Socket.
    private ObjectOutputStream out;// Ροή εξόδου προς το Socket.
    private HashMap<String,Store> storedData = new HashMap<>(); // Καταστήματα και τα δεδομένα τους.
    private final Socket socket; // το socket που συνδέει τον master με τους workers.
    private final Map<String, Integer> storeLocks = new HashMap<>();
    private final Map<String, PriorityQueue<Integer>> storeQueues = new HashMap<>();
    private final Map<String, Integer> currentLocks = new HashMap<>();

    // Constructor
    public WorkerNode(Socket socket) {
        this.socket = socket;
        try{
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public boolean isAlive() {
        return socket!= null && socket.isConnected() && !socket.isClosed();
    }
    // TRY LOCK
    private synchronized boolean tryLock(String storeName, int requestId) {
        if (!storeLocks.containsKey(storeName)) {
            storeLocks.put(storeName, requestId);
            return true;
        }
        return false;
    }
    private synchronized void releaseLock(String storeName, int requestId) {
        PriorityQueue<Integer> queue = storeQueues.get(storeName);
        if (queue != null) {
            queue.remove(requestId);
            if (queue.isEmpty()) {
                storeQueues.remove(storeName);
                storeLocks.remove(storeName);
            }
            else {
                currentLocks.put(storeName, queue.peek());
            }
        }
        notifyAll();
    }
    private synchronized void acquireLockBlocking(String storeName, int requestId) {
        storeQueues.computeIfAbsent(storeName, k -> new PriorityQueue<>()).add(requestId);

        while(true) {
            int lowestId = storeQueues.get(storeName).peek();
            Integer current = currentLocks.get(storeName);

            if (lowestId == requestId && (current == null || current == requestId)) {
                currentLocks.put(storeName, requestId);
                return;
            }

            try {
                wait(); // περιμένει μέχρι να ειδοποιηθεί
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for lock on store: " + storeName);
            }
        }
    }

    // Προσθέτει ένα νέο κατάστημα στον worker.
    public synchronized void addStore(Request request) {
        Store store = (Store) request.getData();
        storedData.put(store.getStoreName(),store);
    }

    // Αφαιρεί ένα κατάστημα από τα δεδομένα του worker.
    public synchronized void removeStore(Request request) {
        Store store = (Store)request.getData();
        storedData.remove(store.getStoreName(),store);
    }

    /*
     *Ενέργειες που εκτελούνται όταν ξεκινήσει ένα νήμα από τον worker:
     * 1. Περιμένει αντικείμενα από τον master.
     * 2. Επεξεργάζεται αιτήματα του manager/client ανάλογα με τον τύπο τους. */
    @Override
    public void run() {
        while(true) {
            try{
                Object obj = in.readObject();
                System.out.println("Received object of type : " + obj.getClass().getName());

                if(obj instanceof Request request) {
                    handleRequest(request);
                } else{
                    throw new IllegalStateException("Received object is not of type Request");
                }
            }catch (IOException| ClassNotFoundException e) {
                System.out.println("Error processing object: " + e.getMessage());
                e.printStackTrace();
                break;
            }
            if(socket.isClosed()) {
                System.err.println("Socket closed");
                break;
            }
        }
    }

    // Επεξεργασία κάθε είδους αιτήματος.
    private void handleRequest(Request request) throws IOException {
        RequestType type = request.getRequestType();
        System.out.println("Received request of type: " + type);

        switch(type) {
            // Προσθήκη καταστημάτων
            case ADD_STORE -> {
                if(request.getData() instanceof Store store) {
                    System.out.println("Handling ADD STORE REQUEST for " + store.getStoreName());
                    addStore(request); // Αποθηκεύει το κατάστημα στο map.
                }else{
                    System.out.println("Invalid data type for ADD STORE REQUEST.");
                }
            }
            case REMOVE_STORE -> {
                // Αφαίρεση καταστημάτων.
                if(request.getData() instanceof  Store store) {
                    System.out.println("Handling REMOVE STORE REQUEST for " + store.getStoreName());
                    removeStore(request); // Αφαιρεί καταστήματα από το map.
                } else{
                    System.out.println("Invalid data type for REMOVE STORE REQUEST.");
                }
            }
            case UPDATE_PRODUCT -> {
                // Ενημέρωση ποσότητας προϊόντος σε κατάστημα.
                if (request.getData() instanceof Store updatedStore) {
                    String storeName = updatedStore.getStoreName();
                    Store existingStore = storedData.get(storeName);

                    if(existingStore != null) {
                        List <Product> existingProducts = existingStore.getProducts();
                        for(Product existingProduct : existingProducts) {
                            // Βρίσκει το προϊόν και ενημερώνει την ποσότητα.
                            if(existingProduct.getProductName().equals(((Product)request.getData2()).getProductName())) {
                                int newAmount = ((Product)request.getData2()).getAvailableAmount();
                                existingProduct.setAvailableAmount(newAmount);
                                System.out.println("Updated Product: " + ((Product)request.getData2()).getProductName() + " with new amount: " + newAmount);
                                break;
                            }
                        }
                    } else{
                        System.out.println("Store does not exist.");
                    }
                }
            }
            case ADD_PRODUCT -> {
                // Προσθήκη νέου προϊόντος σε υπάρχον κατάστημα.
                if(request.getData() instanceof Store updatedStore) {
                    String storeName = updatedStore.getStoreName();
                    Store existingStore = storedData.get(storeName);

                    if(existingStore != null) {
                        List<Product> existingProducts = existingStore.getProducts();
                        existingProducts.add((Product)request.getData2()); // Προσθέτει νέο προϊόν
                        System.out.println("Added Product: " + ((Product)request.getData2()).getProductName() + " to Store: " + storeName);
                    }else{
                        System.out.println("Store does not exist.");
                    }
                }
            }
            case REMOVE_PRODUCT -> {
                // Αφαίρεση προϊόντος από υπάρχον κατάστημα.
                if(request.getData() instanceof  Store updatedStore) {
                    String storeName = updatedStore.getStoreName();
                    Store existingStore = storedData.get(storeName);

                    if(existingStore != null) {
                        List<Product> existingProducts = existingStore.getProducts();
                        for(Product existingProduct : existingProducts) {
                            if(existingProduct.getProductName().equals(((Product)request.getData2()).getProductName())) {
                                existingStore.removeProduct(existingProduct); // Αφαιρεί το προϊόν
                                System.out.println("Removed Product: " + ((Product)request.getData2()).getProductName() + " from Store: " + storeName);
                                break;
                            }
                        }
                    }else{
                        System.out.println("Store does not exist.");

                    }
                }
            }
            // Στέλνει όλα τα αποθηκευμένα καταστήματα στον reducer.
            case DISPLAY_ALL_STORES -> {

                HashMap<String,Store> storeList = new HashMap<>();

                Filter filter = null;
                if(request.getData2() instanceof Filter) {
                    filter = (Filter) request.getData2();
                }

                for(Store store : storedData.values()) {
                    if(filter == null || Filter.storeMatchesFilter(store, filter)) {
                        storeList.put(store.getStoreName(), store);
                    }
                }
                try{
                    Socket reducedSocket = new Socket("localhost" , 9000); // Σύνδεση με reducer.
                    ObjectOutputStream outToReducer = new ObjectOutputStream(reducedSocket.getOutputStream());
                    System.out.println("size of storeList: " + storeList.size());
                    outToReducer.writeObject(storeList); // Αποστολή λίστας.
                    outToReducer.flush();
                    System.out.println("Worker sent stores to reducer");
                } catch(IOException e){
                    System.err.println("Failed to send stores to reducer: " + e.getMessage());
                }
            }



            case FILTER_STORES -> {
                // Φιλτράρει καταστήματα βάσει φίλτρου και τα στέλνει στον reducer.
                if(request.getData2() instanceof  Filter filter) {
                    HashMap<String,Store> filteredStores = new HashMap<>();

                    for(Store store : storedData.values()) {
                        if(Filter.storeMatchesFilter(store, filter)) {
                            filteredStores.put(store.getStoreName(), store);
                        }
                    }

                    try{
                        Socket reducedSocket = new Socket("localhost" , 9000);
                        ObjectOutputStream outToReducer = new ObjectOutputStream(reducedSocket.getOutputStream());
                        outToReducer.writeObject(filteredStores);
                        outToReducer.flush();
                        System.out.println("Worker sent stores to reducer");
                    } catch (IOException e){
                        System.err.println("Failed to send stores to reducer: " + e.getMessage());
                    }
                } else{
                    System.out.println("Invalid data type for FILTER STORES REQUEST.");
                }
            }

            case SALES_BY_STORE_TYPE -> {
                if (request.getData2() instanceof Filter filter) {
                    String desiredCategory = filter.getFoodCategory();
                    HashMap<String, Store> result = new HashMap<>();
                    for (Store store : storedData.values()) {
                        if (store.getFoodCategory() != null &&
                                store.getFoodCategory().equals(desiredCategory)) {
                            result.put(store.getStoreName(), store);
                        }
                    }


                    try{
                        Socket reducedSocket = new Socket("localhost" , 9000);
                        ObjectOutputStream outToReducer = new ObjectOutputStream(reducedSocket.getOutputStream());
                        outToReducer.writeObject(result);
                        outToReducer.flush();
                        System.out.println("Worker sent stores to reducer");
                    } catch (IOException e){
                        System.err.println("Failed to send stores to reducer: " + e.getMessage());
                    }
                } else{
                    System.out.println("Invalid data type for FILTER STORES REQUEST.");
                }

            }

            case SALES_BY_PRODUCT_CATEGORY -> {
                if (request.getData2() instanceof Filter filter) {
                    String desiredProductType = filter.getProductType();
                    HashMap<String,Store> result = new HashMap<>();
                    for (Store store : storedData.values()) {
                        boolean hasMatchingProduct = false;
                        for (Product prod : store.getProducts()) {
                            if (prod.getProductType() != null &&
                                    prod.getProductType().equals(desiredProductType)) {
                                hasMatchingProduct = true;
                                break;
                            }
                        }
                        if (hasMatchingProduct) {
                            result.put(store.getStoreName(), store);
                        }
                    }

                    for (Store s : result.values()) {
                        for (Product p : s.getProducts()) {
                            if (p.getProductType() != null &&
                                    p.getProductType().equals(desiredProductType)) {
                                int sold = s.getSalesPerProduct(p);
                                System.out.println(
                                        "[WORKER → SENDING] " + s.getStoreName() +
                                                " – Product: " + p.getProductName() +
                                                " – Sales: " + sold
                                );
                            }
                        }
                    }

                    try{
                        Socket reducedSocket = new Socket("localhost" , 9000);
                        ObjectOutputStream outToReducer = new ObjectOutputStream(reducedSocket.getOutputStream());
                        outToReducer.writeObject(result);
                        outToReducer.flush();
                        System.out.println("Worker sent stores to reducer");
                    } catch (IOException e){
                        System.err.println("Failed to send stores to reducer: " + e.getMessage());
                    }
                } else{
                    System.out.println("Invalid data type for FILTER STORES REQUEST.");
                }

            }


            case UPDATE_STORE -> {
                if (request.getData() instanceof Store store) {
                    String storeName = store.getStoreName();
                    int requestId = request.getRequest_id();

                    acquireLockBlocking(storeName, requestId);
                    try {
                        Store existingsStore = storedData.get(storeName);
                        if (existingsStore != null) {

                            existingsStore.setNoOfVotes(existingsStore.getNoOfVotes() + 1);
                        }
                        else {
                            System.out.println("Store does not exist.");
                        }
                    }
                    finally {
                        releaseLock(storeName, requestId);
                    }
                }
            }
            case BUY_PRODUCT -> {
                // Διαχειρίζεται αγορά προϊόντων από ένα κατάστημα.
                if (request.getData() instanceof Store store && request.getData2() instanceof Map) {
                    String storeName = store.getStoreName();
                    int requestId = request.getRequest_id();

                    acquireLockBlocking(storeName, requestId);
                    try {
                        Store existingsStore = storedData.get(storeName);
                        if (existingsStore != null) {
                            Map<String, Integer> productMap = (Map<String, Integer>) request.getData2();
                            List<Product> products = existingsStore.getProducts();

                            for (Product product : products) {
                                if (productMap.containsKey(product.getProductName())) {
                                    int quantityToBuy = productMap.get(product.getProductName());
                                    int currentAmount = product.getAvailableAmount();
                                    if (currentAmount >= quantityToBuy) {
                                        product.setAvailableAmount(currentAmount - quantityToBuy);
                                        System.out.println("[BUY_PRODUCT] Trying to buy " +
                                                quantityToBuy + "x " + product.getProductName() +
                                                ", before: sales=" + product.getSales());
                                        product.addSales(quantityToBuy);
                                        System.out.println("[BUY_PRODUCT] After addSales(): sales=" + product.getSales());
                                        System.out.println("Processed purchase of: " + quantityToBuy + "x" + product.getProductName());
                                    } else {
                                        System.out.println("Not enough stock for: " + product.getProductName());
                                    }
                                }
                            }
                        }
                        else {
                            System.out.println("Store does not exist.");
                        }
                    }
                    finally {
                        releaseLock(storeName, requestId);
                    }
                }
                else {
                    System.out.println("Invalid Data Type for BUY_PRODUCT REQUEST.");
                }
            }
            default -> {}
        }
    }
    // Επιστρέφει όλα τα καταστήματα που είναι αποθηκευμένα στον worker.
    public synchronized HashMap<String,Store> getStoredData() {
        return storedData;
    }
    // Αναμονή για συγχρονισμό με άλλα νήματα.
    public synchronized void waitForStoreData() throws InterruptedException {
        wait();
    }
    // Ειδοποίηση άλλων νημάτων ότι τα δεδομένα καταστημάτων είναι έτοιμα.
    public synchronized void notifyStoreDataReady() {
        notifyAll();
    }
    public Socket getSocket() {
        return socket;
    }
    public ObjectOutputStream getOut() {
        return out;
    }
    public ObjectInputStream getIn(){
        return in;
    }

    public static void main (String [] args) {
        try{
            Socket workerSocket = new Socket("localhost", 7000);
            System.out.println("Worker connected to Master");
            WorkerNode worker = new WorkerNode(workerSocket);
            new Thread(worker).start();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
