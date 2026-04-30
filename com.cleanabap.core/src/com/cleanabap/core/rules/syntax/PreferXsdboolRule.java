package com.cleanabap.core.rules.syntax;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.List;

/**
 * <b>Rule:</b> Prefer {@code xsdbool(...)} to {@code boolc(...)}.
 *
 * <p>{@code boolc} returns a single-character {@code abap_bool} but with
 * problematic implicit conversion behaviour. {@code xsdbool} returns the
 * same value with cleaner semantics and is recommended by the SAP guide.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * lv_flag = boolc( lv_count > 0 ).
 * </pre>
 *
 * <h3>After:</h3>
 * <pre>
 * lv_flag = xsdbool( lv_count > 0 ).
 * </pre>
 */
public class PreferXsdboolRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Avoid obsolete language elements (boolc)",
            "#avoid-obsolete-language-elements"),
    };

    @Override public RuleID getID()             { return RuleID.PREFER_XSDBOOL; }
    @Override public String getName()           { return "Prefer xsdbool( ) to boolc( )"; }
    @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Replaces calls to the legacy boolc() built-in with xsdbool(), "
             + "which has cleaner implicit-conversion semantics.";
    }

    @Override
    public String getExampleBefore() { return "lv_flag = boolc( lv_count > 0 )."; }
    @Override
    public String getExampleAfter()  { return "lv_flag = xsdbool( lv_count > 0 )."; }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        List<Token> tokens = stmt.getTokens();
        boolean changed = false;

        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (!t.isIdentifier()) continue;
            if (!t.textEqualsIgnoreCase("boolc")) continue;

            // Verify it's a function call: next significant token is '('.
            Token after = nextSig(tokens, i);
            if (after == null) continue;
            if (!"(".equals(after.getText())) continue;

            String original = t.getText();
            t.setText("xsdbool");
            result.addChange(t.getLine(), original + "( ... )", "xsdbool( ... )",
                "Replaced boolc() with xsdbool()");
            changed = true;
        }

        if (changed) result.setSourceModified(true);
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
