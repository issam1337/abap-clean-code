package com.cleanabap.core.rules.formatting;

import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Keep line length within a threshold (analysis-only).
 *
 * <p>Flags any source line that is longer than the configured maximum.
 * The Clean ABAP guide suggests staying around 100 characters. Auto-fix
 * is not attempted: line breaking ABAP requires aware-handling of
 * keywords, parameter lists, string templates, etc.</p>
 */
public class KeepLineLengthRule extends Rule {

    /** Default soft cap on line width. */
    private static final int DEFAULT_MAX = 120;

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Stick to a reasonable line length", "#stick-to-a-reasonable-line-length"),
    };

    @Override public RuleID getID()             { return RuleID.KEEP_LINE_LENGTH; }
    @Override public String getName()           { return "Keep lines within " + DEFAULT_MAX + " characters"; }
    @Override public RuleCategory getCategory() { return RuleCategory.FORMATTING; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports source lines longer than " + DEFAULT_MAX + " characters.";
    }

    @Override
    public CleanupResult apply(CodeDocument doc) {
        CleanupResult result = new CleanupResult(getID());
        String src = doc.getCurrentSource();
        if (src == null || src.isEmpty()) return result;

        String[] lines = src.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            int len = lines[i].length();
            // Compensate trailing CR if any (split with \r?\n already handles it,
            // but defensively strip any stray \r).
            if (len > 0 && lines[i].charAt(len - 1) == '\r') len--;
            if (len > DEFAULT_MAX) {
                result.addFinding(i + 1, RuleSeverity.INFO,
                    "Line is " + len + " characters (> " + DEFAULT_MAX + ")");
            }
        }
        return result;
    }
}
