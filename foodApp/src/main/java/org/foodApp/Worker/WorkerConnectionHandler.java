package org.foodApp.Worker;

import org.foodApp.MasterServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// Αυτή η κλάση χειρίζεται τις συνδέσεις των worker node.
//Κάθε φορά που συνδέεται ένας worker, καταγράφεται και ελέγχεται ο αριθμός των συνδεδεμένων worker.
public class WorkerConnectionHandler implements Runnable {

    //Server socket που ακούει τις συνδέσεις των worker.
    private final ServerSocket workerSocketServer;
    //Αντικείμενο master για κλήση συναρτήσεων για τον έλεγχο των worker.
    private final MasterServer master;

    // Constructor
    public WorkerConnectionHandler(ServerSocket serverSocket, MasterServer master) {
        this.workerSocketServer = serverSocket;
        this.master = master;
    }

    // Βρόχος που δέχεται συνδέσεις από workers, καταχωρεί όποιον καινούργιο worker συνδεθεί και ξεκινάει την κατανομή των καταστημάτων.
    @Override
    public void run() {
        while (true) {
            try{
                //Αναμονή για νέα σύνδεση από worker.
                Socket workerSocket = workerSocketServer.accept();
                //Δημιουργία αντικειμένου για worker node.
                WorkerNode node = new WorkerNode(workerSocket);
                //Ανάθεση id
                int id = master.getWorkerCount();
                master.registerWorker(id,node);
                System.out.println("Worker registered (ID : " + id + ") from " + workerSocket.getInetAddress());
                //Όταν συνδεθούν όλοι οι workers, ξεκινάει η κατανομή των καταστημάτων
                if (master.getWorkerCount() == master.getExpectedWorkerCount()) {
                    System.out.println("All expected workers connected.... Starting distributing stores to workers...");
                    master.saveStoresToWorkers();
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}
