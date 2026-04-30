package com.cleanabap.core.rules.declarations;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Unchain TYPES declarations
 *
 * <p>Reports chained {@code TYPES:} declarations. The actual unchaining
 * is performed by the parser's {@code resolveChains()} phase, which handles
 * any chain keyword (DATA, TYPES, CONSTANTS, FIELD-SYMBOLS, STATICS).
 * This rule emits findings so the user is informed about each occurrence.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * TYPES: ty_id     TYPE i,
 *        ty_name   TYPE string,
 *        ty_active TYPE abap_bool.
 * </pre>
 *
 * <h3>After:</h3>
 * <pre>
 * TYPES ty_id     TYPE i.
 * TYPES ty_name   TYPE string.
 * TYPES ty_active TYPE abap_bool.
 * </pre>
 */
public class UnchainTypesDeclarationsRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Do not chain up-front declarations",
            "#do-not-chain-up-front-declarations"),
        new RuleReference(RuleSource.CODE_PAL_FOR_ABAP,
            "Chain Declaration Usage",
            "chain-declaration-usage.md"),
    };

    @Override public RuleID getID()             { return RuleID.UNCHAIN_TYPES_DECLARATIONS; }
    @Override public String getName()           { return "Unchain TYPES declarations"; }
    @Override public RuleCategory getCategory() { return RuleCategory.DECLARATIONS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public boolean isEssential()       { return true; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Replaces TYPES: chain declarations with individual TYPES statements. "
             + "Each type alias gets its own declaration line.";
    }

    @Override
    public String getExampleBefore() {
        return "TYPES: ty_id   TYPE i,\n"
             + "       ty_name TYPE string.";
    }

    @Override
    public String getExampleAfter() {
        return "TYPES ty_id   TYPE i.\n"
             + "TYPES ty_name TYPE string.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.isChained()) return;
        if (!stmt.isTypesDeclaration()) return;

        result.addFinding(stmt.getStartLine(), RuleSeverity.WARNING,
            "Chained TYPES declaration detected — unchain into individual statements");
    }
}
