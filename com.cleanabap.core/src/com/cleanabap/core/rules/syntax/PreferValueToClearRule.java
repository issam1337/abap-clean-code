package com.cleanabap.core.rules.syntax;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Prefer {@code VALUE #( )} to {@code CLEAR} (analysis-only).
 *
 * <p>Reports {@code CLEAR} statements that target a single variable. The
 * SAP guide recommends initialising structures and tables with the
 * inline {@code VALUE #( )} constructor at the assignment site rather
 * than zeroing them with {@code CLEAR}.</p>
 *
 * <p>Auto-rewrite is skipped because the right replacement depends on
 * the variable's type (structure, internal table, or scalar) which can
 * only be known at compile time.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * CLEAR ls_record.
 * READ TABLE lt_data INTO ls_record WITH KEY id = lv_id.
 * </pre>
 *
 * <h3>After (manual):</h3>
 * <pre>
 * READ TABLE lt_data INTO ls_record WITH KEY id = lv_id.   " no need to clear first
 * </pre>
 */
public class PreferValueToClearRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Prefer VALUE to CLEAR",
            "#prefer-value-to-clear"),
    };

    @Override public RuleID getID()             { return RuleID.PREFER_VALUE_TO_CLEAR; }
    @Override public String getName()           { return "Prefer VALUE to CLEAR"; }
    @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports CLEAR statements; recommends inline VALUE #( ) "
             + "construction at the assignment site instead.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        Token kw = stmt.getFirstKeyword();
        if (kw == null || !kw.textEqualsIgnoreCase("CLEAR")) return;

        result.addFinding(stmt.getStartLine(), RuleSeverity.INFO,
            "CLEAR detected — consider initialising with VALUE #( ) at the assignment site");
    }
}
