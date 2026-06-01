package com.akatsuki.webweaver.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class LinkHeuristicScorer {

    public double score(String url, String anchor, String topic) {
        double score = 0.0;
        String lowerUrl = url.toLowerCase();
        String lowerAnchor = anchor.toLowerCase().trim();
        String exactTopic = topic.toLowerCase().trim();

        // 1. EXACT PHRASE MATCH (The Golden Rule)
        // If the exact phrase "rock music" is in the anchor or URL, give it massive points
        if (lowerAnchor.contains(exactTopic)) {
            score += 0.8;
        }
        if (lowerUrl.contains(exactTopic.replace(" ", "-")) || lowerUrl.contains(exactTopic.replace(" ", "_"))) {
            score += 0.8;
        }

        // 2. Individual Word Matches
        String[] keywords = exactTopic.split("\\s+");
        for (String kw : keywords) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(kw) + "\\b");
            if (pattern.matcher(lowerUrl).find()) {
                score += 0.3;
            }
            if (pattern.matcher(lowerAnchor).find()) {
                score += 0.2;
            }
        }

        // 3. Structural Penalties
        if (lowerUrl.matches(".*(login|password|cart|checkout|signup|register|terms|privacy).*")) {
            score -= 1.0;
        }

        return Math.max(0.0, Math.min(score, 1.0));
    }

    public boolean isPageContentRelevant(String contentText, String topic) {
        if (contentText == null || contentText.isEmpty()) return false;

        String lowerContent = contentText.toLowerCase();
        String exactTopic = topic.toLowerCase().trim();

        // 1. Does the exact phrase "rock music" appear in the article?
        if (lowerContent.contains(exactTopic)) {
            return true; // Instant pass!
        }

        // 2. If not, make sure ALL individual words appear at least once
        String[] keywords = exactTopic.split("\\s+");
        int uniqueKeywordMatches = 0;

        for (String kw : keywords) {
            Matcher m = Pattern.compile("\\b" + Pattern.quote(kw) + "\\b").matcher(lowerContent);
            if (m.find()) {
                uniqueKeywordMatches++; // Only increments once per unique keyword found
            }
        }

        // Must find ALL words in the topic (e.g., both "rock" AND "music")
        return uniqueKeywordMatches >= keywords.length;
    }
}