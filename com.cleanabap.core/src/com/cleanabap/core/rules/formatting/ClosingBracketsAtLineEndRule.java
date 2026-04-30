package com.cleanabap.core.rules.formatting;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Place closing brackets at line end (analysis-only).
 *
 * <p>Reports multi-line method calls / constructors where the closing
 * {@code )} sits alone on its own line. The Clean ABAP guide recommends
 * keeping closing brackets on the same line as the last argument.
 * Auto-rewrite is omitted because moving the bracket interacts with
 * trailing comments and indentation.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * call_method(
 *   iv_a = 1
 *   iv_b = 2
 * ).
 * </pre>
 *
 * <h3>After (manual):</h3>
 * <pre>
 * call_method(
 *   iv_a = 1
 *   iv_b = 2 ).
 * </pre>
 */
public class ClosingBracketsAtLineEndRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Close brackets at line end", "#close-brackets-at-line-end"),
    };

    @Override public RuleID getID()             { return RuleID.CLOSING_BRACKETS_AT_LINE_END; }
    @Override public String getName()           { return "Close brackets at line end"; }
    @Override public RuleCategory getCategory() { return RuleCategory.FORMATTING; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports multi-line calls whose closing ) sits on its own line.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (stmt.getLineCount() < 2) return;
        for (Token t : stmt.getTokens()) {
            if (t.getType() != Token.Type.PUNCTUATION) continue;
            if (!")".equals(t.getText())) continue;
            // Check whether the closing paren is the only significant token on its line.
            if (t.isFirstTokenInLine()) {
                result.addFinding(t.getLine(), RuleSeverity.INFO,
                    "Closing ) on its own line — move to end of previous line");
                return; // one finding per statement
            }
        }
    }
}
