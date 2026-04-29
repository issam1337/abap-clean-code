package com.cleanabap.core.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete ABAP statement — the sequence of tokens
 * from (and including) the first keyword up to (and including) the
 * terminating period.
 *
 * <p>In ABAP, every statement ends with a period. Chain notation
 * (COLON + COMMA) is resolved into individual statements during parsing.</p>
 */
public class AbapStatement {

    private final List<Token> tokens;
    private int startLine;
    private int endLine;

    // Navigation within the code document
    private AbapStatement prev;
    private AbapStatement next;

    // Parent command block (IF...ENDIF, LOOP...ENDLOOP, etc.)
    private AbapStatement parentBlock;
    private final List<AbapStatement> children = new ArrayList<>();

    public AbapStatement() {
        this.tokens = new ArrayList<>();
    }

    public AbapStatement(List<Token> tokens) {
        this.tokens = new ArrayList<>(tokens);
        if (!tokens.isEmpty()) {
            this.startLine = tokens.get(0).getLine();
            this.endLine = tokens.get(tokens.size() - 1).getLine();
        }
    }

    // ─── Token Access ────────────────────────────────────────────

    public List<Token> getTokens()       { return tokens; }
    public int getTokenCount()           { return tokens.size(); }
    public Token getFirstToken()         { return tokens.isEmpty() ? null : tokens.get(0); }
    public Token getLastToken()          { return tokens.isEmpty() ? null : tokens.get(tokens.size() - 1); }

    public void addToken(Token token) {
        tokens.add(token);
        if (tokens.size() == 1) {
            startLine = token.getLine();
        }
        endLine = token.getLine();
    }

    /**
     * Get the first keyword token (skipping whitespace/comments).
     */
    public Token getFirstKeyword() {
        for (Token t : tokens) {
            if (t.isKeyword()) return t;
        }
        return null;
    }

    /**
     * Get all tokens of a specific type.
     */
    public List<Token> getTokensByType(Token.Type type) {
        List<Token> result = new ArrayList<>();
        for (Token t : tokens) {
            if (t.getType() == type) result.add(t);
        }
        return result;
    }

    // ─── Statement Classification ────────────────────────────────

    /** Does this statement begin with the given keyword? */
    public boolean startsWithKeyword(String keyword) {
        Token kw = getFirstKeyword();
        return kw != null && kw.textEqualsIgnoreCase(keyword);
    }

    /** Does this statement begin with any of the given keywords? */
    public boolean startsWithAnyKeyword(String... keywords) {
        Token kw = getFirstKeyword();
        if (kw == null) return false;
        for (String k : keywords) {
            if (kw.textEqualsIgnoreCase(k)) return true;
        }
        return false;
    }

    /** Is this a DATA declaration? */
    public boolean isDataDeclaration() {
        return startsWithAnyKeyword("DATA", "CLASS-DATA", "STATICS");
    }

    /** Is this a TYPES declaration? */
    public boolean isTypesDeclaration() {
        return startsWithKeyword("TYPES");
    }

    /** Is this a CONSTANTS declaration? */
    public boolean isConstantsDeclaration() {
        return startsWithKeyword("CONSTANTS");
    }

    /** Is this a METHOD declaration/implementation start? */
    public boolean isMethodStart() {
        return startsWithKeyword("METHOD");
    }

    /** Is this a block opener (IF, LOOP, DO, WHILE, TRY, CASE)? */
    public boolean isBlockOpener() {
        return startsWithAnyKeyword("IF", "LOOP", "DO", "WHILE", "TRY", "CASE");
    }

    /** Is this a block closer (ENDIF, ENDLOOP, ENDDO, etc.)? */
    public boolean isBlockCloser() {
        return startsWithAnyKeyword("ENDIF", "ENDLOOP", "ENDDO",
            "ENDWHILE", "ENDTRY", "ENDCASE", "ENDMETHOD", "ENDCLASS",
            "ENDFORM", "ENDFUNCTION");
    }

    /** Is this a comment-only statement? */
    public boolean isCommentOnly() {
        for (Token t : tokens) {
            if (t.getType() != Token.Type.WHITESPACE &&
                t.getType() != Token.Type.NEWLINE &&
                t.getType() != Token.Type.COMMENT_LINE &&
                t.getType() != Token.Type.COMMENT_FULL) {
                return false;
            }
        }
        return !tokens.isEmpty();
    }

    /** Is this an empty line? */
    public boolean isEmptyLine() {
        for (Token t : tokens) {
            if (t.getType() != Token.Type.WHITESPACE &&
                t.getType() != Token.Type.NEWLINE) {
                return false;
            }
        }
        return true;
    }

    /** Does this statement contain a chain colon? */
    public boolean isChained() {
        for (Token t : tokens) {
            if (t.isColon()) return true;
            if (t.isKeyword() || t.isIdentifier()) break;
        }
        // Check after keyword
        for (Token t : tokens) {
            if (t.isColon()) return true;
        }
        return false;
    }

    // ─── Text Reconstruction ─────────────────────────────────────

    /**
     * Reconstruct the source text of this statement.
     */
    public String toSourceCode() {
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            sb.append(t.getText());
        }
        return sb.toString();
    }

    /**
     * Get the "normalized" text — no extra whitespace, no comments.
     */
    public String toNormalizedText() {
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            if (t.isComment() || t.getType() == Token.Type.WHITESPACE ||
                t.getType() == Token.Type.NEWLINE) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(t.getText());
        }
        return sb.toString();
    }

    // ─── Navigation ──────────────────────────────────────────────

    public AbapStatement getPrev()            { return prev; }
    public AbapStatement getNext()            { return next; }
    public AbapStatement getParentBlock()     { return parentBlock; }
    public List<AbapStatement> getChildren()  { return children; }

    public void setPrev(AbapStatement prev)               { this.prev = prev; }
    public void setNext(AbapStatement next)               { this.next = next; }
    public void setParentBlock(AbapStatement parentBlock)  { this.parentBlock = parentBlock; }

    public void addChild(AbapStatement child) {
        children.add(child);
        child.setParentBlock(this);
    }

    // ─── Line Info ───────────────────────────────────────────────

    public int getStartLine()  { return startLine; }
    public int getEndLine()    { return endLine; }
    public int getLineCount()  { return endLine - startLine + 1; }

    @Override
    public String toString() {
        String text = toNormalizedText();
        if (text.length() > 60) text = text.substring(0, 57) + "...";
        return String.format("Statement[L%d-%d: %s]", startLine, endLine, text);
    }
}
