package com.cleanabap.core.rules.commands;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.List;

/**
 * <b>Rule:</b> Catch specific exceptions, not {@code cx_root} (analysis-only).
 *
 * <p>Reports {@code CATCH cx_root.} and {@code CATCH cx_static_check.}
 * — these are too broad; specific exception classes lead to better
 * error handling. Auto-rewrite is unsafe because the right replacement
 * depends on which exceptions the {@code TRY} block can actually raise.</p>
 *
 * <h3>Before:</h3>
 * <pre>
 * TRY.
 *     do_something( ).
 *   CATCH cx_root INTO DATA(lo_error).
 *     ...
 * ENDTRY.
 * </pre>
 *
 * <h3>After (manual):</h3>
 * <pre>
 * CATCH cx_concrete_problem INTO DATA(lo_error).
 * </pre>
 */
public class PreferCatchingSpecificRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Catch specific", "#catch-specific"),
    };

    @Override public RuleID getID()             { return RuleID.PREFER_CATCHING_SPECIFIC; }
    @Override public String getName()           { return "Catch specific exceptions, not cx_root"; }
    @Override public RuleCategory getCategory() { return RuleCategory.COMMANDS; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Reports CATCH cx_root / cx_static_check — too broad, prefer "
             + "specific exception classes.";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.startsWithKeyword("CATCH")) return;

        List<Token> tokens = stmt.getTokens();
        boolean sawCatch = false;
        for (Token t : tokens) {
            if (t.getType() == Token.Type.WHITESPACE
                    || t.getType() == Token.Type.NEWLINE
                    || t.isComment()) continue;
            if (!sawCatch) {
                if (t.isKeyword() && t.textEqualsIgnoreCase("CATCH")) sawCatch = true;
                continue;
            }
            // Skip optional BEFORE/INTO etc — only INTO/BEFORE are relevant after class names.
            if (t.isKeyword() && t.textEqualsIgnoreCase("BEFORE")) return; // CATCH BEFORE UNWIND - leave alone
            if (!t.isIdentifier() && !t.isKeyword()) return;
            String name = t.getText();
            if ("CX_ROOT".equalsIgnoreCase(name) || "CX_STATIC_CHECK".equalsIgnoreCase(name)) {
                result.addFinding(stmt.getStartLine(), RuleSeverity.WARNING,
                    "CATCH " + name + " is too broad — prefer specific exception classes");
            }
            return; // first identifier after CATCH is the class name we care about
        }
    }
}
