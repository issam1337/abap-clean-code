package com.cleanabap.core.parser;

/**
 * Represents a single token in an ABAP code document.
 * Tokens are the atomic units produced by the {@link AbapLexer}
 * and consumed by the {@link AbapParser} to build {@link AbapStatement}s.
 */
public class Token {

    // ─── Token Types ─────────────────────────────────────────────
    public enum Type {
        KEYWORD,            // ABAP keywords: DATA, IF, METHOD, SELECT ...
        IDENTIFIER,         // Variable names, method names, class names
        LITERAL_STRING,     // 'string' or `string template`
        LITERAL_NUMBER,     // 123, 3.14
        OPERATOR,           // =, <>, +, -, *, /, >=, <=, etc.
        PUNCTUATION,        // . , : ( )
        COMMENT_LINE,       // " inline comment
        COMMENT_FULL,       // * full-line comment
        PRAGMA,             // ##NO_TEXT, ##NEEDED etc.
        PSEUDO_COMMENT,     // "#EC ... pseudo comment
        WHITESPACE,         // spaces, tabs (preserved for formatting)
        NEWLINE,            // line breaks
        PERIOD,             // statement-ending period
        COLON,              // chain colon
        COMMA,              // chain comma
        STRING_TEMPLATE,    // |...{ expr }...| string templates
        FIELD_SYMBOL,       // <fs_name>
        UNKNOWN
    }

    // ─── Fields ──────────────────────────────────────────────────
    private Type type;
    private String text;
    private int line;           // 1-based source line number
    private int column;         // 0-based column
    private int offset;         // absolute offset in source
    private int length;

    // Linked list pointers for token chain
    private Token prev;
    private Token next;

    // ─── Constructors ────────────────────────────────────────────

    public Token(Type type, String text, int line, int column, int offset) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.column = column;
        this.offset = offset;
        this.length = text.length();
    }

    /** Factory: create a keyword token */
    public static Token keyword(String text, int line, int col, int offset) {
        return new Token(Type.KEYWORD, text, line, col, offset);
    }

    /** Factory: create an identifier token */
    public static Token identifier(String text, int line, int col, int offset) {
        return new Token(Type.IDENTIFIER, text, line, col, offset);
    }

    /** Factory: create a synthetic token (e.g., for rule-generated code) */
    public static Token synthetic(Type type, String text, int sourceLine) {
        return new Token(type, text, sourceLine, 0, -1);
    }

    // ─── Getters/Setters ─────────────────────────────────────────

    public Type getType()     { return type; }
    public String getText()   { return text; }
    public int getLine()      { return line; }
    public int getColumn()    { return column; }
    public int getOffset()    { return offset; }
    public int getLength()    { return length; }

    public Token getPrev()    { return prev; }
    public Token getNext()    { return next; }

    public void setText(String text) {
        this.text = text;
        this.length = text.length();
    }

    public void setType(Type type)       { this.type = type; }
    public void setLine(int line)        { this.line = line; }
    public void setColumn(int column)    { this.column = column; }

    // ─── Linked List Operations ──────────────────────────────────

    public void linkAfter(Token predecessor) {
        this.prev = predecessor;
        if (predecessor != null) {
            this.next = predecessor.next;
            predecessor.next = this;
            if (this.next != null) {
                this.next.prev = this;
            }
        }
    }

    public void linkBefore(Token successor) {
        this.next = successor;
        if (successor != null) {
            this.prev = successor.prev;
            successor.prev = this;
            if (this.prev != null) {
                this.prev.next = this;
            }
        }
    }

    public void remove() {
        if (prev != null) prev.next = next;
        if (next != null) next.prev = prev;
        prev = null;
        next = null;
    }

    // ─── Query Methods ───────────────────────────────────────────

    public boolean isKeyword()         { return type == Type.KEYWORD; }
    public boolean isIdentifier()      { return type == Type.IDENTIFIER; }
    public boolean isComment()         { return type == Type.COMMENT_LINE || type == Type.COMMENT_FULL; }
    public boolean isPeriod()          { return type == Type.PERIOD; }
    public boolean isColon()           { return type == Type.COLON; }
    public boolean isComma()           { return type == Type.COMMA; }
    public boolean isPragma()          { return type == Type.PRAGMA; }
    public boolean isStringLiteral()   { return type == Type.LITERAL_STRING || type == Type.STRING_TEMPLATE; }
    public boolean isOperator()        { return type == Type.OPERATOR; }

    public boolean isKeyword(String keyword) {
        return type == Type.KEYWORD && text.equalsIgnoreCase(keyword);
    }

    public boolean isAnyKeyword(String... keywords) {
        if (type != Type.KEYWORD) return false;
        for (String kw : keywords) {
            if (text.equalsIgnoreCase(kw)) return true;
        }
        return false;
    }

    public boolean textEquals(String other) {
        return text.equals(other);
    }

    public boolean textEqualsIgnoreCase(String other) {
        return text.equalsIgnoreCase(other);
    }

    /** Is this the first non-whitespace token on its line? */
    public boolean isFirstTokenInLine() {
        Token p = prev;
        while (p != null && p.line == this.line) {
            if (p.type != Type.WHITESPACE && p.type != Type.NEWLINE) {
                return false;
            }
            p = p.prev;
        }
        return true;
    }

    /** Is this the only non-whitespace token on its line? */
    public boolean isOnlyTokenInLine() {
        return isFirstTokenInLine() && isLastTokenInLine();
    }

    /** Is this the last non-whitespace token on its line? */
    public boolean isLastTokenInLine() {
        Token n = next;
        while (n != null && n.line == this.line) {
            if (n.type != Type.WHITESPACE && n.type != Type.NEWLINE) {
                return false;
            }
            n = n.next;
        }
        return true;
    }

    // ─── Object Overrides ────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("Token[%s, \"%s\", L%d:C%d]", type, text, line, column);
    }
}
