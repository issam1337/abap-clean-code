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

            List<Token> tokens = stmt.getTokens();

            // Locate the chain colon.
            int colonIdx = -1;
            for (int i = 0; i < tokens.size(); i++) {
                if (tokens.get(i).isColon()) { colonIdx = i; break; }
            }
            if (colonIdx < 0) {
                resolved.add(stmt);
                continue;
            }

            // Find the keyword token immediately before the colon, skipping
            // whitespace, newlines, comments and pragmas. This is the chain
            // keyword (DATA, TYPES, FIELD-SYMBOLS, STATICS, ...).
            int kwIdx = -1;
            for (int i = colonIdx - 1; i >= 0; i--) {
                Token t = tokens.get(i);
                if (t.getType() == Token.Type.WHITESPACE
                        || t.getType() == Token.Type.NEWLINE
                        || t.isComment()
                        || t.isPragma()) {
                    continue;
                }
                kwIdx = i;
                break;
            }
            if (kwIdx < 0) {
                resolved.add(stmt);
                continue;
            }

            Token chainKeyword = tokens.get(kwIdx);

            // Compute the indent of the chain-keyword line so we can repeat
            // it on every unchained line.
            String indent = computeIndent(tokens, kwIdx);

            // Emit any tokens that appear before the chain keyword (file-level
            // comments, blank lines, the leading indent) as their own
            // statement so that their original formatting is preserved.
            if (kwIdx > 0) {
                AbapStatement leading = new AbapStatement();
                for (int i = 0; i < kwIdx; i++) {
                    leading.addToken(tokens.get(i));
                }
                if (leading.getTokenCount() > 0) {
                    resolved.add(leading);
                }
            }

            // Split the post-colon tokens into segments separated by commas.
            List<List<Token>> segments = new ArrayList<>();
            List<Token> currentSegment = new ArrayList<>();
            for (int i = colonIdx + 1; i < tokens.size(); i++) {
                Token t = tokens.get(i);
                if (t.isComma()) {
                    if (!currentSegment.isEmpty()) {
                        segments.add(currentSegment);
                        currentSegment = new ArrayList<>();
                    }
                } else if (t.isPeriod()) {
                    // The terminating period is re-emitted explicitly below.
                } else {
                    currentSegment.add(t);
                }
            }
            if (!currentSegment.isEmpty()) {
                segments.add(currentSegment);
            }

            // Emit one statement per segment.
            boolean isFirst = true;
            for (List<Token> segment : segments) {
                AbapStatement newStmt = new AbapStatement();
                int line = chainKeyword.getLine();

                // The first segment's leading newline + indent are already
                // present in the "leading" statement (or absent because the
                // chain starts at file offset 0). Subsequent segments need
                // their own newline + indent so they don't run together.
                if (!isFirst) {
                    newStmt.addToken(Token.synthetic(
                            Token.Type.NEWLINE, "\n", line));
                    if (!indent.isEmpty()) {
                        newStmt.addToken(Token.synthetic(
                                Token.Type.WHITESPACE, indent, line));
                    }
                }

                newStmt.addToken(Token.synthetic(
                        chainKeyword.getType(), chainKeyword.getText(), line));
                newStmt.addToken(Token.synthetic(
                        Token.Type.WHITESPACE, " ", line));

                // Skip leading whitespace/newlines inside the segment.
                int segStart = 0;
                while (segStart < segment.size()) {
                    Token.Type tt = segment.get(segStart).getType();
                    if (tt == Token.Type.WHITESPACE || tt == Token.Type.NEWLINE) {
                        segStart++;
                    } else {
                        break;
                    }
                }
                for (int i = segStart; i < segment.size(); i++) {
                    Token st = segment.get(i);
                    newStmt.addToken(Token.synthetic(
                            st.getType(), st.getText(), st.getLine()));
                }

                newStmt.addToken(Token.synthetic(
                        Token.Type.PERIOD, ".", line));

                resolved.add(newStmt);
                isFirst = false;
            }
        }

        return resolved;
    }

    /**
     * Compute the indent (run of whitespace) preceding the token at
     * {@code kwIdx} on the same line. Returns "" if the keyword is not
     * preceded only by whitespace on its line.
     */
    private String computeIndent(List<Token> tokens, int kwIdx) {
        StringBuilder indent = new StringBuilder();
        for (int i = kwIdx - 1; i >= 0; i--) {
            Token t = tokens.get(i);
            if (t.getType() == Token.Type.NEWLINE) break;
            if (t.getType() == Token.Type.WHITESPACE) {
                indent.insert(0, t.getText());
            } else {
                return "";
            }
        }
        return indent.toString();
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
