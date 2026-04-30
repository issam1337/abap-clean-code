package com.cleanabap.core.rules.syntax;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Rule:</b> Replace TRANSLATE ... TO UPPER/LOWER CASE with to_upper / to_lower
 *
 * <h3>Before:</h3>
 * <pre>
 * TRANSLATE lv_name TO UPPER CASE.
 * TRANSLATE lv_id   TO LOWER CASE.
 * </pre>
 *
 * <h3>After:</h3>
 * <pre>
 * lv_name = to_upper( lv_name ).
 * lv_id   = to_lower( lv_id ).
 * </pre>
 *
 * <p>The {@code TRANSLATE ... USING} substitution form is left untouched —
 * it has no functional equivalent.</p>
 */
public class ReplaceTranslateRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Prefer functional to procedural language constructs",
            "#prefer-functional-to-procedural-language-constructs"),
    };

    @Override public RuleID getID()             { return RuleID.REPLACE_TRANSLATE; }
    @Override public String getName()           { return "Replace TRANSLATE TO UPPER/LOWER CASE"; }
    @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Replaces TRANSLATE x TO UPPER CASE with x = to_upper( x ), "
             + "and the LOWER CASE form with to_lower(...).";
    }

    @Override
    public String getExampleBefore() { return "TRANSLATE lv_name TO UPPER CASE."; }
    @Override
    public String getExampleAfter()  { return "lv_name = to_upper( lv_name )."; }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        Token kw = stmt.getFirstKeyword();
        if (kw == null || !kw.textEqualsIgnoreCase("TRANSLATE")) return;

        List<Token> sig = significant(stmt);
        // Expect 6 significant tokens: TRANSLATE x TO UPPER CASE .  or  ... LOWER CASE .
        if (sig.size() != 6) return;
        if (!sig.get(0).textEqualsIgnoreCase("TRANSLATE")) return;
        Token target = sig.get(1);
        if (!target.isIdentifier()) return;
        if (!sig.get(2).textEqualsIgnoreCase("TO")) return;

        String caseDir = sig.get(3).getText().toUpperCase();
        String func;
        if ("UPPER".equals(caseDir))      func = "to_upper";
        else if ("LOWER".equals(caseDir)) func = "to_lower";
        else                              return;

        if (!sig.get(4).textEqualsIgnoreCase("CASE")) return;
        if (!sig.get(5).isPeriod())                   return;

        String original = stmt.toNormalizedText();
        String replacement = target.getText() + " = " + func + "( " + target.getText() + " ).";

        String source = doc.getCurrentSource();
        String stmtText = stmt.toSourceCode().trim();
        if (source.contains(stmtText)) {
            String indent = extractIndent(stmt);
            doc.updateSource(source.replace(stmtText, indent + replacement));
            result.addChange(stmt.getStartLine(), original, replacement,
                "Replaced TRANSLATE TO " + caseDir + " CASE with " + func + "(...)");
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
