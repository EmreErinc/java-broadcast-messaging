
import java.io.*;
import java.net.*;
import java.util.*;

public class MultiEchoClientListener {

  private static InetAddress host;
  private static final int PORT = 1234;

  public static void main(String[] args) {
    try {
      host = InetAddress.getLocalHost();
    } catch (UnknownHostException uhEx) {
      System.out.println("\nHost ID not found!\n");
      System.exit(1);
    }
    sendMessages();
  }

  private static void sendMessages() {
    Socket socket = null;

    try {
      socket = new Socket(host, PORT);
      Scanner networkInput = new Scanner(socket.getInputStream());

      String response;
      do {
        response = networkInput.nextLine();
        System.out.println("\nSERVER> " + response);
      } while (true);
    } catch (IOException ioEx) {
      ioEx.printStackTrace();
    } finally {
      try {
        System.out.println("\nClosing connection...");
        socket.close();
      } catch (IOException ioEx) {
        System.out.println("Unable to disconnect!");
        System.exit(1);
      }
    }
  }
}
