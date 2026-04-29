package com.cleanabap.core.rulebase;

/**
 * A typed configuration value for a rule.
 * Rules expose these to allow user customization.
 *
 * <p>Examples:
 * <ul>
 *   <li>ConfigValue&lt;Boolean&gt; "processChains" — whether to unchain before processing</li>
 *   <li>ConfigValue&lt;Integer&gt; "maxLineLength" — maximum allowed line length</li>
 *   <li>ConfigValue&lt;String&gt;  "assertClassName" — name of the assert class</li>
 * </ul>
 */
public class ConfigValue<T> {

    public enum ConfigType {
        BOOLEAN, INTEGER, STRING, ENUM
    }

    private final Rule owner;
    private final String key;
    private final String displayName;
    private final T defaultValue;
    private T value;
    private final ConfigType type;
    private final String description;

    public ConfigValue(Rule owner, String key, String displayName,
                       T defaultValue, ConfigType type, String description) {
        this.owner = owner;
        this.key = key;
        this.displayName = displayName;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.type = type;
        this.description = description;
    }

    // ─── Convenience Factories ───────────────────────────────────

    public static ConfigValue<Boolean> ofBoolean(Rule owner, String key,
            String displayName, boolean defaultValue, String description) {
        return new ConfigValue<>(owner, key, displayName, defaultValue,
            ConfigType.BOOLEAN, description);
    }

    public static ConfigValue<Integer> ofInteger(Rule owner, String key,
            String displayName, int defaultValue, String description) {
        return new ConfigValue<>(owner, key, displayName, defaultValue,
            ConfigType.INTEGER, description);
    }

    public static ConfigValue<String> ofString(Rule owner, String key,
            String displayName, String defaultValue, String description) {
        return new ConfigValue<>(owner, key, displayName, defaultValue,
            ConfigType.STRING, description);
    }

    // ─── Access ──────────────────────────────────────────────────

    public Rule getOwner()        { return owner; }
    public String getKey()        { return key; }
    public String getDisplayName() { return displayName; }
    public T getDefaultValue()    { return defaultValue; }
    public T getValue()           { return value; }
    public ConfigType getType()   { return type; }
    public String getDescription() { return description; }

    public void setValue(T value)  { this.value = value; }
    public void reset()            { this.value = defaultValue; }

    /** Unique key: ruleID + "." + key */
    public String getFullKey() {
        return owner.getID().name() + "." + key;
    }
}
