package com.cleanabap.core.rules.comments;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.Locale;

/**
 * <b>Rule:</b> Prefer pragmas to {@code "#EC} pseudo-comments
 * (analysis-only).
 *
 * <p>Pragmas like {@code ##NEEDED}, {@code ##NO_TEXT} are the modern,
 * compiler-aware replacement for the older {@code "#EC NEEDED} pseudo-
 * comment family. Auto-rewrite is left manual so the developer can
 * pick the right pragma name and placement.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * lv_unused = 1.   "#EC NEEDED
 * </pre>
 *
 * <h3>After (manual):</h3>
 * <pre>
 * lv_unused = 1 ##NEEDED.
 * </pre>
 */
public class PreferPragmasRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Prefer pragmas to pseudo comments", "#prefer-pragmas-to-pseudo-comments"),
    };

    @Override public RuleID getID()             { return RuleID.PREFER_PRAGMAS; }
    @Override public String getName()           { return "Prefer pragmas to \"#EC pseudo-comments"; }
    @Override public RuleCategory getCategory() { return RuleCategory.COMMENTS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    protected boolean shouldProcessComments() { return true; }

    @Override
    public String getDescription() {
        return "Reports \"#EC pseudo-comments; suggests the equivalent "
             + "##PRAGMA form.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        for (Token t : stmt.getTokens()) {
            if (t.getType() != Token.Type.PSEUDO_COMMENT
                    && t.getType() != Token.Type.COMMENT_LINE
                    && t.getType() != Token.Type.COMMENT_FULL) continue;
            String upper = t.getText().toUpperCase(Locale.ROOT);
            if (upper.contains("\"#EC") || upper.contains("#EC ")) {
                result.addFinding(t.getLine(), RuleSeverity.INFO,
                    "\"#EC pseudo-comment — prefer the equivalent ##PRAGMA");
                return;
            }
        }
    }
}
