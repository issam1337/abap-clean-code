package com.cleanabap.core.rulebase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of applying a single cleanup rule to a code document.
 * Contains both the list of changes made and any findings/hints.
 */
public class CleanupResult {

    private final RuleID ruleId;
    private final List<CleanupChange> changes;
    private final List<CleanupFinding> findings;
    private boolean sourceModified;

    public CleanupResult(RuleID ruleId) {
        this.ruleId = ruleId;
        this.changes = new ArrayList<>();
        this.findings = new ArrayList<>();
        this.sourceModified = false;
    }

    // ─── Builder Methods ─────────────────────────────────────────

    public CleanupResult addChange(int line, String before, String after, String description) {
        changes.add(new CleanupChange(line, before, after, description));
        sourceModified = true;
        return this;
    }

    public CleanupResult addFinding(int line, RuleSeverity severity, String message) {
        findings.add(new CleanupFinding(ruleId, line, severity, message));
        return this;
    }

    public void setSourceModified(boolean modified) {
        this.sourceModified = modified;
    }

    // ─── Access ──────────────────────────────────────────────────

    public RuleID getRuleId()                              { return ruleId; }
    public List<CleanupChange> getChanges()                { return Collections.unmodifiableList(changes); }
    public List<CleanupFinding> getFindings()               { return Collections.unmodifiableList(findings); }
    public boolean isSourceModified()                       { return sourceModified; }
    public int getChangeCount()                             { return changes.size(); }
    public int getFindingCount()                            { return findings.size(); }

    /** Factory: empty result (no changes, no findings) */
    public static CleanupResult empty(RuleID ruleId) {
        return new CleanupResult(ruleId);
    }

    // ═══════════════════════════════════════════════════════════════
    // Inner Classes
    // ═══════════════════════════════════════════════════════════════

    /**
     * A single change made by a rule.
     */
    public static class CleanupChange {
        private final int line;
        private final String before;
        private final String after;
        private final String description;

        public CleanupChange(int line, String before, String after, String description) {
            this.line = line;
            this.before = before;
            this.after = after;
            this.description = description;
        }

        public int getLine()          { return line; }
        public String getBefore()     { return before; }
        public String getAfter()      { return after; }
        public String getDescription() { return description; }
    }
}
