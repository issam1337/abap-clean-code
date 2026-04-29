package com.cleanabap.core.rulebase;

/**
 * A finding reported by a rule that cannot be auto-fixed.
 * These appear in the "Findings" panel and require manual review.
 */
public class CleanupFinding {

    private final RuleID ruleId;
    private final int line;
    private final RuleSeverity severity;
    private final String message;

    public CleanupFinding(RuleID ruleId, int line, RuleSeverity severity, String message) {
        this.ruleId = ruleId;
        this.line = line;
        this.severity = severity;
        this.message = message;
    }

    public RuleID getRuleId()        { return ruleId; }
    public int getLine()             { return line; }
    public RuleSeverity getSeverity() { return severity; }
    public String getMessage()       { return message; }

    @Override
    public String toString() {
        return String.format("[%s] L%d: %s - %s", severity, line, ruleId, message);
    }
}
