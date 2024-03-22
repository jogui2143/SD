import java.rmi.registry.*;

public class GatewayS{
  public static void main(String[] args){
    try{
      GatewayFunc obj = new GatewayFunc();
      Registry reg = LocateRegistry.createRegistry(1099);
      reg.rebind("Gateway", obj);
      System.out.println("Gateway is running...");
    } catch (Exception e){
      System.err.println("Exception on gateway" + e.toString());
      e.printStackTrace();
    }
  }
}