import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;



public class Client {
    private static volatile boolean exitAdminPage = false; // flag to control the admin page loop
    public static void main(String args[]) {
        try {
            String registryAddress = AppConfig.getProperty("rmi.registry.address");
            int registryPort = Integer.parseInt(AppConfig.getProperty("rmi.registry.port"));

            Registry registry = LocateRegistry.getRegistry(registryAddress, registryPort);
            
            GatewayInterface gateway = (GatewayInterface) registry.lookup("Gateway");

            try (Scanner scanner = new Scanner(System.in)) {
                int option = 0;

                while (option != 5) {
                    printMenu();
                    try {
                        option = scanner.nextInt();
                        scanner.nextLine(); 

                        switch (option) {
                            case 1:
                                indexURL(gateway, scanner);
                                break;
                            case 2:
                                searchPages(gateway, scanner);
                                break;
                            case 3:
                                searchLeadURLs(gateway, scanner);
                                break;
                            case 4:
                                displayAdminPage(gateway);
                                break;
                            case 5:
                                System.out.println("Exiting...");
                                break;
                            default:
                                System.out.println("Invalid option");
                        }
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter a number.");
                        scanner.nextLine(); 
                    }
                }
            }
        } catch (Exception e) {
            handleException("Exception on client", e);
        }
    }

    private static void printMenu() {
        System.out.println("1. Index URL");
        System.out.println("2. Search term on pages");
        System.out.println("3. Search all URLs that lead to a specific URL");
        System.out.println("4. Admin page");
        System.out.println("5. Exit");
        System.out.print("Option: ");
    }

    private static void indexURL(GatewayInterface gateway, Scanner scanner) {
        try {
            System.out.print("URL: ");
            String url = scanner.nextLine();
            DepthControl depthControl = new DepthControl(url, 0);
            gateway.queueUpUrl(depthControl);
            System.out.println("URL indexed successfully.");
        } catch (RemoteException e) {
            handleException("Error indexing URL", e);
        }
    }

    private static void searchPages(GatewayInterface gateway, Scanner scanner) {
        try {
            System.out.print("Search term: ");
            String term = scanner.nextLine();
            int pageNumber = 1;

            ConcurrentSkipListSet<PageContent> results = gateway.searchInfo(term);

            if (!results.isEmpty()) {
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
                        System.out.println("Text: " + content.getShortCitation());
                        System.out.println("Links: " + content.getNumberOfReferences());
                        System.out.println(); // For better readability
                        iterator.remove();
                        count++;
                    }

                    System.out.println("----------------Page number---------------" + pageNumber);
                    pageNumber++;
                    System.out.println("Move forward press 1");

                    int fds = scanner.nextInt();
                    forward = fds == 1;

                    if (!iterator.hasNext() && forward) {
                        System.out.println("No more results for " + term);
                        break;
                    }
                }
            } else {
                System.out.println("Could not find results for " + term);
            }
        } catch (RemoteException e) {
            handleException("Error searching pages", e);
        }
    }

    private static void searchLeadURLs(GatewayInterface gateway, Scanner scanner) {
        try {
            System.out.print("URL: ");
            String url = scanner.nextLine();
            List<String> urls = gateway.searchURL(url);

            if (!urls.isEmpty()) {
                System.out.println("URLs that lead to " + url + ":");
                for (String u : urls) {
                    System.out.println(u);
                }
            } else {
                System.out.println("No URLs found that lead to " + url);
            }
        } catch (RemoteException e) {
            handleException("Error searching lead URLs", e);
        }
    }

    private static void displayAdminPage(GatewayInterface gateway) {

        
        try {
            List<String> lastTopTerms = new ArrayList<>();
            Set<String> lastActiveBarrels = new HashSet<>();
            Thread userThread = new Thread(() -> {
                System.out.println("Enter 'q' to exit admin page.");
                @SuppressWarnings("resource")
                Scanner scanner = new Scanner(System.in);
                String input = scanner.nextLine();
                if ("q".equals(input.trim().toLowerCase())) {
                    exitAdminPage = true;// or use a flag to signal main thread to stop
                }
                
            });
    
            userThread.start(); // Start the user input thread
            while (!exitAdminPage) { // Infinite loop to continuously check for updates
                // Get current top searched terms and active barrels from the server
                List<String> currentTopTerms = gateway.getTopSearchedTerms();
                Set<String> currentActiveBarrels = gateway.getActiveBarrels();
    
                // Check if there are any changes in top searched terms
                if (!currentTopTerms.equals(lastTopTerms)) {
                    System.out.println("Top 10 searched terms:");
                    for (String t : currentTopTerms) {
                        System.out.println(t);
                    }
                    lastTopTerms = new ArrayList<>(currentTopTerms); // Update the last known state
                }
    
                // Check if there are any changes in active barrels
                if (!currentActiveBarrels.equals(lastActiveBarrels)) {
                    System.out.println("Active barrels:");
                    for (String barrelInfo : currentActiveBarrels) {
                        System.out.println(barrelInfo);
                    }
                    lastActiveBarrels = new HashSet<>(currentActiveBarrels); // Update the last known state
                }
    
                try {
                    Thread.sleep(5000); // 5 seconds delay
                } catch (InterruptedException e) {
                    System.out.println("Interrupted while waiting. Exiting...");
                    break;
                }
            }
            userThread.interrupt(); // Interrupt user input thread to exit it
        } catch (RemoteException e) {
            handleException("Error displaying admin page", e);
        }
    }

    private static void handleException(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();
    }
}
