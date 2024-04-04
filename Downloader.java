
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;


public class Downloader {
    private static final String MULTICAST_ADDRESS = "225.1.2.3";
    private static final int MULTICAST_PORT = 7002;
    private static final int MAX_PACKET_SIZE = 65000;
    private static int msgId = 0;
    private static Registry registry;
    private static GatewayInterface gateway;
    private static final Map<String, Integer> urlInboundReferenceCount = new HashMap<>();

    public static void main(String[] args) {
        initialize();
        startCrawlingLoop();
    }

    private static void initialize() {
        try {
            registry = LocateRegistry.getRegistry("localhost");
            gateway = (GatewayInterface) registry.lookup("Gateway");
        } catch (RemoteException | NotBoundException e) {
            handleException("Error initializing RMI registry", e);
        }
    }

   

    private static void startCrawlingLoop() {
        try {
            while (true) {
                final DepthControl obj = gateway.getNewUrl();
                if (obj != null) {
                    processUrl(obj);
                } else {
                    System.out.println("No URL to process. Waiting...");
                    Thread.sleep(500);
                }
            }
        } catch (RemoteException | InterruptedException e) {
            handleException("Error during crawling loop", e);
        }
    }

    private static void processUrl(DepthControl dcObj) {
        String url = dcObj.getUrl();
        try {
            // Check if the URL protocol is HTTP or HTTPS
            if (url.startsWith("http://") || url.startsWith("https://")) {
                Document doc = Jsoup.connect(url).get();
                System.out.println("Processing URL: " + url);
                extractHyperlinks(doc, dcObj);
                sendPageContentInfo(doc, url);
            } else {
                System.out.println("Skipping non-HTTP/HTTPS URL: " + url);
                // Handle non-HTTP/HTTPS URLs differently or log them
            }
        } catch (IOException e) {
            handleException("Error processing URL: " + url, e);
        }
    }

    private static void extractHyperlinks(Document doc, DepthControl dcObj) throws RemoteException {
        List<String> hyperlinks = new ArrayList<>();
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String newUrl = link.attr("abs:href");
            hyperlinks.add(newUrl);
            urlInboundReferenceCount.merge(newUrl, 1, Integer::sum);
            DepthControl newDc = new DepthControl(newUrl, dcObj.getDepth() + 1);
            gateway.queueUpUrl(newDc);
        }
    }

    private static void sendPageContentInfo(Document doc, String url) {
        try {
            List<String> hyperlinks = extractHyperlinks(doc);
            int numberOfInboundReferences = urlInboundReferenceCount.getOrDefault(url, 0);
            PageContent info = new PageContent(doc.title(), doc.body().text(), url, hyperlinks, numberOfInboundReferences);
            sendInfo(info);
        } catch (IOException e) {
            handleException("Error sending page content info", e);
        }
    }

    private static List<String> extractHyperlinks(Document doc) {
        List<String> hyperlinks = new ArrayList<>();
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String newUrl = link.attr("abs:href");
            hyperlinks.add(newUrl);
        }

        System.out.println("Contents of urlInboundReferenceCount:");
    for (Map.Entry<String, Integer> entry : urlInboundReferenceCount.entrySet()) {
        System.out.println(entry.getKey() + ": " + entry.getValue());
    }
        return hyperlinks;
    }

    private static void sendInfo(PageContent info) throws IOException {
        System.out.println("Sending page content info...");
        InetAddress multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);
        NetworkInterface networkInterface = NetworkInterface.getNetworkInterfaces().nextElement();
        msgId++;

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream);
             ObjectOutputStream objectStream = new ObjectOutputStream(gzipStream)) {

            objectStream.writeObject(info);
            objectStream.close();

            byte[] data = byteStream.toByteArray();
            int allParts = (data.length + MAX_PACKET_SIZE - 1) / MAX_PACKET_SIZE;

            try (MulticastSocket socket = new MulticastSocket()) {
                socket.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);

                for (int i = 0; i < allParts; i++) {
                    int start = i * MAX_PACKET_SIZE;
                    int end = Math.min(data.length, (i + 1) * MAX_PACKET_SIZE);
                    byte[] section = Arrays.copyOfRange(data, start, end);

                    try (ByteArrayOutputStream sectionStream = new ByteArrayOutputStream();
                         DataOutputStream dataStream = new DataOutputStream(sectionStream)) {

                        dataStream.writeInt(msgId);
                        dataStream.writeInt(i);
                        dataStream.writeInt(allParts);
                        dataStream.write(section);

                        byte[] packetData = sectionStream.toByteArray();
                        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, multicastAddress, MULTICAST_PORT);
                        socket.send(packet);
                    }
                }
            }
        }
    }

    private static void handleException(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();
    }
}

