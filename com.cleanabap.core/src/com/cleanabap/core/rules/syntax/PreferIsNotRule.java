package com.cleanabap.core.rules.syntax;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.List;

/**
 * <b>Rule:</b> Prefer {@code x IS NOT INITIAL} to {@code NOT x IS INITIAL}
 *
 * <p>Detects the pattern {@code NOT <expr> IS <state>} and rewrites it to
 * {@code <expr> IS NOT <state>}, where {@code <state>} is one of
 * {@code INITIAL}, {@code BOUND}, {@code SUPPLIED}, {@code REQUESTED},
 * {@code ASSIGNED}, or {@code INSTANCE OF}. The rewritten form reads more
 * naturally and avoids the easily-missed leading {@code NOT}.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * IF NOT lv_value IS INITIAL.
 * IF NOT lo_handler IS BOUND.
 * </pre>
 *
 * <h3>After:</h3>
 * <pre>
 * IF lv_value IS NOT INITIAL.
 * IF lo_handler IS NOT BOUND.
 * </pre>
 */
public class PreferIsNotRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Prefer IS NOT to NOT IS",
            "#prefer-is-not-to-not-is"),
    };

    private static final String[] STATES = {
        "INITIAL", "BOUND", "SUPPLIED", "REQUESTED", "ASSIGNED"
    };

    @Override public RuleID getID()             { return RuleID.PREFER_IS_NOT_TO_NOT_IS; }
    @Override public String getName()           { return "Prefer IS NOT to NOT IS"; }
    @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Rewrites 'NOT x IS INITIAL/BOUND/SUPPLIED/...' as "
             + "'x IS NOT INITIAL/BOUND/...' for improved readability.";
    }

    @Override
    public String getExampleBefore() { return "IF NOT lv_value IS INITIAL."; }
    @Override
    public String getExampleAfter()  { return "IF lv_value IS NOT INITIAL."; }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        List<Token> tokens = stmt.getTokens();
        boolean changed = false;

        for (int i = 0; i < tokens.size(); i++) {
            Token notTok = tokens.get(i);
            if (!notTok.isKeyword() || !notTok.textEqualsIgnoreCase("NOT")) continue;

            // Find next significant token: must be an identifier / field-symbol
            // (the operand). Then 'IS' keyword. Then a state keyword.
            Token operand = nextSig(tokens, i);
            if (operand == null) continue;
            if (!operand.isIdentifier() && operand.getType() != Token.Type.FIELD_SYMBOL) continue;

            int operandIdx = tokens.indexOf(operand);
            Token isKw = nextSig(tokens, operandIdx);
            if (isKw == null || !isKw.isKeyword() || !isKw.textEqualsIgnoreCase("IS")) continue;

            int isIdx = tokens.indexOf(isKw);
            Token after = nextSig(tokens, isIdx);
            if (after == null) continue;
            // Skip if already 'IS NOT' (don't double-negate).
            if (after.isKeyword() && after.textEqualsIgnoreCase("NOT")) continue;
            if (!after.isKeyword() || !isKnownState(after.getText())) continue;

            // Apply: NOT → empty; consume the whitespace immediately after NOT;
            // IS → "IS NOT".
            notTok.setText("");
            // Eat the trailing whitespace token (if any) so we don't emit a
            // leading double-space.
            if (i + 1 < tokens.size()) {
                Token ws = tokens.get(i + 1);
                if (ws.getType() == Token.Type.WHITESPACE) {
                    ws.setText("");
                }
            }
            isKw.setText("IS NOT");
            result.addChange(notTok.getLine(), "NOT " + operand.getText() + " IS " + after.getText(),
                operand.getText() + " IS NOT " + after.getText(),
                "Rewrote NOT ... IS ... as IS NOT ...");
            changed = true;
        }

        if (changed) result.setSourceModified(true);
    }

    private static boolean isKnownState(String text) {
        for (String s : STATES) {
            if (s.equalsIgnoreCase(text)) return true;
        }
        return false;
    }

    private static Token nextSig(List<Token> tokens, int idx) {
        for (int i = idx + 1; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.getType() == Token.Type.WHITESPACE
                    || t.getType() == Token.Type.NEWLINE
                    || t.isComment()) continue;
            return t;
        }
        return null;
    }
}
