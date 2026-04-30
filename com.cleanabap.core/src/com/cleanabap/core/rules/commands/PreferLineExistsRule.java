package com.cleanabap.core.rules.commands;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.List;

/**
 * <b>Rule:</b> Prefer {@code line_exists( )} to {@code READ TABLE …
 * TRANSPORTING NO FIELDS} for existence checks (analysis-only).
 *
 * <p>The modern, declarative way to test whether a row exists in an
 * internal table is {@code line_exists( itab[ ... ] )}, which avoids
 * the dummy-read pattern. Auto-rewrite is skipped because building the
 * correct {@code itab[ k = v ]} expression from a {@code WITH KEY}
 * clause requires careful translation of free-form key components.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * READ TABLE lt_data WITH KEY id = lv_id TRANSPORTING NO FIELDS.
 * IF sy-subrc = 0. ... ENDIF.
 * </pre>
 *
 * <h3>After (manual):</h3>
 * <pre>
 * IF line_exists( lt_data[ id = lv_id ] ). ... ENDIF.
 * </pre>
 */
public class PreferLineExistsRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Use line_exists for existence checks", "#use-line_exists-for-the-fastest-existence-check"),
    };

    @Override public RuleID getID()             { return RuleID.PREFER_LINE_EXISTS; }
    @Override public String getName()           { return "Prefer line_exists( ) to READ TABLE … TRANSPORTING NO FIELDS"; }
    @Override public RuleCategory getCategory() { return RuleCategory.COMMANDS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports READ TABLE … TRANSPORTING NO FIELDS used as an existence "
             + "check, recommending the line_exists( itab[ … ] ) function.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.startsWithKeyword("READ")) return;
        // Look for TABLE then TRANSPORTING NO FIELDS in the keyword stream.
        List<Token> tokens = stmt.getTokens();
        boolean sawTable = false;
        boolean sawTransporting = false;
        boolean sawNo = false;
        for (Token t : tokens) {
            if (!t.isKeyword()) continue;
            String u = t.getText().toUpperCase();
            switch (u) {
                case "TABLE":        sawTable = true; break;
                case "TRANSPORTING": sawTransporting = true; break;
                case "NO":           if (sawTransporting) sawNo = true; break;
                case "FIELDS":
                    if (sawTable && sawTransporting && sawNo) {
                        result.addFinding(stmt.getStartLine(), RuleSeverity.INFO,
                            "READ TABLE … TRANSPORTING NO FIELDS — prefer line_exists( itab[ … ] )");
                        return;
                    }
                    break;
                default: break;
            }
        }
    }
}
