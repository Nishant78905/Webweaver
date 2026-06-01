package com.akatsuki.webweaver.model;

import java.util.Map;

public class ParsedPage {
    private final Map<String, String> links;
    private final String textContent;

    public ParsedPage(Map<String, String> links, String textContent) {
        this.links = links;
        this.textContent = textContent;
    }

    public Map<String, String> getLinks() { return links; }
    public String getTextContent() { return textContent; }
}