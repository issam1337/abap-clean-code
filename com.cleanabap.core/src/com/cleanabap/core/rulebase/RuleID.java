package com.cleanabap.core.rulebase;

/**
 * Unique identifier for each cleanup rule.
 * This enum is the canonical list of all rules in the system.
 * Each rule class returns its ID via {@code getID()}.
 */
public enum RuleID {
    // ── Syntax Modernization ─────────────────────────────
    PREFER_NEW_TO_CREATE_OBJECT,
    PREFER_INLINE_DATA,
    PREFER_VALUE_TO_CLEAR,
    PREFER_IS_NOT_TO_NOT_IS,
    PREFER_COMPARISON_OPERATORS,
    PREFER_XSDBOOL,
    PREFER_OPTIONAL_EXPORTING,
    REPLACE_OBSOLETE_ADD,
    REPLACE_MOVE_TO,
    REPLACE_TRANSLATE,
    REPLACE_CALL_METHOD,
    PREFER_STRING_TEMPLATES,
    PREFER_ABAP_BOOL,
    REPLACE_DESCRIBE_TABLE,

    // ── Declarations ─────────────────────────────────────
    UNCHAIN_DATA_DECLARATIONS,
    UNCHAIN_TYPES_DECLARATIONS,
    UNCHAIN_CONSTANTS,
    PREFER_FINAL,
    REMOVE_UNUSED_VARIABLES,

    // ── Naming ───────────────────────────────────────────
    NO_HUNGARIAN_NOTATION,
    DESCRIPTIVE_NAMES,

    // ── Comments ─────────────────────────────────────────
    CONVERT_STAR_COMMENTS,
    REMOVE_COMMENTED_CODE,
    NO_END_OF_COMMENTS,
    PREFER_PRAGMAS,

    // ── Formatting ───────────────────────────────────────
    CLOSING_BRACKETS_AT_LINE_END,
    KEEP_LINE_LENGTH,
    AND_OR_AT_LINE_START,

    // ── Empty Lines & Spaces ─────────────────────────────
    REMOVE_TRAILING_SPACES,
    STANDARDIZE_EMPTY_LINES,
    EMPTY_LINE_AFTER_METHOD,
    REMOVE_NEEDLESS_SPACES,

    // ── Commands ─────────────────────────────────────────
    PREFER_CASE_TO_IF_CHAIN,
    MAX_NESTING_DEPTH,
    SIMPLIFY_IF_RETURN_TO_CHECK,
    PREFER_LINE_EXISTS,
    PREFER_METHOD_SMALL,
    PREFER_CATCHING_SPECIFIC,

    // ── Alignment ────────────────────────────────────────
    ALIGN_PARAMETERS,
    ALIGN_DECLARATIONS,
}
