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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.zip.GZIPInputStream;

public class Barrel{

    private static final String MULTICAST_ADDRESS = "225.1.2.3";
    private static final int MT_PORT = 7002;

    private MulticastSocket socket;
    private InetAddress gpAddress;
    private NetworkInterface netInterface;

    // HashMap to store PageContent objects indexed by words.
   // ConcurrentHashMap to store PageContent objects indexed by words.
    private static final ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>> pages = new ConcurrentHashMap<>();

    private static final Set<UUID> activeBarrels = new HashSet<>();
    private final UUID id; // Unique ID for each Barrel instance

    public Barrel() throws IOException {
        gpAddress = InetAddress.getByName(MULTICAST_ADDRESS);
        socket = new MulticastSocket(MT_PORT);
        netInterface = NetworkInterface.getNetworkInterfaces().nextElement();

        if (gpAddress instanceof InetAddress) {
            socket.setOption(StandardSocketOptions.IP_MULTICAST_IF, netInterface);
        }
        
        socket.joinGroup(new InetSocketAddress(gpAddress, MT_PORT), netInterface);
        synchronized (Barrel.class) {
            this.id = UUID.randomUUID(); // Generate a unique ID
        }
        activeBarrels.add(this.id);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> activeBarrels.remove(this.id)));
        
    }

    public UUID getId() throws RemoteException {
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
                        System.err.println("Exception on listenMsg" + e.toString());
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                System.err.println("Exception on listenMsg" + e.toString());
                e.printStackTrace();
            }
        }
    }

    private void storeMsg(PageContent info) {
        // Split text on characters that are not letters, apostrophes, hyphens, or specified special characters
        String[] words = info.getText().split("[^\\p{L}'-^~´`ç]+");
    
        for (String word : words) {
            if (!word.trim().isEmpty()) {
                String lowerCaseWord = word.toLowerCase();
                pages.computeIfAbsent(lowerCaseWord, k -> new ConcurrentSkipListSet<>()).add(info);
            }
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("[Barrel] Starting Barrel service...");
            System.out.println("[Barrel] Creating RMI registry on port 1100");
            //LocateRegistry.createRegistry(1099);
    
            System.out.println("[Barrel] Instantiating Barrel and BarrelFunc objects");
            Barrel barrel = new Barrel();
            BarrelFunc barrelFunc = new BarrelFunc(barrel.getId(), pages);
    
            System.out.println("[Barrel] Getting the Registry reference");
            Registry reg = LocateRegistry.getRegistry("localhost", 1099);
    
            System.out.println("[Barrel] Binding the BarrelFunc to the registry");
            reg.rebind("Barrel", barrelFunc);
    
            System.out.println("[Barrel] Starting to listen for messages");
            barrel.listenMsg();
        } catch (IOException e) {
            System.err.println("[Barrel] Exception occurred: " + e.toString());
            e.printStackTrace();
        }
    }
}
