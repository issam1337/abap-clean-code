package com.cleanabap.core.rules.spaces;

import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <b>Rule:</b> Remove needless intra-line whitespace (auto-fix).
 *
 * <p>Currently handles two safe normalisations on each line that does
 * not start with a comment marker:</p>
 * <ul>
 *   <li>collapses 2+ consecutive spaces (not at the start of the line)
 *       down to a single space;</li>
 *   <li>removes any space immediately preceding the terminating period.</li>
 * </ul>
 *
 * <p>Indentation, string literals, and string templates would need
 * a token-aware pass to be touched safely; those are intentionally
 * left for a future pretty-printer rule. The current scope handles
 * the most common copy-paste artefacts.</p>
 */
public class RemoveNeedlessSpacesRule extends Rule {

    private static final Pattern MULTI_SPACE_INSIDE = Pattern.compile("(\\S)  +");
    private static final Pattern SPACE_BEFORE_PERIOD = Pattern.compile(" +\\.\\s*$");

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Don't obsess about formatting", "#dont-obsess-about-formatting"),
    };

    @Override public RuleID getID()             { return RuleID.REMOVE_NEEDLESS_SPACES; }
    @Override public String getName()           { return "Remove needless spaces"; }
    @Override public RuleCategory getCategory() { return RuleCategory.EMPTY_LINES; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Collapses runs of internal spaces to a single space and "
             + "removes any space directly before the terminating period.";
    }

    @Override
    public CleanupResult apply(CodeDocument doc) {
        CleanupResult result = new CleanupResult(getID());
        String src = doc.getCurrentSource();
        if (src == null || src.isEmpty()) return result;

        String[] lines = src.split("\\r?\\n", -1);
        boolean modified = false;
        StringBuilder out = new StringBuilder(src.length());

        // Preserve original line endings: detect what was used.
        String eol = src.contains("\r\n") ? "\r\n" : "\n";

        for (int i = 0; i < lines.length; i++) {
            String orig = lines[i];
            String line = orig;

            // Skip comment-only lines (starting with " or *).
            String trimmed = line.replaceFirst("^\\s+", "");
            boolean isComment = trimmed.startsWith("\"") || trimmed.startsWith("*");

            if (!isComment && !line.isEmpty()) {
                // Collapse multi-space runs that sit inside the code.
                Matcher m = MULTI_SPACE_INSIDE.matcher(line);
                line = m.replaceAll("$1 ");
                // Remove space immediately before the period.
                line = SPACE_BEFORE_PERIOD.matcher(line).replaceAll(".");
            }

            if (!line.equals(orig)) modified = true;
            out.append(line);
            if (i < lines.length - 1) out.append(eol);
        }

        if (modified) {
            doc.updateSource(out.toString());
            result.addChange(0, "<file>", "<file>",
                "Removed needless spaces");
            result.setSourceModified(true);
        }
        return result;
    }
}
