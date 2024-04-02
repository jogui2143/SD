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
import java.util.Set;
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

    private static final Set<Integer> activeBarrels = new HashSet<>();

    private int id;

    public static int count = 0;

    public Barrel() throws IOException {
        gpAddress = InetAddress.getByName(MULTICAST_ADDRESS);
        socket = new MulticastSocket(MT_PORT);
        netInterface = NetworkInterface.getNetworkInterfaces().nextElement();

        if (gpAddress instanceof InetAddress) {
            socket.setOption(StandardSocketOptions.IP_MULTICAST_IF, netInterface);
        }
        id = count++;
        socket.joinGroup(new InetSocketAddress(gpAddress, MT_PORT), netInterface);
        activeBarrels.add(this.id);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> activeBarrels.remove(this.id)));
        
    }

    public static Set<Integer> getActiveBarrels() {
        return new HashSet<>(activeBarrels);
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
            Barrel barrel = new Barrel();
            BarrelFunc barrelFunc = new BarrelFunc(pages);
            Registry reg = LocateRegistry.getRegistry(1099);
            reg.rebind("Barrel", barrelFunc);
            barrel.listenMsg();
        } catch (IOException e) {
            System.err.println("Exception on barrel" + e.toString());
            e.printStackTrace();
        }
    }
}
