package org.foodApp.Manager;

import org.foodApp.Request;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ActionsForManager implements Runnable {
    ObjectInputStream in;
    ObjectOutputStream out;

    public ActionsForManager(Socket connection) {
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            out.flush(); // flush header immediately
            in = new ObjectInputStream(connection.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            Request t = (Request) in.readObject();

            out.writeObject(t);
            out.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
