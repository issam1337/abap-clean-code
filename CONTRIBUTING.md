# Contributing to ABAP Clean Code Tool

Thank you for your interest in contributing! This project follows the
architecture established by SAP's ABAP Cleaner and aims to implement
the full SAP Clean ABAP Styleguide as automated rules.

## Areas for Contribution

### 1. Implement New Rules (Most Needed!)

The `RuleRegistry` contains TODO markers for ~30 rules that need implementation.
Priority rules still needed:

**High Priority (Essential rules):**
- `PreferIsNotRule` — `NOT x IS INITIAL` → `x IS NOT INITIAL`
- `ReplaceMoveToRule` — `MOVE a TO b` → `b = a`
- `ReplaceCallMethodRule` — `CALL METHOD o->m` → `o->m( )`
- `PreferAbapBoolRule` — `'X'` / `space` → `abap_true` / `abap_false`
- `ReplaceDescribeTableRule` — `DESCRIBE TABLE t LINES n` → `n = lines( t )`
- `PreferOptionalExportingRule` — omit `EXPORTING` keyword

**Medium Priority:**
- `PreferInlineDataRule` — inline `DATA(...)` declarations
- `ReplaceTranslateRule` — `TRANSLATE TO UPPER/LOWER` → `to_upper/to_lower( )`
- `PreferStringTemplatesRule` — `CONCATENATE` → `|...|` templates
- `RemoveTrailingSpacesRule` / `StandardizeEmptyLinesRule`
- `KeepLineLengthRule` / `ClosingBracketsAtLineEndRule`

**Analysis Rules (hint-only, no auto-fix):**
- `NoHungarianNotationRule` — flag `lv_`, `lt_`, `ls_` prefixes
- `MaxNestingDepthRule` — flag nesting > 5 levels
- `PreferMethodSmallRule` — flag methods > 30 statements
- `RemoveCommentedCodeRule` — detect commented-out ABAP code

### 2. Parser Improvements

The lexer/parser can be extended to handle:
- EML (Entity Manipulation Language) statements
- ABAP SQL (new SELECT syntax)
- CDS annotations
- Inline declarations in complex expressions
- Better chain resolution edge cases

### 3. UI Enhancements

- Synchronized scrolling in diff panels
- Syntax highlighting in the StyledText widgets
- Rule search/filter in the interactive dialog
- Export diff as HTML report
- Dark theme support

### 4. Test Coverage

Every rule should have tests covering:
- Basic transformation (before → after)
- Edge cases (comments in weird places, multi-line statements)
- Non-matching input (rule correctly does nothing)
- Interaction with chain resolution
- Metadata correctness (ID, category, severity, references)

## How to Implement a Rule

Follow the pattern in `PreferNewToCreateObjectRule.java`:

```java
package com.cleanabap.core.rules.syntax;

import com.cleanabap.core.parser.*;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;

public class MyRule extends RuleForStatements {

    private static final RuleReference[] REFERENCES = {
        new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
            "Guideline title from CleanABAP.md",
            "#markdown-anchor"),
    };

    @Override public RuleID getID()       { return RuleID.MY_RULE; }
    @Override public String getName()     { return "Human-readable name"; }
    @Override public String getDescription() {
        return "Detailed description of what this rule does and why.";
    }
    @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
    @Override public RuleSeverity getSeverity()  { return RuleSeverity.WARNING; }
    @Override public boolean isEssential()       { return true; }  // if mandated by styleguide
    @Override public RuleReference[] getReferences() { return REFERENCES; }

    @Override
    public String getExampleBefore() {
        return "    OBSOLETE_SYNTAX arg1 arg2.";
    }

    @Override
    public String getExampleAfter() {
        return "    modern_syntax( arg1 = arg2 ).";
    }

    @Override
    protected void processStatement(AbapStatement stmt,
                                     CodeDocument doc,
                                     CleanupResult result) {
        // 1. Check if this statement matches the pattern
        if (!stmt.startsWithKeyword("OBSOLETE_SYNTAX")) return;

        // 2. Extract relevant tokens
        // 3. Build the replacement text
        // 4. Apply the change to the document
        // 5. Report the change

        result.addChange(stmt.getStartLine(),
            "original text", "replacement text",
            "Description of the change");
    }
}
```

## Code Style

- Java 17, no external dependencies in core
- Follow existing naming conventions
- Javadoc on all public classes and methods
- Each rule in its own file, in the correct sub-package

## Submitting Changes

1. Fork the repository
2. Create a feature branch: `git checkout -b rule/prefer-is-not`
3. Add the rule + tests
4. Run `mvn verify` to ensure all tests pass
5. Submit a pull request with:
   - Description of the rule
   - Link to the Clean ABAP Styleguide section
   - Before/after code examples
