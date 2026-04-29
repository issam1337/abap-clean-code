package com.cleanabap.core.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lexer (tokenizer) for ABAP source code.
 *
 * Breaks raw ABAP source text into a stream of {@link Token}s.
 * Handles ABAP-specific constructs: string templates, field symbols,
 * pragmas, pseudo-comments, chain colons, etc.
 */
public class AbapLexer {

    private static final Set<String> ABAP_KEYWORDS = new HashSet<>();

    static {
        // Core keywords (non-exhaustive — extend as needed)
        String[] kws = {
            "REPORT", "PROGRAM", "CLASS", "ENDCLASS", "METHOD", "ENDMETHOD",
            "DATA", "TYPES", "CONSTANTS", "FIELD-SYMBOLS", "STATICS",
            "IF", "ELSE", "ELSEIF", "ENDIF", "CASE", "WHEN", "ENDCASE",
            "DO", "ENDDO", "WHILE", "ENDWHILE", "LOOP", "ENDLOOP",
            "AT", "ENDAT", "FORM", "ENDFORM", "FUNCTION", "ENDFUNCTION",
            "TRY", "CATCH", "CLEANUP", "ENDTRY", "RAISE",
            "SELECT", "ENDSELECT", "INSERT", "UPDATE", "DELETE", "MODIFY",
            "WRITE", "MESSAGE", "RETURN", "EXIT", "CHECK", "CONTINUE",
            "CLEAR", "FREE", "REFRESH", "APPEND", "COLLECT", "READ",
            "SORT", "DESCRIBE", "MOVE", "COMPUTE", "ADD", "SUBTRACT",
            "MULTIPLY", "DIVIDE", "CONCATENATE", "CONDENSE", "TRANSLATE",
            "REPLACE", "SHIFT", "SPLIT", "SEARCH", "FIND", "OVERLAY",
            "CALL", "CREATE", "IMPORT", "EXPORT", "ASSIGN",
            "NEW", "VALUE", "REF", "CORRESPONDING", "CONV", "COND", "SWITCH",
            "IMPORTING", "EXPORTING", "CHANGING", "RETURNING", "RECEIVING",
            "RAISING", "EXCEPTIONS", "TABLES",
            "TYPE", "LIKE", "REF", "TO", "OF", "TABLE", "STANDARD", "SORTED",
            "HASHED", "WITH", "DEFAULT", "KEY", "UNIQUE", "NON-UNIQUE",
            "BEGIN", "END", "INCLUDE", "STRUCTURE",
            "PUBLIC", "PROTECTED", "PRIVATE", "SECTION",
            "DEFINITION", "IMPLEMENTATION", "ABSTRACT", "FINAL",
            "INHERITING", "FROM", "INTERFACES", "METHODS", "ALIASES",
            "CLASS-DATA", "CLASS-METHODS", "CLASS-EVENTS",
            "EVENTS", "FOR", "EVENT", "FRIENDS",
            "CREATE", "OBJECT", "INTO", "RESULT",
            "AND", "OR", "NOT", "IS", "INITIAL", "BOUND", "ASSIGNED",
            "SUPPLIED", "REQUESTED", "IN", "BETWEEN",
            "EQ", "NE", "LT", "GT", "LE", "GE", "CO", "CN", "CA", "NA",
            "CS", "NS", "CP", "NP",
            "ABAP_TRUE", "ABAP_FALSE", "SPACE",
            "SY", "SYST",
            "AUTHORITY-CHECK", "COMMIT", "ROLLBACK", "WORK",
            "SUBMIT", "LEAVE", "SET", "GET", "PERFORM",
            "ASSIGNING", "REFERENCE", "CASTING",
            "ASSERT", "BREAK-POINT", "LOG-POINT",
            "REDUCE", "FILTER", "FOR", "IN", "UNTIL", "WHERE", "THEN",
            "LINES", "LINE_EXISTS", "LINE_INDEX",
            "XSDBOOL", "BOOLC",
            "READ-ONLY", "REDEFINITION",
        };
        for (String kw : kws) {
            ABAP_KEYWORDS.add(kw.toUpperCase());
        }
    }

    // ─── Lexer State ─────────────────────────────────────────────
    private final String source;
    private int pos;
    private int line;
    private int col;
    private final List<Token> tokens;

    public AbapLexer(String source) {
        this.source = source;
        this.pos = 0;
        this.line = 1;
        this.col = 0;
        this.tokens = new ArrayList<>();
    }

    // ─── Main Entry Point ────────────────────────────────────────

    /**
     * Tokenize the entire source and return a linked list of tokens.
     */
    public List<Token> tokenize() {
        while (pos < source.length()) {
            char c = peek();

            if (c == '\n') {
                addToken(Token.Type.NEWLINE, "\n");
                advance();
                line++;
                col = 0;
                continue;
            }

            if (c == '\r') {
                advance();
                if (peek() == '\n') advance();
                addToken(Token.Type.NEWLINE, "\n");
                line++;
                col = 0;
                continue;
            }

            // Whitespace
            if (c == ' ' || c == '\t') {
                readWhitespace();
                continue;
            }

            // Full-line comment (* at column 0 or after only whitespace)
            if (c == '*' && isStartOfLine()) {
                readFullLineComment();
                continue;
            }

            // Inline comment
            if (c == '"') {
                readInlineComment();
                continue;
            }

            // Pragmas ##...
            if (c == '#' && peekAt(1) == '#') {
                readPragma();
                continue;
            }

            // String literal 'text'
            if (c == '\'') {
                readStringLiteral();
                continue;
            }

            // String template |...|
            if (c == '|') {
                readStringTemplate();
                continue;
            }

            // Field symbol <fs>
            if (c == '<' && isFieldSymbolStart()) {
                readFieldSymbol();
                continue;
            }

            // Period (statement end)
            if (c == '.') {
                addToken(Token.Type.PERIOD, ".");
                advance();
                continue;
            }

            // Colon (chain)
            if (c == ':') {
                addToken(Token.Type.COLON, ":");
                advance();
                continue;
            }

            // Comma (chain separator)
            if (c == ',') {
                addToken(Token.Type.COMMA, ",");
                advance();
                continue;
            }

            // Parentheses
            if (c == '(' || c == ')') {
                addToken(Token.Type.PUNCTUATION, String.valueOf(c));
                advance();
                continue;
            }

            // Multi-char operators
            if (isOperatorChar(c)) {
                readOperator();
                continue;
            }

            // Numbers
            if (Character.isDigit(c)) {
                readNumber();
                continue;
            }

            // Identifiers / Keywords
            if (isIdentifierStart(c)) {
                readIdentifierOrKeyword();
                continue;
            }

            // Unknown character — skip
            addToken(Token.Type.UNKNOWN, String.valueOf(c));
            advance();
        }

        // Link tokens into a doubly-linked list
        for (int i = 1; i < tokens.size(); i++) {
            tokens.get(i).linkAfter(tokens.get(i - 1));
        }

        return tokens;
    }

    // ─── Token Reading Methods ───────────────────────────────────

    private void readWhitespace() {
        int start = pos;
        while (pos < source.length() && (peek() == ' ' || peek() == '\t')) {
            advance();
        }
        addTokenAt(Token.Type.WHITESPACE, source.substring(start, pos), start);
    }

    private void readFullLineComment() {
        int start = pos;
        while (pos < source.length() && peek() != '\n' && peek() != '\r') {
            advance();
        }
        addTokenAt(Token.Type.COMMENT_FULL, source.substring(start, pos), start);
    }

    private void readInlineComment() {
        int start = pos;
        // Check for pseudo-comment "#EC
        if (pos + 3 < source.length() && source.substring(pos, pos + 3).equals("\"#E")) {
            while (pos < source.length() && peek() != '\n' && peek() != '\r') {
                advance();
            }
            addTokenAt(Token.Type.PSEUDO_COMMENT, source.substring(start, pos), start);
            return;
        }
        while (pos < source.length() && peek() != '\n' && peek() != '\r') {
            advance();
        }
        addTokenAt(Token.Type.COMMENT_LINE, source.substring(start, pos), start);
    }

    private void readPragma() {
        int start = pos;
        advance(); // #
        advance(); // #
        while (pos < source.length() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            advance();
        }
        addTokenAt(Token.Type.PRAGMA, source.substring(start, pos), start);
    }

    private void readStringLiteral() {
        int start = pos;
        advance(); // opening '
        while (pos < source.length()) {
            if (peek() == '\'') {
                advance();
                if (pos < source.length() && peek() == '\'') {
                    advance(); // escaped ''
                } else {
                    break; // end of string
                }
            } else {
                advance();
            }
        }
        addTokenAt(Token.Type.LITERAL_STRING, source.substring(start, pos), start);
    }

    private void readStringTemplate() {
        int start = pos;
        advance(); // opening |
        int braceDepth = 0;
        while (pos < source.length()) {
            char c = peek();
            if (c == '{') braceDepth++;
            if (c == '}') braceDepth--;
            if (c == '|' && braceDepth <= 0) {
                advance();
                break;
            }
            if (c == '\\') { advance(); advance(); continue; } // escape
            advance();
        }
        addTokenAt(Token.Type.STRING_TEMPLATE, source.substring(start, pos), start);
    }

    private void readFieldSymbol() {
        int start = pos;
        advance(); // <
        while (pos < source.length() && peek() != '>') {
            advance();
        }
        if (pos < source.length()) advance(); // >
        addTokenAt(Token.Type.FIELD_SYMBOL, source.substring(start, pos), start);
    }

    private void readOperator() {
        int start = pos;
        char c = peek();

        // Two-char operators
        if (pos + 1 < source.length()) {
            String two = source.substring(pos, pos + 2);
            if (two.equals("=>") || two.equals("->") || two.equals("<>") ||
                two.equals(">=") || two.equals("<=") || two.equals("+=") ||
                two.equals("-=") || two.equals("*=") || two.equals("/=") ||
                two.equals("&&") || two.equals("?=")) {
                advance();
                advance();
                addTokenAt(Token.Type.OPERATOR, two, start);
                return;
            }
        }
        // Single-char operators
        addToken(Token.Type.OPERATOR, String.valueOf(c));
        advance();
    }

    private void readNumber() {
        int start = pos;
        while (pos < source.length() && (Character.isDigit(peek()) || peek() == '.')) {
            advance();
        }
        addTokenAt(Token.Type.LITERAL_NUMBER, source.substring(start, pos), start);
    }

    private void readIdentifierOrKeyword() {
        int start = pos;
        while (pos < source.length() && isIdentifierPart(peek())) {
            advance();
        }
        String word = source.substring(start, pos);
        Token.Type type = ABAP_KEYWORDS.contains(word.toUpperCase())
            ? Token.Type.KEYWORD
            : Token.Type.IDENTIFIER;
        addTokenAt(type, word, start);
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private char peek() {
        return pos < source.length() ? source.charAt(pos) : '\0';
    }

    private char peekAt(int ahead) {
        int idx = pos + ahead;
        return idx < source.length() ? source.charAt(idx) : '\0';
    }

    private void advance() {
        pos++;
        col++;
    }

    private boolean isStartOfLine() {
        // Check if only whitespace precedes on this line
        for (int i = pos - 1; i >= 0; i--) {
            char c = source.charAt(i);
            if (c == '\n' || c == '\r') return true;
            if (c != ' ' && c != '\t') return false;
        }
        return true; // start of file
    }

    private boolean isFieldSymbolStart() {
        // Look ahead for closing > with valid identifier chars
        for (int i = pos + 1; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '>') return true;
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') return false;
        }
        return false;
    }

    private boolean isOperatorChar(char c) {
        return "=<>+-*/&?@".indexOf(c) >= 0;
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '/' || c == '%';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '/' || c == '~' || c == '%';
    }

    private void addToken(Token.Type type, String text) {
        tokens.add(new Token(type, text, line, col, pos));
    }

    private void addTokenAt(Token.Type type, String text, int startPos) {
        tokens.add(new Token(type, text, line, col - (pos - startPos), startPos));
    }
}
