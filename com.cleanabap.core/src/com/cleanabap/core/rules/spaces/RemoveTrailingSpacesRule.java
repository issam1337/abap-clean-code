package com.cleanabap.core.rules.spaces;

import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Remove trailing whitespace on every line (auto-fix).
 *
 * <p>Strips any sequence of spaces / tabs immediately preceding a line
 * break. This is one of the rare formatting changes that's <em>always</em>
 * safe — it doesn't change ABAP semantics in any way, and the diff is
 * always the smallest possible.</p>
 *
 * <h3>Before (the {@code _} marks a trailing space):</h3>
 * <pre>
 * DATA lv_x TYPE i._
 * </pre>
 *
 * <h3>After:</h3>
 * <pre>
 * DATA lv_x TYPE i.
 * </pre>
 */
public class RemoveTrailingSpacesRule extends Rule {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Don't obsess about formatting", "#dont-obsess-about-formatting"),
    };

    @Override public RuleID getID()             { return RuleID.REMOVE_TRAILING_SPACES; }
    @Override public String getName()           { return "Remove trailing whitespace"; }
    @Override public RuleCategory getCategory() { return RuleCategory.EMPTY_LINES; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Removes trailing spaces and tabs from every line.";
    }

    @Override
    public CleanupResult apply(CodeDocument doc) {
        CleanupResult result = new CleanupResult(getID());
        String src = doc.getCurrentSource();
        if (src == null || src.isEmpty()) return result;

        // Strip trailing spaces / tabs before each line break (handles \r\n and \n).
        String cleaned = src.replaceAll("[ \\t]+(\\r?\\n)", "$1");
        // Also strip trailing whitespace at end-of-file.
        cleaned = cleaned.replaceAll("[ \\t]+$", "");

        if (!cleaned.equals(src)) {
            int changes = countTrimmedLines(src);
            doc.updateSource(cleaned);
            result.addChange(0, "<file>", "<file>",
                "Removed trailing whitespace on " + changes + " line(s)");
            result.setSourceModified(true);
        }
        return result;
    }

    private static int countTrimmedLines(String src) {
        int count = 0;
        int i = 0;
        while (i < src.length()) {
            int eol = src.indexOf('\n', i);
            int lineEnd = (eol < 0) ? src.length() : eol;
            // Walk back over CR if present.
            int realEnd = lineEnd;
            if (realEnd > i && src.charAt(realEnd - 1) == '\r') realEnd--;
            // Was there trailing space/tab before realEnd?
            int p = realEnd - 1;
            if (p >= i && (src.charAt(p) == ' ' || src.charAt(p) == '\t')) count++;
            if (eol < 0) break;
            i = eol + 1;
        }
        return count;
    }
}
