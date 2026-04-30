package com.cleanabap.core.rules.syntax;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Rule:</b> Replace MOVE x TO y with y = x
 *
 * <h3>Before:</h3>
 * <pre>
 * MOVE lv_amount TO lv_total.
 * MOVE 'X' TO lv_flag.
 * </pre>
 *
 * <h3>After:</h3>
 * <pre>
 * lv_total = lv_amount.
 * lv_flag  = 'X'.
 * </pre>
 *
 * <p>Skipped: {@code MOVE-CORRESPONDING}, {@code MOVE EXACT},
 * {@code MOVE ... PERCENTAGE ...}, {@code MOVE ... ?TO ...} (downcasts).</p>
 */
public class ReplaceMoveToRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Avoid obsolete language elements",
            "#avoid-obsolete-language-elements"),
    };

    @Override public RuleID getID()             { return RuleID.REPLACE_MOVE_TO; }
    @Override public String getName()           { return "Replace MOVE x TO y with y = x"; }
    @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public boolean isEssential()       { return true; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Replaces 'MOVE x TO y.' with the modern direct assignment 'y = x.'";
    }

    @Override
    public String getExampleBefore() { return "MOVE lv_amount TO lv_total."; }
    @Override
    public String getExampleAfter()  { return "lv_total = lv_amount."; }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        Token kw = stmt.getFirstKeyword();
        if (kw == null || !kw.textEqualsIgnoreCase("MOVE")) return;

        // Collect significant tokens (skip whitespace/newlines/comments).
        List<Token> sig = new ArrayList<>();
        for (Token t : stmt.getTokens()) {
            if (t.getType() == Token.Type.WHITESPACE
                    || t.getType() == Token.Type.NEWLINE
                    || t.isComment()) continue;
            sig.add(t);
        }

        // Expect: MOVE <source> TO <target> .
        if (sig.size() < 5) return;
        if (!sig.get(0).textEqualsIgnoreCase("MOVE")) return;
        if (!sig.get(sig.size() - 1).isPeriod()) return;

        // Find the TO keyword. Reject if EXACT/PERCENTAGE/?TO/UP-TO appears.
        int toIdx = -1;
        for (int i = 1; i < sig.size() - 1; i++) {
            Token t = sig.get(i);
            if (t.textEqualsIgnoreCase("EXACT")
                    || t.textEqualsIgnoreCase("PERCENTAGE")
                    || t.textEqualsIgnoreCase("?TO")) return;
            if (t.isKeyword() && t.textEqualsIgnoreCase("TO")) {
                toIdx = i;
                break;
            }
        }
        if (toIdx < 1 || toIdx >= sig.size() - 2) return;

        // <source> = sig[1..toIdx-1]; <target> = sig[toIdx+1..n-2]
        // For safety only support a SINGLE-token target (identifier or field-symbol).
        if (toIdx + 2 != sig.size() - 1) return;
        Token target = sig.get(toIdx + 1);
        if (!target.isIdentifier() && target.getType() != Token.Type.FIELD_SYMBOL) return;

        // Build the source side as the original tokens' text concatenated.
        StringBuilder srcText = new StringBuilder();
        for (int i = 1; i < toIdx; i++) {
            if (srcText.length() > 0) srcText.append(' ');
            srcText.append(sig.get(i).getText());
        }
        if (srcText.length() == 0) return;

        String original = stmt.toNormalizedText();
        String replacement = target.getText() + " = " + srcText + ".";

        String source = doc.getCurrentSource();
        String stmtText = stmt.toSourceCode().trim();
        if (source.contains(stmtText)) {
            String indent = extractIndent(stmt);
            doc.updateSource(source.replace(stmtText, indent + replacement));
            result.addChange(stmt.getStartLine(), original, replacement,
                "Replaced MOVE x TO y with y = x");
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
