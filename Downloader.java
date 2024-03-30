// Import statements for Jsoup (a Java HTML parser) and Java networking and I/O classes.
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;

// Definition of the Downloader class.
public class Downloader {
    // Constants for multicast networking.
    private static final String MULTICAST_ADDRESS = "225.1.2.3";
    private static final int MT_PORT = 7002;
    // A Map to keep track of the number of times each URL is referenced.
    private static final Map<String, Integer> urlReferenceCount = new HashMap<>();

    // GatewayInterface variable for RMI communication.
    private static GatewayInterface gateway;

    // Main method - entry point of the application.
    public static void main(String[] args) {
        try {
            // Connecting to the RMI registry and looking up the GatewayInterface.
            Registry reg = LocateRegistry.getRegistry("localhost");
            gateway = (GatewayInterface) reg.lookup("Gateway");

            DepthControl obj;
            // Continuously loop to get and process new URLs.
            while(true){
                obj = gateway.getNewUrl();
                // Check if a new URL is available for processing.
                if(obj != null){
                    System.out.println("URL: " +obj.getUrl());
                    startCrawling(obj);
                } else{
                    // No new URL to crawl; the thread sleeps for a while.
                    System.out.println("No new URL to crawl");
                    Thread.sleep(5000);
                }
            }
        } catch (Exception e) {
            // Handling exceptions.
            System.err.println("Exception on downloader" + e.toString());
            e.printStackTrace();
        }
    }
    
    // Method to start the web crawling process.
    private static void startCrawling(DepthControl dcObj){
        try {

            String url = dcObj.getUrl();
            // Connect to the URL and parse the HTML document.
            Document doc = Jsoup.connect(url).get();
            
            System.out.println("Processing URL: " + url);
            // Selecting all hyperlink elements in the document.
             Elements links = doc.select("a[href]");
             for(Element link : links){
                String newUrl = link.attr("abs:href");

                // Increment the count for each URL found.
                urlReferenceCount.put(newUrl, urlReferenceCount.getOrDefault(newUrl, 0) + 1);
                DepthControl newDc = new DepthControl(newUrl,dcObj.getDepth() + 1);
                gateway.queueUpUrl(newDc);
            }
            // Extract the title and text content of the web page.
           
            int numberOfLinks = urlReferenceCount.getOrDefault(url, 0);

            // Creating a PageContent object with the extracted information and number of references.
            PageContent info = new PageContent(doc.title(), doc.body().text(), url, numberOfLinks);

            sendInfo(info);

           

        } catch (IOException e) {
            // Handling IOException during web crawling.
            System.err.println("Exception on startCrawling" + e.toString());
            e.printStackTrace();
        } catch (Exception e) {
            // Handling other exceptions during URL queueing.
            System.err.println("Error queueing up :) CARALHO" + e.toString());
            e.printStackTrace();
        }
    }

    // Constants and variable for managing packet size and message IDs.
    private static final int MAX_PACKET_SIZE = 65000; // Maximum size for a datagram packet.
    private static int msgId = 0; // Variable to track message IDs.

    // Method to send the PageContent info using multicast.
    private static void sendInfo(PageContent info) throws IOException {
        System.out.println("sending info ");
        InetAddress gpAddress = InetAddress.getByName(MULTICAST_ADDRESS);
        NetworkInterface netInterface = NetworkInterface.getNetworkInterfaces().nextElement();
        msgId++; // Increment message ID for each new message.

        // Setting up streams for serialization and compression.
        try (ByteArrayOutputStream byteChad = new ByteArrayOutputStream();
             GZIPOutputStream outZip = new GZIPOutputStream(byteChad);
             ObjectOutputStream oos = new ObjectOutputStream(outZip);) {

            // Writing the PageContent object to the ObjectOutputStream.
            oos.writeObject(info);
            oos.close();
            // Convert the serialized data to a byte array.
            byte[] data = byteChad.toByteArray();

            // Calculate the number of packets needed to send the data.
            int allParts = (data.length + MAX_PACKET_SIZE - 1) / MAX_PACKET_SIZE;
            // Create a multicast socket.
            try (MulticastSocket socket = new MulticastSocket()) {
                // Set socket options for multicast.
                socket.setOption(StandardSocketOptions.IP_MULTICAST_IF, netInterface);
                // Loop to send each part of the data as a separate
                // packet.
                for (int i = 0; i < allParts; i++) {
                    // Calculate start and end indices for the current packet's data section.
                    int start = i * MAX_PACKET_SIZE;
                    int end = Math.min(data.length, (i + 1) * MAX_PACKET_SIZE);
                    // Extract the relevant section of data for this packet.
                    byte[] section = Arrays.copyOfRange(data, start, end);

                    // Setting up a stream to construct the packet payload.
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    // Writing message ID, part number, total parts, and the data section to the stream.
                    dos.writeInt(msgId);
                    dos.writeInt(i);
                    dos.writeInt(allParts);
                    dos.write(section);
                    // Convert the stream to a byte array for the packet.
                    byte[] packData = baos.toByteArray();

                    // Creating the datagram packet with the multicast address and port.
                    DatagramPacket packet = new DatagramPacket(packData, packData.length, gpAddress, MT_PORT);
                    // Sending the packet via the multicast socket.
                    socket.send(packet);
                }
            }
        }
    }
}
