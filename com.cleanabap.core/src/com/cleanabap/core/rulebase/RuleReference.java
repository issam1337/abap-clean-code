package com.cleanabap.core.rulebase;

/**
 * Reference linking a cleanup rule to its source documentation.
 * Multiple references can be attached to a single rule (e.g.,
 * both the Clean ABAP Styleguide and Code Pal).
 */
public class RuleReference {

    private final RuleSource source;
    private final String title;
    private final String anchor;  // e.g., "#prefer-new-to-create-object"

    public RuleReference(RuleSource source, String title, String anchor) {
        this.source = source;
        this.title = title;
        this.anchor = anchor;
    }

    public RuleSource getSource() { return source; }
    public String getTitle()      { return title; }
    public String getAnchor()     { return anchor; }

    /**
     * Get the full URL to the reference.
     */
    public String getUrl() {
        if (source.getBaseUrl().isEmpty()) return "";
        return source.getBaseUrl() + anchor;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", source.getDisplayName(), title);
    }
}
