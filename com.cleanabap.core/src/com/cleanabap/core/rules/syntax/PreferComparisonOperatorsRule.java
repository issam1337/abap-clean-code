package com.cleanabap.core.rules.syntax;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>Rule:</b> Prefer =, <>, <=, >= to EQ, NE, LE, GE
 *
 * <p>Replaces legacy text comparison operators with their modern
 * symbolic equivalents for better readability.</p>
 *
 * <h3>Before:</h3>
 * <pre>IF lv_count GT 0 AND lv_name NE space.</pre>
 *
 * <h3>After:</h3>
 * <pre>IF lv_count > 0 AND lv_name <> space.</pre>
 */
public class PreferComparisonOperatorsRule extends RuleForStatements {

    private static final Map<String, String> OPERATOR_MAP = new HashMap<>();
    static {
        OPERATOR_MAP.put("EQ", "=");
        OPERATOR_MAP.put("NE", "<>");
        OPERATOR_MAP.put("LT", "<");
        OPERATOR_MAP.put("GT", ">");
        OPERATOR_MAP.put("LE", "<=");
        OPERATOR_MAP.put("GE", ">=");
    }

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Prefer modern comparison operators",
            "#prefer-functional-to-procedural-language-constructs"),
    };

    @Override public RuleID getID()             { return RuleID.PREFER_COMPARISON_OPERATORS; }
    @Override public String getName()           { return "Prefer =, <>, <= etc. to EQ, NE, LE etc."; }
    @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public boolean isEssential()       { return true; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Replaces legacy text comparison operators (EQ, NE, LT, GT, LE, GE) "
             + "with modern symbolic operators (=, <>, <, >, <=, >=).";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        boolean changed = false;

        for (Token token : stmt.getTokens()) {
            if (token.isKeyword()) {
                String upper = token.getText().toUpperCase();
                String replacement = OPERATOR_MAP.get(upper);
                if (replacement != null) {
                    // Verify it's used as operator context (preceded/followed by identifiers or literals)
                    Token prev = findPrevSignificant(token);
                    Token next = findNextSignificant(token);
                    if (prev != null && next != null &&
                        isOperand(prev) && isOperand(next)) {
                        result.addChange(token.getLine(),
                            token.getText(), replacement,
                            "Replaced " + upper + " with " + replacement);
                        token.setText(replacement);
                        token.setType(Token.Type.OPERATOR);
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            doc.updateSource(rebuildSource(doc));
            result.setSourceModified(true);
        }
    }

    private Token findPrevSignificant(Token token) {
        Token t = token.getPrev();
        while (t != null && (t.getType() == Token.Type.WHITESPACE ||
               t.getType() == Token.Type.NEWLINE)) {
            t = t.getPrev();
        }
        return t;
    }

    private Token findNextSignificant(Token token) {
        Token t = token.getNext();
        while (t != null && (t.getType() == Token.Type.WHITESPACE ||
               t.getType() == Token.Type.NEWLINE)) {
            t = t.getNext();
        }
        return t;
    }

    private boolean isOperand(Token t) {
        return t.isIdentifier() || t.isStringLiteral() ||
               t.getType() == Token.Type.LITERAL_NUMBER ||
               t.getType() == Token.Type.FIELD_SYMBOL ||
               t.getType() == Token.Type.PUNCTUATION; // closing paren
    }

    private String rebuildSource(CodeDocument doc) {
        StringBuilder sb = new StringBuilder();
        for (AbapStatement stmt : doc.getStatements()) {
            sb.append(stmt.toSourceCode());
        }
        return sb.toString();
    }
}
