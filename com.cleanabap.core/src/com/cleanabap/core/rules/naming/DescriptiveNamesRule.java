package com.cleanabap.core.rules.naming;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.Locale;
import java.util.Set;

/**
 * <b>Rule:</b> Prefer descriptive names (analysis-only, off by default).
 *
 * <p>Reports declared identifiers that are very short or use single-letter
 * abbreviations like {@code i}, {@code j}, {@code lv_x}, {@code lv_a}.
 * Loop / index variables are fine in tight scopes, but at declaration time
 * a more descriptive name usually pays off.</p>
 *
 * <p>This rule is opinionated — we deliberately leave it inactive by
 * default. Auto-rename would require a project-wide refactor and is out
 * of scope.</p>
 */
public class DescriptiveNamesRule extends RuleForStatements {

    /** Names that are obvious noise — the heuristic flags any of these. */
    private static final Set<String> NOISE_NAMES = Set.of(
        "x", "y", "z",
        "a", "b", "c", "d", "e",
        "i", "j", "k", "l", "m", "n",
        "tmp", "temp", "buf", "buff",
        "lv_x", "lv_y", "lv_a", "lv_b", "lv_i", "lv_j",
        "ls_x", "ls_y",
        "lt_x", "lt_y",
        "var", "value"
    );

    private static final int MIN_NAME_LENGTH = 3;

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Use descriptive names", "#use-descriptive-names"),
    };

    @Override public RuleID getID()             { return RuleID.DESCRIPTIVE_NAMES; }
    @Override public String getName()           { return "Prefer descriptive names"; }
    @Override public RuleCategory getCategory() { return RuleCategory.NAMING; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override public boolean isActiveByDefault() { return false; }

    @Override
    public String getDescription() {
        return "Reports declared identifiers shorter than " + MIN_NAME_LENGTH
             + " characters or with obvious noise names (i, j, tmp, …).";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.isDataDeclaration() && !stmt.isTypesDeclaration()
                && !stmt.isConstantsDeclaration()) return;

        for (Token t : stmt.getTokens()) {
            if (!t.isIdentifier()) continue;
            String raw = t.getText();
            String lc = raw.toLowerCase(Locale.ROOT);

            if (raw.length() < MIN_NAME_LENGTH) {
                result.addFinding(t.getLine(), RuleSeverity.INFO,
                    "Identifier '" + raw + "' is shorter than " + MIN_NAME_LENGTH
                    + " characters — prefer a descriptive name");
            } else if (NOISE_NAMES.contains(lc)) {
                result.addFinding(t.getLine(), RuleSeverity.INFO,
                    "Identifier '" + raw + "' is a generic placeholder — prefer a descriptive name");
            }
            return; // only consider the declared identifier
        }
    }
}
