import java.rmi.registry.*;

/**
 * Gateway server responsible for initializing and running the gateway component in a distributed web crawling system.
 * This class creates a registry, binds the GatewayFunc object to it, and starts the gateway on the specified RMI port.
 */
public class GatewayS {

    /**
     * The main method serves as the entry point of the application.
     * It initializes and runs the gateway server.
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        try {
            // Load RMI port from properties file
            int rmiPort = Integer.parseInt(AppConfig.getProperty("rmi.registry.host.port"));

            GatewayFunc obj = new GatewayFunc();
            Registry reg = LocateRegistry.createRegistry(rmiPort);
            reg.rebind("Gateway", obj);
            System.out.println("Gateway is running on port " + rmiPort + "...");

        } catch (Exception e) {
            System.err.println("Exception on gateway" + e.toString());
            e.printStackTrace();
        }
    }
}
