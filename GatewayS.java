// Importing necessary classes for Java RMI registry handling.
import java.rmi.registry.*;
import java.io.File;

public class GatewayS {
    // The main method - the entry point of the application.
    public static void main(String[] args) {
        try {
            // Existing setup code...
            GatewayFunc obj = new GatewayFunc();
            Registry reg = LocateRegistry.createRegistry(1099);
            reg.rebind("Gateway", obj);
            System.out.println("Gateway is running...");

            // Add a shutdown hook to delete the barrel_data.dat file
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down Gateway, cleaning up data files...");
                File dataFile = new File("barrel_data.dat");
                if (dataFile.exists() && dataFile.delete()) {
                    System.out.println("barrel_data.dat file deleted successfully.");
                } else {
                    System.out.println("Failed to delete barrel_data.dat file.");
                }
            }));

        } catch (Exception e) {
            System.err.println("Exception on gateway" + e.toString());
            e.printStackTrace();
        }
    }
}

