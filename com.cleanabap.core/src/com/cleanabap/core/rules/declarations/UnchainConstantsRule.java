package com.cleanabap.core.rules.declarations;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Unchain CONSTANTS declarations
 *
 * <p>Reports chained {@code CONSTANTS:} declarations. The actual
 * unchaining is performed by the parser's {@code resolveChains()} phase.
 * This rule emits findings so the user sees each occurrence in the
 * cleanup report.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * CONSTANTS: lc_max     TYPE i      VALUE 100,
 *            lc_default TYPE string VALUE 'X'.
 * </pre>
 *
 * <h3>After:</h3>
 * <pre>
 * CONSTANTS lc_max     TYPE i      VALUE 100.
 * CONSTANTS lc_default TYPE string VALUE 'X'.
 * </pre>
 */
public class UnchainConstantsRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Do not chain up-front declarations",
            "#do-not-chain-up-front-declarations"),
        new RuleReference(RuleSource.CODE_PAL_FOR_ABAP,
            "Chain Declaration Usage",
            "chain-declaration-usage.md"),
    };

    @Override public RuleID getID()             { return RuleID.UNCHAIN_CONSTANTS; }
    @Override public String getName()           { return "Unchain CONSTANTS declarations"; }
    @Override public RuleCategory getCategory() { return RuleCategory.DECLARATIONS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public boolean isEssential()       { return true; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Replaces CONSTANTS: chain declarations with individual CONSTANTS "
             + "statements. Each constant gets its own declaration line.";
    }

    @Override
    public String getExampleBefore() {
        return "CONSTANTS: lc_max     TYPE i VALUE 100,\n"
             + "           lc_default TYPE string VALUE 'X'.";
    }

    @Override
    public String getExampleAfter() {
        return "CONSTANTS lc_max     TYPE i VALUE 100.\n"
             + "CONSTANTS lc_default TYPE string VALUE 'X'.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.isChained()) return;
        if (!stmt.isConstantsDeclaration()) return;

        result.addFinding(stmt.getStartLine(), RuleSeverity.WARNING,
            "Chained CONSTANTS declaration detected — unchain into individual statements");
    }
}
