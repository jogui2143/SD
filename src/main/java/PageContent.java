import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PageContent implements Serializable, Comparable<PageContent> {
    private static final int MAX_CITATION_LENGTH = 500;
    private static final long serialVersionUID=1l;

    private String title;
    private Set<String> words;
    private String url;
    private Set<String> hyperLinks;
    private int numberOfReferences;
    private String shortCitation; // Truncated text as a citation

    public PageContent(String title, String originalText, String url, Set<String> hyperLinkStrings, int numberOfReferences) {
        this.title = title;
        this.url = url;
        this.hyperLinks = hyperLinkStrings != null ? hyperLinkStrings : Set.of();
        this.numberOfReferences = numberOfReferences;
        
        this.words = processText(originalText);
        this.shortCitation = truncateText(originalText);
    }

    private Set<String> processText(String text) {
        if (text == null || text.isEmpty()) {
            return new HashSet<>();
        }
        return Arrays.stream(text.split("\\W+"))
                     .filter(word -> !word.isEmpty())
                     .collect(Collectors.toSet());
    }

    private String truncateText(String text) {
        if (text != null && text.length() > MAX_CITATION_LENGTH) {
            return text.substring(0, MAX_CITATION_LENGTH) + "...";
        }
        return text;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public Set<String> getWords() {
        return words;
    }

    public String getUrl() {
        return url;
    }

    public Set<String> getHyperLinks() {
        return hyperLinks;
    }

    public int getNumberOfReferences() {
        return numberOfReferences;
    }

    public String getShortCitation() {
        return shortCitation;
    }

    @Override
    public int compareTo(PageContent other) {
        return Integer.compare(other.numberOfReferences, this.numberOfReferences);
    }

    @Override
    public String toString() {
        return "PageContent{" +
                "title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", shortCitation='" + shortCitation + '\'' +
                ", numberOfReferences=" + numberOfReferences +
                ", words=" + words +
                '}';
    }
}
