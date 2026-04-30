package com.cleanabap.core.rules.comments;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.regex.Pattern;

/**
 * <b>Rule:</b> Detect commented-out code (analysis-only).
 *
 * <p>Reports comment lines whose body looks like ABAP source —
 * keywords ending with a period, assignment statements, etc. The
 * Clean ABAP guide is firm: commented-out code should be deleted,
 * because version control tracks history.</p>
 *
 * <p>Auto-removal is intentionally not done — the heuristic can have
 * false positives (e.g. a sentence ending in {@code data.}), so the
 * decision to delete is left to the developer.</p>
 *
 * <h3>Detected examples:</h3>
 * <pre>
 * " DATA lv_x TYPE i.
 * * lv_x = 5.
 * "  IF lv_x > 0.
 * </pre>
 */
public class RemoveCommentedCodeRule extends RuleForStatements {

    /** Heuristic regex: starts with a typical ABAP keyword and ends in a period. */
    private static final Pattern CODE_LIKE = Pattern.compile(
        "^\\s*(?:DATA|TYPES|CONSTANTS|FIELD-SYMBOLS|IF|ELSEIF|ELSE|ENDIF|"
      + "LOOP|ENDLOOP|DO|ENDDO|WHILE|ENDWHILE|TRY|CATCH|ENDTRY|"
      + "CASE|WHEN|ENDCASE|METHOD|ENDMETHOD|CLASS|ENDCLASS|INTERFACE|ENDINTERFACE|"
      + "FORM|ENDFORM|PERFORM|FUNCTION|ENDFUNCTION|MODULE|ENDMODULE|"
      + "READ|MODIFY|INSERT|DELETE|APPEND|UPDATE|SELECT|FETCH|"
      + "CALL|MOVE|CLEAR|CHECK|EXIT|RETURN|RAISE|ASSIGN|UNASSIGN|"
      + "LV_|LS_|LT_|LO_|LR_|GV_|GS_|GT_|GO_|GR_|MV_|MS_|MT_|MO_|MR_)\\b.*\\.\\s*$",
        Pattern.CASE_INSENSITIVE);

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Don't comment-out code", "#dont-comment-out-or-deactivate-code"),
    };

    @Override public RuleID getID()             { return RuleID.REMOVE_COMMENTED_CODE; }
    @Override public String getName()           { return "Detect commented-out code"; }
    @Override public RuleCategory getCategory() { return RuleCategory.COMMENTS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    protected boolean shouldProcessComments() { return true; }

    @Override
    public String getDescription() {
        return "Reports comment lines that look like commented-out ABAP code.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.isCommentOnly()) return;
        for (Token t : stmt.getTokens()) {
            if (!t.isComment()) continue;
            String text = t.getText();
            // Strip leading comment markers.
            String body = text.replaceFirst("^\\s*[\"*]\\s*", "");
            if (body.length() < 4) continue;
            if (CODE_LIKE.matcher(body).matches()) {
                result.addFinding(t.getLine(), RuleSeverity.WARNING,
                    "Commented-out code detected — delete it (version control keeps history)");
                return; // one finding per comment statement
            }
        }
    }
}
