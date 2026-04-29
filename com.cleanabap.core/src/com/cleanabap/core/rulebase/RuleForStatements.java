package com.cleanabap.core.rulebase;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.programbase.CodeDocument;

/**
 * Base class for rules that operate on individual ABAP statements.
 *
 * <p>Most cleanup rules work at the statement level: they examine each
 * statement, decide whether a transformation applies, and modify or
 * report on that statement.</p>
 *
 * <p>Subclasses implement {@link #processStatement(AbapStatement, CodeDocument)}
 * for each statement.</p>
 */
public abstract class RuleForStatements extends Rule {

    @Override
    public CleanupResult apply(CodeDocument doc) {
        CleanupResult result = new CleanupResult(getID());

        for (AbapStatement stmt : doc.getStatements()) {
            if (stmt.isCommentOnly() && !shouldProcessComments()) continue;
            if (stmt.isEmptyLine() && !shouldProcessEmptyLines()) continue;

            processStatement(stmt, doc, result);
        }

        return result;
    }

    /**
     * Process a single statement.
     * Subclasses implement their detection and transformation logic here.
     *
     * @param stmt   The statement to process
     * @param doc    The parent code document (for context)
     * @param result Accumulate changes and findings here
     */
    protected abstract void processStatement(AbapStatement stmt,
                                              CodeDocument doc,
                                              CleanupResult result);

    /** Override to true if this rule should also process comment-only lines. */
    protected boolean shouldProcessComments() {
        return false;
    }

    /** Override to true if this rule should also process empty lines. */
    protected boolean shouldProcessEmptyLines() {
        return false;
    }
}
