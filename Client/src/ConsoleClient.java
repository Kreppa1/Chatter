import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

void main() {
    ConsoleClient n = new ConsoleClient();
}

public class ConsoleClient {
    Scanner inputScanner = new Scanner(System.in);
    String host;
    int port;
    String name;
    Socket socket;

    ConsoleClient(){
        System.out.println("Please enter address:");
        String input=inputScanner.nextLine();

        //Eingabe in array splitten um dann sepperat zu speichern
        String[] parts = input.split(":");
        if(parts.length!=3){
            System.err.println("Invalid format, please use IP:PORT:NAME");
            System.exit(0);
        }
        else{
            host=parts[0];
            port=Integer.parseInt(parts[1]);
            name=parts[2];
        }

        connect();
        handle();
    }


    PrintWriter out;
    BufferedReader in;

    private void connect(){
        try {
            socket= new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Connected.");
        } catch (IOException e) {
            System.err.println("Failed to connect.");
            System.exit(0);
        }
    }

    private void handle(){
        Thread listenerThread = new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println(serverMessage);
                }
            } catch (IOException e) {
                System.out.println("Connection closed by server.");
            }
        });
        listenerThread.start();


        // Main loop to send messages
        while (true) {
            String message = inputScanner.nextLine();
            if (message.equalsIgnoreCase("exit")) {
                break;
            }
            out.println(message);
        }
    }
}
