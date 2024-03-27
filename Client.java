// Import statements for Java RMI and utilities.
import java.rmi.registry.Registry;
import java.util.HashSet;
import java.util.Scanner;
import java.rmi.registry.LocateRegistry;

// Definition of the Client class.
public class Client {
    // The main method - the entry point for the application.
    public static void main(String args[]) {
        try {
            // Connecting to the RMI registry at the specified address ('localhost' here).
            Registry reg = LocateRegistry.getRegistry("localhost");

            // Looking up the GatewayInterface in the RMI registry.
            GatewayInterface gateway = (GatewayInterface) reg.lookup("Gateway");

            // Scanner object to read input from the console.
            Scanner scanner = new Scanner(System.in);
            int option = 0;

            // Loop until the user chooses to exit (option 5).
            while (option != 5) {
                // Displaying options to the user.
                System.out.println("1. Index URL");
                System.out.println("2. Search term on pages");
                System.out.println("3. Search all URLs that leads to a specific URL");
                System.out.println("4. Admin page");
                System.out.println("5. Exit");
                System.out.print("Option: ");

                // Reading the user's option.
                option = scanner.nextInt();
                scanner.nextLine();  // Clearing the scanner buffer.

                // Switch case to handle different user options.
                switch (option) {
                    case 1:
                        // Index URL option.
                        System.out.print("URL: ");
                        String url = scanner.nextLine();

                        // Calling the remote method to queue up the URL.
                        gateway.queueUpUrl(url);
                        System.out.println("MERICAAAA FUCK YEAH");
                        break;
                    case 2:
                        System.out.print("Search term: ");
                        String term = scanner.nextLine();
                    
                        HashSet<PageContent> results = gateway.searchinfo(term);
                    
                        if (results != null && !results.isEmpty()) {
                            System.out.println("Results found for " + term + ":");
                            for (PageContent content : results) {
                                System.out.println("Title: " + content.getTitle());
                                System.out.println("URL: " + content.getUrl());
                                System.out.println("Text: " + content.truncateText(content.getText()));
                                System.out.println(); // For better readability
                            }
                        } else {
                            System.out.println("Could not find results for " + term);
                        }
                        break;
                    case 3:
                        // Option for searching URLs that lead to a specific URL (code commented out).
                        // System.out.print("URL: ");
                        // url = scanner.nextLine();
                        // gateway.searchURL(url);
                        break;
                    case 4:
                        // Admin page access option (code commented out).
                        System.out.print("Username: ");
                        String username = scanner.nextLine();
                        System.out.print("Password: ");
                        String password = scanner.nextLine();
                        // gateway.adminPage(username, password);
                        break;
                    case 5:
                        // Exit option.
                        break;
                    default:
                        // Handling invalid options.
                        System.out.println("Invalid option");
                }
            }
        } catch (Exception e) {
            // Catching and handling exceptions.
            System.err.println("Exception on client" + e.toString());
            e.printStackTrace();
        }
    }
}
