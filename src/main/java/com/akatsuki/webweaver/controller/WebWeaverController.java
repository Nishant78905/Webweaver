package com.akatsuki.webweaver.controller;

import com.akatsuki.webweaver.model.WebGraph;
import com.akatsuki.webweaver.service.CrawlerService;
import com.akatsuki.webweaver.service.PageRankService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/crawler")
public class WebWeaverController {

    private final CrawlerService crawlerService;
    private final PageRankService pageRankService;

    public WebWeaverController(CrawlerService crawlerService, PageRankService pageRankService) {
        this.crawlerService = crawlerService;
        this.pageRankService = pageRankService;
    }

    @GetMapping("/run")
    public Map<String, Object> runCrawler(
            @RequestParam String startUrl,
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int topK, // Branching factor for the BFS
            @RequestParam(defaultValue = "3") int depth,
            @RequestParam(defaultValue = "40") int maxPages,
            @RequestParam(defaultValue = "15") int resultLimit) { // NEW: How many results to show in the UI

        startUrl = startUrl.trim();
        if (!startUrl.startsWith("http://") && !startUrl.startsWith("https://")) {
            startUrl = "https://" + startUrl;
        }

        // 1. Run the Crawler & Math Engine
        WebGraph graph = crawlerService.crawl(startUrl, topic, topK, depth, maxPages);
        Map<String, Double> ranks = pageRankService.calculatePageRank(graph);

        // 2. Sort the Hybrid PageRank Scores
        List<Map.Entry<String, Double>> sortedRanks = new ArrayList<>(ranks.entrySet());
        sortedRanks.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        // 3. PHASE 5 FIX: Un-hardcode the UI limit
        List<Map<String, Object>> topPages = new ArrayList<>();
        for (int i = 0; i < Math.min(resultLimit, sortedRanks.size()); i++) {
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("url", sortedRanks.get(i).getKey());
            pageData.put("rank", sortedRanks.get(i).getValue());
            topPages.add(pageData);
        }

        // 4. PHASE 5 FIX: Protect Frontend from OOM (Out of Memory) Crashes
        // If maxPages is 100, the graph could have thousands of edges. Vis.js will crash.
        // We cap the visualization payload to a max of ~150 edges.
        Map<String, List<String>> safeVisualizationGraph = new HashMap<>();
        int edgeCount = 0;

        for (Map.Entry<String, List<String>> entry : graph.getAdjacencyList().entrySet()) {
            if (edgeCount > 150) break;

            safeVisualizationGraph.put(entry.getKey(), entry.getValue());
            if (entry.getValue() != null) {
                edgeCount += entry.getValue().size();
            }
        }

        // 5. Build Response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalVisited", graph.getVisitedPages().size());
        response.put("topPages", topPages);
        response.put("graph", safeVisualizationGraph); // Send the protected sub-graph

        return response;
    }
}