package com.cleanabap.core.rulebase;

/**
 * Sources that a rule can reference for its justification.
 */
public enum RuleSource {
    /** SAP Clean ABAP Style Guide on GitHub */
    ABAP_STYLE_GUIDE("Clean ABAP Styleguide",
        "https://github.com/SAP/styleguides/blob/main/clean-abap/CleanABAP.md"),

    /** SAP Code Pal for ABAP */
    CODE_PAL_FOR_ABAP("Code Pal for ABAP",
        "https://github.com/SAP/code-pal-for-abap"),

    /** ABAP Keyword Documentation */
    ABAP_KEYWORD_DOCU("ABAP Keyword Documentation",
        "https://help.sap.com/doc/abapdocu_latest_index_htm/latest/en-US/"),

    /** Robert C. Martin - Clean Code book */
    CLEAN_CODE_BOOK("Clean Code (Robert C. Martin)", ""),

    /** ABAP Programming Guidelines */
    ABAP_PROGRAMMING_GUIDELINES("ABAP Programming Guidelines", "");

    private final String displayName;
    private final String baseUrl;

    RuleSource(String displayName, String baseUrl) {
        this.displayName = displayName;
        this.baseUrl = baseUrl;
    }

    public String getDisplayName() { return displayName; }
    public String getBaseUrl()     { return baseUrl; }
}
