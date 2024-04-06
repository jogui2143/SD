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

/**
 * The Barrel class represents a container for storing and managing page content
 * and hyperlinks.
 * It utilizes a MulticastSocket for communication and operates on a specified
 * port.
 */
public class Barrel {

    // MulticastSocket for communication
    private MulticastSocket socket;

    // InetAddress for the group communication
    private InetAddress gpAddress;

    // NetworkInterface for network operations
    private NetworkInterface netInterface;

    // ConcurrentHashMap to store pages by their URLs and corresponding content
    private final ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>> pages = new ConcurrentHashMap<>();

    // ConcurrentHashMap to index URLs and their hyperlinks
    private final ConcurrentHashMap<String, Set<String>> urlHyperlinkIndex = new ConcurrentHashMap<>();

    // Set to keep track of active barrels by their UUIDs
    private static final Set<UUID> activeBarrels = new HashSet<>();

    // Unique identifier for the barrel
    private final UUID id;

    // AtomicLong to track the last store time
    private final AtomicLong lastStoreTime = new AtomicLong(System.currentTimeMillis());

    // ScheduledExecutorService for periodic tasks
    private ScheduledExecutorService scheduler;

    // Port number for communication
    private int port;

    /**
     * Constructs a Barrel object with the specified port.
     * Loads the initial state and initializes necessary components for
     * communication.
     * 
     * @param port The port number for communication.
     * @throws IOException If an I/O error occurs during initialization.
     */
    public Barrel(int port) throws IOException {
        this.port = port;
        loadState();
        try {
            // Initialize multicast communication
            String multicastAddress = AppConfig.getProperty("multicast.address");
            int multicastPort = Integer.parseInt(AppConfig.getProperty("multicast.port"));
            gpAddress = InetAddress.getByName(multicastAddress);
            socket = new MulticastSocket(multicastPort);
            netInterface = NetworkInterface.getNetworkInterfaces().nextElement();

            if (gpAddress.isMulticastAddress()) {
                socket.setOption(StandardSocketOptions.IP_MULTICAST_IF, netInterface);
                socket.joinGroup(new InetSocketAddress(gpAddress, multicastPort), netInterface);
            }

            // Generate unique identifier for the barrel
            synchronized (Barrel.class) {
                this.id = UUID.randomUUID();
            }

            // Add this barrel to the set of active barrels
            activeBarrels.add(this.id);

            // Add shutdown hook to remove this barrel from active barrels upon exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> activeBarrels.remove(this.id)));

            // Start periodic save task
            startPeriodicSave();

        } catch (IOException e) {
            // Handle IOException
            handleException("Error initializing Barrel", e);
            throw e;
        } catch (NoSuchElementException e) {
            // Handle NoSuchElementException
            handleException("No network interface available", e);
            throw new IOException("No network interface available", e);
        }
    }

    public UUID getId() {
        return this.id;
    }

    /**
     * Starts a periodic task to save the state of the barrel.
     * The task checks for inactivity and saves the state if there has been activity
     * within the last 30 seconds.
     */
    private void startPeriodicSave() {
        // Create a single-threaded scheduled executor
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // Define the task to be executed periodically
        Runnable task = () -> {
            // Calculate time elapsed since the last store operation
            long timeSinceLastStore = System.currentTimeMillis() - lastStoreTime.get();

            // Check if there has been activity within the last 30 seconds
            if (timeSinceLastStore <= 30000) { // 30 seconds inactivity check
                // Save the state
                saveState();
            } else {
                // Optional: Log or perform any action when the save is skipped due to
                // inactivity.
            }
        };

        // Initial delay before the first execution of the task (0 seconds)
        int initialDelay = 0;

        // Periodic delay between successive executions of the task (10 seconds)
        int periodicDelay = 10;

        // Schedule the task to be executed at fixed rate
        scheduler.scheduleAtFixedRate(task, initialDelay, periodicDelay, TimeUnit.SECONDS);
    }

    /**
     * Listens for incoming messages on the socket and processes them.
     * This method continuously listens for incoming messages and processes them
     * accordingly.
     * It reconstructs fragmented messages, decompresses them, and stores the
     * resulting page content.
     */
    public void listenMsg() {
        // Buffer to hold incoming data
        byte[] buffer = new byte[65536];

        // DatagramPacket to hold incoming packets
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        // Map to store message sections by message ID
        Map<Integer, Map<Integer, byte[]>> sections = new HashMap<>();

        // Map to track the number of sections received for each message ID
        Map<Integer, Integer> sectionsCount = new HashMap<>();

        // Continuously listen for incoming messages
        while (true) {
            try {
                // Receive the incoming packet
                socket.receive(packet);

                // Create input streams for reading from the packet data
                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                DataInputStream dis = new DataInputStream(bais);

                // Read message ID, part number, and total parts from the packet
                int msgId = dis.readInt();
                int partNumber = dis.readInt();
                int allParts = dis.readInt();

                // Read the data section from the packet
                byte[] dataSection = new byte[packet.getLength() - 12];
                dis.readFully(dataSection);

                // Store the data section in the sections map
                sections.putIfAbsent(msgId, new HashMap<>());
                sections.get(msgId).put(partNumber, dataSection);
                sectionsCount.putIfAbsent(msgId, 0);

                // If all parts of the message have been received
                if (sections.get(msgId).size() == allParts) {
                    // Reconstruct the message from its parts
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    for (int i = 0; i < allParts; i++) {
                        baos.write(sections.get(msgId).get(i));
                    }

                    // Decompress and deserialize the message
                    try (GZIPInputStream inZip = new GZIPInputStream(new ByteArrayInputStream(baos.toByteArray()));
                            ObjectInputStream ois = new ObjectInputStream(inZip)) {
                        Object obj = ois.readObject();
                        if (obj instanceof PageContent) {
                            // If the deserialized object is a PageContent, store it
                            PageContent info = (PageContent) obj;
                            storeMsg(info);
                        }
                    } catch (ClassNotFoundException e) {
                        // Handle ClassNotFoundException
                        handleException("ClassNotFoundException in listenMsg", e);
                    }
                }
            } catch (IOException e) {
                // Handle IOException
                handleException("IOException in listenMsg", e);
            }
        }
    }

    /**
     * Stores the given PageContent information.
     * This method stores the provided PageContent information, indexing words and
     * hyperlinks.
     * It converts words to lowercase before indexing to ensure case-insensitive
     * retrieval.
     * 
     * @param info The PageContent information to store.
     */
    private void storeMsg(PageContent info) {
        // Get the set of words from PageContent and index them
        Set<String> words = info.getWords();
        for (String word : words) {
            String lowerCaseWord = word.toLowerCase();
            pages.computeIfAbsent(lowerCaseWord, k -> new ConcurrentSkipListSet<>()).add(info);
        }

        // Store hyperlinks for the URL
        String url = info.getUrl();
        Set<String> hyperlinks = info.getHyperLinks();
        synchronized (urlHyperlinkIndex) {
            urlHyperlinkIndex.put(url, hyperlinks);
        }

        // Update the last store time
        lastStoreTime.set(System.currentTimeMillis());

        // Reactivate periodic save if it was deactivated due to inactivity
        if (scheduler.isShutdown() || scheduler.isTerminated()) {
            startPeriodicSave();
        }
    }

    /**
     * Stops the periodic save task if it is running.
     * This method shuts down the scheduler, effectively stopping the periodic save
     * task.
     */
    public void stopPeriodicSave() {
        // Check if the scheduler is initialized
        if (scheduler != null) {
            // Shut down the scheduler immediately
            scheduler.shutdownNow();
        }
    }

    /**
     * Saves the current state of the Barrel to a file.
     * This method serializes the 'pages' and 'urlHyperlinkIndex' data structures
     * and writes them to a file using ObjectOutputStream.
     * The file is named 'barrelState_portNumber.dat', where 'portNumber' is the
     * port of the Barrel.
     * The method tries to acquire an exclusive lock on the file before writing to
     * ensure consistency.
     * If the lock cannot be acquired, the method prints a message and does not save
     * the state.
     * 
     * @throws IOException                  If an I/O error occurs during the file
     *                                      operations.
     * @throws OverlappingFileLockException If another thread holds an overlapping
     *                                      lock on the file.
     */
    private void saveState() {
        // Define the filename based on the port number
        String filename = "barrelState_" + port + ".dat";

        // Use a RandomAccessFile to get a FileChannel and ObjectOutputStream
        try (RandomAccessFile raf = new RandomAccessFile(filename, "rw");
                FileChannel fileChannel = raf.getChannel();
                ObjectOutputStream oos = new ObjectOutputStream(Channels.newOutputStream(fileChannel))) {

            // Try acquiring the lock
            FileLock lock = fileChannel.tryLock();
            if (lock != null) {
                try {
                    // Write 'pages' and 'urlHyperlinkIndex' to the file
                    oos.writeObject(pages);
                    oos.writeObject(urlHyperlinkIndex);
                    System.out.println("[Barrel] State saved successfully.");
                } finally {
                    lock.release(); // Make sure to release the lock
                }
            } else {
                // Print a message if the lock cannot be acquired
                System.out.println("[Barrel] Could not acquire the lock. State not saved.");
            }
        } catch (IOException | OverlappingFileLockException e) {
            // Handle IOException or OverlappingFileLockException
            handleException("Error saving state", e);
            // Re-throw the exception
        }
    }

    /**
     * Loads the state of the Barrel from a file.
     * This method deserializes the stored 'pages' and 'urlHyperlinkIndex' data
     * structures
     * from the file specified by the filename "barrelState_portNumber.dat", where
     * 'portNumber' is the port of the Barrel.
     * It populates the 'pages' and 'urlHyperlinkIndex' maps with the deserialized
     * data.
     * The method tries to acquire an exclusive lock on the file before reading to
     * ensure consistency.
     * If the lock cannot be acquired, the method prints a message and does not load
     * the state.
     * Suppresses unchecked warnings for casting deserialized objects to their
     * respective types.
     */
    @SuppressWarnings("unchecked")
    private void loadState() {
        // Define the filename based on the port number
        String filename = "barrelState_" + port + ".dat";
        File file = new File(filename);

        // Skip loading if the file doesn't exist or is empty
        if (!file.exists() || file.length() == 0) {
            System.out.println("[Barrel] No existing state to load (file not found or empty).");
            return;
        }

        // Use a RandomAccessFile to get a FileChannel and ObjectInputStream
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
                FileChannel fileChannel = raf.getChannel();
                ObjectInputStream ois = new ObjectInputStream(Channels.newInputStream(fileChannel))) {

            // Try acquiring the lock
            FileLock lock = fileChannel.lock(0L, Long.MAX_VALUE, true);
            if (lock != null) {
                try {
                    // Deserialize and populate 'pages' and 'urlHyperlinkIndex' maps
                    pages.putAll((ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>>) ois.readObject());
                    urlHyperlinkIndex.putAll((ConcurrentHashMap<String, Set<String>>) ois.readObject());
                    System.out.println("[Barrel] State loaded successfully.");
                } finally {
                    lock.release(); // Make sure to release the lock
                }
            } else {
                // Print a message if the lock cannot be acquired
                System.out.println("[Barrel] Could not acquire the lock. State not loaded.");
            }
        } catch (IOException | ClassNotFoundException | OverlappingFileLockException e) {
            // Handle IOException, ClassNotFoundException, or OverlappingFileLockException
            handleException("Error loading state", e);
        }
    }

    /**
     * The main method of the Barrel application.
     * It starts the Barrel service, creates necessary components, and binds them to
     * the RMI registry.
     * It then starts listening for incoming messages.
     * 
     * @param args The command-line arguments passed to the program. Not used in
     *             this implementation.
     */
    public static void main(String[] args) {
        // Retrieve the RMI registry port from the application configuration
        int port = Integer.parseInt(AppConfig.getProperty("rmi.registry.port"));

        try {
            // Start the Barrel service
            System.out.println("[Barrel] Starting Barrel service...");
            System.out.println("[Barrel] Creating RMI registry on port " + port);

            // Create RMI registry if it doesn't exist
            try {
                LocateRegistry.createRegistry(port);
            } catch (RemoteException e) {
                System.out.println("[Barrel] RMI registry already exists on port " + port);
            }

            // Instantiate Barrel and BarrelFunc objects
            System.out.println("[Barrel] Instantiating Barrel and BarrelFunc objects");
            Barrel barrel = new Barrel(port);
            BarrelFunc barrelFunc = new BarrelFunc(barrel.getId(), barrel.pages, barrel.urlHyperlinkIndex);

            // Get the Registry reference
            System.out.println("[Barrel] Getting the Registry reference");
            Registry reg = LocateRegistry.getRegistry("localhost", port);

            // Bind the BarrelFunc to the registry
            System.out.println("[Barrel] Binding the BarrelFunc to the registry");
            reg.rebind("Barrel", barrelFunc);

            // Start listening for messages
            System.out.println("[Barrel] Starting to listen for messages");
            barrel.listenMsg();
        } catch (IOException e) {
            // Handle IOException
            System.err.println("[Barrel] Exception occurred: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Handles exceptions occurred during Barrel operations.
     * This method prints an error message along with the exception details to the
     * standard error stream.
     * 
     * @param message The descriptive message indicating the context of the
     *                exception.
     * @param e       The exception that occurred.
     */
    private static void handleException(String message, Exception e) {
        // Print an error message along with the exception details to the standard error
        // stream
        System.err.println("[Barrel] " + message + ": " + e.toString());
        e.printStackTrace();
    }
}
