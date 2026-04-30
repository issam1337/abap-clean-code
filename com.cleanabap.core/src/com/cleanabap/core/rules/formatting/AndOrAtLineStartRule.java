package com.cleanabap.core.rules.formatting;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Place {@code AND} / {@code OR} at the start of a
 * continuation line, not the end (analysis-only).
 *
 * <p>For multi-line conditions the Clean ABAP guide recommends starting
 * the continuation line with the connective so the operator structure
 * is immediately visible. Auto-rewrite would need to re-flow the
 * condition across lines, which is left for a future formatter.</p>
 *
 * <h3>Discouraged:</h3>
 * <pre>
 * IF lv_a = 1 AND
 *    lv_b = 2 OR
 *    lv_c = 3.
 * </pre>
 *
 * <h3>Recommended:</h3>
 * <pre>
 * IF    lv_a = 1
 *   AND lv_b = 2
 *   OR  lv_c = 3.
 * </pre>
 */
public class AndOrAtLineStartRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "AND/OR at the start of the continuation line", "#put-andor-at-the-beginning-of-line"),
    };

    @Override public RuleID getID()             { return RuleID.AND_OR_AT_LINE_START; }
    @Override public String getName()           { return "Place AND/OR at line start"; }
    @Override public RuleCategory getCategory() { return RuleCategory.FORMATTING; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports multi-line conditions where AND / OR appears at the "
             + "end of the line instead of the beginning of the next.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (stmt.getLineCount() < 2) return;
        // Restrict to statements that contain a condition.
        if (!stmt.startsWithAnyKeyword("IF", "ELSEIF", "WHILE", "CHECK")) return;

        for (Token t : stmt.getTokens()) {
            if (!t.isKeyword()) continue;
            if (!t.textEqualsIgnoreCase("AND") && !t.textEqualsIgnoreCase("OR")) continue;
            if (t.isLastTokenInLine()) {
                result.addFinding(t.getLine(), RuleSeverity.INFO,
                    "'" + t.getText().toUpperCase() + "' at line end — move to the start of the next line");
                return; // one finding per statement
            }
        }
    }
}
