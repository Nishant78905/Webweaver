package com.akatsuki.webweaver.service;

import com.akatsuki.webweaver.model.WebGraph;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PageRankService {

    private static final double DAMPING_FACTOR = 0.85;
    private static final int ITERATIONS = 10;

    public Map<String, Double> calculatePageRank(WebGraph graph) {
        Map<String, List<String>> adjList = graph.getAdjacencyList();
        int totalNodes = adjList.size();

        if (totalNodes == 0) return new HashMap<>();

        Map<String, Double> ranks = new HashMap<>();
        for (String node : adjList.keySet()) {
            ranks.put(node, 1.0 / totalNodes);
        }

        // --- PART 1: STRUCTURAL PAGERANK WITH DANGLING NODE FIX ---
        for (int i = 0; i < ITERATIONS; i++) {
            Map<String, Double> newRanks = new HashMap<>();

            // 1a. Calculate "Leaked" Rank from Dangling Nodes
            double danglingSum = 0.0;
            for (String node : adjList.keySet()) {
                List<String> outboundLinks = adjList.get(node);
                if (outboundLinks == null || outboundLinks.isEmpty()) {
                    danglingSum += ranks.get(node);
                }
            }

            // 1b. Redistribute Rank
            for (String node : adjList.keySet()) {
                // Base formula + redistributed dangling rank
                double rankSum = (1.0 - DAMPING_FACTOR) / totalNodes;
                rankSum += DAMPING_FACTOR * (danglingSum / totalNodes);

                // Add standard inbound link rank
                for (Map.Entry<String, List<String>> entry : adjList.entrySet()) {
                    String potentialSource = entry.getKey();
                    List<String> outboundLinks = entry.getValue();

                    if (outboundLinks != null && outboundLinks.contains(node)) {
                        rankSum += DAMPING_FACTOR * (ranks.get(potentialSource) / outboundLinks.size());
                    }
                }
                newRanks.put(node, rankSum);
            }
            ranks = newRanks;
        }

        // --- PART 2: TOPIC-SENSITIVE HYBRID MULTIPLIER ---
        Map<String, Double> finalHybridRanks = new HashMap<>();
        for (Map.Entry<String, Double> entry : ranks.entrySet()) {
            String url = entry.getKey();
            double structuralRank = entry.getValue();

            // Fetch the heuristic topic score we saved during the crawl
            double relevanceScore = graph.getRelevanceScore(url);

            // Final Rank = Network Authority * Semantic Relevance
            double finalScore = structuralRank * relevanceScore;
            finalHybridRanks.put(url, finalScore);
        }

        return finalHybridRanks;
    }
}