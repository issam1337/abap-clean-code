package com.cleanabap.core.rules.commands;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Keep methods small (analysis-only).
 *
 * <p>Reports {@code METHOD … ENDMETHOD} bodies that exceed a configurable
 * line threshold. Long methods are typically a smell; the Clean ABAP
 * guide recommends extracting cohesive sub-tasks. Auto-fix is impossible:
 * splitting a method into smaller ones requires semantic understanding
 * of the data flow.</p>
 */
public class PreferMethodSmallRule extends RuleForStatements {

    /** Default soft cap on method body line count. */
    private static final int DEFAULT_MAX_LINES = 40;

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Keep methods small", "#keep-methods-small"),
    };

    @Override public RuleID getID()             { return RuleID.PREFER_METHOD_SMALL; }
    @Override public String getName()           { return "Keep methods small"; }
    @Override public RuleCategory getCategory() { return RuleCategory.COMMANDS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports METHOD … ENDMETHOD bodies longer than "
             + DEFAULT_MAX_LINES + " lines.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.isMethodStart()) return;

        // Walk forward to find the matching ENDMETHOD.
        AbapStatement walker = stmt.getNext();
        AbapStatement end = null;
        while (walker != null) {
            if (walker.startsWithKeyword("ENDMETHOD")) { end = walker; break; }
            walker = walker.getNext();
        }
        if (end == null) return;

        int lines = end.getEndLine() - stmt.getStartLine() + 1;
        if (lines > DEFAULT_MAX_LINES) {
            result.addFinding(stmt.getStartLine(), RuleSeverity.INFO,
                "Method body is " + lines + " lines (> " + DEFAULT_MAX_LINES
                + ") — consider extracting sub-tasks");
        }
    }
}
