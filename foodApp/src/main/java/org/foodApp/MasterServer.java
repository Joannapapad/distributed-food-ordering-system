package org.foodApp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.foodApp.Worker.WorkerConnectionHandler;
import org.foodApp.Worker.WorkerNode;


public class MasterServer {
    // workerId σε WorkerNode αντικείμενα
    private final HashMap<String, List<Integer>> storeToWorkerMap = new HashMap<>();
    private HashMap<Integer, WorkerNode> workerNodeHashMap = new HashMap<>();
    private int workerCount = 0; // Μετρητής συνδεδεμένων worker
    private final int expectedWorkerCount;// Αναμενόμενος αριθμός worker



    public static void main(String [] args) {
        int expectedWorkers = 3; // Προεπιλεγμένος αριθμός από workers

        // Δίνεται αριθμός από την γραμμή εντολών
        if(args.length >0) {
            try{
                expectedWorkers = Integer.parseInt(args[0]);
            } catch(NumberFormatException e) {
                System.out.println("Invalid argument for worker count...");
            }
        }
        // Εκκίνηση του master server
        new MasterServer(expectedWorkers).openServer();
    }



    // Constructor
    public MasterServer(int expectedWorkerCount) {
        this.expectedWorkerCount = expectedWorkerCount;

        try{
            masterToClientSocket = new ServerSocket(5000);
            masterToWorkerSocket = new ServerSocket(7000);
            providerToManagerSocket = new ServerSocket(6000);
            masterToReducerSocket = new ServerSocket(8000);
        }catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Sockets για τις επικοινωνίες
    ServerSocket masterToClientSocket;
    ServerSocket masterToWorkerSocket;
    ServerSocket providerToManagerSocket;
    ServerSocket masterToReducerSocket;
    private List<Store> finalStoreList = new ArrayList<>(); // τελική λίστα καταστημάτων από reducer
    private boolean finalStoresReady = false;


    // Εκκίνηση όλων των threads
    void openServer() {
        System.out.println("Master server started on port 7000...");
        System.out.println("Master server started on port 5000...");
        System.out.println("Master server started on port 6000...");
        System.out.println("Master server started on port 8000...");

        // Handler για σύνδεση με τους workers
        new Thread(new WorkerConnectionHandler(masterToWorkerSocket,this)).start();

        // Thread για σύνδεση με manager
        new Thread (() -> {
            while(true) {
                try{
                    Socket managerSocket = providerToManagerSocket.accept();
                    System.out.println("Manager connected from: " + managerSocket.getInetAddress());
                    handleManagerRequest(managerSocket);
                }catch (IOException e){
                    System.err.println("Failed to accept manager connection" + e.getMessage());
                }
            }
        }).start();

        // Thread για σύνδεση με client
        new Thread(() -> {
            while (true) {
                try{
                    Socket clientSocket = masterToClientSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }catch(IOException e) {
                    System.err.println("Failed to accept client connection" + e.getMessage());
                }
            }
        }).start();

        // Thread για την λήψη των δεδομένων από τον reducer
        new Thread(()-> {
            while(true){
                try{
                    Socket reducerSocket = masterToReducerSocket.accept();
                    ObjectInputStream in = new ObjectInputStream(reducerSocket.getInputStream());
                    List<Store> finalStores = (List<Store>) in.readObject();
                    System.out.println("Master received reduced stores : " + finalStores.size());

                    synchronized (this){
                        finalStoreList = finalStores;
                        finalStoresReady = true;
                        notifyAll(); // ειδοποίηση των client/manager ότι τα δεδομένα είναι έτοιμα
                    }
                }catch(ClassNotFoundException | IOException e){
                    throw new RuntimeException(e);
                }
            }
        }).start();

    }

    // Καταχώρηση worker στον master
    public synchronized void registerWorker(int id, WorkerNode node) {
        workerNodeHashMap.put(id,node);
        workerCount++;
        new Thread(node).start();// εκκίνηση του worker
    }

    public synchronized int getWorkerCount(){
        return workerCount;
    }

    public int getExpectedWorkerCount() {
        return expectedWorkerCount;
    }

    // Καθορισμός worker για συγκεκριμένο κατάστημα με βάση το hash του ονόματος
    public int workerForStore(String storeName) {
        return Math.abs((storeName.hashCode() % workerCount));
    }

    // Ανάγνωση των καταστημάτων από package.json και διανομή τους στους workers
    public void saveStoresToWorkers() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = getClass().getResourceAsStream("/package.json");
        if(is == null) {
            throw new IOException("Package file not found");
        }

        List<Store> stores = mapper.readValue(is, new TypeReference<List<Store>>() {});
        for(Store store : stores) {
            int primaryWorker = workerForStore(store.getStoreName());
            int replicaWorker = (primaryWorker + 1) % workerCount;

            Request request = new Request( store, RequestType.ADD_STORE);

            WorkerNode primary = workerNodeHashMap.get(primaryWorker);
            if(primary != null) {
                primary.addStore(request);
                ObjectOutputStream out = primary.getOut();
                out.writeObject(request);
                out.flush();
                System.out.println("Store : " + store.getStoreName() + " added to worker " + primaryWorker);

            }

            WorkerNode replica = workerNodeHashMap.get(replicaWorker);
            if(replica!= null && replica!= primary) {
                replica.addStore(request);
                ObjectOutputStream out = replica.getOut();
                out.writeObject(request);
                out.flush();
                System.out.println("Store : "+ store.getStoreName() + " added to  replica worker " + replicaWorker);
            }

            storeToWorkerMap.put(store.getStoreName(), List.of(primaryWorker,replicaWorker));
        }
    }

    // Διαχείρηση αιτήματος client
    private void handleClient(Socket clientSocket) {
        try(ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

            out.flush();
            Object requestObj = in.readObject();
            System.out.println("Received request from client : " + requestObj.getClass().getName());

            if(requestObj instanceof  Request request) {
                System.out.println("Handling request type : " + request.getRequestType());
                switch(request.getRequestType()) {

                    // Στέλνει το αίτημα σε όλους τους workers ώστε να στείλουν καταστήματα ή φίλτρα
                    // Περιμένει απάντηση από τον reducer
                    case DISPLAY_ALL_STORES , FILTER_STORES -> {

                        for(WorkerNode worker : workerNodeHashMap.values()) {
                            ObjectOutputStream workerOut = worker.getOut();
                            synchronized (workerOut) {
                                workerOut.writeObject(request);
                                workerOut.flush();
                            }
                        }
                        // Αναμονή απάντησης από τον reducer
                        synchronized (this) {
                            while(!finalStoresReady) {
                                wait();
                            }
                            System.out.println("final stores sent");
                            out.writeObject(finalStoreList);
                            out.flush();
                            finalStoresReady = false;
                            finalStoreList = new ArrayList<>();
                        }
                    }

                    // Ο πελάτης αγοράζει προιόν από το κατάστημα
                    // Το αίτημα αγοράς προωθείται στον worker που διαχειρίζεται το συγκεκριμένο κατάστημα
                    case BUY_PRODUCT, UPDATE_STORE -> {
                        Store store = (Store) request.getData();
                        List<Integer> workersIds = storeToWorkerMap.get(store.getStoreName());

                        if (workersIds == null || workersIds.isEmpty()) {
                            out.writeObject("Store not found");
                            out.flush();
                            break;
                        }

                        boolean sent = false;
                        for (int id : workersIds) {
                            WorkerNode node = workerNodeHashMap.get(id);

                            if (node == null || !node.isAlive()) {
                                System.out.println("Skipping worker " + id + " (down)");
                                workerNodeHashMap.remove(id);
                                storeToWorkerMap.values().forEach(list -> list.remove((Integer) id));
                                continue;
                            }

                            try {
                                ObjectOutputStream workerOut = node.getOut();
                                synchronized (workerOut) {
                                    workerOut.writeObject(request);
                                    workerOut.flush();
                                }
                                sent = true;
                                System.out.println(request.getRequestType()
                                        + " for " + store.getStoreName()
                                        + " routed to worker " + id);
                            } catch (SocketException se) {
                                System.out.println("Worker " + id + " disconnected (SocketException), removing");
                                workerNodeHashMap.remove(id);
                                storeToWorkerMap.values().forEach(list -> list.remove((Integer) id));
                            } catch (IOException ioe) {
                                System.err.println("I/O error sending to worker " + id + ": " + ioe.getMessage());
                            }
                        }

                        // Απάντηση στον client
                        if (sent) {
                            out.writeObject(store);

                        } else {
                            out.writeObject("No available worker for store " + store.getStoreName());
                        }
                        out.flush();
                    }

                    default -> out.writeObject("Invalid request");
                }
            }
        } catch(IOException |ClassNotFoundException |InterruptedException e) {
            System.out.println("Client request failed " + e.getMessage());
        }
    }

    // Διαχείρηση αιτήματος από τον manager
    private void handleManagerRequest(Socket managerSocket) {
        try(ObjectOutputStream out = new ObjectOutputStream(managerSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(managerSocket.getInputStream())){

            out.flush();
            Object requestObj = in.readObject();
            System.out.println("Received request from manager :" + requestObj.getClass().getName());

            if(requestObj instanceof Request request) {
                System.out.println("Handling request type :" + request.getRequestType());
                switch (request.getRequestType()) {
                    // Ο manager προσθέτει ή αφαιρεί ένα κατάστημα
                    //Το αίτημα προωθείται στον κατάλληλο worker βάσει του ονόματος του καταστήματος
                    case ADD_STORE , REMOVE_STORE -> {
                        Store store = (Store) request.getData();
                        String name = store.getStoreName();

                        List<Integer> workersIds = storeToWorkerMap.get(name);
                        if(workersIds == null) {
                            int p = workerForStore(name);
                            int r = (p + 1) % workerCount;
                            workersIds = List.of(p,r);
                            storeToWorkerMap.put(name,workersIds);
                        }

                        boolean sent = false;

                        for(int id : workersIds) {
                            WorkerNode node = workerNodeHashMap.get(id);

                            if (node == null || !node.isAlive()) {
                                System.out.println("Skipping worker " + id + " (down)");
                                workerNodeHashMap.remove(id);
                                storeToWorkerMap.values().forEach(list -> list.remove((Integer) id));
                                continue;
                            }

                            if(node!= null && node.isAlive()) {
                                ObjectOutputStream workerOut = node.getOut();
                                synchronized (workerOut) {
                                    workerOut.writeObject(request);
                                    workerOut.flush();
                                }

                                sent = true;
                                System.out.println("Store : "+ name + "added to worker" + id);
                            }else{
                                System.err.println("Worker" + id +"unavailable");
                            }

                        }
                        if(sent) {
                            out.writeObject(store);
                            out.flush();
                        }
                        if(!sent) {
                            out.writeObject("No available worker for store " + name);
                            out.flush();
                        }else{
                            out.writeObject(store);
                            out.flush();
                        }
                    }

                    // Ο manager ζητά λίστα καταστημάτων ή στατιστικά πωλήσεων
                    // Το αίτημα πηγαίνει σε όλους τους workers και τα αποτελέσματα συλλέγονται από τον reducer
                    case DISPLAY_ALL_STORES , SALES_BY_STORE_TYPE , SALES_BY_PRODUCT_CATEGORY -> {

                        for(WorkerNode worker : workerNodeHashMap.values()) {
                            ObjectOutputStream workerOut = worker.getOut();
                            synchronized (workerOut) {
                                workerOut.writeObject(request);
                                workerOut.flush();
                            }
                        }
                        synchronized (this) {
                            while(!finalStoresReady) {
                                wait();
                            }
                            System.out.println("final stores sent");
                            out.writeObject(finalStoreList);
                            finalStoresReady = false;
                            finalStoreList = new ArrayList<>();
                        }
                    }
                    //Ο manager διαχειρίζεται τα προιόντα ενός συγκεκριμένου καταστήματος
                    // Το αίτημα στέλνεται στον worker που χειρίζεται το συγκεκριμένο κατάστημα
                    case UPDATE_PRODUCT , REMOVE_PRODUCT , ADD_PRODUCT -> {
                        Store store = (Store) request.getData();
                        List<Integer> workersIds = storeToWorkerMap.get(store.getStoreName());
                        if(workersIds == null) {
                            out.writeObject("Store not found");
                            out.flush();
                            break;
                        }
                        boolean sent = false;
                        for(int id : workersIds) {
                            WorkerNode node = workerNodeHashMap.get(id);

                            if (node == null || !node.isAlive()) {
                                System.out.println("Skipping worker " + id + " (down)");
                                workerNodeHashMap.remove(id);
                                storeToWorkerMap.values().forEach(list -> list.remove((Integer) id));
                                continue;
                            }

                            if(node!= null && node.isAlive()) {
                                ObjectOutputStream workerOut = node.getOut();
                                synchronized (workerOut) {
                                    workerOut.writeObject(request);
                                    workerOut.flush();
                                }
                                sent = true;
                                System.out.println("Store : "+ store.getStoreName() + "added to worker" + id);
                            }
                        }
                        if(sent) {
                            out.writeObject(store);
                            out.flush();
                        }
                        if(!sent) {

                            out.writeObject("No available worker for store " + store.getStoreName());
                            out.flush();
                        }else{
                            out.writeObject(store);
                            out.flush();
                        }
                    }
                    default -> out.writeObject("Invalid request");

                }
            }else{
                out.writeObject("Invalid request");
            }

        }catch (IOException | ClassNotFoundException| InterruptedException e) {
            System.err.println("Manager request fail " + e.getMessage());
        }

    }
}