import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

public class Barrel {

    
    private MulticastSocket socket;
    private InetAddress gpAddress;
    private NetworkInterface netInterface;

    private final ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>> pages = new ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>>();
    private final ConcurrentHashMap<String, Set<String>> urlHyperlinkIndex = new ConcurrentHashMap<String, Set<String>>();
    private static final Set<UUID> activeBarrels = new HashSet<>();
    private final UUID id;
    private final AtomicLong lastStoreTime = new AtomicLong(System.currentTimeMillis());
    private ScheduledExecutorService scheduler;

    public Barrel() throws IOException {
        loadState();
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
        startPeriodicSave();
        

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

    private void startPeriodicSave() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = () -> {
            long timeSinceLastStore = System.currentTimeMillis() - lastStoreTime.get();
            if (timeSinceLastStore <= 30000) {  // 30 seconds inactivity check
                saveState();
            } else {
                // Optional: Log or perform any action when the save is skipped due to inactivity.
            }
        };
        int initialDelay = 0;
        int periodicDelay = 10;

        scheduler.scheduleAtFixedRate(task, initialDelay, periodicDelay, TimeUnit.SECONDS);
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

        lastStoreTime.set(System.currentTimeMillis());

        // Reactivate periodic save if it was deactivated due to inactivity
        if (scheduler.isShutdown() || scheduler.isTerminated()) {
            startPeriodicSave();
        }

    }


    public void stopPeriodicSave() {
        if (scheduler != null) {
            scheduler.shutdownNow();
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


    private void saveState() {
        // Use a RandomAccessFile to get a FileChannel
        try (RandomAccessFile raf = new RandomAccessFile("barrelState.dat", "rw");
             FileChannel fileChannel = raf.getChannel();
             ObjectOutputStream oos = new ObjectOutputStream(Channels.newOutputStream(fileChannel))) {
            
            // Try acquiring the lock
            FileLock lock = fileChannel.tryLock();
            if (lock != null) {
                try {
                    oos.writeObject(pages);
                    oos.writeObject(urlHyperlinkIndex);
                    System.out.println("[Barrel] State saved successfully.");
                } finally {
                    lock.release(); // Make sure to release the lock
                }
            } else {
                System.out.println("[Barrel] Could not acquire the lock. State not saved.");
            }
        } catch (IOException | OverlappingFileLockException e) {
            handleException("Error saving state", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadState() {
        File file = new File("barrelState.dat");
    
        if (!file.exists() || file.length() == 0) {
            System.out.println("[Barrel] No existing state to load (file not found or empty).");
            return; // Skip loading if the file doesn't exist or is empty
        }
    
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
             FileChannel fileChannel = raf.getChannel();
             ObjectInputStream ois = new ObjectInputStream(Channels.newInputStream(fileChannel))) {
    
            FileLock lock = fileChannel.lock(0L, Long.MAX_VALUE, true);
            if (lock != null) {
                try {
                    pages.putAll((ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>>) ois.readObject());
                    urlHyperlinkIndex.putAll((ConcurrentHashMap<String, Set<String>>) ois.readObject());
                    System.out.println("[Barrel] State loaded successfully.");
                } finally {
                    lock.release();
                }
            } else {
                System.out.println("[Barrel] Could not acquire the lock. State not loaded.");
            }
        } catch (IOException | ClassNotFoundException | OverlappingFileLockException e) {
            handleException("Error loading state", e);
        }
    }
    


    private static void handleException(String message, Exception e) {
        System.err.println("[Barrel] " + message + ": " + e.toString());
        e.printStackTrace();
    }
}


