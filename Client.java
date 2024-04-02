
// Import statements for Java RMI and utilities.
import java.rmi.registry.Registry;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentSkipListSet;
import java.rmi.RemoteException;
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
                scanner.nextLine(); // Clearing the scanner buffer.

                // Switch case to handle different user options.
                switch (option) {
                    case 1:
                        // Index URL option.
                        System.out.print("URL: ");
                        String url = scanner.nextLine();
                        DepthControl depthControl = new DepthControl(url, 0);
                        // Calling the remote method to queue up the URL.
                        gateway.queueUpUrl(depthControl);
                        System.out.println("MERICAAAA FUCK YEAH");
                        break;
                    case 2:
                        System.out.print("Search term: ");
                        String term = scanner.nextLine();
                        int pageNumber = 1;

                        ConcurrentSkipListSet<PageContent> results = gateway.searchinfo(term);

                        // Convert HashSet to List and sort it by the number of links.

                        // queres avancar
                        if (results != null && !results.isEmpty()) {
                            System.out.println("Results found for " + term + ":");
                            boolean forward = true;

                            while (forward) {
                                int count = 1;
                                System.out.println("----------------Page number---------------" + pageNumber);

                                Iterator<PageContent> iterator = results.iterator();
                                while (iterator.hasNext() && count <= 10) {
                                    PageContent content = iterator.next();
                                    System.out.println("Title: " + content.getTitle());
                                    System.out.println("URL: " + content.getUrl());
                                    System.out.println("Text: " + content.truncateText(content.getText()));
                                    System.out.println("Links: " + content.getNumberOfReferences());
                                    System.out.println(); // For better readability
                                    iterator.remove();
                                    count++;
                                }

                                System.out.println("----------------Page number---------------" + pageNumber);
                                pageNumber++;
                                System.out.println("Move forward press 1");

                                // Here you should handle possible InputMismatchException in case of wrong input
                                int fds = scanner.nextInt();
                                forward = fds == 1;
                                if (!iterator.hasNext() && forward) {
                                    System.out.println("No more results for " + term);
                                    break;
                                }
                            }
                        }

                        // escolha frente ou sai se sair break
                        else {
                            System.out.println("Could not find results for " + term);
                        }
                        break;
                    case 3:
                        // Option for searching URLs that lead to a specific URL (code commented out).
                        System.out.print("URL: ");
                        url = scanner.nextLine();
                        List<String> urls = gateway.searchURL(url);
                        if (urls != null && !urls.isEmpty()) {
                            System.out.println("URLs that lead to " + url + ":");
                            for (String u : urls) {
                                System.out.println(u);
                            }
                        } else {
                            System.out.println("No URLs found that lead to " + url);
                        }
                        break;
                    case 4:
                        // info page access option
                        // here you should be able to see the 10 most searched terms, the list of active
                        // barrels and the reponse time of the server
                        try {
                            List<String> topTerms = gateway.getTopSearchedTerms();
                            System.out.println("Top 10 searched terms:");
                            for (String t : topTerms) {
                                System.out.println(t);
                            }

                            /*
                             * long responseTime = gateway.getServerResponseTime();
                             * System.out.println("Server Response Time: " + responseTime + "ms");
                             */
                        } catch (RemoteException e) {
                            System.err.println("Remote exception retrieving info page data: " + e.getMessage());
                            e.printStackTrace();
                        }
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
