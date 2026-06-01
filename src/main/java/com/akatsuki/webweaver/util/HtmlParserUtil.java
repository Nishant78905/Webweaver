package com.akatsuki.webweaver.util;

import com.akatsuki.webweaver.model.ParsedPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HtmlParserUtil {

    // PHASE 2 FIX: Massively expanded blocklist to catch utility pages, social media, and legal boilerplate
    // Added Wikipedia namespaces and query-param traps to the blocklist
    private static final List<String> JUNK_PATTERNS = List.of(
            "login", "signup", "register", "password", "cart", "checkout",
            "privacy", "terms", "cookie", "gdpr", "policy",
            "about", "contact", "sitemap", "careers", "press",
            "subscribe", "newsletter", "rss", "feed",
            "share", "tweet", "facebook.com", "twitter.com", "linkedin.com", "instagram.com", "youtube.com", "whatsapp:",

            // --- NEW: WIKIPEDIA & UTILITY TRAPS ---
            "/w/index.php", "special:", "help:", "user:", "wikipedia:", "talk:", "category:", "?action=", "?title="
    );
    /**
     * PHASE 1 FIX: Normalizes URLs to prevent duplicates in the graph
     */
    private String normalizeUrl(String url) {
        int hashIndex = url.indexOf('#');
        if (hashIndex != -1) url = url.substring(0, hashIndex);

        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        int utmIndex = url.indexOf("?utm_");
        if (utmIndex != -1) url = url.substring(0, utmIndex);

        return url.trim();
    }

    public ParsedPage parsePage(String url) {
        Map<String, String> candidateLinks = new HashMap<>();
        String pageText = ""; // NEW: Hold the content text

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) WebWeaver-Academic-Bot")
                    .timeout(10000)
                    .get();

            // 1. Structural Pruning (Kept from Phase 2)
// 1. Structural Pruning (Safer Version)
            doc.select("nav, header, footer, aside, .sidebar, .advertisement, .cookie-banner, #mw-navigation, #p-navigation").remove();
            // 2. NEW: Extract the pure readable text from the remaining DOM
            pageText = doc.body().text();

            // 3. Extract Links (Kept from Phase 2)
            Elements aTags = doc.select("a[href]");
            for (Element tag : aTags) {
                String rawLink = tag.attr("abs:href");
                String text = tag.text().trim();

                if (rawLink.startsWith("http")) {
                    String link = normalizeUrl(rawLink);
                    boolean isJunk = JUNK_PATTERNS.stream().anyMatch(link.toLowerCase()::contains);

                    if (!isJunk && !text.isEmpty()) {
                        candidateLinks.put(link, text);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Skipped uncooperative/broken page: " + url);
        }

        // Return both the links and the text content
        return new ParsedPage(candidateLinks, pageText);
    }
}