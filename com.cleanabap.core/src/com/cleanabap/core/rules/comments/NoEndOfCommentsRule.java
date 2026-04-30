package com.cleanabap.core.rules.comments;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> No "end-of" comments on block closers (analysis-only).
 *
 * <p>Reports comments tacked onto {@code ENDIF}, {@code ENDLOOP},
 * {@code ENDMETHOD}, {@code ENDCLASS}, {@code ENDFORM}, etc. that just
 * label the closer with the construct it ends — these are noise and
 * should be removed; modern editors handle bracket matching.</p>
 *
 * <h3>Detected examples:</h3>
 * <pre>
 * ENDIF.    " end of if
 * ENDLOOP. " end loop over data
 * ENDMETHOD. "process_record
 * </pre>
 */
public class NoEndOfCommentsRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Don't add method signatures or end-of comments", "#dont-add-method-signatures-or-end-of-comments"),
    };

    @Override public RuleID getID()             { return RuleID.NO_END_OF_COMMENTS; }
    @Override public String getName()           { return "Remove end-of comments on block closers"; }
    @Override public RuleCategory getCategory() { return RuleCategory.COMMENTS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports trailing comments on block closers (ENDIF, ENDLOOP, "
             + "ENDMETHOD, …) that merely label the construct.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.isBlockCloser()) return;

        for (Token t : stmt.getTokens()) {
            if (!t.isComment()) continue;
            // We only flag inline comments (COMMENT_LINE), since full-line
            // comments aren't part of the closer statement in practice.
            if (t.getType() != Token.Type.COMMENT_LINE) continue;
            result.addFinding(t.getLine(), RuleSeverity.INFO,
                "End-of comment on block closer — remove (modern editors handle matching)");
            return;
        }
    }
}
