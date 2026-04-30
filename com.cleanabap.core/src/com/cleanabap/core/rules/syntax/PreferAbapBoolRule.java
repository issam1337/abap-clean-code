package com.cleanabap.core.rules.syntax;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.List;

/**
 * <b>Rule:</b> Prefer {@code abap_true} / {@code abap_false} to literal
 * {@code 'X'} / {@code ' '} (analysis-only).
 *
 * <p>Detects assignments that look like {@code lv_flag = 'X'.} or
 * {@code lv_flag = ' '.} where the variable name follows the common
 * Boolean-flag convention ({@code lv_flag}, {@code lv_*_flag},
 * {@code is_*}, {@code has_*}, ending in {@code _bool}, etc.). For these,
 * the SAP guide recommends {@code abap_true} / {@code abap_false}.</p>
 *
 * <p>This is a heuristic — when in doubt the rule stays silent. Auto-rewrite
 * is unsafe because {@code 'X'} can also be a legitimate single-character
 * payload value.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * lv_is_active = 'X'.
 * </pre>
 *
 * <h3>After (manual):</h3>
 * <pre>
 * lv_is_active = abap_true.
 * </pre>
 */
public class PreferAbapBoolRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Use abap_bool to store truth values",
            "#use-abap_bool-for-truth-values"),
    };

    @Override public RuleID getID()             { return RuleID.PREFER_ABAP_BOOL; }
    @Override public String getName()           { return "Prefer abap_true / abap_false to 'X' / space"; }
    @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports assignments of literal 'X' or ' ' to variables whose "
             + "names suggest a boolean flag, recommending abap_true / abap_false.";
    }

    @Override
    public String getExampleBefore() { return "lv_is_active = 'X'."; }
    @Override
    public String getExampleAfter()  { return "lv_is_active = abap_true."; }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        List<Token> tokens = stmt.getTokens();
        // Find the first identifier and check the assignment shape:
        //   <ident> = '<single-char>' .
        Token ident = null;
        boolean sawEquals = false;
        Token literal = null;
        for (Token t : tokens) {
            if (t.getType() == Token.Type.WHITESPACE
                    || t.getType() == Token.Type.NEWLINE
                    || t.isComment()) continue;
            if (ident == null) {
                if (!t.isIdentifier()) return;
                ident = t;
                continue;
            }
            if (!sawEquals) {
                if (t.isOperator() && "=".equals(t.getText())) { sawEquals = true; continue; }
                return;
            }
            if (literal == null) {
                if (t.getType() != Token.Type.LITERAL_STRING) return;
                literal = t;
                continue;
            }
            // After the literal we expect period.
            if (!t.isPeriod()) return;
            break;
        }
        if (ident == null || literal == null) return;

        String litText = literal.getText();
        // Must be 'X' or '-' or ' ' (length 3 with quotes).
        if (litText.length() != 3) return;
        char inner = litText.charAt(1);
        if (inner != 'X' && inner != 'x' && inner != ' ' && inner != '-') return;

        if (!looksLikeBooleanFlag(ident.getText())) return;

        String suggestion = (inner == 'X' || inner == 'x') ? "abap_true" : "abap_false";
        result.addFinding(stmt.getStartLine(), RuleSeverity.INFO,
            "Variable '" + ident.getText() + "' looks like a boolean flag — "
            + "consider using " + suggestion + " instead of " + litText);
    }

    private static boolean looksLikeBooleanFlag(String name) {
        String lc = name.toLowerCase();
        return lc.contains("flag")
            || lc.contains("bool")
            || lc.endsWith("_b")
            || lc.startsWith("is_") || lc.contains("_is_")
            || lc.startsWith("has_") || lc.contains("_has_")
            || lc.startsWith("can_") || lc.contains("_can_");
    }
}
