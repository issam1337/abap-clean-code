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

import java.util.List;

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
        highlightDifferences();
    }

    private void highlightDifferences() {
        // Basic line-based highlighting
        String[] origLines = originalSource.split("\n", -1);
        String[] cleanLines = session.getCleanedSource().split("\n", -1);

        Display display = getShell().getDisplay();
        Color removedBg = new Color(display, 255, 220, 220);
        Color addedBg = new Color(display, 220, 255, 220);

        int maxLines = Math.max(origLines.length, cleanLines.length);
        for (int i = 0; i < maxLines; i++) {
            String orig = i < origLines.length ? origLines[i] : "";
            String clean = i < cleanLines.length ? cleanLines[i] : "";

            if (!orig.equals(clean)) {
                if (i < origLines.length) {
                    highlightLine(leftPanel, i, removedBg);
                }
                if (i < cleanLines.length) {
                    highlightLine(rightPanel, i, addedBg);
                }
            }
        }
    }

    private void highlightLine(StyledText text, int lineIndex, Color bg) {
        try {
            int start = text.getOffsetAtLine(lineIndex);
            int length = text.getLine(lineIndex).length();
            org.eclipse.swt.custom.StyleRange range =
                new org.eclipse.swt.custom.StyleRange(start, length, null, bg);
            text.setStyleRange(range);
        } catch (Exception e) {
            // Line index out of range
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
        List<CleanupResult.CleanupChange> changes = session.getAllChanges();
        if (changes.isEmpty()) return;
        currentFindingIndex = Math.max(0, currentFindingIndex - 1);
        scrollToChange(changes.get(currentFindingIndex));
    }

    private void navigateNext() {
        List<CleanupResult.CleanupChange> changes = session.getAllChanges();
        if (changes.isEmpty()) return;
        currentFindingIndex = Math.min(changes.size() - 1, currentFindingIndex + 1);
        scrollToChange(changes.get(currentFindingIndex));
    }

    private void scrollToChange(CleanupResult.CleanupChange change) {
        try {
            int line = Math.max(0, change.getLine() - 1);
            if (line < leftPanel.getLineCount()) {
                leftPanel.setTopIndex(Math.max(0, line - 3));
            }
            if (line < rightPanel.getLineCount()) {
                rightPanel.setTopIndex(Math.max(0, line - 3));
            }
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
