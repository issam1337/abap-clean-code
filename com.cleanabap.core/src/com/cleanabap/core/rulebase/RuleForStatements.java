package com.cleanabap.core.rulebase;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.programbase.CodeDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for rules that operate on individual ABAP statements.
 *
 * <p>Most cleanup rules work at the statement level: they examine each
 * statement, decide whether a transformation applies, and modify or
 * report on that statement.</p>
 *
 * <p>Subclasses implement {@link #processStatement(AbapStatement, CodeDocument, CleanupResult)}
 * for each statement.</p>
 *
 * <h3>Source-update contract:</h3>
 * <p>Subclasses have two ways to make changes:</p>
 * <ol>
 *   <li><b>Token-level edits</b> — mutate {@link com.cleanabap.core.parser.Token}
 *       objects on the supplied statement and call {@code result.setSourceModified(true)}.
 *       Do <em>not</em> call {@link CodeDocument#updateSource(String)} yourself; this base
 *       class will rebuild the source once after all statements have been processed,
 *       using a stable snapshot so multiple edits in different statements all survive.</li>
 *   <li><b>String-level replacement</b> — call {@link CodeDocument#updateSource(String)}
 *       directly with a new source. The base class detects this and skips the snapshot
 *       rebuild so the rule's edits are preserved.</li>
 * </ol>
 */
public abstract class RuleForStatements extends Rule {

    /** Snapshot of the statement list captured at the start of {@link #apply}. */
    private List<AbapStatement> statementSnapshot;

    @Override
    public CleanupResult apply(CodeDocument doc) {
        CleanupResult result = new CleanupResult(getID());

        // Snapshot the statement list so iteration stays stable even if a rule
        // (or a previously-iterated statement) replaces doc.statements via
        // doc.updateSource(...). Without this, any token-level edits made on
        // the second-or-later statement get lost when subsequent rebuilds
        // read the freshly re-parsed (and unmodified) doc.statements list.
        statementSnapshot = new ArrayList<>(doc.getStatements());

        // Capture the source string before iteration so we can detect whether
        // a subclass already wrote back via doc.updateSource(...) (string-replace
        // style rule). If yes, we leave it alone; if no, we rebuild from the
        // snapshot to materialize any token-level edits.
        String sourceBefore = doc.getCurrentSource();

        for (AbapStatement stmt : statementSnapshot) {
            if (stmt.isCommentOnly() && !shouldProcessComments()) continue;
            if (stmt.isEmptyLine() && !shouldProcessEmptyLines()) continue;

            processStatement(stmt, doc, result);
        }

        // If token-level edits happened (sourceModified is true) and no
        // string-replace style rule already mutated doc.currentSource, rebuild
        // the document source from the snapshot exactly once.
        if (result.isSourceModified()
                && doc.getCurrentSource().equals(sourceBefore)) {
            StringBuilder sb = new StringBuilder();
            for (AbapStatement s : statementSnapshot) {
                sb.append(s.toSourceCode());
            }
            String rebuilt = sb.toString();
            if (!rebuilt.equals(sourceBefore)) {
                doc.updateSource(rebuilt);
            }
        }

        statementSnapshot = null;
        return result;
    }

    /**
     * Source rebuilt from the snapshot captured at the start of {@link #apply}.
     * Subclasses doing token-level edits can use this if they need an
     * intermediate view of the modified source (e.g., for fallback diffing).
     */
    protected String rebuildFromSnapshot() {
        if (statementSnapshot == null) return "";
        StringBuilder sb = new StringBuilder();
        for (AbapStatement s : statementSnapshot) {
            sb.append(s.toSourceCode());
        }
        return sb.toString();
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
