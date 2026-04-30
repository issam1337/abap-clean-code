package com.cleanabap.core.rules.declarations;

import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.Map;

/**
 * <b>Rule:</b> Prefer FINAL for write-once locals (analysis-only).
 *
 * <p>Reports {@code DATA name [...]} declarations whose corresponding
 * variable is assigned exactly once and read at least once afterwards.
 * Such variables can be replaced with the inline {@code FINAL(...)}
 * declaration, which makes the read-only intent explicit and lets the
 * compiler enforce it.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * DATA lv_count TYPE i.
 * lv_count = lines( lt_items ).
 * WRITE lv_count.
 * </pre>
 *
 * <h3>After (manual rewrite):</h3>
 * <pre>
 * FINAL(lv_count) = lines( lt_items ).
 * WRITE lv_count.
 * </pre>
 */
public class PreferFinalRule extends Rule {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Prefer FINAL over DATA when possible",
            "#prefer-final-over-data-when-possible"),
    };

    @Override public RuleID getID()             { return RuleID.PREFER_FINAL; }
    @Override public String getName()           { return "Prefer FINAL for write-once variables"; }
    @Override public RuleCategory getCategory() { return RuleCategory.DECLARATIONS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public int getMinAbapRelease()     { return 754; } // FINAL was added in 7.54
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports local variables that are assigned exactly once. "
             + "These can be declared inline with FINAL(name) to make the "
             + "read-only intent explicit (requires ABAP 7.54+).";
    }

    @Override
    public String getExampleBefore() {
        return "DATA lv_count TYPE i.\n"
             + "lv_count = lines( lt_items ).\n"
             + "WRITE lv_count.";
    }

    @Override
    public String getExampleAfter() {
        return "FINAL(lv_count) = lines( lt_items ).\n"
             + "WRITE lv_count.";
    }

    @Override
    public CleanupResult apply(CodeDocument doc) {
        CleanupResult result = new CleanupResult(getID());

        Map<String, LocalVariableUsageAnalyzer.VarInfo> infos =
                LocalVariableUsageAnalyzer.analyze(doc);

        for (LocalVariableUsageAnalyzer.VarInfo info : infos.values()) {
            // Exactly one write and at least one read.
            if (info.writeStmts.size() == 1 && !info.readStmts.isEmpty()) {
                result.addFinding(info.declStmt.getStartLine(), RuleSeverity.INFO,
                    "Variable '" + info.name + "' is assigned only once — "
                    + "consider declaring it inline with FINAL(" + info.name + ")");
            }
        }

        return result;
    }
}
