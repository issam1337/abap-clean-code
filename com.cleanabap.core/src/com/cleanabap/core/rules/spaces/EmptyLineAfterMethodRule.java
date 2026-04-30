package com.cleanabap.core.rules.spaces;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Insert a blank line between consecutive method
 * implementations (analysis-only).
 *
 * <p>Reports {@code ENDMETHOD.} immediately followed by another
 * {@code METHOD …} without an intervening blank line. Auto-rewrite
 * is skipped because the right number / placement of blanks varies
 * across team styles.</p>
 */
public class EmptyLineAfterMethodRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Don't obsess about formatting", "#dont-obsess-about-formatting"),
    };

    @Override public RuleID getID()             { return RuleID.EMPTY_LINE_AFTER_METHOD; }
    @Override public String getName()           { return "Insert blank line between methods"; }
    @Override public RuleCategory getCategory() { return RuleCategory.EMPTY_LINES; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports ENDMETHOD followed immediately by another METHOD with "
             + "no blank line in between.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.startsWithKeyword("ENDMETHOD")) return;

        AbapStatement next = stmt.getNext();
        if (next == null) return;
        // If the immediate next statement is a METHOD opener (no empty-line
        // statement between them), flag it.
        if (next.startsWithKeyword("METHOD")) {
            result.addFinding(next.getStartLine(), RuleSeverity.INFO,
                "Missing blank line between method implementations");
        }
    }
}
