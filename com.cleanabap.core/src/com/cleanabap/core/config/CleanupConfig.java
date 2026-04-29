package com.cleanabap.core.config;

/**
 * Global configuration for the cleanup engine.
 * Settings that apply across all profiles.
 */
public class CleanupConfig {

    private static CleanupConfig instance;

    /** Default ABAP release to assume when not detected */
    private int defaultAbapRelease = 757;

    /** Maximum line length for the line-length rule */
    private int maxLineLength = 120;

    /** Max nesting depth before flagging */
    private int maxNestingDepth = 5;

    /** Max statements per method before flagging */
    private int maxStatementsPerMethod = 30;

    /** Show rule references in findings */
    private boolean showReferences = true;

    public static synchronized CleanupConfig getInstance() {
        if (instance == null) {
            instance = new CleanupConfig();
        }
        return instance;
    }

    // Getters/Setters
    public int getDefaultAbapRelease()     { return defaultAbapRelease; }
    public int getMaxLineLength()          { return maxLineLength; }
    public int getMaxNestingDepth()        { return maxNestingDepth; }
    public int getMaxStatementsPerMethod() { return maxStatementsPerMethod; }
    public boolean isShowReferences()      { return showReferences; }

    public void setDefaultAbapRelease(int r)      { this.defaultAbapRelease = r; }
    public void setMaxLineLength(int l)            { this.maxLineLength = l; }
    public void setMaxNestingDepth(int d)          { this.maxNestingDepth = d; }
    public void setMaxStatementsPerMethod(int s)   { this.maxStatementsPerMethod = s; }
    public void setShowReferences(boolean s)       { this.showReferences = s; }
}
