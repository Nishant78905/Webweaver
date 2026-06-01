package com.akatsuki.webweaver.service;

import com.akatsuki.webweaver.model.WebGraph;
import com.akatsuki.webweaver.util.HtmlParserUtil;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;

@Service
public class CrawlerService {

    private final HtmlParserUtil htmlParserUtil;
    private final LinkHeuristicScorer heuristicScorer;

    public CrawlerService(HtmlParserUtil htmlParserUtil, LinkHeuristicScorer heuristicScorer) {
        this.htmlParserUtil = htmlParserUtil;
        this.heuristicScorer = heuristicScorer;
    }

    private String getDomain(String urlString) {
        try {
            URI uri = new URI(urlString);
            String domain = uri.getHost();
            return domain != null && domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (Exception e) {
            return "";
        }
    }

    public WebGraph crawl(String startUrl, String topic, int topK, int maxDepth, int maxPages) {
        WebGraph graph = new WebGraph();
        Queue<String> queue = new LinkedList<>();
        String baseDomain = getDomain(startUrl);

        queue.add(startUrl);
        graph.addVisitedPage(startUrl);
        graph.setRelevanceScore(startUrl, 1.0);
        int currentDepth = 0;

        // Core BFS Traversal Loop
        while (!queue.isEmpty() && currentDepth <= maxDepth) {
            if (graph.getVisitedPages().size() >= maxPages) break;

            int levelSize = queue.size();
            // Inside the BFS for loop:
            for (int i = 0; i < levelSize; i++) {
                String currentUrl = queue.poll();

                // 1. Parse the page to get BOTH links and content
                com.akatsuki.webweaver.model.ParsedPage parsedData = htmlParserUtil.parsePage(currentUrl);

                // 2. SEARCH-SPACE PRUNING: Is the page content actually about our topic?
                // (Always allow the startUrl at depth 0 to expand, but check everything else)
                if (currentDepth > 0) {
                    boolean isRelevant = heuristicScorer.isPageContentRelevant(parsedData.getTextContent(), topic);
                    if (!isRelevant) {
                        System.out.println("Pruned dead end (Irrelevant Content): " + currentUrl);
                        continue; // DROP THE NODE! Do not process its links.
                    }
                }

                Map<String, String> candidates = parsedData.getLinks();
                if (candidates.isEmpty()) continue;

                List<Map.Entry<String, Double>> scoredLinks = new ArrayList<>();
                for (Map.Entry<String, String> entry : candidates.entrySet()) {
                    String link = entry.getKey();
                    String anchor = entry.getValue();

                    double finalScore = heuristicScorer.score(link, anchor, topic);

                    // NEW: Threshold Filter! Only add the link if it scored higher than 0
                    if (finalScore > 0.0) {
                        scoredLinks.add(new AbstractMap.SimpleEntry<>(link, finalScore));
                    }
                }

                // 3. Sort candidates by Score (Descending)
                scoredLinks.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

                // ... Inside the loop over scoredLinks ...
                int added = 0;
                for (Map.Entry<String, Double> scoredLink : scoredLinks) {
                    if (added >= topK) break;

                    String link = scoredLink.getKey();
                    double score = scoredLink.getValue();
                    graph.setRelevanceScore(link, score);


                    // CRITICAL BFS FIX: Only add to the queue if we haven't hit the max depth limit!
                    if (currentDepth < maxDepth) {
                        if (!graph.getVisitedPages().contains(link)) {
                            if (graph.getVisitedPages().size() < maxPages) {
                                graph.addVisitedPage(link);
                                queue.add(link);
                                graph.addEdge(currentUrl, link);
                                added++;
                            }
                        } else {
                            graph.addEdge(currentUrl, link);
                            added++;
                        }
                    } else {
                        // At the final depth, just map the edge for PageRank,
                        // but DO NOT waste memory putting it in the BFS queue.
                        graph.addEdge(currentUrl, link);
                        added++;
                    }
                }

                // Polite crawling delay
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
            currentDepth++;
        }
        return graph;
    }
}