package com.cleanabap.core.rules.alignment;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

/**
 * <b>Rule:</b> Align consecutive {@code DATA / TYPES / CONSTANTS}
 * declarations (analysis-only).
 *
 * <p>For a run of two or more consecutive declarations, the {@code TYPE}
 * keyword should sit at a consistent column. This is a heuristic check —
 * we look at the column of the first {@code TYPE} keyword in two
 * neighbouring declarations and flag a mismatch.</p>
 *
 * <p>Auto-realignment is not attempted; reliable column-level rewriting
 * requires a dedicated formatter, which is a separate work item.</p>
 */
public class AlignDeclarationsRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Align type clauses", "#align-type-clauses"),
    };

    @Override public RuleID getID()             { return RuleID.ALIGN_DECLARATIONS; }
    @Override public String getName()           { return "Align consecutive DATA/TYPES/CONSTANTS declarations"; }
    @Override public RuleCategory getCategory() { return RuleCategory.ALIGNMENT; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports consecutive declarations whose TYPE keyword does not "
             + "sit at the same column.";
    }

    /** Tracks the TYPE-column of the previous declaration. */
    private Integer prevTypeColumn = null;

    @Override
    public CleanupResult apply(CodeDocument doc) {
        prevTypeColumn = null;          // reset across documents
        return super.apply(doc);
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.isDataDeclaration() && !stmt.isTypesDeclaration() && !stmt.isConstantsDeclaration()) {
            prevTypeColumn = null;     // sequence broken
            return;
        }

        Integer col = findTypeKeywordColumn(stmt);
        if (col == null) {
            prevTypeColumn = null;
            return;
        }
        if (prevTypeColumn != null && !col.equals(prevTypeColumn)) {
            result.addFinding(stmt.getStartLine(), RuleSeverity.INFO,
                "TYPE column (" + col + ") differs from previous declaration ("
                + prevTypeColumn + ") — consider aligning");
        }
        prevTypeColumn = col;
    }

    private static Integer findTypeKeywordColumn(AbapStatement stmt) {
        for (Token t : stmt.getTokens()) {
            if (t.isKeyword() && t.textEqualsIgnoreCase("TYPE")) {
                return t.getColumn();
            }
        }
        return null;
    }
}
