package com.cleanabap.core.rules.alignment;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Align method-call parameter assignments (analysis-only).
 *
 * <p>For multi-line calls of the form:</p>
 * <pre>
 * lo_handler->process(
 *     iv_id    = 1
 *     iv_name  = 'X'
 *     iv_count = 4 ).
 * </pre>
 * <p>the {@code =} signs should line up. This rule reports calls where
 * two or more {@code parameter = value} lines have differently-indented
 * {@code =}.</p>
 *
 * <p>Auto-fix is not attempted here: realigning columns reliably across
 * an arbitrary call requires column-aware editing, which is out of scope
 * for the lightweight token model.</p>
 */
public class AlignParametersRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Align parameters", "#align-assignments-but-not-with-others-the-same-with-types-and-parameters"),
    };

    @Override public RuleID getID()             { return RuleID.ALIGN_PARAMETERS; }
    @Override public String getName()           { return "Align method-call parameters"; }
    @Override public RuleCategory getCategory() { return RuleCategory.ALIGNMENT; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports multi-line method calls whose parameter '=' signs "
             + "are not vertically aligned.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (stmt.getLineCount() < 2) return;

        Integer firstCol = null;
        int badLine = -1;
        int seenLine = -1;
        for (Token t : stmt.getTokens()) {
            if (!t.isOperator() || !"=".equals(t.getText())) continue;
            // Only the first `=` per source line counts.
            if (t.getLine() == seenLine) continue;
            seenLine = t.getLine();
            if (firstCol == null) {
                firstCol = t.getColumn();
            } else if (t.getColumn() != firstCol) {
                badLine = t.getLine();
                break;
            }
        }
        if (badLine > 0) {
            result.addFinding(badLine, RuleSeverity.INFO,
                "Parameter '=' columns are not aligned across lines");
        }
    }
}
