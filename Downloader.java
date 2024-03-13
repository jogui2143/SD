import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.net.URI;
public class Downloader {

    private HashSet<String> visitedLinks;

   
    private Queue<UrlDepthPair> queue;

    // Maximum depth the crawler will go to from the starting URL.
    private int maxDepth = 5;

    
    private class UrlDepthPair {
        public String url;
        public int depth;

        public UrlDepthPair(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }
    }

    
    public Downloader() {
        visitedLinks = new HashSet<>();
        queue = new LinkedList<>();
    }

   
    public void startCrawling(String startUrl) {
        // Add the starting URL to the queue with a depth of 0.
        queue.add(new UrlDepthPair(startUrl, 0));

        // Loop to process each URL in the queue until the queue is empty.
        while (!queue.isEmpty()) {
            // Poll the queue to retrieve and remove the next UrlDepthPair.
            UrlDepthPair pair = queue.poll();
            String currentUrl = pair.url;
            int depth = pair.depth;

            // Check if the URL has not been visited and is within the maximum depth.
            if (!visitedLinks.contains(currentUrl) && depth <= maxDepth && isValidURL(currentUrl)) {
                try {
                    // Use Jsoup to connect to the URL and parse its HTML content.
                    Document document = Jsoup.connect(currentUrl)
                                             .timeout(5000) // Set a 5-second timeout for the connection.
                                             .get();
                    // Process the HTML page to find new links.
                    processPage(document, depth);
                    // Add the URL to the set of visited links.
                    visitedLinks.add(currentUrl);
                    System.out.println("Visited: " + currentUrl);
                } catch (IOException e) {
                    // Handle exceptions for issues like network errors.
                    System.err.println("Error accessing URL '" + currentUrl + "': " + e.getMessage());
                } catch (Exception e) {
                    // Handle any other unexpected exceptions.
                    System.err.println("Unexpected error for URL '" + currentUrl + "': " + e.getMessage());
                }
            }
        }
    }

    // Method to process the HTML content of a page and enqueue new URLs.
    private void processPage(Document document, int depth) {
        // Extract all links (anchor tags with href attributes) from the HTML document.
        Elements linksOnPage = document.select("a[href]");
        // If not at the maximum depth, add new URLs found on the page to the queue.
        if (depth < maxDepth) {
            for (Element page : linksOnPage) {
                String newUrl = page.attr("abs:href");
                // Check if the URL has not been visited before adding it to the queue.
                if (!visitedLinks.contains(newUrl)) {
                    queue.add(new UrlDepthPair(newUrl, depth + 1));
                }
            }
        }
    }

    // Method to validate whether a given string is a well-formed URL with HTTP or HTTPS protocol.
    private boolean isValidURL(String urlString) {
        try {
            URI uri = new URI(urlString);
            URL url = uri.toURL();
            return url.getProtocol().equals("http") || url.getProtocol().equals("https");
        } catch (Exception e) {
            // If the URL is malformed, return false.
            return false;
        }
    }

    public static void main(String[] args) {
        
        Downloader obj = new Downloader();
        obj.startCrawling("https://facebook.com/");
    }
}