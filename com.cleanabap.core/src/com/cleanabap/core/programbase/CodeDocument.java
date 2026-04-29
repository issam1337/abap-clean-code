package com.cleanabap.core.programbase;

import com.cleanabap.core.parser.AbapParser;
import com.cleanabap.core.parser.AbapStatement;

import java.util.List;

/**
 * Represents an entire ABAP code document — the top-level container
 * that cleanup rules operate on. Built from raw source code via
 * the {@link AbapParser}.
 */
public class CodeDocument {

    private final String originalSource;
    private String currentSource;
    private List<AbapStatement> statements;
    private final AbapParser parser;

    // Metadata
    private String objectName;
    private String objectType; // CLASS, PROGRAM, FUNCTION-POOL, etc.
    private int abapRelease;   // e.g., 757 for 7.57

    public CodeDocument(String source) {
        this.originalSource = source;
        this.currentSource = source;
        this.parser = new AbapParser();
        this.statements = parser.parse(source);
    }

    // ─── Access ──────────────────────────────────────────────────

    public String getOriginalSource()       { return originalSource; }
    public String getCurrentSource()        { return currentSource; }
    public List<AbapStatement> getStatements() { return statements; }

    public String getObjectName()           { return objectName; }
    public String getObjectType()           { return objectType; }
    public int getAbapRelease()             { return abapRelease; }

    public void setObjectName(String name)  { this.objectName = name; }
    public void setObjectType(String type)  { this.objectType = type; }
    public void setAbapRelease(int release) { this.abapRelease = release; }

    // ─── Mutation ────────────────────────────────────────────────

    /**
     * Update the source code and re-parse.
     * Called after a rule modifies the text.
     */
    public void updateSource(String newSource) {
        this.currentSource = newSource;
        this.statements = parser.parse(newSource);
    }

    /**
     * Reconstruct source from current statements.
     */
    public String reconstructSource() {
        StringBuilder sb = new StringBuilder();
        for (AbapStatement stmt : statements) {
            sb.append(stmt.toSourceCode());
        }
        currentSource = sb.toString();
        return currentSource;
    }

    /**
     * Has the code been modified from the original?
     */
    public boolean isModified() {
        return !originalSource.equals(currentSource);
    }

    // ─── Statistics ──────────────────────────────────────────────

    public int getStatementCount() {
        return statements.size();
    }

    public int getLineCount() {
        return currentSource.split("\n", -1).length;
    }

    /**
     * Get the nesting depth at a specific statement.
     */
    public int getNestingDepth(AbapStatement stmt) {
        int depth = 0;
        AbapStatement parent = stmt.getParentBlock();
        while (parent != null) {
            depth++;
            parent = parent.getParentBlock();
        }
        return depth;
    }
}
