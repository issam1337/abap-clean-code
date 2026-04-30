package com.cleanabap.core.rules.commands;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.List;

/**
 * <b>Rule:</b> Simplify {@code IF cond. RETURN. ENDIF.} to
 * {@code CHECK NOT cond.} (analysis-only).
 *
 * <p>The pattern of an early-exit guard wrapped in a one-statement {@code
 * IF} block is succinctly expressed as a {@code CHECK} statement. Auto-rewrite
 * is left manual because flipping the predicate (adding/removing {@code NOT})
 * requires understanding the underlying expression's structure.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * IF lv_input IS INITIAL.
 *   RETURN.
 * ENDIF.
 * </pre>
 *
 * <h3>After (manual):</h3>
 * <pre>
 * CHECK lv_input IS NOT INITIAL.
 * </pre>
 */
public class SimplifyIfReturnToCheckRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Replace IF with RETURN by CHECK", "#prefer-check-against-nested-if"),
    };

    @Override public RuleID getID()             { return RuleID.SIMPLIFY_IF_RETURN_TO_CHECK; }
    @Override public String getName()           { return "Simplify IF + RETURN guard to CHECK"; }
    @Override public RuleCategory getCategory() { return RuleCategory.COMMANDS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.INFO; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports IF/RETURN/ENDIF guards that can be simplified to a "
             + "single CHECK statement.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.startsWithKeyword("IF")) return;

        // Walk forward, skipping comments/empty lines, to find the next two
        // significant statements. Pattern is: IF ... / RETURN. / ENDIF.
        AbapStatement second = nextSignificant(stmt);
        if (second == null) return;
        if (!second.startsWithKeyword("RETURN")) return;
        // Make sure RETURN is the only statement in the body (no value).
        List<Token> bodyTokens = second.getTokens();
        int sigCount = 0;
        for (Token t : bodyTokens) {
            if (t.getType() == Token.Type.WHITESPACE
                    || t.getType() == Token.Type.NEWLINE
                    || t.isComment()) continue;
            sigCount++;
        }
        // Expect exactly RETURN + period.
        if (sigCount != 2) return;

        AbapStatement third = nextSignificant(second);
        if (third == null) return;
        if (!third.startsWithKeyword("ENDIF")) return;

        result.addFinding(stmt.getStartLine(), RuleSeverity.INFO,
            "IF…RETURN…ENDIF guard — simplify to a single CHECK statement");
    }

    private static AbapStatement nextSignificant(AbapStatement stmt) {
        AbapStatement n = stmt.getNext();
        while (n != null && (n.isCommentOnly() || n.isEmptyLine())) {
            n = n.getNext();
        }
        return n;
    }
}
