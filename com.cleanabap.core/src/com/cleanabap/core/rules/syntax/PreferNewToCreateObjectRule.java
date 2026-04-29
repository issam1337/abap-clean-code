package com.cleanabap.core.rules.syntax;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

import java.util.List;

/**
 * <b>Rule:</b> Prefer NEW to CREATE OBJECT
 *
 * <p>Replaces the obsolete {@code CREATE OBJECT ref TYPE class.}
 * with the modern {@code ref = NEW class( ).} syntax.</p>
 *
 * <p>Reference: Clean ABAP §
 * <a href="https://github.com/SAP/styleguides/blob/main/clean-abap/CleanABAP.md#prefer-new-to-create-object">
 * Prefer NEW to CREATE OBJECT</a></p>
 *
 * <h3>Before:</h3>
 * <pre>
 * CREATE OBJECT lo_handler TYPE zcl_handler.
 * CREATE OBJECT lo_util.
 * </pre>
 *
 * <h3>After:</h3>
 * <pre>
 * lo_handler = NEW zcl_handler( ).
 * lo_util = NEW #( ).
 * </pre>
 */
public class PreferNewToCreateObjectRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Prefer NEW to CREATE OBJECT",
            "#prefer-new-to-create-object"),
        new RuleReference(RuleSource.CODE_PAL_FOR_ABAP,
            "Prefer NEW to CREATE OBJECT",
            "prefer-new-to-create-object.md"),
    };

    @Override public RuleID getID()           { return RuleID.PREFER_NEW_TO_CREATE_OBJECT; }
    @Override public String getName()         { return "Prefer NEW to CREATE OBJECT"; }
    @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public boolean isEssential()     { return true; }
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getDescription() {
        return "Replaces 'CREATE OBJECT ref [TYPE class].' with 'ref = NEW class( ).' "
             + "or 'ref = NEW #( ).' using modern ABAP syntax.";
    }

    @Override
    public String getExampleBefore() {
        return "CREATE OBJECT lo_handler TYPE zcl_handler.\n"
             + "CREATE OBJECT lo_util.";
    }

    @Override
    public String getExampleAfter() {
        return "lo_handler = NEW zcl_handler( ).\n"
             + "lo_util = NEW #( ).";
    }

    @Override
    protected void processStatement(AbapStatement stmt, CodeDocument doc, CleanupResult result) {
        if (!stmt.startsWithKeyword("CREATE")) return;

        List<Token> tokens = stmt.getTokens();
        Token createToken = null;
        Token objectToken = null;
        Token varToken = null;
        Token typeToken = null;
        Token classToken = null;

        // Parse: CREATE OBJECT <var> [TYPE <class>] [EXPORTING ...].
        int state = 0;
        for (Token t : tokens) {
            if (t.getType() == Token.Type.WHITESPACE || t.getType() == Token.Type.NEWLINE) continue;

            switch (state) {
                case 0: // expect CREATE
                    if (t.isKeyword("CREATE")) { createToken = t; state = 1; }
                    else return;
                    break;
                case 1: // expect OBJECT
                    if (t.isKeyword("OBJECT")) { objectToken = t; state = 2; }
                    else return;
                    break;
                case 2: // expect variable name
                    if (t.isIdentifier() || t.getType() == Token.Type.FIELD_SYMBOL) {
                        varToken = t; state = 3;
                    } else return;
                    break;
                case 3: // expect TYPE or EXPORTING or period
                    if (t.isKeyword("TYPE")) { typeToken = t; state = 4; }
                    else if (t.isKeyword("EXPORTING")) {
                        // CREATE OBJECT with EXPORTING — more complex, skip for now
                        return;
                    }
                    else if (t.isPeriod()) { state = 99; }
                    break;
                case 4: // expect class name
                    if (t.isIdentifier()) { classToken = t; state = 5; }
                    else return;
                    break;
                case 5: // expect EXPORTING or period
                    if (t.isKeyword("EXPORTING")) return; // too complex for simple replacement
                    if (t.isPeriod()) state = 99;
                    break;
            }
        }

        if (state != 99 || varToken == null) return;

        // Build replacement
        String varName = varToken.getText();
        String className = (classToken != null) ? classToken.getText() : "#";
        String replacement = varName + " = NEW " + className + "( ).";

        String original = stmt.toNormalizedText();

        // Apply the change to the source
        String source = doc.getCurrentSource();
        String pattern = buildPattern(tokens);
        if (pattern != null && source.contains(pattern)) {
            String indent = getIndent(stmt);
            doc.updateSource(source.replace(pattern, indent + replacement));
            result.addChange(stmt.getStartLine(), original, replacement,
                "Replaced CREATE OBJECT with NEW");
        }
    }

    private String buildPattern(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            sb.append(t.getText());
        }
        return sb.toString().trim();
    }

    private String getIndent(AbapStatement stmt) {
        Token first = stmt.getFirstToken();
        if (first != null && first.getType() == Token.Type.WHITESPACE) {
            return first.getText();
        }
        return "";
    }
}
