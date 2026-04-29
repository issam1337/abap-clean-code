package com.cleanabap.core.profiles;

import com.cleanabap.core.rulebase.*;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

/**
 * A cleanup profile that determines which rules are active and how they
 * are configured. Profiles can be saved, loaded, imported, and exported.
 *
 * <p>Built-in profiles:
 * <ul>
 *   <li><b>Essential</b> — only rules explicitly demanded by the Clean ABAP Styleguide (~40%)</li>
 *   <li><b>Default</b> — recommended set of rules for most teams</li>
 *   <li><b>Full</b> — all available rules enabled</li>
 * </ul>
 *
 * <p>Custom profiles can be created by copying a built-in profile and
 * adjusting individual rule activation and configuration.</p>
 */
public class CleanupProfile {

    public enum ProfileType {
        ESSENTIAL, DEFAULT, FULL, CUSTOM
    }

    private String name;
    private ProfileType type;
    private String description;
    private LocalDate lastModified;
    private boolean autoActivateNewRules;

    /** Rule ID → active state */
    private final Map<RuleID, Boolean> ruleActivation = new LinkedHashMap<>();

    /** Rule ID.configKey → serialized value */
    private final Map<String, String> configOverrides = new HashMap<>();

    // ─── Factories ───────────────────────────────────────────────

    /**
     * Create the "Essential" profile — only styleguide-mandated rules.
     */
    public static CleanupProfile createEssential() {
        CleanupProfile profile = new CleanupProfile();
        profile.name = "Essential";
        profile.type = ProfileType.ESSENTIAL;
        profile.description = "Only rules explicitly demanded by the Clean ABAP Styleguide.";
        profile.autoActivateNewRules = false;

        for (Rule rule : RuleRegistry.getInstance().getAllRules()) {
            profile.ruleActivation.put(rule.getID(), rule.isEssential());
        }
        return profile;
    }

    /**
     * Create the "Default" profile — recommended for most teams.
     */
    public static CleanupProfile createDefault() {
        CleanupProfile profile = new CleanupProfile();
        profile.name = "Default";
        profile.type = ProfileType.DEFAULT;
        profile.description = "Recommended set of rules for most development teams.";
        profile.autoActivateNewRules = true;

        for (Rule rule : RuleRegistry.getInstance().getAllRules()) {
            profile.ruleActivation.put(rule.getID(), rule.isActiveByDefault());
        }
        return profile;
    }

    /**
     * Create the "Full" profile — all rules enabled.
     */
    public static CleanupProfile createFull() {
        CleanupProfile profile = new CleanupProfile();
        profile.name = "Full";
        profile.type = ProfileType.FULL;
        profile.description = "All available cleanup rules enabled.";
        profile.autoActivateNewRules = true;

        for (Rule rule : RuleRegistry.getInstance().getAllRules()) {
            profile.ruleActivation.put(rule.getID(), true);
        }
        return profile;
    }

    /**
     * Create a custom profile by copying another profile.
     */
    public static CleanupProfile createCustom(String name, CleanupProfile source) {
        CleanupProfile profile = new CleanupProfile();
        profile.name = name;
        profile.type = ProfileType.CUSTOM;
        profile.description = "Custom profile based on " + source.getName();
        profile.autoActivateNewRules = source.autoActivateNewRules;
        profile.ruleActivation.putAll(source.ruleActivation);
        profile.configOverrides.putAll(source.configOverrides);
        return profile;
    }

    // ─── Rule Activation ─────────────────────────────────────────

    public boolean isRuleActive(RuleID ruleId) {
        return ruleActivation.getOrDefault(ruleId, false);
    }

    public void setRuleActive(RuleID ruleId, boolean active) {
        ruleActivation.put(ruleId, active);
        if (type != ProfileType.CUSTOM) {
            type = ProfileType.CUSTOM;
        }
        lastModified = LocalDate.now();
    }

    public void toggleRule(RuleID ruleId) {
        setRuleActive(ruleId, !isRuleActive(ruleId));
    }

    /** Get all active rule IDs. */
    public Set<RuleID> getActiveRuleIds() {
        Set<RuleID> active = new LinkedHashSet<>();
        for (var entry : ruleActivation.entrySet()) {
            if (entry.getValue()) active.add(entry.getKey());
        }
        return active;
    }

    public int getActiveRuleCount() {
        return (int) ruleActivation.values().stream().filter(v -> v).count();
    }

    // ─── Configuration Overrides ─────────────────────────────────

    public void setConfigOverride(String fullKey, String value) {
        configOverrides.put(fullKey, value);
    }

    public String getConfigOverride(String fullKey) {
        return configOverrides.get(fullKey);
    }

    public boolean hasConfigOverride(String fullKey) {
        return configOverrides.containsKey(fullKey);
    }

    // ─── Metadata ────────────────────────────────────────────────

    public String getName()            { return name; }
    public ProfileType getType()       { return type; }
    public String getDescription()     { return description; }
    public LocalDate getLastModified() { return lastModified; }
    public boolean isAutoActivateNewRules() { return autoActivateNewRules; }

    public void setName(String name)   { this.name = name; }
    public void setDescription(String desc) { this.description = desc; }
    public void setAutoActivateNewRules(boolean auto) { this.autoActivateNewRules = auto; }

    // ─── Serialization ───────────────────────────────────────────

    /**
     * Export this profile to a Properties format for sharing.
     */
    public Properties toProperties() {
        Properties props = new Properties();
        props.setProperty("profile.name", name);
        props.setProperty("profile.type", type.name());
        props.setProperty("profile.description", description != null ? description : "");
        props.setProperty("profile.autoActivateNew", String.valueOf(autoActivateNewRules));

        for (var entry : ruleActivation.entrySet()) {
            props.setProperty("rule." + entry.getKey().name(), String.valueOf(entry.getValue()));
        }

        for (var entry : configOverrides.entrySet()) {
            props.setProperty("config." + entry.getKey(), entry.getValue());
        }

        return props;
    }

    /**
     * Load a profile from Properties.
     */
    public static CleanupProfile fromProperties(Properties props) {
        CleanupProfile profile = new CleanupProfile();
        profile.name = props.getProperty("profile.name", "Imported");
        profile.type = ProfileType.CUSTOM;
        profile.description = props.getProperty("profile.description", "");
        profile.autoActivateNewRules = Boolean.parseBoolean(
            props.getProperty("profile.autoActivateNew", "true"));

        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("rule.")) {
                String ruleIdName = key.substring(5);
                try {
                    RuleID ruleId = RuleID.valueOf(ruleIdName);
                    profile.ruleActivation.put(ruleId,
                        Boolean.parseBoolean(props.getProperty(key)));
                } catch (IllegalArgumentException e) {
                    // Unknown rule ID — skip (may be from newer version)
                }
            } else if (key.startsWith("config.")) {
                profile.configOverrides.put(key.substring(7), props.getProperty(key));
            }
        }

        return profile;
    }

    /**
     * Save profile to a file.
     */
    public void saveToFile(File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            toProperties().store(out, "ABAP Clean Code Tool - Profile: " + name);
        }
    }

    /**
     * Load profile from a file.
     */
    public static CleanupProfile loadFromFile(File file) throws IOException {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
        }
        return fromProperties(props);
    }

    @Override
    public String toString() {
        return String.format("Profile[%s (%s), %d/%d rules active]",
            name, type, getActiveRuleCount(),
            RuleRegistry.getInstance().getRuleCount());
    }
}
