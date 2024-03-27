// Importing necessary classes for Java RMI registry handling.
import java.rmi.registry.*;

// Definition of the GatewayS class.
public class GatewayS {
    // The main method - the entry point of the application.
    public static void main(String[] args) {
        try {
            // Creating an instance of GatewayFunc.
            GatewayFunc obj = new GatewayFunc();

            // Creating an RMI registry on the local host at port 1099.
            Registry reg = LocateRegistry.createRegistry(1099);

            // Binding the GatewayFunc object to the name "Gateway" in the registry.
            // This makes it accessible to remote clients.
            reg.rebind("Gateway", obj);

            // Indicating successful start of the Gateway service.
            System.out.println("Gateway is running...");
        } catch (Exception e) {
            // Catching and handling exceptions, such as issues in setting up the RMI registry or binding the object.
            System.err.println("Exception on gateway" + e.toString());
            e.printStackTrace();
        }
    }
}
