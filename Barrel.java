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
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.zip.GZIPInputStream;

public class Barrel {

    
    private MulticastSocket socket;
    private InetAddress gpAddress;
    private NetworkInterface netInterface;

    private final HashMap<String, ConcurrentSkipListSet<PageContent>> pages = new HashMap<>();
    private final HashMap<String, Set<String>> urlHyperlinkIndex = new HashMap<>();
    private static final Set<UUID> activeBarrels = new HashSet<>();
    private final UUID id;

    public Barrel() throws IOException {
    try {
        String multicastAddress = AppConfig.getProperty("multicast.address");
        int multicastPort = Integer.parseInt(AppConfig.getProperty("multicast.port"));
        gpAddress = InetAddress.getByName(multicastAddress);
        socket = new MulticastSocket(multicastPort);
        netInterface = NetworkInterface.getNetworkInterfaces().nextElement();

       if (gpAddress.isMulticastAddress()) {
            socket.setOption(StandardSocketOptions.IP_MULTICAST_IF, netInterface);
            socket.joinGroup(new InetSocketAddress(gpAddress, multicastPort), netInterface);
        }
        
        synchronized (Barrel.class) {
            this.id = UUID.randomUUID();
        }
        
        activeBarrels.add(this.id);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> activeBarrels.remove(this.id)));
        

    } catch (IOException e) {
        handleException("Error initializing Barrel", e);
        throw e; 
    } catch (NoSuchElementException e) {
        handleException("No network interface available", e);
        throw new IOException("No network interface available", e); 
    }
}

    public UUID getId() {
        return this.id;
    }

    public void listenMsg() {
        byte[] buffer = new byte[65536];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        Map<Integer, Map<Integer, byte[]>> sections = new HashMap<>();
        Map<Integer, Integer> sectionsCount = new HashMap<>();

        while (true) {
            try {
                socket.receive(packet);
                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                DataInputStream dis = new DataInputStream(bais);

                int msgId = dis.readInt();
                int partNumber = dis.readInt();
                int allParts = dis.readInt();

                byte[] dataSection = new byte[packet.getLength() - 12];
                dis.readFully(dataSection);

                sections.putIfAbsent(msgId, new HashMap<>());
                sections.get(msgId).put(partNumber, dataSection);
                sectionsCount.putIfAbsent(msgId, 0);

                if (sections.get(msgId).size() == allParts) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    for (int i = 0; i < allParts; i++) {
                        baos.write(sections.get(msgId).get(i));
                    }

                    try (GZIPInputStream inZip = new GZIPInputStream(new ByteArrayInputStream(baos.toByteArray()));
                         ObjectInputStream ois = new ObjectInputStream(inZip)) {
                        Object obj = ois.readObject();
                        if (obj instanceof PageContent) {
                            PageContent info = (PageContent) obj;
                            storeMsg(info);
                        }
                    } catch (ClassNotFoundException e) {
                        handleException("ClassNotFoundException in listenMsg", e);
                    }
                }
            } catch (IOException e) {
                handleException("IOException in listenMsg", e);
            }
        }
    }
    private void storeMsg(PageContent info) {
        Set<String> words = info.getWords(); // Get the set of words from PageContent
        for (String word : words) {
            String lowerCaseWord = word.toLowerCase();
            pages.computeIfAbsent(lowerCaseWord, k -> new ConcurrentSkipListSet<>()).add(info);
        }


        // Storing hyperlinks for the URL
        String url = info.getUrl();
        //System.out.println(url);
        Set<String> hyperlinks = info.getHyperLinks();
        synchronized (urlHyperlinkIndex) {
           
            //System.out.println(hyperlinks);
            urlHyperlinkIndex.put(url, hyperlinks);
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(AppConfig.getProperty("rmi.registry.port"));

        try {
            System.out.println("[Barrel] Starting Barrel service...");
            System.out.println("[Barrel] Creating RMI registry on port " + port);
            try {
                LocateRegistry.createRegistry(port);
            } catch (RemoteException e) {
                System.out.println("[Barrel] RMI registry already exists on port 1099");
            }

            System.out.println("[Barrel] Instantiating Barrel and BarrelFunc objects");
            Barrel barrel = new Barrel();
           

            
            BarrelFunc barrelFunc = new BarrelFunc(barrel.getId(), barrel.pages, barrel.urlHyperlinkIndex);

           
            System.out.println("[Barrel] Getting the Registry reference");
            Registry reg = LocateRegistry.getRegistry("localhost", port);

            System.out.println("[Barrel] Binding the BarrelFunc to the registry");
            reg.rebind("Barrel", barrelFunc);

            System.out.println("[Barrel] Starting to listen for messages");
            barrel.listenMsg();
        } catch (IOException e) {
            System.err.println("[Barrel] Exception occurred: " + e.toString());
            e.printStackTrace();
        }
    }


    private static void handleException(String message, Exception e) {
        System.err.println("[Barrel] " + message + ": " + e.toString());
        e.printStackTrace();
    }
}


