package com.cleanabap.core.rulebase;

import com.cleanabap.core.rules.alignment.AlignDeclarationsRule;
import com.cleanabap.core.rules.alignment.AlignParametersRule;
import com.cleanabap.core.rules.commands.MaxNestingDepthRule;
import com.cleanabap.core.rules.commands.PreferCaseToIfChainRule;
import com.cleanabap.core.rules.commands.PreferCatchingSpecificRule;
import com.cleanabap.core.rules.commands.PreferLineExistsRule;
import com.cleanabap.core.rules.commands.PreferMethodSmallRule;
import com.cleanabap.core.rules.commands.SimplifyIfReturnToCheckRule;
import com.cleanabap.core.rules.comments.ConvertStarCommentsRule;
import com.cleanabap.core.rules.comments.NoEndOfCommentsRule;
import com.cleanabap.core.rules.comments.PreferPragmasRule;
import com.cleanabap.core.rules.comments.RemoveCommentedCodeRule;
import com.cleanabap.core.rules.declarations.PreferFinalRule;
import com.cleanabap.core.rules.declarations.PreferInlineDataRule;
import com.cleanabap.core.rules.declarations.RemoveUnusedVariablesRule;
import com.cleanabap.core.rules.declarations.UnchainConstantsRule;
import com.cleanabap.core.rules.declarations.UnchainDataDeclarationsRule;
import com.cleanabap.core.rules.declarations.UnchainTypesDeclarationsRule;
import com.cleanabap.core.rules.formatting.AndOrAtLineStartRule;
import com.cleanabap.core.rules.formatting.ClosingBracketsAtLineEndRule;
import com.cleanabap.core.rules.formatting.KeepLineLengthRule;
import com.cleanabap.core.rules.naming.DescriptiveNamesRule;
import com.cleanabap.core.rules.naming.NoHungarianNotationRule;
import com.cleanabap.core.rules.spaces.EmptyLineAfterMethodRule;
import com.cleanabap.core.rules.spaces.RemoveNeedlessSpacesRule;
import com.cleanabap.core.rules.spaces.RemoveTrailingSpacesRule;
import com.cleanabap.core.rules.spaces.StandardizeEmptyLinesRule;
import com.cleanabap.core.rules.syntax.PreferAbapBoolRule;
import com.cleanabap.core.rules.syntax.PreferComparisonOperatorsRule;
import com.cleanabap.core.rules.syntax.PreferIsNotRule;
import com.cleanabap.core.rules.syntax.PreferNewToCreateObjectRule;
import com.cleanabap.core.rules.syntax.PreferOptionalExportingRule;
import com.cleanabap.core.rules.syntax.PreferStringTemplatesRule;
import com.cleanabap.core.rules.syntax.PreferValueToClearRule;
import com.cleanabap.core.rules.syntax.PreferXsdboolRule;
import com.cleanabap.core.rules.syntax.ReplaceCallMethodRule;
import com.cleanabap.core.rules.syntax.ReplaceDescribeTableRule;
import com.cleanabap.core.rules.syntax.ReplaceMoveToRule;
import com.cleanabap.core.rules.syntax.ReplaceObsoleteAddRule;
import com.cleanabap.core.rules.syntax.ReplaceTranslateRule;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central registry of all cleanup rules.
 *
 * <p>All rules must be registered here. The registry provides
 * lookup by ID, category, and profile filtering.</p>
 *
 * <p>To add a new rule:
 * <ol>
 *   <li>Create the rule class extending {@link Rule} or {@link RuleForStatements}</li>
 *   <li>Add its {@link RuleID} to the enum</li>
 *   <li>Register it in {@link #registerAllRules()}</li>
 * </ol>
 */
public class RuleRegistry {

    private static RuleRegistry instance;

    private final Map<RuleID, Rule> rulesById = new LinkedHashMap<>();
    private final List<Rule> orderedRules = new ArrayList<>();

    private RuleRegistry() {
        registerAllRules();
    }

    public static synchronized RuleRegistry getInstance() {
        if (instance == null) {
            instance = new RuleRegistry();
        }
        return instance;
    }

    // ─── Rule Registration ───────────────────────────────────────

    /**
     * Register all rules in execution order.
     *
     * <p>Order matters! Rules are applied sequentially, and some rules
     * depend on prior rules having already cleaned the code.
     * General ordering:
     * <ol>
     *   <li>Declarations (unchain first, so other rules see clean statements)</li>
     *   <li>Syntax modernization</li>
     *   <li>Commands</li>
     *   <li>Comments</li>
     *   <li>Empty lines & spaces</li>
     *   <li>Formatting</li>
     *   <li>Alignment (last, after all structural changes)</li>
     * </ol>
     */
    private void registerAllRules() {
        // ── Phase 1: Declarations ────────────────────────────────
        register(new UnchainDataDeclarationsRule());
        register(new UnchainTypesDeclarationsRule());
        register(new UnchainConstantsRule());
        register(new PreferInlineDataRule());
        register(new PreferFinalRule());
        register(new RemoveUnusedVariablesRule());

        // ── Phase 2: Syntax Modernization ────────────────────────
        register(new PreferNewToCreateObjectRule());
        register(new PreferComparisonOperatorsRule());
        register(new ReplaceObsoleteAddRule());
        register(new ReplaceMoveToRule());
        register(new ReplaceCallMethodRule());
        register(new ReplaceTranslateRule());
        register(new ReplaceDescribeTableRule());
        register(new PreferIsNotRule());
        register(new PreferAbapBoolRule());
        register(new PreferStringTemplatesRule());
        register(new PreferOptionalExportingRule());
        register(new PreferXsdboolRule());
        register(new PreferValueToClearRule());

        // ── Phase 3: Commands ────────────────────────────────────
        register(new SimplifyIfReturnToCheckRule());
        register(new PreferCaseToIfChainRule());
        register(new MaxNestingDepthRule());
        register(new PreferLineExistsRule());
        register(new PreferMethodSmallRule());
        register(new PreferCatchingSpecificRule());

        // ── Phase 4: Comments ────────────────────────────────────
        register(new ConvertStarCommentsRule());
        register(new RemoveCommentedCodeRule());
        register(new NoEndOfCommentsRule());
        register(new PreferPragmasRule());

        // ── Phase 5: Empty Lines & Spaces ────────────────────────
        register(new RemoveTrailingSpacesRule());
        register(new StandardizeEmptyLinesRule());
        register(new EmptyLineAfterMethodRule());
        register(new RemoveNeedlessSpacesRule());

        // ── Phase 6: Formatting ──────────────────────────────────
        register(new ClosingBracketsAtLineEndRule());
        register(new KeepLineLengthRule());
        register(new AndOrAtLineStartRule());

        // ── Phase 7: Alignment ───────────────────────────────────
        register(new AlignParametersRule());
        register(new AlignDeclarationsRule());

        // ── Phase 8: Naming (analysis only) ──────────────────────
        register(new NoHungarianNotationRule());
        register(new DescriptiveNamesRule());
    }

    private void register(Rule rule) {
        if (rulesById.containsKey(rule.getID())) {
            throw new IllegalStateException("Duplicate rule ID: " + rule.getID());
        }
        rulesById.put(rule.getID(), rule);
        orderedRules.add(rule);
    }

    // ─── Queries ─────────────────────────────────────────────────

    /** Get all registered rules in execution order. */
    public List<Rule> getAllRules() {
        return Collections.unmodifiableList(orderedRules);
    }

    /** Get a rule by its ID. */
    public Rule getRule(RuleID id) {
        return rulesById.get(id);
    }

    /** Get all rules in a specific category. */
    public List<Rule> getRulesByCategory(RuleCategory category) {
        return orderedRules.stream()
            .filter(r -> r.getCategory() == category)
            .collect(Collectors.toList());
    }

    /** Get all "essential" rules (Clean ABAP "must" rules). */
    public List<Rule> getEssentialRules() {
        return orderedRules.stream()
            .filter(Rule::isEssential)
            .collect(Collectors.toList());
    }

    /** Get all rules active by default. */
    public List<Rule> getDefaultRules() {
        return orderedRules.stream()
            .filter(Rule::isActiveByDefault)
            .collect(Collectors.toList());
    }

    /** Total number of registered rules. */
    public int getRuleCount() {
        return orderedRules.size();
    }
}
