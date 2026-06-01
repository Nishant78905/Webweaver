package com.akatsuki.webweaver.model;

import java.util.*;

public class WebGraph {
    private Set<String> visitedPages;
    private Map<String, List<String>> adjacencyList;
    private Map<String, Double> pageRelevanceScores;

    public WebGraph() {
        // LinkedHashSet maintains the order in which we visited the pages
        this.visitedPages = new LinkedHashSet<>();
        this.adjacencyList = new HashMap<>();
        this.pageRelevanceScores = new HashMap<>();
    }

    public void addVisitedPage(String url) {
        visitedPages.add(url);
        adjacencyList.putIfAbsent(url, new ArrayList<>());
    }

    public void addEdge(String sourceUrl, String targetUrl) {
        // Ensure the source node exists
        adjacencyList.putIfAbsent(sourceUrl, new ArrayList<>());

        // CRITICAL FIX: Ensure the target node is tracked so PageRank sees it,
        // even if we hit maxPages and never officially crawl it!
        adjacencyList.putIfAbsent(targetUrl, new ArrayList<>());

        // Draw the directed edge
        adjacencyList.get(sourceUrl).add(targetUrl);
    }

    public void setRelevanceScore(String url, double score) {
        pageRelevanceScores.put(url, score);
    }

    public double getRelevanceScore(String url) {
        // Default to a small non-zero score if unknown, to prevent multiplying by 0
        return pageRelevanceScores.getOrDefault(url, 0.1);
    }


    public Set<String> getVisitedPages() { return visitedPages; }
    public Map<String, List<String>> getAdjacencyList() { return adjacencyList; }
}