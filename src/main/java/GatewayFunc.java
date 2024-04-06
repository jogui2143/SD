import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

public class GatewayFunc extends UnicastRemoteObject implements GatewayInterface {
    private static final int MAX_RETRIES = 3;
    private PriorityBlockingQueue<DepthControl> urlQueue = new PriorityBlockingQueue<>(100, new DepthControlComparator());
    private final ConcurrentHashMap<String, Integer> searchTermFrequencies = new ConcurrentHashMap<>();
    private Map<String, BarrelInterface> barrels = new ConcurrentHashMap<>();
    private Map<String, Integer> barrelPorts = new HashMap<>();
    private String currentBarrelKey = "localhost:1099";
    private List<String> barrelRegistryAddresses = new ArrayList<>();
    private Set<String> queuedUrls = new HashSet<>();

    public GatewayFunc() throws RemoteException {
        super();
        barrelPorts.put("localhost:1099", 1099);
        barrelPorts.put("localhost:1100", 1100);
        barrelRegistryAddresses.addAll(Arrays.asList("localhost:1099", "localhost:1100"));
    }

    private BarrelInterface getBarrel(String key) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(key.split(":")[0], barrelPorts.get(key));
            BarrelInterface barrel = (BarrelInterface) registry.lookup("Barrel");
            barrels.put(key, barrel);
            return barrel;
        } catch (Exception e) {
            barrels.remove(key);
            throw new RemoteException("Connection to barrel failed: " + key, e);
        }
    }

    private synchronized String getNextBarrelKey() {
        currentBarrelKey = currentBarrelKey.equals("localhost:1099") ? "localhost:1100" : "localhost:1099";
        return currentBarrelKey;
    }

    @Override
    public DepthControl getNewUrl() throws RemoteException {
        return urlQueue.poll();
    }

    @Override
    public ConcurrentSkipListSet<PageContent> searchInfo(String term) throws RemoteException {
        int attempts = 0;
        long backoff = 1000;
        while (attempts < MAX_RETRIES) {
            String barrelKey = getNextBarrelKey();
            try {
                BarrelInterface barrel = getBarrel(barrelKey);
                recordSearchTerm(term);
                return barrel.searchUrls(term);
            } catch (Exception e) {
                attempts++;
                try {
                    Thread.sleep(backoff + new Random().nextInt(500));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RemoteException("Thread interrupted during backoff", ie);
                }
                backoff *= 2;
            }
        }
        return new ConcurrentSkipListSet<>();
    }

    public void queueUpUrl(DepthControl url) throws RemoteException {
        if (url.getDepth() <= 2 && !queuedUrls.contains(url.getUrl())) { // Check if the URL already exists in the queued URLs
            url.setTimestamp(System.currentTimeMillis());
            urlQueue.add(url);
            queuedUrls.add(url.getUrl()); // Add the URL to the set of queued URLs
        }
    }

    @Override
    public List<String> searchURL(String url) throws RemoteException {
        int attempts = 0;
        long backoff = 3000;
        while (attempts < MAX_RETRIES) {
            String barrelKey = getNextBarrelKey();
            try {
                BarrelInterface barrel = getBarrel(barrelKey);
                return barrel.searchURL(url);
            } catch (Exception e) {
                attempts++;
                try {
                    Thread.sleep(backoff + new Random().nextInt(500));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RemoteException("Thread interrupted during backoff", ie);
                }
                backoff *= 2;
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<String> getTopSearchedTerms() throws RemoteException {
        List<Map.Entry<String, Integer>> sortedTerms = searchTermFrequencies.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());

        return sortedTerms.stream()
                .limit(10)
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.toList());
    }

    public Set<String> getActiveBarrels() throws RemoteException {
        Set<String> activeBarrelInfo = new HashSet<>();
        for (String registryAddress : barrelRegistryAddresses) {
            try {
                Registry registry = LocateRegistry.getRegistry(registryAddress.split(":")[0],
                        Integer.parseInt(registryAddress.split(":")[1]));
                BarrelInterface barrel = (BarrelInterface) registry.lookup("Barrel");
                activeBarrelInfo.add(barrel.getBarrelInfo());
            } catch (Exception e) {
                System.err.println("Failed to connect to barrel at " + registryAddress + ": " + e.toString());
            }
        }
        return activeBarrelInfo;
    }

    private void recordSearchTerm(String term) {
        searchTermFrequencies.merge(term, 1, Integer::sum);
    }
}
