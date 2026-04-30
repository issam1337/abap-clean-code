package com.cleanabap.core.rules.naming;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.regex.Pattern;

/**
 * <b>Rule:</b> Avoid Hungarian / scope notation in variable names
 * (analysis-only, off by default).
 *
 * <p>The Clean ABAP guide recommends naming variables for what they
 * <em>are</em>, not for their type or scope: prefer {@code customer}
 * over {@code lv_customer_str}, {@code orders} over {@code lt_orders}.</p>
 *
 * <p>This rule is intentionally <b>not active by default</b> — many
 * teams have entrenched naming conventions and would consider these
 * findings noise. Auto-rewrite is impossible; renaming variables
 * requires a refactoring engine that updates every reference.</p>
 *
 * <h3>Detected prefixes (case-insensitive):</h3>
 * <pre>lv_, ls_, lt_, lo_, lr_, li_, ld_, lc_,
 * gv_, gs_, gt_, go_, gr_, gi_, gd_, gc_,
 * iv_, is_, it_, io_, ir_, ii_, id_,
 * ev_, es_, et_, eo_, er_, ei_,
 * cv_, cs_, ct_, co_, cr_,
 * mv_, ms_, mt_, mo_, mr_</pre>
 */
public class NoHungarianNotationRule extends RuleForStatements {

    private static final Pattern HUNGARIAN_PREFIX = Pattern.compile(
        "^(?:[lgiecm][vstordic]_).*", Pattern.CASE_INSENSITIVE);

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Avoid encodings, in particular Hungarian notation",
            "#avoid-encodings-in-particular-hungarian-notation-and-prefixes"),
    };

    @Override public RuleID getID()             { return RuleID.NO_HUNGARIAN_NOTATION; }
    @Override public String getName()           { return "Avoid Hungarian / scope prefixes"; }
    @Override public RuleCategory getCategory() { return RuleCategory.NAMING; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    /** Off by default — naming is opinionated. */
    @Override public boolean isActiveByDefault() { return false; }

    @Override
    public String getDescription() {
        return "Reports variable names that begin with Hungarian / scope "
             + "prefixes (lv_, ls_, gt_, iv_, …).";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.isDataDeclaration() && !stmt.isTypesDeclaration()
                && !stmt.isConstantsDeclaration()) return;

        // Find the first identifier on the declaration line.
        for (Token t : stmt.getTokens()) {
            if (!t.isIdentifier()) continue;
            String name = t.getText();
            if (HUNGARIAN_PREFIX.matcher(name).matches()) {
                result.addFinding(t.getLine(), RuleSeverity.INFO,
                    "Identifier '" + name + "' uses a Hungarian / scope prefix — "
                    + "name variables for their meaning, not their type");
            }
            return; // only consider the declared identifier
        }
    }
}
