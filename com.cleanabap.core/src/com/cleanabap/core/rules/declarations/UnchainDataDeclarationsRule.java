package com.cleanabap.core.rules.declarations;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Unchain DATA declarations
 *
 * <p>Converts chained DATA: declarations into individual statements.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * DATA: lv_name  TYPE string,
 *       lv_count TYPE i,
 *       lt_items TYPE STANDARD TABLE OF vbap.
 * </pre>
 *
 * <h3>After:</h3>
 * <pre>
 * DATA lv_name  TYPE string.
 * DATA lv_count TYPE i.
 * DATA lt_items TYPE STANDARD TABLE OF vbap.
 * </pre>
 *
 * <p>Note: Chain resolution is already handled by the parser's
 * {@code resolveChains()} phase. This rule ensures unchained output
 * is produced even when the parser didn't resolve a particular pattern,
 * and also handles FIELD-SYMBOLS and STATICS chains.</p>
 */
public class UnchainDataDeclarationsRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Do not chain up-front declarations",
            "#do-not-chain-up-front-declarations"),
        new RuleReference(RuleSource.CODE_PAL_FOR_ABAP,
            "Chain Declaration Usage",
            "chain-declaration-usage.md"),
    };

    // Configurable: also unchain FIELD-SYMBOLS and STATICS?
    private final ConfigValue<Boolean> configUnchainFieldSymbols =
        ConfigValue.ofBoolean(this, "unchainFieldSymbols",
            "Also unchain FIELD-SYMBOLS declarations", true,
            "If enabled, FIELD-SYMBOLS: chains are also resolved.");

    private final ConfigValue<Boolean> configUnchainStatics =
        ConfigValue.ofBoolean(this, "unchainStatics",
            "Also unchain STATICS declarations", true,
            "If enabled, STATICS: chains are also resolved.");

    @Override public RuleID getID()             { return RuleID.UNCHAIN_DATA_DECLARATIONS; }
    @Override public String getName()           { return "Unchain DATA declarations"; }
    @Override public RuleCategory getCategory() { return RuleCategory.DECLARATIONS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public boolean isEssential()       { return true; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Replaces DATA: chain declarations with individual DATA statements. "
             + "Each variable gets its own declaration line.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        // The parser already resolves chains in phase 2.
        // This rule acts as a safety net and handles edge cases.
        if (!stmt.isChained()) return;
        if (!stmt.isDataDeclaration() &&
            !(configUnchainFieldSymbols.getValue() && stmt.startsWithKeyword("FIELD-SYMBOLS")) &&
            !(configUnchainStatics.getValue() && stmt.startsWithKeyword("STATICS"))) {
            return;
        }

        result.addFinding(stmt.getStartLine(), RuleSeverity.WARNING,
            "Chained " + stmt.getFirstKeyword().getText() + " declaration detected — "
            + "unchain into individual statements");
    }

    @Override
    public java.util.List<ConfigValue<?>> getConfigValues() {
        return java.util.List.of(configUnchainFieldSymbols, configUnchainStatics);
    }
}
