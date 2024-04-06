
// Import statements for Jsoup (a Java HTML parser) and Java networking and I/O classes.
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;

/**
 * The Downloader class represents a component responsible for downloading web
 * pages.
 * It initializes necessary components and starts the crawling loop.
 */
public class Downloader {

    private static int msgId = 0; // Message ID counter
    private static Registry registry; // RMI registry
    private static GatewayInterface gateway; // Gateway interface for communication with the server
    private static final ConcurrentHashMap<String, Integer> urlInboundReferenceCount = new ConcurrentHashMap<>(); // Map
                                                                                                                  // to
                                                                                                                  // track
                                                                                                                  // inbound
                                                                                                                  // reference
                                                                                                                  // counts
                                                                                                                  // for
                                                                                                                  // URLs

    /**
     * The main method of the Downloader class.
     * It initializes necessary components and starts the crawling loop.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        initialize();
        startCrawlingLoop();
    }

    /**
     * Initializes the RMI registry and obtains the gateway interface.
     * It retrieves the RMI host and port from the application configuration
     * properties.
     * 
     * @throws RemoteException   If a communication-related exception occurs during
     *                           RMI initialization.
     * @throws NotBoundException If the specified name in the registry is not
     *                           currently bound.
     */
    private static void initialize() {
        try {
            // Retrieve RMI host and port from application configuration properties
            String rmiHost = AppConfig.getProperty("rmi.registry.host");
            int rmiPort = Integer.parseInt(AppConfig.getProperty("rmi.registry.port"));

            // Get the RMI registry
            registry = LocateRegistry.getRegistry(rmiHost, rmiPort);

            // Obtain the gateway interface from the registry
            gateway = (GatewayInterface) registry.lookup("Gateway");
        } catch (RemoteException | NotBoundException e) {
            // Handle exceptions
            handleException("Error initializing RMI registry", e);
        }
    }

    /**
     * Starts the crawling loop to continuously fetch new URLs from the gateway and
     * process them.
     * 
     * <p>
     * This method retrieves new URLs from the gateway interface in a loop and
     * processes them.
     * If no URL is available, it waits for a short period before checking again.
     * 
     * @throws RemoteException      If a communication-related exception occurs
     *                              during RMI communication.
     * @throws InterruptedException If the thread is interrupted while sleeping.
     */
    private static void startCrawlingLoop() {
        try {
            while (true) {
                // Retrieve new URL from the gateway
                final DepthControl obj = gateway.getNewUrl();

                if (obj != null) {
                    // Process the URL
                    processUrl(obj);
                } else {
                    // If no URL is available, wait for a short period
                    //System.out.println("No URL to process. Waiting...");
                    Thread.sleep(500);
                }
            }
        } catch (RemoteException | InterruptedException e) {
            // Handle exceptions
            handleException("Error during crawling loop", e);
        }
    }

    /**
     * Processes the given URL.
     * 
     * <p>
     * This method processes the provided URL by fetching its content, extracting
     * hyperlinks,
     * and sending page content information to the gateway.
     * 
     * @param dcObj The DepthControl object representing the URL to be processed.
     */
    private static void processUrl(DepthControl dcObj) {
        String url = dcObj.getUrl();
        try {
            // Check if the URL starts with "http://" or "https://"
            if (url.startsWith("http://") || url.startsWith("https://")) {
                // Fetch the content of the URL using Jsoup
                Document doc = Jsoup.connect(url).get();
                System.out.println("Processing URL: " + url);

                // Extract hyperlinks and process them
                Set<String> hyperlinks = extractAndProcessHyperlinks(doc, dcObj);

                // Send page content information to the gateway
                sendPageContentInfo(doc, url, hyperlinks);
            } else {
                // Skip non-HTTP/HTTPS URLs
                System.out.println("Skipping non-HTTP/HTTPS URL: " + url);

            }
        } catch (IOException e) {
            // Handle IO exceptions
            handleException("Error processing URL: " + url, e);
        }
    }

    /**
     * Extracts hyperlinks from the provided HTML document and queues them up for
     * processing.
     * 
     * <p>
     * This method extracts hyperlinks from the given HTML document using Jsoup
     * library.
     * It then creates DepthControl objects for each extracted URL and queues them
     * up for processing
     * by the gateway. Additionally, it updates the count of inbound references for
     * each URL.
     * 
     * @param doc   The HTML document from which hyperlinks are to be extracted.
     * @param dcObj The DepthControl object representing the URL being processed.
     * @return A set containing the unique hyperlinks extracted from the document.
     * @throws RemoteException If a remote communication-related exception occurs.
     */
    private static Set<String> extractAndProcessHyperlinks(Document doc, DepthControl dcObj) throws RemoteException {
        Set<String> uniqueHyperlinks = new HashSet<>();

        // Select all anchor elements with an href attribute
        Elements links = doc.select("a[href]");

        // Iterate over each link element
        for (Element link : links) {
            // Extract the absolute URL from the href attribute
            String newUrl = link.attr("abs:href");

            // Add the URL to the set of unique hyperlinks if not already present
            if (uniqueHyperlinks.add(newUrl)) {
                System.out.println("Added: " + newUrl);

                // Create a new DepthControl object for the extracted URL with increased depth
                //DepthControl newDc = new DepthControl(newUrl, dcObj.getDepth() + 1);

                // Queue up the new URL for processing by the gateway
                //gateway.queueUpUrl(newDc);

                // Update the count of inbound references for the URL
                urlInboundReferenceCount.merge(newUrl, 1, Integer::sum);
            }
        }
        return uniqueHyperlinks;
    }

    /**
     * Sends page content information to the gateway.
     * 
     * <p>
     * This method creates a PageContent object using the provided HTML document,
     * URL, set of hyperlinks, and the count of inbound references for the URL. It
     * then
     * sends this information to the gateway for further processing.
     * 
     * @param doc        The HTML document from which page content information is
     *                   extracted.
     * @param url        The URL of the webpage.
     * @param hyperlinks The set of hyperlinks present in the webpage.
     */
    private static void sendPageContentInfo(Document doc, String url, Set<String> hyperlinks) {
        try {
            // Retrieve the number of inbound references for the URL, defaulting to 0 if not
            // found
            int numberOfInboundReferences = urlInboundReferenceCount.getOrDefault(url, 0);

            // Create a PageContent object using the document, URL, hyperlinks, and inbound
            // reference count
            PageContent info = new PageContent(doc.title(), doc.body().text(), url, hyperlinks,
                    numberOfInboundReferences);

            // Send the page content information to the gateway for further processing
            sendInfo(info);
        } catch (IOException e) {
            // Handle IOException by logging the error
            handleException("Error sending page content info", e);
        }
    }

    /**
     * Sends page content information over a multicast network.
     * 
     * <p>
     * This method serializes the provided PageContent object, compresses it using
     * GZIP,
     * and then sends it over a multicast network. The multicast address, port, and
     * maximum
     * packet size are retrieved from the application configuration. The method
     * splits the
     * serialized data into sections based on the maximum packet size and sends each
     * section
     * as a separate DatagramPacket.
     * 
     * @param info The PageContent object containing information about the webpage.
     * @throws IOException           If an I/O error occurs during the multicast
     *                               operation.
     * @throws IllegalStateException If the multicast address, port, or max packet
     *                               size is not defined
     *                               in the application configuration, or if an
     *                               invalid value is encountered.
     */
    private static void sendInfo(PageContent info) throws IOException {
        System.out.println("Sending page content info...");

        // Retrieve multicast address from configuration
        String multicastAddress = AppConfig.getProperty("multicast.address");
        if (multicastAddress == null) {
            throw new IllegalStateException("Multicast address is not defined in the configuration.");
        }

        // Retrieve multicast port from configuration
        int multicastPort;
        try {
            String portString = AppConfig.getProperty("multicast.port");
            if (portString == null) {
                throw new IllegalStateException("Multicast port is not defined in the configuration.");
            }
            multicastPort = Integer.parseInt(portString);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid multicast port: " + e.getMessage(), e);
        }

        // Retrieve max packet size from configuration
        int maxPacketSize;
        try {
            String maxPacketSizeString = AppConfig.getProperty("max.packet.size");
            if (maxPacketSizeString == null) {
                throw new IllegalStateException("Max packet size is not defined in the configuration.");
            }
            maxPacketSize = Integer.parseInt(maxPacketSizeString);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid max packet size: " + e.getMessage(), e);
        }

        // Get network interface for multicast
        NetworkInterface networkInterface = NetworkInterface.getNetworkInterfaces().nextElement();
        msgId++;

        // Serialize and send the data over multicast
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream);
                ObjectOutputStream objectStream = new ObjectOutputStream(gzipStream)) {

            // Serialize the PageContent object
            objectStream.writeObject(info);
            objectStream.close();

            // Convert serialized data to byte array
            byte[] data = byteStream.toByteArray();
            int allParts = (data.length + maxPacketSize - 1) / maxPacketSize;

            // Send data in sections over multicast
            try (MulticastSocket socket = new MulticastSocket()) {
                socket.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);

                // Send each section of data as a separate DatagramPacket
                for (int i = 0; i < allParts; i++) {
                    int start = i * maxPacketSize;
                    int end = Math.min(data.length, (i + 1) * maxPacketSize);
                    byte[] section = Arrays.copyOfRange(data, start, end);

                    try (ByteArrayOutputStream sectionStream = new ByteArrayOutputStream();
                            DataOutputStream dataStream = new DataOutputStream(sectionStream)) {

                        // Write section data to output stream
                        dataStream.writeInt(msgId);
                        dataStream.writeInt(i);
                        dataStream.writeInt(allParts);
                        dataStream.write(section);

                        // Get byte array of section data and create DatagramPacket
                        byte[] packetData = sectionStream.toByteArray();
                        DatagramPacket packet = new DatagramPacket(packetData, packetData.length,
                                InetAddress.getByName(multicastAddress), multicastPort);

                        // Send DatagramPacket over multicast
                        socket.send(packet);
                    }
                }
            }
        }
    }

    /**
     * Handles an exception by printing an error message and stack trace to the
     * standard error stream.
     * 
     * @param message The error message to be printed.
     * @param e       The exception that occurred.
     */
    private static void handleException(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();
    }

}
