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

/**
 * The <code>Client</code> class represents the client application for
 * interacting with the Gateway server.
 * It allows users to perform various operations such as indexing URLs,
 * searching pages, and accessing administrative features.
 */
public class Client {
    private static volatile boolean exitAdminPage = false; // flag to control the admin page loop
    private static Cache<String, ConcurrentSkipListSet<PageContent>> searchCache = new Cache<>(10); // cache with
                                                                                                    // capacity 10
    private static Cache<String, List<String>> urlCache = new Cache<>(10); // another cache for URLs

    /**
     * The main entry point of the client application.
     * This method initializes the RMI connection with the Gateway server and
     * provides a menu-driven interface for user interaction.
     * 
     * @param args The command-line arguments (not used).
     */
    public static void main(String args[]) {
        try {
            // Get the registry address and port from the application properties
            String registryAddress = AppConfig.getProperty("rmi.registry.address");
            int registryPort = Integer.parseInt(AppConfig.getProperty("rmi.registry.port"));

            // Connect to the RMI registry
            Registry registry = LocateRegistry.getRegistry(registryAddress, registryPort);

            // Lookup the GatewayInterface from the registry
            GatewayInterface gateway = (GatewayInterface) registry.lookup("Gateway");

            // Initialize scanner for user input
            try (Scanner scanner = new Scanner(System.in)) {
                int option = 0;

                // Main menu loop
                while (option != 5) {
                    printMenu();
                    try {
                        // Read user input for menu option
                        option = scanner.nextInt();
                        scanner.nextLine();

                        // Process user choice
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
            // Handle any exceptions that occur during execution
            handleException("Exception on client", e);
        }
    }

    /**
     * Prints the menu options for user interaction.
     * This method displays a menu with various options for user interaction, such
     * as indexing URLs, searching pages, and accessing administrative features.
     */
    private static void printMenu() {
        System.out.println("1. Index URL");
        System.out.println("2. Search term on pages");
        System.out.println("3. Search all URLs that lead to a specific URL");
        System.out.println("4. Admin page");
        System.out.println("5. Exit");
        System.out.print("Option: ");
    }

    /**
     * Indexes a URL by adding it to the processing queue on the Gateway server.
     * This method prompts the user to enter a URL, creates a DepthControl object
     * with depth set to 0,
     * and then queues up the URL for processing on the Gateway server.
     * 
     * @param gateway The GatewayInterface instance for interacting with the Gateway
     *                server.
     * @param scanner The Scanner object for user input.
     */
    private static void indexURL(GatewayInterface gateway, Scanner scanner) {
        try {
            // Prompt the user to enter a URL
            System.out.print("URL: ");
            String url = scanner.nextLine();

            // Create a DepthControl object with depth set to 0
            DepthControl depthControl = new DepthControl(url, 0);

            // Queue up the URL for processing on the Gateway server
            gateway.queueUpUrl(depthControl);

            // Display success message
            System.out.println("URL indexed successfully.");
        } catch (RemoteException e) {
            // Handle RemoteException
            handleException("Error indexing URL", e);
        }
    }

    /**
     * Searches for pages containing a specified search term using the Gateway
     * server.
     * This method prompts the user to enter a search term, sends a request to the
     * Gateway server to search for pages containing the term,
     * and presents the search results to the user. It also utilizes a cache to
     * store and retrieve previous search results.
     * 
     * @param gateway The GatewayInterface instance for interacting with the Gateway
     *                server.
     * @param scanner The Scanner object for user input.
     */
    private static void searchPages(GatewayInterface gateway, Scanner scanner) {
        try {
            // Prompt the user to enter a search term
            System.out.print("Search term: ");
            String term = scanner.nextLine();

            // Request search results from the Gateway server
            ConcurrentSkipListSet<PageContent> results = gateway.searchInfo(term);

            // Check if results are not empty
            if (!results.isEmpty()) {
                // Update cache with new results
                searchCache.put(term, new ConcurrentSkipListSet<>(results)); // Clone results to keep original set
                                                                             // unmodified
            } else if (searchCache.containsKey(term)) {
                // Use cached results if Gateway returned empty results
                System.out.println("Using cached results for " + term);
                results = searchCache.get(term);
            } else {
                System.out.println("Could not find results for " + term);
                return; // Exit the method if no results are found
            }

            // Present search results to the user
            presentResults(results, term, scanner);
        } catch (RemoteException e) {
            // Handle RemoteException
            handleException("Error searching pages", e);
        }
    }

    /**
     * Presents the search results to the user in a paginated manner.
     * This method iterates through the search results and displays them to the user
     * in pages of 10 results each.
     * It prompts the user to move forward to the next page or exit the results
     * display.
     * 
     * @param results The set of search results to be presented.
     * @param term    The search term used to retrieve the results.
     * @param scanner The Scanner object for user input.
     */
    private static void presentResults(ConcurrentSkipListSet<PageContent> results, String term, Scanner scanner) {
        int pageNumber = 1;
        boolean forward = true;

        // Paginate through the search results
        while (forward) {
            int count = 1;
            System.out.println("----------------Page number---------------" + pageNumber);
            Iterator<PageContent> iterator = results.iterator();

            // Display up to 10 results on each page
            while (iterator.hasNext() && count <= 10) {
                PageContent content = iterator.next();
                System.out.println("Title: " + content.getTitle());
                System.out.println("URL: " + content.getUrl());
                System.out.println("Text: " + content.getShortCitation());
                System.out.println("Links: " + content.getNumberOfReferences());
                System.out.println(); // For better readability
                count++;
                iterator.remove();
            }

            

            // Prompt the user to move forward to the next page
            System.out.println("----------------Page number---------------" + pageNumber);
            pageNumber++;
            System.out.println("Move forward press 1");

            // Read user input to determine if they want to continue to the next page
            int fds = scanner.nextInt();
            scanner.nextLine(); // Consume the rest of the line to handle next input correctly
            forward = fds == 1;

            // Check if there are no more results and the user wants to continue
            if (!iterator.hasNext() && forward) {
                System.out.println("No more results for " + term);
                break;
            }
        }
    }

    /**
     * Searches for URLs that lead to a specific URL using the Gateway server.
     * This method prompts the user to enter a URL, sends a request to the Gateway
     * server to search for URLs that lead to the specified URL,
     * and displays the search results to the user. It also utilizes a cache to
     * store and retrieve previous search results.
     * 
     * @param gateway The GatewayInterface instance for interacting with the Gateway
     *                server.
     * @param scanner The Scanner object for user input.
     */
    private static void searchLeadURLs(GatewayInterface gateway, Scanner scanner) {
        try {
            // Prompt the user to enter a URL
            System.out.print("URL: ");
            String url = scanner.nextLine();

            // Request URLs that lead to the specified URL from the Gateway server
            List<String> urls = gateway.searchURL(url);

            // Check if URLs are not empty
            if (!urls.isEmpty()) {
                // Update cache with new results
                urlCache.put(url, urls);
                displayUrls(urls);
            } else {
                // Check cache if Gateway returned empty results
                if (urlCache.containsKey(url)) {
                    System.out.println("Using cached URLs for " + url);
                    displayUrls(urlCache.get(url));
                } else {
                    System.out.println("No URLs found that lead to " + url);
                }
            }
        } catch (RemoteException e) {
            // Handle RemoteException
            handleException("Error searching lead URLs", e);
        }
    }

    /**
     * Displays a list of URLs to the console.
     * This method iterates through the list of URLs and prints each URL to the
     * console.
     * 
     * @param urls The list of URLs to be displayed.
     */
    private static void displayUrls(List<String> urls) {
        for (String u : urls) {
            System.out.println(u);
        }
    }

    /**
     * Displays the admin page, showing top searched terms and active barrels.
     * This method continuously retrieves and displays updates on the top searched
     * terms and active barrels from the Gateway server.
     * It runs an input thread to allow the user to exit the admin page by typing
     * 'q'.
     * 
     * @param gateway The GatewayInterface instance for interacting with the Gateway
     *                server.
     */
    private static void displayAdminPage(GatewayInterface gateway) {
        try {
            List<String> lastTopTerms = new ArrayList<>();
            Set<String> lastActiveBarrels = new HashSet<>();

            // Thread for user input
            Thread userThread = new Thread(() -> {
                System.out.println("Enter 'q' to exit admin page.");
                @SuppressWarnings("resource")
                Scanner scanner = new Scanner(System.in);
                String input = scanner.nextLine();
                if ("q".equals(input.trim().toLowerCase())) {
                    exitAdminPage = true; // Signal main thread to stop
                }
            });

            userThread.start(); // Start the user input thread

            // Infinite loop to continuously check for updates
            while (!exitAdminPage) {
                // Get current top searched terms and active barrels from the server
                List<String> currentTopTerms = gateway.getTopSearchedTerms();
                Set<String> currentActiveBarrels = gateway.getActiveBarrels();

                // Check for changes in top searched terms
                if (!currentTopTerms.equals(lastTopTerms)) {
                    System.out.println("Top 10 searched terms:");
                    for (String t : currentTopTerms) {
                        System.out.println(t);
                    }
                    lastTopTerms = new ArrayList<>(currentTopTerms); // Update last known state
                }

                // Check for changes in active barrels
                if (!currentActiveBarrels.equals(lastActiveBarrels)) {
                    System.out.println("Active barrels:");
                    for (String barrelInfo : currentActiveBarrels) {
                        System.out.println(barrelInfo);
                    }
                    lastActiveBarrels = new HashSet<>(currentActiveBarrels); // Update last known state
                }

                try {
                    Thread.sleep(5000); // 5 seconds delay
                } catch (InterruptedException e) {
                    System.out.println("Interrupted while waiting. Exiting...");
                    break;
                }
            }

            userThread.interrupt(); // Interrupt user input thread to exit
        } catch (RemoteException e) {
            // Handle RemoteException
            handleException("Error displaying admin page", e);
        }
    }

    /**
     * Handles an exception by printing an error message and stack trace to the
     * standard error stream.
     * This method prints the given error message along with the error message of
     * the exception to the standard error stream,
     * followed by the stack trace of the exception.
     * 
     * @param message The error message to be displayed.
     * @param e       The exception that occurred.
     */
    private static void handleException(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();
    }

}
