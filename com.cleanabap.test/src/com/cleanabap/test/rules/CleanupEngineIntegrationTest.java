package com.cleanabap.test.rules;

import com.cleanabap.core.config.CleanupEngine;
import com.cleanabap.core.config.CleanupEngine.CleanupSession;
import com.cleanabap.core.profiles.CleanupProfile;
import com.cleanabap.core.rulebase.RuleID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for the {@link CleanupEngine}.
 * Tests multiple rules applied together to realistic ABAP code.
 */
class CleanupEngineIntegrationTest {

    private CleanupEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CleanupEngine();
    }

    @Test
    @DisplayName("Full cleanup with Default profile transforms sample code")
    void testFullCleanupDefaultProfile() {
        String source = ""
            + "CLASS zcl_demo IMPLEMENTATION.\n"
            + "  METHOD main.\n"
            + "    DATA lv_count TYPE i.\n"
            + "    DATA lo_handler TYPE REF TO zcl_handler.\n"
            + "\n"
            + "    CREATE OBJECT lo_handler.\n"
            + "    ADD 1 TO lv_count.\n"
            + "    IF lv_count GT 0.\n"
            + "      WRITE lv_count.\n"
            + "    ENDIF.\n"
            + "  ENDMETHOD.\n"
            + "ENDCLASS.\n";

        CleanupProfile profile = CleanupProfile.createDefault();
        CleanupSession session = engine.clean(source, profile);

        assertTrue(session.isModified(), "Code should be modified");
        assertTrue(session.getAppliedRuleCount() > 0, "At least one rule should apply");
        assertTrue(session.getElapsedMs() >= 0, "Elapsed time should be non-negative");

        String cleaned = session.getCleanedSource();

        // Verify specific transformations
        assertFalse(cleaned.contains("CREATE OBJECT"),
            "CREATE OBJECT should be replaced");
        assertFalse(cleaned.contains("ADD 1 TO"),
            "ADD ... TO should be replaced with +=");
        assertFalse(cleaned.toUpperCase().contains(" GT "),
            "GT should be replaced with >");

        // Verify positive expectations
        assertTrue(cleaned.contains("NEW #(") || cleaned.contains("NEW zcl_handler"),
            "Should use NEW syntax");
        assertTrue(cleaned.contains("+="),
            "Should use += operator");

        // Print summary for debugging
        System.out.println(session.getSummary());
    }

    @Test
    @DisplayName("Essential profile applies fewer rules than Full")
    void testEssentialVsFullProfile() {
        String source = ""
            + "* This is a full-line comment\n"
            + "  METHOD demo.\n"
            + "    DATA lv_x TYPE i.\n"
            + "    CREATE OBJECT lo_obj.\n"
            + "    ADD 1 TO lv_x.\n"
            + "    IF lv_x GT 0.\n"
            + "      WRITE lv_x.\n"
            + "    ENDIF.\n"
            + "  ENDMETHOD.\n";

        CleanupProfile essential = CleanupProfile.createEssential();
        CleanupProfile full = CleanupProfile.createFull();

        CleanupSession essentialSession = engine.clean(source, essential);
        CleanupSession fullSession = engine.clean(source, full);

        assertTrue(fullSession.getAppliedRuleCount() >= essentialSession.getAppliedRuleCount(),
            "Full profile should apply at least as many rules as Essential");

        // Essential should NOT convert * comments (it's not an essential rule)
        // Full SHOULD convert * comments
        String essentialCleaned = essentialSession.getCleanedSource();
        String fullCleaned = fullSession.getCleanedSource();

        // Both should fix CREATE OBJECT (it's essential)
        assertFalse(essentialCleaned.contains("CREATE OBJECT"),
            "Essential should fix CREATE OBJECT");
        assertFalse(fullCleaned.contains("CREATE OBJECT"),
            "Full should fix CREATE OBJECT");
    }

    @Test
    @DisplayName("Empty source produces no changes")
    void testEmptySource() {
        CleanupProfile profile = CleanupProfile.createDefault();
        CleanupSession session = engine.clean("", profile);

        assertFalse(session.isModified());
        assertEquals(0, session.getAppliedRuleCount());
        assertEquals(0, session.getAllFindings().size());
    }

    @Test
    @DisplayName("Already-clean code produces no changes")
    void testAlreadyCleanCode() {
        String cleanSource = ""
            + "  METHOD demo.\n"
            + "    DATA(lo_obj) = NEW zcl_handler( ).\n"
            + "    lv_count += 1.\n"
            + "    IF lv_count > 0.\n"
            + "      WRITE lv_count.\n"
            + "    ENDIF.\n"
            + "  ENDMETHOD.\n";

        CleanupProfile profile = CleanupProfile.createDefault();
        CleanupSession session = engine.clean(cleanSource, profile);

        assertEquals(0, session.getAppliedRuleCount(),
            "No rules should apply to already-clean code");
    }

    @Test
    @DisplayName("Cleanup with ABAP release restriction")
    void testAbapReleaseRestriction() {
        String source = "    CREATE OBJECT lo_handler.\n";

        CleanupProfile profile = CleanupProfile.createFull();

        // With high release → should apply
        CleanupEngine.CleanupOptions opts = new CleanupEngine.CleanupOptions()
            .setAbapRelease(757);
        CleanupSession session = engine.clean(source, profile, opts);
        assertTrue(session.isModified(),
            "Should apply rules for ABAP 7.57");

        // With very low release → some rules may be skipped
        CleanupEngine.CleanupOptions lowOpts = new CleanupEngine.CleanupOptions()
            .setAbapRelease(700);
        CleanupSession lowSession = engine.clean(source, profile, lowOpts);
        // Rules requiring > 7.00 should still apply since CREATE OBJECT → NEW needs 7.40
        // This depends on individual rule minAbapRelease settings
    }

    @Test
    @DisplayName("Custom profile with specific rules disabled")
    void testCustomProfileDisabledRules() {
        String source = ""
            + "    CREATE OBJECT lo_handler.\n"
            + "    ADD 1 TO lv_count.\n"
            + "    IF lv_count GT 0.\n"
            + "    ENDIF.\n";

        // Create profile with only comparison operators rule active
        CleanupProfile custom = CleanupProfile.createCustom("test",
            CleanupProfile.createFull());
        // Disable all rules
        for (RuleID id : RuleID.values()) {
            custom.setRuleActive(id, false);
        }
        // Enable only comparison operators
        custom.setRuleActive(RuleID.PREFER_COMPARISON_OPERATORS, true);

        CleanupSession session = engine.clean(source, custom);

        String cleaned = session.getCleanedSource();

        // GT should be replaced
        assertFalse(cleaned.toUpperCase().contains(" GT "),
            "GT should be replaced");

        // CREATE OBJECT should NOT be replaced (rule disabled)
        assertTrue(cleaned.contains("CREATE OBJECT"),
            "CREATE OBJECT should remain (rule disabled)");

        // ADD should NOT be replaced (rule disabled)
        assertTrue(cleaned.contains("ADD 1 TO"),
            "ADD should remain (rule disabled)");
    }

    @Test
    @DisplayName("Session provides correct statistics")
    void testSessionStatistics() {
        String source = ""
            + "    CREATE OBJECT lo_handler.\n"
            + "    ADD 1 TO lv_count.\n";

        CleanupProfile profile = CleanupProfile.createFull();
        CleanupSession session = engine.clean(source, profile);

        assertTrue(session.getTotalActiveRules() > 0);
        assertNotNull(session.getSummary());
        assertFalse(session.getSummary().isEmpty());
        assertNotNull(session.getCleanedSource());
        assertNotNull(session.getOriginalSource());
        assertEquals(source, session.getOriginalSource());
    }

    @Test
    @DisplayName("Multiple transformations on same code preserve correctness")
    void testMultipleTransformationsChained() {
        String source = ""
            + "* header comment\n"
            + "  METHOD demo.\n"
            + "    DATA lv_count TYPE i.\n"
            + "    CREATE OBJECT lo_obj.\n"
            + "    ADD 1 TO lv_count.\n"
            + "    SUBTRACT 2 FROM lv_count.\n"
            + "    MULTIPLY lv_count BY 3.\n"
            + "    DIVIDE lv_count BY 4.\n"
            + "    IF lv_count EQ 0.\n"
            + "      WRITE 'zero'.\n"
            + "    ENDIF.\n"
            + "  ENDMETHOD.\n";

        CleanupProfile profile = CleanupProfile.createFull();
        CleanupSession session = engine.clean(source, profile);

        String cleaned = session.getCleanedSource();

        // All obsolete arithmetic should be replaced
        assertFalse(cleaned.contains("ADD "), "ADD should be replaced");
        assertFalse(cleaned.contains("SUBTRACT "), "SUBTRACT should be replaced");
        assertFalse(cleaned.contains("MULTIPLY "), "MULTIPLY should be replaced");
        assertFalse(cleaned.contains("DIVIDE "), "DIVIDE should be replaced");

        // Should contain modern operators
        assertTrue(cleaned.contains("+="), "Should have +=");
        assertTrue(cleaned.contains("-="), "Should have -=");
        assertTrue(cleaned.contains("*="), "Should have *=");
        assertTrue(cleaned.contains("/="), "Should have /=");

        System.out.println("=== Cleaned Output ===");
        System.out.println(cleaned);
        System.out.println("======================");
        System.out.println(session.getSummary());
    }
}
