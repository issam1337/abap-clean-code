package com.cleanabap.core.rulebase;

/**
 * Severity levels for cleanup findings.
 */
public enum RuleSeverity {
    /** Must fix — violates a core Clean ABAP rule */
    ERROR("Error", 1),

    /** Should fix — recommended by the Styleguide */
    WARNING("Warning", 2),

    /** Consider fixing — advisory, may depend on team preference */
    INFO("Info", 3),

    /** Hint only — informational, no auto-fix available */
    HINT("Hint", 4);

    private final String displayName;
    private final int priority;

    RuleSeverity(String displayName, int priority) {
        this.displayName = displayName;
        this.priority = priority;
    }

    public String getDisplayName() { return displayName; }
    public int getPriority()       { return priority; }
}
