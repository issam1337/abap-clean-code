package com.cleanabap.core.rules.declarations;

import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.Map;

/**
 * <b>Rule:</b> Detect unused local variables (analysis-only).
 *
 * <p>Reports {@code DATA name [...]} declarations whose name never appears
 * anywhere else in the source. Removal is left to the developer: silently
 * deleting variables that are referenced via {@code FIELD-SYMBOLS},
 * dynamic access ({@code ASSIGN (name)}, etc.), or includes is unsafe.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * METHOD do_something.
 *   DATA lv_unused TYPE i.        " never read or written
 *   DATA lv_used   TYPE i.
 *   lv_used = 1.
 *   WRITE lv_used.
 * ENDMETHOD.
 * </pre>
 *
 * <h3>After (manual cleanup):</h3>
 * <pre>
 * METHOD do_something.
 *   DATA lv_used TYPE i.
 *   lv_used = 1.
 *   WRITE lv_used.
 * ENDMETHOD.
 * </pre>
 */
public class RemoveUnusedVariablesRule extends Rule {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Avoid obsolete language elements",
            "#avoid-obsolete-language-elements"),
        new RuleReference(RuleSource.CODE_PAL_FOR_ABAP,
            "Unused Variables",
            "unused-variables.md"),
    };

    @Override public RuleID getID()             { return RuleID.REMOVE_UNUSED_VARIABLES; }
    @Override public String getName()           { return "Remove unused variables"; }
    @Override public RuleCategory getCategory() { return RuleCategory.DECLARATIONS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports DATA declarations whose name is never referenced "
             + "elsewhere in the source. The variable can usually be removed, "
             + "but verify there's no dynamic / FIELD-SYMBOLS access first.";
    }

    @Override
    public String getExampleBefore() {
        return "DATA lv_unused TYPE i.\n"
             + "DATA lv_used   TYPE i.\n"
             + "lv_used = 1.";
    }

    @Override
    public String getExampleAfter() {
        return "DATA lv_used TYPE i.\n"
             + "lv_used = 1.";
    }

    @Override
    public CleanupResult apply(CodeDocument doc) {
        CleanupResult result = new CleanupResult(getID());

        Map<String, LocalVariableUsageAnalyzer.VarInfo> infos =
                LocalVariableUsageAnalyzer.analyze(doc);

        for (LocalVariableUsageAnalyzer.VarInfo info : infos.values()) {
            if (info.totalReferences == 0) {
                result.addFinding(info.declStmt.getStartLine(), RuleSeverity.WARNING,
                    "Variable '" + info.name + "' is declared but never referenced — "
                    + "consider removing the declaration");
            }
        }

        return result;
    }
}
