package com.cleanabap.core.rules.declarations;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.Map;

/**
 * <b>Rule:</b> Prefer inline DATA for assigned-immediately-after locals
 * (analysis-only).
 *
 * <p>Reports {@code DATA name [...]} declarations that are immediately
 * followed by an assignment to {@code name}. Such variables can be
 * declared inline with {@code DATA(name) = expr.} which lets the
 * compiler infer the type and removes one line of boilerplate.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * DATA lv_count TYPE i.
 * lv_count = lines( lt_items ).
 * </pre>
 *
 * <h3>After (manual rewrite):</h3>
 * <pre>
 * DATA(lv_count) = lines( lt_items ).
 * </pre>
 */
public class PreferInlineDataRule extends Rule {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Prefer inline to up-front declarations",
            "#prefer-inline-to-up-front-declarations"),
    };

    @Override public RuleID getID()             { return RuleID.PREFER_INLINE_DATA; }
    @Override public String getName()           { return "Prefer inline DATA(...) declarations"; }
    @Override public RuleCategory getCategory() { return RuleCategory.DECLARATIONS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public int getMinAbapRelease()     { return 750; } // DATA(...) inline since 7.40
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports local DATA declarations that are immediately followed "
             + "by an assignment, suggesting they be merged into an inline "
             + "DATA(name) = ... form (requires ABAP 7.40+).";
    }

    @Override
    public String getExampleBefore() {
        return "DATA lv_count TYPE i.\n"
             + "lv_count = lines( lt_items ).";
    }

    @Override
    public String getExampleAfter() {
        return "DATA(lv_count) = lines( lt_items ).";
    }

    @Override
    public CleanupResult apply(CodeDocument doc) {
        CleanupResult result = new CleanupResult(getID());

        Map<String, LocalVariableUsageAnalyzer.VarInfo> infos =
                LocalVariableUsageAnalyzer.analyze(doc);

        for (LocalVariableUsageAnalyzer.VarInfo info : infos.values()) {
            if (info.writeStmts.size() != 1) continue;
            AbapStatement firstWrite = info.writeStmts.get(0);
            // Must be the very next non-comment, non-empty statement after the decl.
            if (!isImmediatelyAfter(info.declStmt, firstWrite)) continue;

            result.addFinding(info.declStmt.getStartLine(), RuleSeverity.INFO,
                "Variable '" + info.name + "' is declared then immediately "
                + "assigned — consider using inline DATA(" + info.name + ") = ...");
        }

        return result;
    }

    /**
     * True if {@code candidate} is the first non-trivial statement following
     * {@code anchor} via the {@code next} chain (skipping empty lines and
     * comment-only statements).
     */
    private boolean isImmediatelyAfter(AbapStatement anchor, AbapStatement candidate) {
        AbapStatement cursor = anchor.getNext();
        while (cursor != null) {
            if (cursor.isEmptyLine() || cursor.isCommentOnly()) {
                cursor = cursor.getNext();
                continue;
            }
            return cursor == candidate;
        }
        return false;
    }
}
