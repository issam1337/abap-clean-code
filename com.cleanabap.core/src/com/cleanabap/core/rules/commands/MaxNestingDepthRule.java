package com.cleanabap.core.rules.commands;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Limit nesting depth (analysis-only).
 *
 * <p>Reports any block opener that sits at a nesting depth above the
 * configured threshold. The Clean ABAP guide suggests keeping deep
 * control-flow nesting low; the conventional recommendation is no more
 * than 3 levels. Auto-rewrite is not safe — flattening control flow
 * requires semantic refactoring (extract method, guard clauses, etc.).</p>
 */
public class MaxNestingDepthRule extends RuleForStatements {

    /** Default maximum allowed nesting depth (depth 0 = top level). */
    private static final int DEFAULT_MAX_DEPTH = 3;

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Keep nesting depth low", "#keep-the-nesting-depth-low"),
    };

    @Override public RuleID getID()             { return RuleID.MAX_NESTING_DEPTH; }
    @Override public String getName()           { return "Limit nesting depth"; }
    @Override public RuleCategory getCategory() { return RuleCategory.COMMANDS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports control-flow blocks that nest deeper than "
             + DEFAULT_MAX_DEPTH + " levels.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.isBlockOpener()) return;
        int depth = doc.getNestingDepth(stmt);
        if (depth >= DEFAULT_MAX_DEPTH) {
            result.addFinding(stmt.getStartLine(), RuleSeverity.WARNING,
                "Block at nesting depth " + depth + " — consider flattening (guard clauses, extract method)");
        }
    }
}
