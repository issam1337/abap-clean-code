package com.cleanabap.core.rules.commands;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Prefer {@code CASE} to long {@code IF / ELSEIF} chains
 * comparing the same variable (analysis-only).
 *
 * <p>Reports {@code IF} blocks followed by 2+ {@code ELSEIF} branches.
 * In most cases an equivalent {@code CASE / WHEN} reads better. The rule
 * is intentionally heuristic — it doesn't analyse the predicates, just
 * the count.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * IF lv_kind = 'A'.       …
 * ELSEIF lv_kind = 'B'.   …
 * ELSEIF lv_kind = 'C'.   …
 * ENDIF.
 * </pre>
 *
 * <h3>After (manual):</h3>
 * <pre>
 * CASE lv_kind.
 *   WHEN 'A'. …
 *   WHEN 'B'. …
 *   WHEN 'C'. …
 * ENDCASE.
 * </pre>
 */
public class PreferCaseToIfChainRule extends RuleForStatements {

    private static final int MIN_BRANCHES = 2;   // beyond IF: at least 2 ELSEIFs

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Prefer CASE to long IF/ELSEIF chains", "#prefer-case-to-else-if-for-multiple-alternative-conditions"),
    };

    @Override public RuleID getID()             { return RuleID.PREFER_CASE_TO_IF_CHAIN; }
    @Override public String getName()           { return "Prefer CASE to IF/ELSEIF chain"; }
    @Override public RuleCategory getCategory() { return RuleCategory.COMMANDS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports IF blocks chained with 2+ ELSEIF branches; suggests "
             + "rewriting as a CASE statement.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.startsWithKeyword("IF")) return;

        int elseifs = 0;
        int depth = 1;
        AbapStatement walker = stmt.getNext();
        while (walker != null && depth > 0) {
            Token kw = walker.getFirstKeyword();
            if (kw != null) {
                String u = kw.getText().toUpperCase();
                if ("IF".equals(u)) depth++;
                else if ("ENDIF".equals(u)) {
                    depth--;
                    if (depth == 0) break;
                }
                else if (depth == 1 && "ELSEIF".equals(u)) elseifs++;
            }
            walker = walker.getNext();
        }

        if (elseifs >= MIN_BRANCHES) {
            result.addFinding(stmt.getStartLine(), RuleSeverity.INFO,
                "IF chain with " + (elseifs + 1) + " branches — consider rewriting as CASE");
        }
    }
}
