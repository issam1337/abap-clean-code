# ABAP Clean Code Tool

An Eclipse ADT plugin for automated ABAP code cleanup, based on the
[SAP Clean ABAP Styleguide](https://github.com/SAP/styleguides/blob/main/clean-abap/CleanABAP.md).
Inspired by [SAP's ABAP Cleaner](https://github.com/SAP/abap-cleaner).

---

## Overview

ABAP Clean Code Tool applies **40+ cleanup rules** to your ABAP code with a single keystroke.
Rules are organized into configurable **profiles** and cover syntax modernization, declaration cleanup,
naming conventions, formatting, comments, and more.

### Key Features

- **Automated cleanup** — Ctrl+4 applies all active rules instantly
- **Interactive review** — Ctrl+Shift+4 opens a side-by-side diff for change-by-change approval
- **Read-only preview** — Ctrl+Shift+5 previews changes without locking the code
- **Configurable profiles** — Essential (~40%), Default, Full, or Custom profiles
- **Team sharing** — Export/import profiles as `.profile` files
- **ABAP release awareness** — Rules auto-skip on older NetWeaver versions

---

## Architecture

```
abap-clean-code-tool/
│
├── com.cleanabap.core/              ← Core engine (pure Java, no Eclipse deps)
│   └── src/com/cleanabap/core/
│       ├── parser/                  ← ABAP lexer, parser, token & statement model
│       │   ├── Token.java           ← Atomic token with type, text, line/col, linked list
│       │   ├── AbapLexer.java       ← Tokenizer: source → Token stream
│       │   ├── AbapStatement.java   ← Statement: grouped tokens, block tree
│       │   └── AbapParser.java      ← Parser: tokens → statements, chain resolution
│       │
│       ├── programbase/
│       │   └── CodeDocument.java    ← Top-level container: source → parsed document
│       │
│       ├── rulebase/                ← Rule framework
│       │   ├── Rule.java            ← Abstract base class for all rules
│       │   ├── RuleForStatements.java ← Base for statement-level rules
│       │   ├── RuleID.java          ← Enum of all rule identifiers
│       │   ├── RuleCategory.java    ← Rule categories (Syntax, Declarations, etc.)
│       │   ├── RuleSeverity.java    ← Error / Warning / Info / Hint
│       │   ├── RuleSource.java      ← Reference sources (Styleguide, Code Pal, etc.)
│       │   ├── RuleReference.java   ← Link to specific guideline section
│       │   ├── RuleRegistry.java    ← Central registry, rule ordering, queries
│       │   ├── ConfigValue.java     ← Typed configuration option for rules
│       │   ├── CleanupResult.java   ← Changes + findings from one rule execution
│       │   └── CleanupFinding.java  ← Analysis-only finding (no auto-fix)
│       │
│       ├── rules/                   ← Concrete rule implementations
│       │   ├── syntax/
│       │   │   ├── PreferNewToCreateObjectRule.java
│       │   │   ├── PreferComparisonOperatorsRule.java
│       │   │   └── ReplaceObsoleteAddRule.java
│       │   ├── declarations/
│       │   │   └── UnchainDataDeclarationsRule.java
│       │   ├── comments/
│       │   │   └── ConvertStarCommentsRule.java
│       │   ├── commands/            ← (TODO: SimplifyIfReturn, MaxNesting, etc.)
│       │   ├── formatting/          ← (TODO: LineLengthRule, BracketRule, etc.)
│       │   ├── naming/              ← (TODO: HungarianNotation analysis)
│       │   ├── alignment/           ← (TODO: AlignParameters, AlignDeclarations)
│       │   └── emptylines/          ← (TODO: TrailingSpaces, EmptyLines, etc.)
│       │
│       ├── profiles/
│       │   └── CleanupProfile.java  ← Profile: Essential/Default/Full/Custom
│       │
│       └── config/
│           ├── CleanupEngine.java   ← Main orchestrator: profile + doc → session
│           └── CleanupConfig.java   ← Global settings
│
├── com.cleanabap.plugin/            ← Eclipse ADT plugin (SWT UI)
│   ├── META-INF/MANIFEST.MF         ← OSGi bundle with ADT dependencies
│   ├── plugin.xml                   ← Commands, handlers, menus, keybindings
│   └── src/com/cleanabap/plugin/
│       ├── Activator.java
│       ├── ui/
│       │   ├── handlers/
│       │   │   ├── CleanAutomatedHandler.java    ← Ctrl+4
│       │   │   ├── CleanInteractiveHandler.java  ← Ctrl+Shift+4
│       │   │   ├── CleanPreviewHandler.java      ← Ctrl+Shift+5
│       │   │   ├── ConfigureProfilesHandler.java
│       │   │   └── CleanupHandlerUtil.java       ← Shared utilities
│       │   └── dialogs/
│       │       ├── InteractiveCleanupDialog.java  ← Side-by-side diff review
│       │       ├── ProfileConfigDialog.java       ← Profile & rules configuration
│       │       ├── CleanupPreferencePage.java     ← Preferences main page
│       │       └── ProfilePreferencePage.java     ← Preferences sub-page
│       └── integration/
│           ├── EditorHelper.java     ← ADT editor interaction utilities
│           └── ProfileManager.java   ← Profile loading, saving, switching
│
├── com.cleanabap.test/              ← JUnit 5 tests
│   └── src/com/cleanabap/test/
│       ├── parser/
│       │   └── AbapLexerTest.java
│       └── rules/
│           ├── PreferNewToCreateObjectRuleTest.java
│           └── CleanupEngineIntegrationTest.java
│
├── releng/
│   ├── com.cleanabap.feature/
│   │   └── feature.xml              ← Eclipse feature definition
│   └── com.cleanabap.updatesite/    ← P2 update site for distribution
│
└── pom.xml                          ← Maven/Tycho parent POM
```

---

## Rule Catalog

### Syntax Modernization (Essential)
| Rule | Before | After |
|------|--------|-------|
| Prefer NEW to CREATE OBJECT | `CREATE OBJECT lo TYPE zcl.` | `lo = NEW zcl( ).` |
| Prefer modern comparison ops | `IF x GT 0.` | `IF x > 0.` |
| Replace obsolete ADD/SUBTRACT | `ADD 1 TO x.` | `x += 1.` |
| Replace MOVE TO | `MOVE a TO b.` | `b = a.` |
| Replace CALL METHOD | `CALL METHOD o->m.` | `o->m( ).` |
| Prefer IS NOT to NOT IS | `NOT x IS INITIAL` | `x IS NOT INITIAL` |
| Prefer abap_true/false | `= 'X'.` | `= abap_true.` |
| Omit optional EXPORTING | `o->m( EXPORTING x = 1 )` | `o->m( x = 1 )` |
| Replace DESCRIBE TABLE | `DESCRIBE TABLE t LINES n.` | `n = lines( t ).` |
| Prefer pragmas | `"#EC NOTEXT` | `##NO_TEXT` |

### Syntax Modernization (Optional)
| Rule | Before | After |
|------|--------|-------|
| Prefer VALUE to CLEAR | `CLEAR ls.` | `ls = VALUE #( ).` |
| Replace TRANSLATE | `TRANSLATE x TO UPPER CASE.` | `x = to_upper( x ).` |
| Prefer string templates | `CONCATENATE a b INTO c.` | `c = \|{ a }{ b }\|.` |
| Prefer xsdbool | `IF cond. r = abap_true. ELSE...` | `r = xsdbool( cond ).` |

### Declarations
| Rule | Description |
|------|-------------|
| Unchain DATA | Split `DATA: a, b, c.` into individual declarations |
| Unchain TYPES/CONSTANTS | Same for TYPES: and CONSTANTS: chains |
| Prefer inline DATA (hint) | Flag standalone DATA that can be inlined |
| Prefer FINAL (hint) | Flag immutable variables for FINAL( ) |

### Comments
| Rule | Description |
|------|-------------|
| Convert * to " | Replace full-line `*` comments with `"` |
| Flag commented-out code | Detect commented ABAP statements |
| Remove end-of comments | Remove `" ENDMETHOD` type comments |

### Formatting & Spacing
| Rule | Description |
|------|-------------|
| Remove trailing whitespace | Strip trailing spaces |
| Standardize empty lines | Max 1 blank line between statements |
| Move closing brackets | Close `)` on previous line |
| Move AND/OR to line start | Boolean operators start the continuation line |
| Flag >120 char lines | Flag lines exceeding maximum width |

### Analysis Hints (No Auto-Fix)
| Rule | Description |
|------|-------------|
| Avoid Hungarian notation | Flag `lv_`, `lt_`, `ls_` prefixed names |
| Flag deep nesting | Nesting > 5 levels |
| Flag large methods | Methods > 30 statements |
| Prefer specific exceptions | Flag `CATCH cx_root` |

---

## How to Add a New Rule

1. **Add the RuleID** to `com.cleanabap.core.rulebase.RuleID`:
   ```java
   MY_NEW_RULE,
   ```

2. **Create the rule class** in the appropriate `rules/` sub-package:
   ```java
   public class MyNewRule extends RuleForStatements {
       private static final RuleReference[] REFERENCES = {
           new RuleReference(RuleSource.ABAP_STYLE_GUIDE,
               "Guideline Title", "#anchor-link"),
       };

       @Override public RuleID getID()       { return RuleID.MY_NEW_RULE; }
       @Override public String getName()     { return "My New Rule"; }
       @Override public String getDescription() { return "What it does."; }
       @Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
       @Override public boolean isEssential() { return true; }
       @Override public RuleReference[] getReferences() { return REFERENCES; }

       @Override
       protected void processStatement(AbapStatement stmt,
                                        CodeDocument doc,
                                        CleanupResult result) {
           // Detection and transformation logic
       }
   }
   ```

3. **Register** in `RuleRegistry.registerAllRules()`:
   ```java
   register(new MyNewRule());
   ```

4. **Write tests** in `com.cleanabap.test`:
   ```java
   @Test void testMyNewRule() {
       // input → expected output
   }
   ```

---

## Building

### Prerequisites
- Java 17+
- Maven 3.8+
- Eclipse 2024-03+ with ADT (for plugin testing)

### Build Commands

```bash
# Build all modules
mvn clean verify

# Build only the core engine (no Eclipse deps)
cd com.cleanabap.core && mvn clean package

# Run tests
mvn test

# Build the Eclipse update site
mvn clean verify -pl releng/com.cleanabap.updatesite
```

### Installing in ADT

1. Build the update site: `mvn clean verify`
2. In Eclipse ADT: Help → Install New Software...
3. Add local update site: `releng/com.cleanabap.updatesite/target/repository`
4. Select "ABAP Clean Code Tool" and install

---

## Design Principles

Following the architecture of SAP's ABAP Cleaner:

1. **No backend calls** — all cleanup is local, based on the source text only
2. **Functionality unchanged** — rules must not alter program behavior
3. **Configurable** — every rule can be toggled; many have fine-tuning options
4. **Non-destructive** — interactive mode lets users reject individual changes
5. **Ordered execution** — rules run in a specific sequence (declarations → syntax → formatting → alignment)
6. **Release-aware** — rules can declare a minimum ABAP release requirement

---

## License

Apache License, Version 2.0

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on adding rules,
reporting issues, and submitting pull requests.
