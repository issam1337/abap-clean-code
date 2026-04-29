package com.cleanabap.core.rulebase;

/**
 * Categories for organizing cleanup rules.
 * Mirrors the structure of the Clean ABAP Styleguide sections.
 */
public enum RuleCategory {
    SYNTAX("Syntax Modernization",
        "Replaces obsolete language constructs with modern ABAP equivalents"),

    DECLARATIONS("Declarations",
        "Improves variable, type, and constant declarations"),

    NAMING("Naming Conventions",
        "Enforces descriptive naming and discourages encodings"),

    COMMANDS("Commands",
        "Simplifies control flow and method calls"),

    FORMATTING("Formatting",
        "Adjusts line length, bracket placement, and code layout"),

    EMPTY_LINES("Empty Lines & Spaces",
        "Standardizes whitespace, blank lines, and spacing"),

    COMMENTS("Comments",
        "Improves comment style and removes noise"),

    ALIGNMENT("Alignment",
        "Aligns parameters, declarations, and assignments");

    private final String displayName;
    private final String description;

    RuleCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
