package org.foodApp.Reducer;

import org.foodApp.Store;
import org.foodApp.Worker.WorkerNode;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StoreReducerThread implements Runnable {
    private Socket socket;
    private int expectedWorkers;

    public StoreReducerThread(Socket socket, int expectedWorkers) {
        this.socket = socket;
        this.expectedWorkers = expectedWorkers;
    }

    // μέθοδος υπεύθυνη για την αποστολή των αποτελεσμάτων στον master
    public void sentToMaster(List<Store> stores) {
        try{
            Socket mastersocket = new Socket("localhost", 8000);
            ObjectOutputStream out = new ObjectOutputStream(mastersocket.getOutputStream());
            out.writeObject(stores);
            out.flush();
            System.out.println("Sent stores to master");
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }
    }
    @Override
    public void run() {
        try{
            ObjectInputStream inFromWorker = new ObjectInputStream(socket.getInputStream());
           HashMap<String,Store>workerStores = (HashMap<String, Store>) inFromWorker.readObject();

            // συγχρονισμός για αποφυγή απώλειας stores που έρχονται από πολλούς workers
            synchronized (StoreReducer.reducedStores) {
                StoreReducer.reducedStores.putAll(workerStores);
                System.out.println("Received stores from Worker: " + workerStores.size());
                StoreReducer.workersNo++;
                if (StoreReducer.workersNo == expectedWorkers) {
                    System.out.println("All Workers received stores. Sending stores to Master..");

                    List<Store> storesToMaster = new ArrayList<>(StoreReducer.reducedStores.values());
                    sentToMaster(storesToMaster);
                    StoreReducer.reducedStores.clear();
                    StoreReducer.workersNo = 0;
                }
                StoreReducer.reducedStores.notifyAll(); // ενημέρωση threads για την προσθήκη stores από τον επόμενο worker
            }
        }
        catch (ClassNotFoundException | IOException e){
            throw new RuntimeException(e);
        }
    }
}