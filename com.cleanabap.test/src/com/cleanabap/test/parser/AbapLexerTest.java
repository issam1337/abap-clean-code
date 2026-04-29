package com.cleanabap.test.parser;

import com.cleanabap.core.parser.AbapLexer;
import com.cleanabap.core.parser.Token;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link AbapLexer} — ABAP tokenizer.
 */
class AbapLexerTest {

    private List<Token> tokenize(String source) {
        return new AbapLexer(source).tokenize();
    }

    /** Filter out whitespace/newline tokens for easier assertions */
    private List<Token> significant(List<Token> tokens) {
        return tokens.stream()
            .filter(t -> t.getType() != Token.Type.WHITESPACE &&
                         t.getType() != Token.Type.NEWLINE)
            .collect(Collectors.toList());
    }

    // ─── Basic Token Recognition ─────────────────────────────────

    @Test
    @DisplayName("Tokenize simple DATA declaration")
    void testSimpleDataDeclaration() {
        List<Token> tokens = significant(tokenize("DATA lv_name TYPE string."));

        assertEquals(4, tokens.size());
        assertEquals(Token.Type.KEYWORD, tokens.get(0).getType());
        assertEquals("DATA", tokens.get(0).getText());
        assertEquals(Token.Type.IDENTIFIER, tokens.get(1).getType());
        assertEquals("lv_name", tokens.get(1).getText());
        assertEquals(Token.Type.KEYWORD, tokens.get(2).getType());
        assertEquals("TYPE", tokens.get(2).getText());
        // "string" could be keyword or identifier depending on context
        assertTrue(tokens.get(3).isPeriod() || tokens.get(3).isIdentifier()
            || tokens.get(3).isKeyword());
    }

    @Test
    @DisplayName("Tokenize string literal")
    void testStringLiteral() {
        List<Token> tokens = significant(tokenize("lv_text = 'Hello World'."));

        boolean hasString = tokens.stream()
            .anyMatch(t -> t.getType() == Token.Type.LITERAL_STRING
                && t.getText().equals("'Hello World'"));
        assertTrue(hasString, "Should contain string literal token");
    }

    @Test
    @DisplayName("Tokenize string template")
    void testStringTemplate() {
        List<Token> tokens = significant(tokenize("lv_msg = |Count: { lv_count }|."));

        boolean hasTemplate = tokens.stream()
            .anyMatch(t -> t.getType() == Token.Type.STRING_TEMPLATE);
        assertTrue(hasTemplate, "Should contain string template token");
    }

    @Test
    @DisplayName("Tokenize field symbol")
    void testFieldSymbol() {
        List<Token> tokens = significant(tokenize("ASSIGN lt_data[ 1 ] TO <ls_row>."));

        boolean hasFS = tokens.stream()
            .anyMatch(t -> t.getType() == Token.Type.FIELD_SYMBOL
                && t.getText().equals("<ls_row>"));
        assertTrue(hasFS, "Should contain field symbol token");
    }

    @Test
    @DisplayName("Tokenize pragma")
    void testPragma() {
        List<Token> tokens = significant(tokenize("DATA lv_x TYPE i ##NEEDED."));

        boolean hasPragma = tokens.stream()
            .anyMatch(t -> t.getType() == Token.Type.PRAGMA
                && t.getText().equals("##NEEDED"));
        assertTrue(hasPragma, "Should contain pragma token");
    }

    @Test
    @DisplayName("Tokenize full-line comment (* at line start)")
    void testFullLineComment() {
        List<Token> tokens = tokenize("* This is a comment");

        boolean hasComment = tokens.stream()
            .anyMatch(t -> t.getType() == Token.Type.COMMENT_FULL);
        assertTrue(hasComment, "Should contain full-line comment token");
    }

    @Test
    @DisplayName("Tokenize inline comment")
    void testInlineComment() {
        List<Token> tokens = tokenize("DATA lv_x TYPE i. \" counter");

        boolean hasComment = tokens.stream()
            .anyMatch(t -> t.getType() == Token.Type.COMMENT_LINE);
        assertTrue(hasComment, "Should contain inline comment token");
    }

    @Test
    @DisplayName("Tokenize pseudo comment")
    void testPseudoComment() {
        List<Token> tokens = tokenize("DATA lv_x TYPE i. \"#EC NOTEXT");

        boolean hasPseudo = tokens.stream()
            .anyMatch(t -> t.getType() == Token.Type.PSEUDO_COMMENT);
        assertTrue(hasPseudo, "Should contain pseudo comment token");
    }

    // ─── Operators ───────────────────────────────────────────────

    @Test
    @DisplayName("Tokenize two-char operators")
    void testTwoCharOperators() {
        List<Token> tokens = significant(tokenize("lo_obj->method( ). cl=>static( ). a += b. c <> d."));

        List<String> ops = tokens.stream()
            .filter(t -> t.getType() == Token.Type.OPERATOR)
            .map(Token::getText)
            .collect(Collectors.toList());

        assertTrue(ops.contains("->"), "Should contain ->");
        assertTrue(ops.contains("=>"), "Should contain =>");
        assertTrue(ops.contains("+="), "Should contain +=");
        assertTrue(ops.contains("<>"), "Should contain <>");
    }

    // ─── Chain Notation ──────────────────────────────────────────

    @Test
    @DisplayName("Tokenize chain colon and comma")
    void testChainNotation() {
        List<Token> tokens = significant(tokenize("DATA: lv_a TYPE i, lv_b TYPE string."));

        boolean hasColon = tokens.stream().anyMatch(Token::isColon);
        boolean hasComma = tokens.stream().anyMatch(Token::isComma);

        assertTrue(hasColon, "Should contain colon token");
        assertTrue(hasComma, "Should contain comma token");
    }

    // ─── Multi-Line ──────────────────────────────────────────────

    @Test
    @DisplayName("Tokenize multi-line method call")
    void testMultiLineMethodCall() {
        String source = "lo_obj->process(\n"
                       + "  EXPORTING iv_id = lv_id\n"
                       + "  IMPORTING ev_result = lv_result ).";

        List<Token> tokens = tokenize(source);
        assertFalse(tokens.isEmpty());

        // Check line numbers are tracked
        Token lastToken = tokens.get(tokens.size() - 1);
        assertTrue(lastToken.getLine() >= 3, "Last token should be on line 3+");
    }

    // ─── Edge Cases ──────────────────────────────────────────────

    @Test
    @DisplayName("Empty source produces no tokens")
    void testEmptySource() {
        List<Token> tokens = tokenize("");
        assertTrue(tokens.isEmpty());
    }

    @Test
    @DisplayName("Escaped quotes in string literal")
    void testEscapedQuotes() {
        List<Token> tokens = significant(tokenize("lv_s = 'It''s a test'."));

        boolean hasString = tokens.stream()
            .anyMatch(t -> t.getType() == Token.Type.LITERAL_STRING
                && t.getText().contains("''"));
        assertTrue(hasString, "Should handle escaped quotes in strings");
    }

    @Test
    @DisplayName("Token linked list is connected")
    void testTokenLinking() {
        List<Token> tokens = tokenize("DATA lv_x TYPE i.");

        for (int i = 1; i < tokens.size(); i++) {
            assertNotNull(tokens.get(i).getPrev(),
                "Token at index " + i + " should have a prev pointer");
            assertEquals(tokens.get(i - 1), tokens.get(i).getPrev(),
                "Prev pointer should point to preceding token");
        }

        for (int i = 0; i < tokens.size() - 1; i++) {
            assertNotNull(tokens.get(i).getNext(),
                "Token at index " + i + " should have a next pointer");
            assertEquals(tokens.get(i + 1), tokens.get(i).getNext(),
                "Next pointer should point to following token");
        }
    }

    // ─── Number Literals ─────────────────────────────────────────

    @Test
    @DisplayName("Tokenize numeric literals")
    void testNumericLiterals() {
        List<Token> tokens = significant(tokenize("lv_x = 42. lv_y = 3.14."));

        List<Token> numbers = tokens.stream()
            .filter(t -> t.getType() == Token.Type.LITERAL_NUMBER)
            .collect(Collectors.toList());

        assertTrue(numbers.size() >= 2, "Should find at least 2 number tokens");
    }
}
