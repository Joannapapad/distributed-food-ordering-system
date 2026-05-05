package org.foodApp.Reducer;

import org.foodApp.Store;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// κλαση υπεύθυνη για την συλλογή των αποτελεσμάτων από όλους τους workers
public class StoreReducer {
    public static HashMap<String,Store>reducedStores = new HashMap<>();// λίστα για αποθήκευση καταστημάτων που έχουν σταλεί από τους workers
    public static int workersNo = 0;

    //αρχικοποίηση επικοινωνίας με workers
    public void init(int expectedWorkers) {
        try (ServerSocket serverSocket = new ServerSocket(9000)) {
            System.out.println("Reducer started on port 9000..");
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new StoreReducerThread(socket, expectedWorkers)).start();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        int expectedWorkers = 3; // Προεπιλεγμένος αριθμός από workers

        // Δίνεται αριθμός από την γραμμή εντολών
        if(args.length >0) {
            try{
                expectedWorkers = Integer.parseInt(args[0]);
            } catch(NumberFormatException e) {
                System.out.println("Invalid argument for worker count...");
            }
        }
        StoreReducer reducer = new StoreReducer();
        reducer.init(expectedWorkers);
    }
}