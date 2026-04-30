package com.cleanabap.core.rules.syntax;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Replace {@code CALL METHOD obj->m( ... )} with the direct
 * functional call {@code obj->m( ... )} (analysis-only).
 *
 * <p>Auto-rewrite is skipped because the variants of {@code CALL METHOD}
 * (with {@code EXPORTING} / {@code IMPORTING} / {@code EXCEPTIONS}, no
 * parentheses, dynamic method names, etc.) require careful handling that
 * a string-replace cannot guarantee. The rule emits a finding so the
 * developer can choose the right modern equivalent.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * CALL METHOD lo_handler-&gt;process( EXPORTING iv_id = lv_id ).
 * </pre>
 *
 * <h3>After (manual):</h3>
 * <pre>
 * lo_handler-&gt;process( EXPORTING iv_id = lv_id ).
 * </pre>
 */
public class ReplaceCallMethodRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Omit the optional keyword EXPORTING / Avoid CALL METHOD",
            "#omit-the-optional-keyword-exporting"),
    };

    @Override public RuleID getID()             { return RuleID.REPLACE_CALL_METHOD; }
    @Override public String getName()           { return "Replace CALL METHOD with direct call"; }
    @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports CALL METHOD statements that should be rewritten as the modern "
             + "functional call obj->m( ... ).";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        Token kw = stmt.getFirstKeyword();
        if (kw == null || !kw.textEqualsIgnoreCase("CALL")) return;

        // Look for the second keyword token, must be METHOD.
        boolean sawCall = false;
        for (Token t : stmt.getTokens()) {
            if (!t.isKeyword()) continue;
            if (!sawCall) { sawCall = true; continue; }
            if (t.textEqualsIgnoreCase("METHOD")) {
                result.addFinding(stmt.getStartLine(), RuleSeverity.WARNING,
                    "CALL METHOD detected — rewrite as direct functional call obj->m( ... )");
            }
            return;
        }
    }
}
