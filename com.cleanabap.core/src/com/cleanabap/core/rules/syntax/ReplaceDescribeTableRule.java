package com.cleanabap.core.rules.syntax;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Rule:</b> Replace DESCRIBE TABLE itab LINES n with n = lines( itab )
 *
 * <h3>Before:</h3>
 * <pre>
 * DESCRIBE TABLE lt_items LINES lv_count.
 * </pre>
 *
 * <h3>After:</h3>
 * <pre>
 * lv_count = lines( lt_items ).
 * </pre>
 *
 * <p>Variants with {@code KIND} or {@code OCCURS} clauses are skipped —
 * they have no direct functional equivalent.</p>
 */
public class ReplaceDescribeTableRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Prefer functional to procedural language constructs",
            "#prefer-functional-to-procedural-language-constructs"),
    };

    @Override public RuleID getID()             { return RuleID.REPLACE_DESCRIBE_TABLE; }
    @Override public String getName()           { return "Replace DESCRIBE TABLE LINES with lines( )"; }
    @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Replaces 'DESCRIBE TABLE itab LINES n.' with 'n = lines( itab ).' "
             + "Variants using KIND/OCCURS are not transformed.";
    }

    @Override
    public String getExampleBefore() { return "DESCRIBE TABLE lt_items LINES lv_count."; }
    @Override
    public String getExampleAfter()  { return "lv_count = lines( lt_items )."; }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        Token kw = stmt.getFirstKeyword();
        if (kw == null || !kw.textEqualsIgnoreCase("DESCRIBE")) return;

        List<Token> sig = significant(stmt);
        // Expect: DESCRIBE TABLE <itab> LINES <n> .
        if (sig.size() != 6) return;
        if (!sig.get(0).textEqualsIgnoreCase("DESCRIBE")) return;
        if (!sig.get(1).textEqualsIgnoreCase("TABLE"))    return;
        Token itab = sig.get(2);
        if (!itab.isIdentifier()) return;
        if (!sig.get(3).textEqualsIgnoreCase("LINES"))   return;
        Token target = sig.get(4);
        if (!target.isIdentifier()) return;
        if (!sig.get(5).isPeriod())                       return;

        String original = stmt.toNormalizedText();
        String replacement = target.getText() + " = lines( " + itab.getText() + " ).";

        String source = doc.getCurrentSource();
        String stmtText = stmt.toSourceCode().trim();
        if (source.contains(stmtText)) {
            String indent = extractIndent(stmt);
            doc.updateSource(source.replace(stmtText, indent + replacement));
            result.addChange(stmt.getStartLine(), original, replacement,
                "Replaced DESCRIBE TABLE LINES with lines( )");
        }
    }

    private List<Token> significant(AbapStatement stmt) {
        List<Token> out = new ArrayList<>();
        for (Token t : stmt.getTokens()) {
            if (t.getType() == Token.Type.WHITESPACE
                    || t.getType() == Token.Type.NEWLINE
                    || t.isComment()) continue;
            out.add(t);
        }
        return out;
    }

    private String extractIndent(AbapStatement stmt) {
        Token first = stmt.getFirstToken();
        if (first != null && first.getType() == Token.Type.WHITESPACE) {
            return first.getText();
        }
        return "";
    }
}
