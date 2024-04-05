import java.rmi.registry.*;

public class GatewayS {

    // The main method - the entry point of the application.
    public static void main(String[] args) {
        try {
            // Load RMI port from properties file
            int rmiPort = Integer.parseInt(AppConfig.getProperty("rmi.registry.port"));

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