
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class MultiEchoServerNIO {

  private static ServerSocketChannel serverSocketChannel;
  private static final int PORT = 1234;
  private static List<SocketChannel> channelList = new ArrayList<>();

  private static Selector selector;

  public static void main(String[] args) {
    ServerSocket serverSocket;

    System.out.println("Opening port...\n");

    try {
      serverSocketChannel = ServerSocketChannel.open();
      serverSocketChannel.configureBlocking(false);
      serverSocket = serverSocketChannel.socket();

      InetSocketAddress netAddress = new InetSocketAddress(PORT);

      //Bind socket to port...
      serverSocket.bind(netAddress);

      //Create a new Selector object for detecting input from channels...
      selector = Selector.open();

      //Register ServerSocketChannel with Selector for receiving incoming connections...
      serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

    } catch (IOException ioEx) {
      ioEx.printStackTrace();
      System.exit(1);
    }

    processConnections();
  }

  private static void processConnections() {
    do {
      try {
        //Get number of events (new connection(s) and/or data transmissions from existing connection(s))...
        int numKeys = selector.select();

        if (numKeys > 0) {
          //Extract event(s) that have been triggered ...
          Set eventKeys = selector.selectedKeys();

          //Set up Iterator to cycle though set of events...
          Iterator keyCycler = eventKeys.iterator();

          while (keyCycler.hasNext()) {
            SelectionKey key = (SelectionKey) keyCycler.next();

            //Retrieve set of ready ops for this key (as a bit pattern)...
            int keyOps = key.readyOps();

            if ((keyOps & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {//New connection.
              acceptConnection(key);
              continue;
            }
            if ((keyOps & SelectionKey.OP_READ) == SelectionKey.OP_READ) {//Data from existing client.
              acceptData(key);
            }
          }
        }
      } catch (IOException ioEx) {
        ioEx.printStackTrace();
        System.exit(1);
      }
    } while (true);
  }

  private static void acceptConnection(SelectionKey key) throws IOException {//Accept incoming connection and add to list.
    SocketChannel socketChannel;
    Socket socket;

    socketChannel = serverSocketChannel.accept();
    socketChannel.configureBlocking(false);
    socket = socketChannel.socket();
    System.out.println("Connection on " + socket + ".");

    //Register SocketChannel for receiving data...
    socketChannel.register(selector, SelectionKey.OP_READ);

    channelList.add(socketChannel);

    //Avoid re-processing this event as though it were a new one (next time through loop)...
    selector.selectedKeys().remove(key);
  }

  private static void acceptData(SelectionKey key) throws IOException {//Accept data from existing connection.
    SocketChannel socketChannel;
    Socket socket;
    ByteBuffer buffer = ByteBuffer.allocate(2048);
    //Above used for reading/writing data from/to SocketChannel.

    socketChannel = (SocketChannel) key.channel();
    buffer.clear();
    int numBytes = socketChannel.read(buffer);
    System.out.println(numBytes + " bytes read.");
    socket = socketChannel.socket();

    if (numBytes == -1) //OP_READ event also triggered by closure of connection or error of some kind. In either case, numBytes = -1.
    {
      //Request that registration of this key's channel with its selector be cancelled...
      key.cancel();

      System.out.println("\nClosing socket " + socket + "...");
      closeSocket(socket);
    } else {
      try {
        buffer.flip();

        ByteBuffer ackBuffer = ByteBuffer.allocate(2048).put("ok\n".getBytes());
        ackBuffer.flip();

        ByteBuffer buff = ByteBuffer.allocate(2048).put(buffer);

        for (SocketChannel s : channelList) {
          if (s.socket().equals(socketChannel.socket())) {
            s.write(ackBuffer);
          } else {
            buff.flip();
            s.write(buff);
          }
        }

      } catch (IOException ioEx) {
        System.out.println("\nClosing socket " + socket + "...");
        closeSocket(socket);
      }
    }
    //Remove this event, to avoid re-processing it as though it were a new one...
    selector.selectedKeys().remove(key);
  }

  private static void closeSocket(Socket socket) {
    try {
      if (socket != null) {
        socket.close();
      }
    } catch (IOException ioEx) {
      System.out.println("*** Unable to close socket! ***");
    }
  }
}
