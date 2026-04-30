package com.cleanabap.core.rules.syntax;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Prefer string templates to {@code CONCATENATE} or the
 * {@code &amp;&amp;} operator (analysis-only).
 *
 * <p>Reports {@code CONCATENATE} statements; the modern equivalent is the
 * inline string template syntax {@code |...{ }...|}. Auto-rewrite is
 * skipped because {@code CONCATENATE} has variants ({@code SEPARATED BY},
 * {@code RESPECTING BLANKS}, {@code IN CHARACTER MODE}) that need careful
 * mapping.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * CONCATENATE 'Hello' lv_name 'today is' lv_date INTO lv_msg SEPARATED BY space.
 * </pre>
 *
 * <h3>After (manual):</h3>
 * <pre>
 * lv_msg = |Hello { lv_name } today is { lv_date }|.
 * </pre>
 */
public class PreferStringTemplatesRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Use string templates to assemble text",
            "#use-string-templates-to-assemble-text"),
    };

    @Override public RuleID getID()             { return RuleID.PREFER_STRING_TEMPLATES; }
    @Override public String getName()           { return "Prefer string templates to CONCATENATE"; }
    @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports CONCATENATE statements, recommending the inline string "
             + "template syntax |...{ var }...|.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        Token kw = stmt.getFirstKeyword();
        if (kw == null || !kw.textEqualsIgnoreCase("CONCATENATE")) return;

        result.addFinding(stmt.getStartLine(), RuleSeverity.WARNING,
            "CONCATENATE detected — consider rewriting as a string template |...{ }...|");
    }
}
