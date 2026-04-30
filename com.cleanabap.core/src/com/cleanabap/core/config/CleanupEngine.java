package com.cleanabap.core.config;

import com.cleanabap.core.profiles.CleanupProfile;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The main cleanup engine — orchestrates rule execution on code documents.
 *
 * <p>Usage:
 * <pre>
 * CleanupEngine engine = new CleanupEngine();
 * CleanupProfile profile = CleanupProfile.createDefault();
 * CleanupSession session = engine.clean(sourceCode, profile);
 *
 * String cleanedCode = session.getCleanedSource();
 * List&lt;CleanupFinding&gt; findings = session.getAllFindings();
 * </pre>
 *
 * <p>The engine:
 * <ol>
 *   <li>Parses the source code into a {@link CodeDocument}</li>
 *   <li>Iterates through active rules in the profile</li>
 *   <li>Applies each rule, collecting results</li>
 *   <li>Returns a {@link CleanupSession} with the cleaned code and findings</li>
 * </ol>
 */
public class CleanupEngine {

    /**
     * Maximum number of cleanup passes to attempt before giving up. Most
     * documents converge after 1–2 passes; the cap prevents an infinite
     * loop in case two rules ever oscillate.
     */
    private static final int MAX_PASSES = 10;

    private final RuleRegistry registry;

    public CleanupEngine() {
        this.registry = RuleRegistry.getInstance();
    }

    // ─── Main API ────────────────────────────────────────────────

    /**
     * Clean the given ABAP source code using the specified profile.
     *
     * @param source  Raw ABAP source code
     * @param profile Cleanup profile determining active rules
     * @return Session containing cleaned code and all results
     */
    public CleanupSession clean(String source, CleanupProfile profile) {
        return clean(source, profile, new CleanupOptions());
    }

    /**
     * Clean with additional options (e.g., line range restriction).
     */
    public CleanupSession clean(String source, CleanupProfile profile,
                                 CleanupOptions options) {
        long startTime = System.nanoTime();

        // Parse source
        CodeDocument doc = new CodeDocument(source);
        if (options.getAbapRelease() > 0) {
            doc.setAbapRelease(options.getAbapRelease());
        }

        // Get active rules from profile, filtered by options
        List<Rule> activeRules = registry.getAllRules().stream()
            .filter(r -> profile.isRuleActive(r.getID()))
            .filter(r -> doc.getAbapRelease() == 0 || r.getMinAbapRelease() <= doc.getAbapRelease())
            .collect(Collectors.toList());

        // Apply rules in a fixed-point loop: keep running passes until no
        // further changes are observed (or the safety cap MAX_PASSES is hit).
        //
        // Why a loop? Some transformations expose new opportunities for the
        // *same* or *other* rules:
        //   • UNCHAIN turns one DATA: chain into N statements, each of which
        //     a later rule may need to rewrite.
        //   • A rule that mutates statement n may, on rare buggy edge cases,
        //     skip statement n+1 within a single pass; running again catches it.
        // Without the loop, a user has to click "Clean up" repeatedly, which
        // is exactly the symptom reported. The loop ends as soon as a pass
        // produces no source change, so the common case stays a single pass.
        List<CleanupResult> results = new ArrayList<>();
        List<RuleID> appliedRules = new ArrayList<>();

        int passCount = 0;
        boolean changedInPass;
        do {
            changedInPass = false;
            String sourceBeforePass = doc.getCurrentSource();

            for (Rule rule : activeRules) {
                try {
                    // Apply config overrides from profile
                    applyConfigOverrides(rule, profile);

                    String sourceBeforeRule = doc.getCurrentSource();
                    CleanupResult result = rule.apply(doc);
                    results.add(result);

                    boolean ruleChangedSource =
                        result.isSourceModified()
                            || !doc.getCurrentSource().equals(sourceBeforeRule);

                    if (ruleChangedSource) {
                        if (!appliedRules.contains(rule.getID())) {
                            appliedRules.add(rule.getID());
                        }
                    }
                } catch (Exception e) {
                    // Rule failed — log but continue with other rules
                    System.err.println("Rule " + rule.getID() + " failed: " + e.getMessage());
                    CleanupResult errorResult = new CleanupResult(rule.getID());
                    errorResult.addFinding(0, RuleSeverity.ERROR,
                        "Rule execution failed: " + e.getMessage());
                    results.add(errorResult);
                }
            }

            // Did anything in this pass change the source?
            if (!doc.getCurrentSource().equals(sourceBeforePass)) {
                changedInPass = true;
            }
            passCount++;
        } while (changedInPass && passCount < MAX_PASSES);

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;

        return new CleanupSession(
            source,
            doc.getCurrentSource(),
            results,
            appliedRules,
            activeRules.size(),
            elapsed
        );
    }

    /**
     * Apply profile configuration overrides to a rule's config values.
     */
    private void applyConfigOverrides(Rule rule, CleanupProfile profile) {
        for (ConfigValue<?> config : rule.getConfigValues()) {
            String fullKey = config.getFullKey();
            if (profile.hasConfigOverride(fullKey)) {
                String value = profile.getConfigOverride(fullKey);
                applyStringValue(config, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void applyStringValue(ConfigValue<?> config, String stringValue) {
        switch (config.getType()) {
            case BOOLEAN:
                ((ConfigValue<Boolean>) config).setValue(Boolean.parseBoolean(stringValue));
                break;
            case INTEGER:
                ((ConfigValue<Integer>) config).setValue(Integer.parseInt(stringValue));
                break;
            case STRING:
                ((ConfigValue<String>) config).setValue(stringValue);
                break;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CLEANUP SESSION — result container
    // ═══════════════════════════════════════════════════════════════

    /**
     * Result of a cleanup operation.
     */
    public static class CleanupSession {
        private final String originalSource;
        private final String cleanedSource;
        private final List<CleanupResult> results;
        private final List<RuleID> appliedRules;
        private final int totalActiveRules;
        private final long elapsedMs;

        public CleanupSession(String originalSource, String cleanedSource,
                               List<CleanupResult> results, List<RuleID> appliedRules,
                               int totalActiveRules, long elapsedMs) {
            this.originalSource = originalSource;
            this.cleanedSource = cleanedSource;
            this.results = results;
            this.appliedRules = appliedRules;
            this.totalActiveRules = totalActiveRules;
            this.elapsedMs = elapsedMs;
        }

        public String getOriginalSource()  { return originalSource; }
        public String getCleanedSource()    { return cleanedSource; }
        public boolean isModified()         { return !originalSource.equals(cleanedSource); }
        public List<RuleID> getAppliedRules() { return appliedRules; }
        public int getAppliedRuleCount()    { return appliedRules.size(); }
        public int getTotalActiveRules()    { return totalActiveRules; }
        public long getElapsedMs()          { return elapsedMs; }

        /** Get all findings across all rules. */
        public List<CleanupFinding> getAllFindings() {
            return results.stream()
                .flatMap(r -> r.getFindings().stream())
                .sorted((a, b) -> Integer.compare(a.getLine(), b.getLine()))
                .collect(Collectors.toList());
        }

        /** Get all changes across all rules. */
        public List<CleanupResult.CleanupChange> getAllChanges() {
            return results.stream()
                .flatMap(r -> r.getChanges().stream())
                .sorted((a, b) -> Integer.compare(a.getLine(), b.getLine()))
                .collect(Collectors.toList());
        }

        /** Total number of changed lines. */
        public int getChangedLineCount() {
            return (int) getAllChanges().stream()
                .map(CleanupResult.CleanupChange::getLine)
                .distinct()
                .count();
        }

        /** Summary for logging/display. */
        public String getSummary() {
            return String.format(
                "Cleanup complete: %d/%d rules applied, %d changes, %d findings, %dms",
                appliedRules.size(), totalActiveRules,
                getAllChanges().size(), getAllFindings().size(), elapsedMs);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CLEANUP OPTIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Additional options for cleanup execution.
     */
    public static class CleanupOptions {
        private int abapRelease = 0;
        private int startLine = -1;
        private int endLine = -1;

        public int getAbapRelease()   { return abapRelease; }
        public int getStartLine()     { return startLine; }
        public int getEndLine()       { return endLine; }

        public CleanupOptions setAbapRelease(int release) {
            this.abapRelease = release; return this;
        }

        public CleanupOptions setLineRange(int start, int end) {
            this.startLine = start; this.endLine = end; return this;
        }
    }
}
