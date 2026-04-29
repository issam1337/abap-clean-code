package com.cleanabap.core.rulebase;

import com.cleanabap.core.config.CleanupConfig;
import com.cleanabap.core.programbase.CodeDocument;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all cleanup rules.
 *
 * <p>Every rule in the ABAP Clean Code Tool extends this class and implements
 * the core {@link #apply(CodeDocument)} method. Rules follow the architecture
 * of SAP's ABAP Cleaner: each rule is self-contained, configurable, and
 * references the Clean ABAP Styleguide.</p>
 *
 * <h3>Rule Lifecycle:</h3>
 * <ol>
 *   <li>The rule is instantiated and registered in {@link RuleRegistry}</li>
 *   <li>Configuration values are loaded from the active {@link CleanupConfig}</li>
 *   <li>{@link #apply(CodeDocument)} is called for each code document</li>
 *   <li>The rule reports changes via {@link CleanupResult}</li>
 * </ol>
 *
 * <h3>Creating a New Rule:</h3>
 * <pre>
 * public class MyNewRule extends Rule {
 *     private static final RuleReference[] references = {
 *         new RuleReference(RuleSource.ABAP_STYLE_GUIDE, "Title", "#anchor")
 *     };
 *
 *     {@literal @}Override public RuleID getID()          { return RuleID.MY_RULE; }
 *     {@literal @}Override public String getName()        { return "My Rule Name"; }
 *     {@literal @}Override public String getDescription() { return "What it does"; }
 *     {@literal @}Override public RuleCategory getCategory() { return RuleCategory.SYNTAX; }
 *
 *     {@literal @}Override
 *     public CleanupResult apply(CodeDocument doc) {
 *         // ... transformation logic ...
 *     }
 * }
 * </pre>
 */
public abstract class Rule {

    // ─── Identity ────────────────────────────────────────────────

    /** Unique ID for this rule. Must be stable across versions. */
    public abstract RuleID getID();

    /** Human-readable rule name, shown in the UI. */
    public abstract String getName();

    /** Detailed description of what this rule does. */
    public abstract String getDescription();

    /** Category for grouping in the UI. */
    public abstract RuleCategory getCategory();

    /** Severity level of findings. */
    public RuleSeverity getSeverity() {
        return RuleSeverity.WARNING;
    }

    // ─── References ──────────────────────────────────────────────

    /** References to Clean ABAP Styleguide and other sources. */
    public RuleReference[] getReferences() {
        return new RuleReference[0];
    }

    /** Additional hints or known restrictions. */
    public String getHintsAndRestrictions() {
        return "";
    }

    // ─── Activation ──────────────────────────────────────────────

    /** Is this rule active by default in the "Default" profile? */
    public boolean isActiveByDefault() {
        return true;
    }

    /** Is this rule part of the "Essential" profile? (maps to Clean ABAP Styleguide "must" rules) */
    public boolean isEssential() {
        return false;
    }

    /** Date this rule was introduced. Used for "auto-activate new rules" setting. */
    public LocalDate getDateCreated() {
        return LocalDate.of(2024, 1, 1);
    }

    /**
     * Minimum ABAP release required for this rule.
     * Rules that use syntax from newer releases will be skipped
     * on older systems. Default: 750 (7.50).
     */
    public int getMinAbapRelease() {
        return 750;
    }

    // ─── Configuration ───────────────────────────────────────────

    /**
     * Get the configurable options for this rule.
     * Override to add ConfigValue instances.
     */
    public List<ConfigValue<?>> getConfigValues() {
        return new ArrayList<>();
    }

    // ─── Core Logic ──────────────────────────────────────────────

    /**
     * Apply this cleanup rule to the given code document.
     *
     * <p>The rule should:
     * <ul>
     *   <li>Analyze the document's statements</li>
     *   <li>Make changes where the rule's pattern matches</li>
     *   <li>Return a {@link CleanupResult} summarizing changes</li>
     * </ul>
     *
     * <p>If the rule only performs analysis (no auto-fix), it should
     * return findings without modifying the document.</p>
     *
     * @param doc The code document to clean
     * @return Result containing changes and/or findings
     */
    public abstract CleanupResult apply(CodeDocument doc);

    /**
     * Generate an example showing before/after for this rule.
     * Used in the rule configuration UI.
     */
    public String getExampleBefore() { return ""; }
    public String getExampleAfter()  { return ""; }

    // ─── Object Overrides ────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("Rule[%s: %s]", getID(), getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return getID() == ((Rule) obj).getID();
    }

    @Override
    public int hashCode() {
        return getID().hashCode();
    }
}
