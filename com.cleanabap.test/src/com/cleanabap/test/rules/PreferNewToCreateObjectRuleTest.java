package com.cleanabap.test.rules;

import com.cleanabap.core.config.CleanupEngine;
import com.cleanabap.core.config.CleanupEngine.CleanupSession;
import com.cleanabap.core.profiles.CleanupProfile;
import com.cleanabap.core.programbase.CodeDocument;
import com.cleanabap.core.rulebase.*;
import com.cleanabap.core.rules.syntax.PreferNewToCreateObjectRule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PreferNewToCreateObjectRule}.
 *
 * <p>Each test follows the pattern:
 * <ol>
 *   <li>Define ABAP source with the anti-pattern</li>
 *   <li>Run the rule</li>
 *   <li>Assert the cleaned output matches the expected modern syntax</li>
 * </ol>
 */
class PreferNewToCreateObjectRuleTest {

    private PreferNewToCreateObjectRule rule;

    @BeforeEach
    void setUp() {
        rule = new PreferNewToCreateObjectRule();
    }

    @Test
    @DisplayName("CREATE OBJECT ref TYPE class → ref = NEW class( )")
    void testCreateObjectWithType() {
        String input = "    CREATE OBJECT lo_handler TYPE zcl_handler.";
        String expected = "    lo_handler = NEW zcl_handler( ).";

        CodeDocument doc = new CodeDocument(input);
        CleanupResult result = rule.apply(doc);

        assertTrue(result.isSourceModified(), "Source should be modified");
        assertTrue(result.getChangeCount() > 0, "Should report at least one change");
        assertTrue(doc.getCurrentSource().contains("NEW zcl_handler"),
            "Should contain NEW syntax");
    }

    @Test
    @DisplayName("CREATE OBJECT ref (no TYPE) → ref = NEW #( )")
    void testCreateObjectWithoutType() {
        String input = "    CREATE OBJECT lo_util.";
        String expected = "    lo_util = NEW #( ).";

        CodeDocument doc = new CodeDocument(input);
        CleanupResult result = rule.apply(doc);

        assertTrue(result.isSourceModified());
        assertTrue(doc.getCurrentSource().contains("NEW #("),
            "Should use NEW #( ) when TYPE is omitted");
    }

    @Test
    @DisplayName("CREATE OBJECT with EXPORTING should not be transformed")
    void testCreateObjectWithExportingSkipped() {
        String input = "    CREATE OBJECT lo_handler TYPE zcl_handler\n"
                      + "      EXPORTING iv_id = lv_id.";

        CodeDocument doc = new CodeDocument(input);
        CleanupResult result = rule.apply(doc);

        // Complex CREATE OBJECT with EXPORTING should be left unchanged
        // (requires more sophisticated parameter rewriting)
        assertEquals(0, result.getChangeCount(),
            "Should not transform CREATE OBJECT with EXPORTING");
    }

    @Test
    @DisplayName("Non-CREATE statements should be ignored")
    void testNonCreateStatementIgnored() {
        String input = "    DATA lo_handler TYPE REF TO zcl_handler.";

        CodeDocument doc = new CodeDocument(input);
        CleanupResult result = rule.apply(doc);

        assertFalse(result.isSourceModified());
        assertEquals(0, result.getChangeCount());
    }

    @Test
    @DisplayName("Rule metadata is correct")
    void testRuleMetadata() {
        assertEquals(RuleID.PREFER_NEW_TO_CREATE_OBJECT, rule.getID());
        assertEquals(RuleCategory.SYNTAX, rule.getCategory());
        assertEquals(RuleSeverity.WARNING, rule.getSeverity());
        assertTrue(rule.isEssential());
        assertTrue(rule.getReferences().length > 0);
        assertFalse(rule.getDescription().isEmpty());
        assertFalse(rule.getExampleBefore().isEmpty());
        assertFalse(rule.getExampleAfter().isEmpty());
    }
}
