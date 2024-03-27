// Import statements for various Java classes used in the program.
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

// The Barrel class definition begins here.
public class Barrel {

    // Constants for multicast networking.
    private static final String MULTICAST_ADDRESS = "225.1.2.3";
    private static final int MT_PORT = 7002;

    // Variables for socket and network interface.
    private MulticastSocket socket;
    private InetAddress gpAddress;
    private NetworkInterface netInterface;

    // A static hashmap to store URLs.
    private static final HashMap<String, HashSet<String>> urls = new HashMap<>();

    // Constructor of the Barrel class.
    public Barrel() throws IOException {
        // Initializing the multicast address and socket.
        gpAddress = InetAddress.getByName(MULTICAST_ADDRESS);
        socket = new MulticastSocket(MT_PORT);
        netInterface = NetworkInterface.getNetworkInterfaces().nextElement();
        
        // Setting socket options for multicast.
        if (gpAddress instanceof InetAddress) {
            socket.setOption(StandardSocketOptions.IP_MULTICAST_IF, netInterface);
        }

        // Joining the multicast group.
        socket.joinGroup(new InetSocketAddress(gpAddress, MT_PORT), netInterface);
    }

    // Method to listen for multicast messages.
    public void listenMsg() {
        // Buffer for receiving packets.
        byte[] buffer = new byte[65536];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        // Data structures for managing message parts.
        Map<Integer, Map<Integer, byte[]>> sections = new HashMap<>();
        Map<Integer, Integer> sectionsCount = new HashMap<>();

        // Infinite loop to keep listening for messages.
        while (true) {
            try {
                // Receiving a packet.
                socket.receive(packet);

                // Reading data from the packet.
                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                DataInputStream dis = new DataInputStream(bais);

                // Extracting message parts (ID, part number, total parts).
                int msgId = dis.readInt();
                int partNumber = dis.readInt();
                int allParts = dis.readInt();

                // Reading the data section of the packet.
                byte[] dataSection = new byte[packet.getLength() - 12];
                dis.readFully(dataSection);

                // Storing the received part of the message.
                sections.putIfAbsent(msgId, new HashMap<>());
                sections.get(msgId).put(partNumber, dataSection);
                sectionsCount.putIfAbsent(msgId, 0);

                // Check if all parts of the message have been received.
                if (sections.get(msgId).size() == allParts) {
                    // Assemble the complete message.
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    for (int i = 0; i < allParts; i++) {
                        baos.write(sections.get(msgId).get(i));
                    }

                    // Decompressing and deserializing the message.
                    try (GZIPInputStream inZip = new GZIPInputStream(new ByteArrayInputStream(baos.toByteArray()));
                         ObjectInputStream ois = new ObjectInputStream(inZip);) {
                        Object obj = ois.readObject();

                        // Handling the deserialized PageContent object.
                        if (obj instanceof PageContent) {
                            PageContent info = (PageContent) obj;
                            System.out.println("Title: " + info.getTitle());
                            System.out.println("Text: " + info.getText());

                            // Store the message for further processing.
                            storeMsg(info);
                        }
                    } catch (ClassNotFoundException e) {
                        // Handling exceptions during deserialization.
                        System.err.println("Exception on listenMsg" + e.toString());
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                // Handling IOException during packet receiving.
                System.err.println("Exception on listenMsg" + e.toString());
                e.printStackTrace();
            }
        }
    }

    // Method to store the message in the static URL map.
    private void storeMsg(PageContent info) {
        // Extracting URL from the PageContent object.
        String url = info.getUrl();

        // Tokenizing the text content for word processing.
        StringTokenizer st = new StringTokenizer(info.getText(), "\t\n\r\f,.:;?![]'\"");

        // Iterating through each token (word) and storing it
        // in the urls hashmap.
        while (st.hasMoreTokens()) {
          String word = st.nextToken().toLowerCase();
          // For each word, add the URL to the corresponding set in the hashmap.
          urls.computeIfAbsent(word, k -> new HashSet<>()).add(url);
      }
  }

  // The main method - entry point of the program.
  public static void main(String[] args) {
      try {
          // Creating an instance of Barrel.
          Barrel barrel = new Barrel();

          // Creating an instance of BarrelFunc with the URLs hashmap.
          BarrelFunc barrelFunc = new BarrelFunc(urls);

          // Creating and starting a RMI registry on port 1099.
          Registry reg = LocateRegistry.getRegistry(1099);

          // Binding the BarrelFunc object with the name "Barrel" in the registry.
          reg.rebind("Barrel", barrelFunc);

          // Start listening for multicast messages.
          barrel.listenMsg();
      } catch (IOException e) {
          // Handling IOException in the main method.
          System.err.println("Exception on barrel" + e.toString());
          e.printStackTrace();
      }
  }
}
