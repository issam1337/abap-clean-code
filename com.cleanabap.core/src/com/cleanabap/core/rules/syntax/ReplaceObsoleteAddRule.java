package com.cleanabap.core.rules.syntax;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.List;

/**
 * <b>Rule:</b> Replace obsolete ADD/SUBTRACT/MULTIPLY/DIVIDE
 *
 * <p>Replaces procedural arithmetic statements with modern
 * compound assignment operators.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * ADD lv_amount TO lv_total.
 * SUBTRACT 1 FROM lv_count.
 * MULTIPLY lv_price BY lv_factor.
 * DIVIDE lv_total BY lv_count.
 * </pre>
 *
 * <h3>After:</h3>
 * <pre>
 * lv_total += lv_amount.
 * lv_count -= 1.
 * lv_price *= lv_factor.
 * lv_total /= lv_count.
 * </pre>
 */
public class ReplaceObsoleteAddRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Prefer functional to procedural language constructs",
            "#prefer-functional-to-procedural-language-constructs"),
    };

    @Override public RuleID getID()             { return RuleID.REPLACE_OBSOLETE_ADD; }
    @Override public String getName()           { return "Replace obsolete ADD/SUBTRACT/MULTIPLY/DIVIDE"; }
    @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public boolean isEssential()       { return true; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Replaces ADD x TO y with y += x, SUBTRACT x FROM y with y -= x, "
             + "MULTIPLY y BY x with y *= x, and DIVIDE y BY x with y /= x.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        Token firstKw = stmt.getFirstKeyword();
        if (firstKw == null) return;

        String keyword = firstKw.getText().toUpperCase();
        List<Token> significant = getSignificantTokens(stmt);

        switch (keyword) {
            case "ADD":
                // ADD <operand1> TO <operand2>.
                processAddSubtract(significant, "TO", "+=", stmt, doc, result);
                break;
            case "SUBTRACT":
                // SUBTRACT <operand1> FROM <operand2>.
                processAddSubtract(significant, "FROM", "-=", stmt, doc, result);
                break;
            case "MULTIPLY":
                // MULTIPLY <operand1> BY <operand2>.
                processMultiplyDivide(significant, "BY", "*=", stmt, doc, result);
                break;
            case "DIVIDE":
                // DIVIDE <operand1> BY <operand2>.
                processMultiplyDivide(significant, "BY", "/=", stmt, doc, result);
                break;
        }
    }

    /**
     * ADD x TO y → y += x
     * SUBTRACT x FROM y → y -= x
     */
    private void processAddSubtract(List<Token> tokens, String preposition,
                                     String operator, AbapStatement stmt,
                                     CodeDocument doc, CleanupResult result) {
        // Expected: KEYWORD operand1 preposition operand2 PERIOD
        if (tokens.size() < 4) return;

        Token kw = tokens.get(0);
        Token operand1 = tokens.get(1);
        Token prep = tokens.get(2);
        Token operand2 = tokens.get(3);

        if (!prep.textEqualsIgnoreCase(preposition)) return;
        if (!isSimpleOperand(operand1) || !isSimpleOperand(operand2)) return;

        String original = stmt.toNormalizedText();
        String replacement = operand2.getText() + " " + operator + " " + operand1.getText() + ".";

        applyReplacement(stmt, doc, result, original, replacement);
    }

    /**
     * MULTIPLY x BY y → x *= y
     * DIVIDE x BY y → x /= y
     */
    private void processMultiplyDivide(List<Token> tokens, String preposition,
                                        String operator, AbapStatement stmt,
                                        CodeDocument doc, CleanupResult result) {
        if (tokens.size() < 4) return;

        Token kw = tokens.get(0);
        Token operand1 = tokens.get(1);
        Token prep = tokens.get(2);
        Token operand2 = tokens.get(3);

        if (!prep.textEqualsIgnoreCase(preposition)) return;
        if (!isSimpleOperand(operand1) || !isSimpleOperand(operand2)) return;

        String original = stmt.toNormalizedText();
        String replacement = operand1.getText() + " " + operator + " " + operand2.getText() + ".";

        applyReplacement(stmt, doc, result, original, replacement);
    }

    private boolean isSimpleOperand(Token t) {
        return t.isIdentifier() || t.getType() == Token.Type.LITERAL_NUMBER ||
               t.getType() == Token.Type.FIELD_SYMBOL;
    }

    private List<Token> getSignificantTokens(AbapStatement stmt) {
        return stmt.getTokens().stream()
            .filter(t -> t.getType() != Token.Type.WHITESPACE &&
                         t.getType() != Token.Type.NEWLINE &&
                         t.getType() != Token.Type.PERIOD)
            .collect(java.util.stream.Collectors.toList());
    }

    private void applyReplacement(AbapStatement stmt, CodeDocument doc,
                                   CleanupResult result, String original,
                                   String replacement) {
        String source = doc.getCurrentSource();
        String stmtText = stmt.toSourceCode().trim();
        if (source.contains(stmtText)) {
            String indent = extractIndent(stmt);
            doc.updateSource(source.replace(stmtText, indent + replacement));
            result.addChange(stmt.getStartLine(), original, replacement,
                "Replaced obsolete arithmetic with compound operator");
        }
    }

    private String extractIndent(AbapStatement stmt) {
        Token first = stmt.getFirstToken();
        if (first != null && first.getType() == Token.Type.WHITESPACE) {
            return first.getText();
        }
        return "";
    }
}
