package com.cleanabap.plugin.ui.dialogs;

import com.cleanabap.core.config.CleanupEngine;
import com.cleanabap.core.config.CleanupEngine.CleanupSession;
import com.cleanabap.core.profiles.CleanupProfile;
import com.cleanabap.core.rulebase.*;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Interactive cleanup dialog — the core UI of the ABAP Clean Code Tool.
 *
 * <p>Layout mirrors SAP's ABAP Cleaner interactive mode:</p>
 * <pre>
 * ┌──────────────────────────────────────────────────────┐
 * │  Profile: [Default ▾]  Range: [Current Class ▾]     │
 * ├───────────────────────┬──────────────────────────────┤
 * │  ORIGINAL CODE        │  CLEANED CODE                │
 * │  (left panel)         │  (right panel)               │
 * │                       │                              │
 * │  Changed lines are    │  Changed lines are           │
 * │  highlighted in red   │  highlighted in green        │
 * │                       │                              │
 * ├───────────────────────┴──────────────────────────────┤
 * │  Rules Used in Current Selection:                     │
 * │  ☑ Prefer NEW to CREATE OBJECT                       │
 * │  ☑ Replace obsolete ADD/SUBTRACT                      │
 * │  ☐ Convert * comments to " comments                   │
 * ├──────────────────────────────────────────────────────┤
 * │  [Configure...]  [◀ Prev] [Next ▶]   [Cancel] [Apply]│
 * └──────────────────────────────────────────────────────┘
 * </pre>
 */
public class InteractiveCleanupDialog extends Dialog {

    private final String originalSource;
    private CleanupSession session;
    private CleanupProfile profile;
    private boolean readOnly = false;

    // UI components
    private StyledText leftPanel;   // original code
    private StyledText rightPanel;  // cleaned code
    private Table rulesTable;
    private Combo profileCombo;
    private Label statusLabel;

    // State
    private String acceptedSource;
    private int currentFindingIndex = -1;

    // Diff highlight bookkeeping — tracks the lines reported as different
    // by the most recent computeDiff() pass. Used by Next/Prev navigation
    // and to repaint the "current change" highlight without recomputing
    // the whole diff.
    private List<Integer> origDiffLines = new ArrayList<>();
    private List<Integer> cleanDiffLines = new ArrayList<>();
    private org.eclipse.swt.graphics.Color removedBgColor;
    private org.eclipse.swt.graphics.Color addedBgColor;
    private org.eclipse.swt.graphics.Color currentLeftColor;
    private org.eclipse.swt.graphics.Color currentRightColor;

    public InteractiveCleanupDialog(Shell parent, String originalSource,
                                     CleanupSession session,
                                     CleanupProfile profile) {
        super(parent);
        this.originalSource = originalSource;
        this.session = session;
        this.profile = profile;
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getAcceptedSource() {
        return acceptedSource;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        String title = readOnly
            ? "ABAP Clean Code — Read-Only Preview"
            : "ABAP Clean Code — Interactive Cleanup";
        shell.setText(title);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(1200, 800);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));

        // ── Top bar: Profile selector and range ──────────────────
        createTopBar(container);

        // ── Main content: SashForm with left/right panels ────────
        SashForm mainSash = new SashForm(container, SWT.VERTICAL);
        mainSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Code comparison area
        SashForm codeSash = new SashForm(mainSash, SWT.HORIZONTAL);

        createCodePanel(codeSash, "Original Code", true);
        createCodePanel(codeSash, "Cleaned Code", false);
        codeSash.setWeights(new int[] { 50, 50 });

        // Rules panel at bottom
        createRulesPanel(mainSash);

        mainSash.setWeights(new int[] { 70, 30 });

        // ── Status bar ───────────────────────────────────────────
        statusLabel = new Label(container, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        updateStatus();

        // Populate panels
        populatePanels();

        return container;
    }

    private void createTopBar(Composite parent) {
        Composite topBar = new Composite(parent, SWT.NONE);
        topBar.setLayout(new GridLayout(6, false));
        topBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        new Label(topBar, SWT.NONE).setText("Profile:");
        profileCombo = new Combo(topBar, SWT.READ_ONLY);
        profileCombo.setItems(new String[] { "Essential", "Default", "Full" });
        profileCombo.select(1); // Default
        profileCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onProfileChanged(profileCombo.getSelectionIndex());
            }
        });

        new Label(topBar, SWT.NONE).setText("   Range:");
        Combo rangeCombo = new Combo(topBar, SWT.READ_ONLY);
        rangeCombo.setItems(new String[] {
            "Current Statement", "Current Method", "Current Class", "Entire Document"
        });
        rangeCombo.select(3);

        // Navigation buttons
        Button prevBtn = new Button(topBar, SWT.PUSH);
        prevBtn.setText("◀ Prev");
        prevBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) { navigatePrev(); }
        });

        Button nextBtn = new Button(topBar, SWT.PUSH);
        nextBtn.setText("Next ▶");
        nextBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) { navigateNext(); }
        });
    }

    private void createCodePanel(Composite parent, String title, boolean isOriginal) {
        Composite panel = new Composite(parent, SWT.NONE);
        panel.setLayout(new GridLayout(1, false));

        Label label = new Label(panel, SWT.NONE);
        label.setText(title);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        StyledText text = new StyledText(panel, SWT.BORDER | SWT.V_SCROLL |
            SWT.H_SCROLL | SWT.READ_ONLY);
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Font monoFont = new Font(parent.getDisplay(), "Consolas", 10, SWT.NORMAL);
        text.setFont(monoFont);
        text.addDisposeListener(e -> monoFont.dispose());

        if (isOriginal) {
            leftPanel = text;
        } else {
            rightPanel = text;
        }
    }

    private void createRulesPanel(Composite parent) {
        Composite rulesPanel = new Composite(parent, SWT.NONE);
        rulesPanel.setLayout(new GridLayout(2, false));

        Label label = new Label(rulesPanel, SWT.NONE);
        label.setText("Rules Used in Current Selection:");
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        rulesTable = new Table(rulesPanel, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL);
        rulesTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));

        // Populate rules
        RuleRegistry registry = RuleRegistry.getInstance();
        for (Rule rule : registry.getAllRules()) {
            TableItem item = new TableItem(rulesTable, SWT.NONE);
            item.setText(rule.getName());
            item.setChecked(profile.isRuleActive(rule.getID()));
            item.setData(rule);
        }

        rulesTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.detail == SWT.CHECK) {
                    TableItem item = (TableItem) e.item;
                    Rule rule = (Rule) item.getData();
                    profile.setRuleActive(rule.getID(), item.getChecked());
                    rerunCleanup();
                }
            }
        });

        // Configure button
        Button configBtn = new Button(rulesPanel, SWT.PUSH);
        configBtn.setText("Configure...");
        configBtn.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
        configBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ProfileConfigDialog dialog = new ProfileConfigDialog(getShell());
                dialog.open();
                refreshRulesTable();
                rerunCleanup();
            }
        });
    }

    // ─── Panel Population ────────────────────────────────────────

    private void populatePanels() {
        leftPanel.setText(originalSource);
        rightPanel.setText(session.getCleanedSource());
        // Clear any previous style ranges before re-highlighting (setText
        // already does this for the StyledText itself, but keep our cached
        // diff state in sync).
        origDiffLines.clear();
        cleanDiffLines.clear();
        currentFindingIndex = -1;
        ensureDiffColors();
        highlightDifferences();
    }

    private void ensureDiffColors() {
        Display display = getShell().getDisplay();
        if (removedBgColor == null) {
            removedBgColor = new Color(display, 255, 220, 220); // light red
            addedBgColor   = new Color(display, 220, 255, 220); // light green
            currentLeftColor  = new Color(display, 255, 170, 170); // stronger red
            currentRightColor = new Color(display, 170, 230, 170); // stronger green
            // Dispose colors when the dialog shell is destroyed
            getShell().addDisposeListener(e -> {
                if (removedBgColor != null) removedBgColor.dispose();
                if (addedBgColor != null) addedBgColor.dispose();
                if (currentLeftColor != null) currentLeftColor.dispose();
                if (currentRightColor != null) currentRightColor.dispose();
            });
        }
    }

    /**
     * Highlight the lines that actually differ between the original source
     * and the cleaned source using a longest-common-subsequence (LCS) based
     * line diff. The previous implementation compared lines pairwise by
     * index, which (a) flagged everything below the first multi-line edit
     * (e.g., an UNCHAIN that splits one line into three) as "different" and
     * (b) missed real differences when the line counts diverged.
     */
    private void highlightDifferences() {
        String[] origLines  = originalSource.split("\n", -1);
        String[] cleanLines = session.getCleanedSource().split("\n", -1);

        ensureDiffColors();

        // Compute LCS-based diff
        Set<Integer> origDiffs  = new HashSet<>();
        Set<Integer> cleanDiffs = new HashSet<>();
        computeLineDiff(origLines, cleanLines, origDiffs, cleanDiffs);

        origDiffLines  = new ArrayList<>(origDiffs);
        cleanDiffLines = new ArrayList<>(cleanDiffs);
        java.util.Collections.sort(origDiffLines);
        java.util.Collections.sort(cleanDiffLines);

        for (int i : origDiffLines) {
            highlightLine(leftPanel, i, removedBgColor);
        }
        for (int i : cleanDiffLines) {
            highlightLine(rightPanel, i, addedBgColor);
        }
    }

    /**
     * Standard LCS line diff. Lines whose index is in {@code origDiffs} are
     * unique to the original (treated as "removed"); lines in {@code cleanDiffs}
     * are unique to the cleaned source ("added"). Equal lines (the LCS itself)
     * are skipped.
     */
    private void computeLineDiff(String[] a, String[] b,
                                  Set<Integer> aDiffs, Set<Integer> bDiffs) {
        int n = a.length, m = b.length;
        int[][] dp = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (a[i].equals(b[j])) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }
        int i = 0, j = 0;
        while (i < n && j < m) {
            if (a[i].equals(b[j])) {
                i++; j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                aDiffs.add(i);
                i++;
            } else {
                bDiffs.add(j);
                j++;
            }
        }
        while (i < n) { aDiffs.add(i++); }
        while (j < m) { bDiffs.add(j++); }
    }

    private void highlightLine(StyledText text, int lineIndex, Color bg) {
        try {
            if (lineIndex < 0 || lineIndex >= text.getLineCount()) return;
            int start = text.getOffsetAtLine(lineIndex);
            int length = text.getLine(lineIndex).length();
            if (length == 0) length = 1; // give an empty line a visible band
            org.eclipse.swt.custom.StyleRange range =
                new org.eclipse.swt.custom.StyleRange(start, length, null, bg);
            text.setStyleRange(range);
        } catch (Exception e) {
            // Line index out of range — silently ignore
        }
    }

    // ─── Actions ─────────────────────────────────────────────────

    private void onProfileChanged(int index) {
        switch (index) {
            case 0: profile = CleanupProfile.createEssential(); break;
            case 1: profile = CleanupProfile.createDefault(); break;
            case 2: profile = CleanupProfile.createFull(); break;
        }
        refreshRulesTable();
        rerunCleanup();
    }

    private void rerunCleanup() {
        CleanupEngine engine = new CleanupEngine();
        session = engine.clean(originalSource, profile);
        populatePanels();
        updateStatus();
    }

    private void refreshRulesTable() {
        for (TableItem item : rulesTable.getItems()) {
            Rule rule = (Rule) item.getData();
            item.setChecked(profile.isRuleActive(rule.getID()));
        }
    }

    private void navigatePrev() {
        int count = navigationCount();
        if (count == 0) return;
        if (currentFindingIndex <= 0) {
            currentFindingIndex = count - 1; // wrap to last
        } else {
            currentFindingIndex--;
        }
        scrollToCurrent();
    }

    private void navigateNext() {
        int count = navigationCount();
        if (count == 0) return;
        if (currentFindingIndex < 0 || currentFindingIndex >= count - 1) {
            currentFindingIndex = 0; // wrap to first / start at first
        } else {
            currentFindingIndex++;
        }
        scrollToCurrent();
    }

    /**
     * Number of navigable changes: prefer the actual diff lines (which are
     * visually highlighted) over the rule-reported change list. The latter
     * can be empty for rules that mutate source via string-replace without
     * adding entries, while the diff is computed directly from the two
     * source strings.
     */
    private int navigationCount() {
        if (!origDiffLines.isEmpty()) return origDiffLines.size();
        if (!cleanDiffLines.isEmpty()) return cleanDiffLines.size();
        return session.getAllChanges().size();
    }

    private void scrollToCurrent() {
        // Re-paint base diff highlighting first, then overlay the
        // "current change" emphasis in a stronger color on both panels.
        highlightDifferences();

        int leftLine  = pickLine(origDiffLines, currentFindingIndex,
                                 fallbackLineFromChanges(currentFindingIndex));
        int rightLine = pickLine(cleanDiffLines, currentFindingIndex, leftLine);

        if (leftLine >= 0) {
            highlightLine(leftPanel, leftLine, currentLeftColor);
            scrollPanelTo(leftPanel, leftLine);
        }
        if (rightLine >= 0) {
            highlightLine(rightPanel, rightLine, currentRightColor);
            scrollPanelTo(rightPanel, rightLine);
        }

        if (statusLabel != null && !statusLabel.isDisposed()) {
            int total = navigationCount();
            statusLabel.setText(String.format("%s — change %d of %d",
                session.getSummary(),
                Math.min(currentFindingIndex + 1, total),
                total));
        }
    }

    private int pickLine(List<Integer> lines, int idx, int fallback) {
        if (lines == null || lines.isEmpty()) return fallback;
        if (idx < 0) return lines.get(0);
        if (idx >= lines.size()) return lines.get(lines.size() - 1);
        return lines.get(idx);
    }

    private int fallbackLineFromChanges(int idx) {
        List<CleanupResult.CleanupChange> changes = session.getAllChanges();
        if (changes.isEmpty()) return -1;
        int safeIdx = Math.max(0, Math.min(idx, changes.size() - 1));
        return Math.max(0, changes.get(safeIdx).getLine() - 1);
    }

    private void scrollPanelTo(StyledText panel, int line) {
        if (panel == null || panel.isDisposed()) return;
        if (line < 0 || line >= panel.getLineCount()) return;
        try {
            // Center the line in the visible area when possible.
            int linesVisible = Math.max(1,
                panel.getClientArea().height / Math.max(1, panel.getLineHeight()));
            int top = Math.max(0, line - linesVisible / 2);
            panel.setTopIndex(top);
            // Also place the caret offset at the line so screen-readers and
            // selection-based actions work as expected.
            panel.setCaretOffset(panel.getOffsetAtLine(line));
        } catch (Exception e) {
            // Ignore scroll errors
        }
    }

    private void updateStatus() {
        if (statusLabel != null && !statusLabel.isDisposed()) {
            statusLabel.setText(session.getSummary());
        }
    }

    // ─── Dialog Buttons ──────────────────────────────────────────

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        if (readOnly) {
            createButton(parent, IDialogConstants.CANCEL_ID, "Close", true);
        } else {
            createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
            createButton(parent, IDialogConstants.OK_ID, "Apply and Close", true);
        }
    }

    @Override
    protected void okPressed() {
        acceptedSource = session.getCleanedSource();
        super.okPressed();
    }
}
