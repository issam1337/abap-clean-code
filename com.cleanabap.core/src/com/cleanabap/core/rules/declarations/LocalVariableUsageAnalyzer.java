package com.cleanabap.core.rules.declarations;

import com.cleanabap.core.parser.AbapStatement;
import com.cleanabap.core.parser.Token;
import com.cleanabap.core.programbase.CodeDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight, intentionally conservative variable-usage analyzer used by
 * the declarations-phase rules ({@code REMOVE_UNUSED_VARIABLES},
 * {@code PREFER_FINAL}, {@code PREFER_INLINE_DATA}).
 *
 * <p>The analyzer walks the document a single time and collects, for each
 * unchained {@code DATA name [...]} declaration, the count and locations
 * of subsequent references to that name. Rules then read these counts to
 * decide whether to emit a finding.</p>
 *
 * <h3>Design choices (v1):</h3>
 * <ul>
 *   <li>Treats the whole document as one scope. To avoid false positives in
 *       multi-method files, names that appear in more than one DATA
 *       declaration are skipped from analysis altogether.</li>
 *   <li>Considers only single-name {@code DATA} statements (chained
 *       declarations are skipped — they're handled by
 *       {@link UnchainDataDeclarationsRule}).</li>
 *   <li>Skips {@code CLASS-DATA}, {@code STATICS} and {@code FIELD-SYMBOLS}
 *       declarations.</li>
 *   <li>An "assignment" is any statement whose first non-whitespace token
 *       is the variable identifier and whose second non-whitespace,
 *       non-newline token is {@code =}.</li>
 * </ul>
 */
public class LocalVariableUsageAnalyzer {

    /** Per-variable usage record. */
    public static class VarInfo {
        public final String name;
        public final AbapStatement declStmt;
        /** Total non-declaration token occurrences of the name. */
        public int totalReferences;
        /** Statements where the name is the LHS of an assignment (writes). */
        public final List<AbapStatement> writeStmts = new ArrayList<>();
        /** Statements where the name appears NOT as an assignment LHS (reads). */
        public final List<AbapStatement> readStmts = new ArrayList<>();

        public VarInfo(String name, AbapStatement declStmt) {
            this.name = name;
            this.declStmt = declStmt;
        }
    }

    /**
     * Analyze the document and return a map {@code lowercase-name -> VarInfo}
     * for every uniquely-declared local variable.
     */
    public static Map<String, VarInfo> analyze(CodeDocument doc) {
        List<AbapStatement> stmts = doc.getStatements();

        // Pass 1: collect single-DATA declarations.
        Map<String, VarInfo> infos = new HashMap<>();
        Map<String, Integer> nameDeclCount = new HashMap<>();
        for (AbapStatement stmt : stmts) {
            String name = extractDataDeclName(stmt);
            if (name == null) continue;
            String key = name.toLowerCase(Locale.ROOT);
            nameDeclCount.merge(key, 1, Integer::sum);
            // Keep the first decl seen; if a duplicate comes later we'll drop
            // this entry below.
            infos.putIfAbsent(key, new VarInfo(name, stmt));
        }
        // Drop any name with 2+ declarations (likely different scopes / shadowing).
        nameDeclCount.forEach((k, v) -> { if (v > 1) infos.remove(k); });

        if (infos.isEmpty()) return infos;

        // Pass 2: count references in every other statement.
        for (AbapStatement stmt : stmts) {
            // Identify whether this statement is "an assignment to <name>".
            String assignedTo = extractAssignmentTarget(stmt);
            String assignedKey = (assignedTo == null)
                    ? null
                    : assignedTo.toLowerCase(Locale.ROOT);

            for (Token t : stmt.getTokens()) {
                if (t.getType() != Token.Type.IDENTIFIER) continue;
                String key = t.getText().toLowerCase(Locale.ROOT);
                VarInfo info = infos.get(key);
                if (info == null) continue;
                if (stmt == info.declStmt) continue; // skip decl itself

                info.totalReferences++;
                if (assignedKey != null && assignedKey.equals(key)
                        && isFirstIdentifierInStmt(stmt, t)) {
                    if (!info.writeStmts.contains(stmt)) info.writeStmts.add(stmt);
                } else {
                    if (!info.readStmts.contains(stmt)) info.readStmts.add(stmt);
                }
            }
        }

        return infos;
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    /**
     * If {@code stmt} is a single-name {@code DATA name [...]} declaration
     * (no chain colon, no comma), returns the declared identifier text.
     * Otherwise returns null.
     */
    private static String extractDataDeclName(AbapStatement stmt) {
        if (stmt.isChained()) return null;
        Token kw = stmt.getFirstKeyword();
        if (kw == null) return null;
        // Only plain DATA (skip CLASS-DATA, STATICS, FIELD-SYMBOLS, TYPES, CONSTANTS).
        if (!kw.textEqualsIgnoreCase("DATA")) return null;

        // First identifier after the DATA keyword is the variable name.
        boolean afterKeyword = false;
        for (Token t : stmt.getTokens()) {
            if (!afterKeyword) {
                if (t == kw) afterKeyword = true;
                continue;
            }
            if (t.getType() == Token.Type.WHITESPACE
                    || t.getType() == Token.Type.NEWLINE) continue;
            if (t.getType() == Token.Type.IDENTIFIER) return t.getText();
            return null; // unexpected (e.g., DATA( for inline) — bail
        }
        return null;
    }

    /**
     * If {@code stmt} looks like {@code identifier = ... .}, returns the
     * identifier text. Otherwise null. Used to detect assignment targets.
     */
    private static String extractAssignmentTarget(AbapStatement stmt) {
        Token firstIdent = null;
        boolean sawEquals = false;
        for (Token t : stmt.getTokens()) {
            if (t.getType() == Token.Type.WHITESPACE
                    || t.getType() == Token.Type.NEWLINE) continue;
            if (firstIdent == null) {
                if (t.getType() == Token.Type.IDENTIFIER) {
                    firstIdent = t;
                    continue;
                }
                return null; // statement doesn't start with an identifier
            }
            // Token after the first identifier
            if (t.getType() == Token.Type.OPERATOR && "=".equals(t.getText())) {
                sawEquals = true;
            }
            break;
        }
        return (firstIdent != null && sawEquals) ? firstIdent.getText() : null;
    }

    /**
     * True if {@code target} is the first IDENTIFIER token in the statement,
     * i.e. it's the LHS of the assignment.
     */
    private static boolean isFirstIdentifierInStmt(AbapStatement stmt, Token target) {
        for (Token t : stmt.getTokens()) {
            if (t.getType() == Token.Type.IDENTIFIER) return t == target;
        }
        return false;
    }
}
