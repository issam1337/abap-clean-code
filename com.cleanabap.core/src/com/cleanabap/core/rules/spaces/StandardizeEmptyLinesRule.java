package com.cleanabap.core.rules.spaces;

import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Collapse runs of 3+ consecutive blank lines down to 2
 * (auto-fix).
 *
 * <p>Multiple blank lines clutter the source and don't add information.
 * The Clean ABAP guide recommends using single empty lines to separate
 * concerns; this rule guards against accidentally accumulated blank
 * blocks (e.g. from copy-paste).</p>
 */
public class StandardizeEmptyLinesRule extends Rule {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Don't obsess about formatting", "#dont-obsess-about-formatting"),
    };

    @Override public RuleID getID()             { return RuleID.STANDARDIZE_EMPTY_LINES; }
    @Override public String getName()           { return "Collapse 3+ consecutive blank lines"; }
    @Override public RuleCategory getCategory() { return RuleCategory.EMPTY_LINES; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Collapses runs of 3 or more consecutive empty lines to a "
             + "maximum of 2.";
    }

    @Override
    public CleanupResult apply(CodeDocument doc) {
        CleanupResult result = new CleanupResult(getID());
        String src = doc.getCurrentSource();
        if (src == null || src.isEmpty()) return result;

        // Match 3+ newlines (with possible CR) and collapse to two newlines.
        String cleaned = src.replaceAll("(\\r?\\n[ \\t]*){3,}", "\n\n");

        if (!cleaned.equals(src)) {
            doc.updateSource(cleaned);
            result.addChange(0, "<file>", "<file>",
                "Collapsed runs of 3+ blank lines to 2");
            result.setSourceModified(true);
        }
        return result;
    }
}
