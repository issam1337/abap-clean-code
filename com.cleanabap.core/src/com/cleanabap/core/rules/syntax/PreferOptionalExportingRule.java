package com.cleanabap.core.rules.syntax;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.List;

/**
 * <b>Rule:</b> Omit the optional keyword {@code EXPORTING} (analysis-only).
 *
 * <p>Reports method calls that pass parameters via the optional {@code
 * EXPORTING} keyword when no other keywords ({@code IMPORTING}, {@code
 * CHANGING}, {@code RECEIVING}, {@code EXCEPTIONS}) are also used. In those
 * cases the keyword can be dropped for a more concise call.</p>
 *
 * <p>Auto-rewrite is left manual: detecting that {@code EXPORTING} is
 * the only keyword in a call requires balanced-paren parsing that is
 * out of scope for the lightweight statement model used here.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * lo_handler-&gt;process( EXPORTING iv_id = lv_id iv_amount = lv_amount ).
 * </pre>
 *
 * <h3>After (manual):</h3>
 * <pre>
 * lo_handler-&gt;process( iv_id = lv_id iv_amount = lv_amount ).
 * </pre>
 */
public class PreferOptionalExportingRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Omit the optional keyword EXPORTING",
            "#omit-the-optional-keyword-exporting"),
    };

    @Override public RuleID getID()             { return RuleID.PREFER_OPTIONAL_EXPORTING; }
    @Override public String getName()           { return "Omit the optional keyword EXPORTING"; }
    @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports method calls using EXPORTING when no other keyword "
             + "(IMPORTING / CHANGING / RECEIVING / EXCEPTIONS) is present.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        // Cheap heuristic: the statement contains EXPORTING but none of the
        // other parameter keywords. Skip declaration statements.
        if (stmt.startsWithAnyKeyword("METHODS", "CLASS-METHODS", "INTERFACE", "FORM")) return;

        boolean hasExporting = false;
        boolean hasOther = false;
        List<Token> tokens = stmt.getTokens();
        for (Token t : tokens) {
            if (!t.isKeyword()) continue;
            String u = t.getText().toUpperCase();
            switch (u) {
                case "EXPORTING": hasExporting = true; break;
                case "IMPORTING":
                case "CHANGING":
                case "RECEIVING":
                case "EXCEPTIONS":
                    hasOther = true; break;
                default: break;
            }
        }

        if (hasExporting && !hasOther) {
            result.addFinding(stmt.getStartLine(), RuleSeverity.INFO,
                "EXPORTING is optional when it's the only parameter keyword — "
                + "consider removing it");
        }
    }
}
