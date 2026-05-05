package com.katanemimena.yum.ClientConnection;

import android.os.Handler;
import android.util.Log;

import org.foodApp.Request;
import org.foodApp.RequestType;
import org.foodApp.Store;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

// Νήμα που διαχειρίζεται την επικοινωνία μς τον server.
// Στέλνει μηνύματα και λαμβάνει απαντήσεις με object stream.
public class MyThread extends Thread {
    private static final String TAG = "MyThread";

    // Handler για να ειδοποιούμε το ui thread.
    private final Handler handler;
    // Λίστα με καταστήματα.
    private final ArrayList<Store> items;
    // Τύπος αιτήματος που θα σταλεί
    private final RequestType requestType;
    // Πρόσθετο αντικείμενο που μεταφέρεται στο αίτημα.
    private Object payload;

    // Δημιουργία νήματος
    public MyThread(Handler handler, ArrayList<Store> items, RequestType requestType, Object payload) {
        this.handler = handler;
        if (items == null) {
            this.items = new ArrayList<>();
        } else {
            this.items = items;
        }
        this.requestType = requestType;
        this.payload = payload;
    }

    @Override
    public void run() {
        try (
                // σύνδεση με τον server.
                Socket socket = new Socket("10.0.2.2", 5000);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            out.flush();

            Log.d(TAG, "Sending request type: " + requestType);
            // ημιουργούμε και στέλνυμε το αίτημα ανάλογα με τον τύπο του.
            if (requestType == requestType.DISPLAY_ALL_STORES || requestType == requestType.FILTER_STORES){
                Request request = new Request(null, requestType);
                request.setData2(payload);
                out.writeObject(request);
                out.flush();
            }else if(requestType == requestType.UPDATE_STORE){
                // για αγορά προϊόντος στέλνουμε το Store στο οποίο ανήκει.
                Request request = new Request(items.get(0), requestType);
                out.writeObject(request);
                out.flush();
            } else if(requestType == requestType.BUY_PRODUCT){
                // για αγορά προϊόντος στέλνουμε το Store στο οποίο ανήκει.
                Request request = new Request(items.get(0), requestType);
                request.setData2(payload);
                out.writeObject(request);
                out.flush();
            }


            // Διαχείρηση απαντήσεων από τον server.
            if (requestType == RequestType.DISPLAY_ALL_STORES || requestType == RequestType.FILTER_STORES) {

                try {
                    Object response = in.readObject();

                    if (response == null) {
                        items.clear();
                        Log.w(TAG, "Received null response, cleared items list.");
                    } else if (response instanceof List<?>) {
                        List<?> responseList = (List<?>) response;
                        if (responseList.isEmpty()) {
                            items.clear();
                            Log.w(TAG, "Received empty list, cleared items list.");
                        } else if (responseList.get(0) instanceof Store) {
                            // Μετατροπή λίστας σε καταστήματα
                            List<Store> storeList = (List<Store>) responseList;
                            items.clear();
                            items.addAll(storeList);
                            Log.d(TAG, "Updated items list with received stores.");
                        } else {
                            items.clear();
                            Log.e(TAG, "Received list does not contain Store objects. Cleared items.");
                        }
                    } else {
                        items.clear();
                        Log.e(TAG, "Received unexpected response type: " + response.getClass() + ". Cleared items.");
                    }
                } catch (EOFException eof) {
                    // Ο server έκλεισε τη σύνδεση χωρίς να στείλει δεδομένα
                    items.clear();
                    Log.e(TAG, "EOFException: no data received, cleared items list.");
                }
                // Ειδοποίηση UI: 1 -> ενημέρωση λίστας καταστημάτων
                handler.sendEmptyMessage(1);

            } else if (requestType == RequestType.BUY_PRODUCT) {
                // Ειδοποίηση UI: 2 -> ολοκλήρωση αγοράς
                handler.sendEmptyMessage(2);
            }
            else if (requestType == RequestType.UPDATE_STORE) {
                // Ειδοποίηση UI: 2 -> ολοκλήρωση αγοράς
                handler.sendEmptyMessage(2);
            }

        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, "Exception in MyThread", e);
        }
    }

}