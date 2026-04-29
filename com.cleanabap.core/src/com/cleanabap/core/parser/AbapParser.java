package com.cleanabap.core.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Parses a stream of {@link Token}s into a list of {@link AbapStatement}s,
 * resolving chain notation and building the block structure tree.
 */
public class AbapParser {

    /**
     * Parse raw ABAP source code into a structured list of statements.
     *
     * @param source Raw ABAP source code
     * @return Ordered list of parsed statements
     */
    public List<AbapStatement> parse(String source) {
        AbapLexer lexer = new AbapLexer(source);
        List<Token> allTokens = lexer.tokenize();

        // Phase 1: Group tokens into raw statements (period-delimited)
        List<AbapStatement> rawStatements = groupIntoStatements(allTokens);

        // Phase 2: Resolve chain notation (DATA: a, b, c. → 3 statements)
        List<AbapStatement> resolved = resolveChains(rawStatements);

        // Phase 3: Build block structure (IF...ENDIF nesting)
        buildBlockStructure(resolved);

        // Phase 4: Link prev/next
        for (int i = 0; i < resolved.size(); i++) {
            if (i > 0) resolved.get(i).setPrev(resolved.get(i - 1));
            if (i < resolved.size() - 1) resolved.get(i).setNext(resolved.get(i + 1));
        }

        return resolved;
    }

    // ─── Phase 1: Group by Period ────────────────────────────────

    private List<AbapStatement> groupIntoStatements(List<Token> tokens) {
        List<AbapStatement> statements = new ArrayList<>();
        AbapStatement current = new AbapStatement();

        for (Token token : tokens) {
            current.addToken(token);

            if (token.isPeriod()) {
                statements.add(current);
                current = new AbapStatement();
            }
        }

        // Handle trailing tokens without period
        if (current.getTokenCount() > 0) {
            statements.add(current);
        }

        return statements;
    }

    // ─── Phase 2: Resolve Chains ─────────────────────────────────

    /**
     * Resolve ABAP chain notation:
     *   DATA: lv_a TYPE i, lv_b TYPE string.
     * becomes:
     *   DATA lv_a TYPE i.
     *   DATA lv_b TYPE string.
     */
    private List<AbapStatement> resolveChains(List<AbapStatement> statements) {
        List<AbapStatement> resolved = new ArrayList<>();

        for (AbapStatement stmt : statements) {
            if (!stmt.isChained()) {
                resolved.add(stmt);
                continue;
            }

            // Find the keyword before the colon
            List<Token> prefix = new ArrayList<>();
            List<Token> afterColon = new ArrayList<>();
            boolean foundColon = false;

            for (Token t : stmt.getTokens()) {
                if (t.isColon() && !foundColon) {
                    foundColon = true;
                    continue;
                }
                if (!foundColon) {
                    if (t.getType() != Token.Type.WHITESPACE && t.getType() != Token.Type.NEWLINE) {
                        prefix.add(t);
                    }
                } else {
                    afterColon.add(t);
                }
            }

            if (!foundColon || prefix.isEmpty()) {
                resolved.add(stmt);
                continue;
            }

            // Split by comma
            List<List<Token>> segments = new ArrayList<>();
            List<Token> currentSegment = new ArrayList<>();

            for (Token t : afterColon) {
                if (t.isComma()) {
                    if (!currentSegment.isEmpty()) {
                        segments.add(currentSegment);
                        currentSegment = new ArrayList<>();
                    }
                } else if (t.isPeriod()) {
                    // final segment
                } else {
                    currentSegment.add(t);
                }
            }
            if (!currentSegment.isEmpty()) {
                segments.add(currentSegment);
            }

            // Create one statement per segment
            for (List<Token> segment : segments) {
                AbapStatement newStmt = new AbapStatement();
                // Copy prefix tokens
                for (Token pt : prefix) {
                    Token copy = Token.synthetic(pt.getType(), pt.getText(), pt.getLine());
                    newStmt.addToken(copy);
                }
                // Add space
                newStmt.addToken(Token.synthetic(Token.Type.WHITESPACE, " ", 
                    prefix.get(0).getLine()));
                // Add segment tokens (skip leading whitespace)
                boolean started = false;
                for (Token st : segment) {
                    if (!started && (st.getType() == Token.Type.WHITESPACE || 
                        st.getType() == Token.Type.NEWLINE)) continue;
                    started = true;
                    Token copy = Token.synthetic(st.getType(), st.getText(), st.getLine());
                    newStmt.addToken(copy);
                }
                // Add period
                newStmt.addToken(Token.synthetic(Token.Type.PERIOD, ".", 
                    segment.isEmpty() ? prefix.get(0).getLine() : 
                    segment.get(segment.size() - 1).getLine()));
                
                resolved.add(newStmt);
            }
        }

        return resolved;
    }

    // ─── Phase 3: Build Block Structure ──────────────────────────

    private void buildBlockStructure(List<AbapStatement> statements) {
        Stack<AbapStatement> blockStack = new Stack<>();

        for (AbapStatement stmt : statements) {
            if (stmt.isBlockOpener() || stmt.isMethodStart()) {
                if (!blockStack.isEmpty()) {
                    blockStack.peek().addChild(stmt);
                }
                blockStack.push(stmt);
            } else if (stmt.isBlockCloser()) {
                if (!blockStack.isEmpty()) {
                    blockStack.pop();
                }
                if (!blockStack.isEmpty()) {
                    blockStack.peek().addChild(stmt);
                }
            } else {
                if (!blockStack.isEmpty()) {
                    blockStack.peek().addChild(stmt);
                }
            }
        }
    }
}
