package com.cleanabap.core.rules.comments;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Convert * comments to " comments
 *
 * <p>Full-line comments using * at column 1 are replaced with
 * " inline comment syntax, as recommended by Clean ABAP.</p>
 *
 * <h3>Before:</h3>
 * <pre>* This is a comment</pre>
 *
 * <h3>After:</h3>
 * <pre>" This is a comment</pre>
 */
public class ConvertStarCommentsRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Prefer \" to *", "#prefer--to-"),
    };

    @Override public RuleID getID()             { return RuleID.CONVERT_STAR_COMMENTS; }
    @Override public String getName()           { return "Convert * comments to \" comments"; }
    @Override public RuleCategory getCategory() { return RuleCategory.COMMENTS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public boolean isEssential()       { return false; }
    @Override public boolean shouldProcessComments() { return true; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Replaces full-line * comments with \" comments for consistency.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        boolean changed = false;
        for (Token token : stmt.getTokens()) {
            if (token.getType() == Token.Type.COMMENT_FULL) {
                String text = token.getText();
                if (text.startsWith("*")) {
                    String newText = "\"" + text.substring(1);
                    result.addChange(token.getLine(), text, newText,
                        "Converted * comment to \" comment");
                    token.setText(newText);
                    token.setType(Token.Type.COMMENT_LINE);
                    changed = true;
                }
            }
        }
        // Mark the result modified; the base class (RuleForStatements) will
        // rebuild the document source once at the end of apply() from a stable
        // statement-list snapshot. Calling doc.updateSource here would re-parse
        // the statement list mid-iteration and silently drop later edits.
        if (changed) {
            result.setSourceModified(true);
        }
    }
}
