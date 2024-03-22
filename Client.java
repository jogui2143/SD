import java.rmi.registry.Registry;
import java.util.HashSet;
import java.util.Scanner;
import java.rmi.registry.LocateRegistry;

public class Client{
  public static void main(String args[]){
    try{
      Registry reg = LocateRegistry.getRegistry("localhost");
      GatewayInterface gateway = (GatewayInterface) reg.lookup("Gateway");

      Scanner scanner = new Scanner(System.in);
      int option = 0;

      while (option != 4) {
        System.out.println("1. Index URL");
        System.out.println("2. Search term on pages");
        System.out.println("3. Search all URLs that leads to a specific URL");
        System.out.println("4. Admin page");
        System.out.println("5. Exit");
        System.out.print("Option: ");
        option = scanner.nextInt();
        scanner.nextLine();

        switch(option){
          case 1:
            System.out.print("URL: ");
            String url = scanner.nextLine();
            gateway.queueUpUrl(url);
            System.out.println("MERICAAAA FUCK YEAH");
            break;
          case 2:
            System.out.print("Search term: ");
            String term = scanner.nextLine();
            HashSet<String> results = gateway.searchinfo(term);
            if(results != null && !results.isEmpty()){
              System.out.println("Urls found for " + term + ":");
              for(String s : results){
                System.out.println(s);
              }
            }else {
              System.out.println("Could not find URls for "+ term);
            }
            break;
          case 3:
            // System.out.print("URL: ");
            // url = scanner.nextLine();
            // gateway.searchURL(url);
            break;
          case 4:
            System.out.print("Username: ");
            String username = scanner.nextLine();
            System.out.print("Password: ");
            String password = scanner.nextLine();
            // gateway.adminPage(username, password);
            break;
          case 5:
            break;
          default:
            System.out.println("Invalid option");
        }
      }
    }
    catch (Exception e){
      System.err.println("Exception on client" + e.toString());
      e.printStackTrace();
    }
  }
}