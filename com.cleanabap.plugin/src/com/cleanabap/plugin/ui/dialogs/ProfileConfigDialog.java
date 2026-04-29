package com.cleanabap.plugin.ui.dialogs;

import com.cleanabap.core.profiles.CleanupProfile;
import com.cleanabap.core.rulebase.*;
import com.cleanabap.plugin.integration.ProfileManager;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * Dialog for configuring cleanup profiles and individual rules.
 *
 * <p>Layout mirrors ABAP Cleaner's profile configuration:</p>
 * <pre>
 * ┌──────────────────────────────────────────────────────────┐
 * │ Profiles:                                                 │
 * │ [Essential] [Default] [Full] [+New] [Copy] [Delete]       │
 * │ [Import...] [Export...]                                   │
 * │ ☑ Automatically activate new features after updates       │
 * ├────────────────────┬─────────────────────────────────────┤
 * │ Rules:             │ Rule Details:                        │
 * │ ☑ Prefer NEW...    │ Name: Prefer NEW to CREATE OBJECT   │
 * │ ☑ Replace EQ/NE... │ Description: ...                    │
 * │ ☐ Convert * ...    │ References: Clean ABAP §...          │
 * │ ...                │                                     │
 * │                    │ Options:                             │
 * │                    │ ☑ Process chains                     │
 * │                    │                                     │
 * │                    │ Example (before → after):            │
 * │                    │ CREATE OBJECT lo. → lo = NEW #( ).   │
 * ├────────────────────┴─────────────────────────────────────┤
 * │ [Save Profiles and Exit]                    [Cancel]      │
 * └──────────────────────────────────────────────────────────┘
 * </pre>
 */
public class ProfileConfigDialog extends Dialog {

    private List profileList;
    private Tree rulesTree;
    private StyledText exampleText;
    private Label descriptionLabel;
    private Label referencesLabel;
    private Composite optionsComposite;

    private CleanupProfile currentProfile;

    public ProfileConfigDialog(Shell parent) {
        super(parent);
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
        currentProfile = ProfileManager.getInstance().getActiveProfile();
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("ABAP Clean Code — Profile & Rules Configuration");
    }

    @Override
    protected Point getInitialSize() {
        return new Point(1000, 700);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));

        // ── Profile selector bar ─────────────────────────────────
        createProfileBar(container);

        // ── Main content ─────────────────────────────────────────
        SashForm sash = new SashForm(container, SWT.HORIZONTAL);
        sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Left: Rules tree
        createRulesTree(sash);

        // Right: Rule details
        createRuleDetails(sash);

        sash.setWeights(new int[] { 40, 60 });

        return container;
    }

    private void createProfileBar(Composite parent) {
        Composite bar = new Composite(parent, SWT.NONE);
        bar.setLayout(new GridLayout(8, false));
        bar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        new Label(bar, SWT.NONE).setText("Profiles:");

        profileList = new List(bar, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        GridData listData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        listData.heightHint = 50;
        profileList.setLayoutData(listData);
        for (CleanupProfile p : ProfileManager.getInstance().getAllProfiles()) {
            profileList.add(p.getName());
        }
        profileList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = profileList.getSelectionIndex();
                if (idx >= 0) {
                    currentProfile = ProfileManager.getInstance().getAllProfiles().get(idx);
                    refreshRulesTree();
                }
            }
        });

        Button newBtn = new Button(bar, SWT.PUSH);
        newBtn.setText("New");
        newBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                InputDialog dlg = new InputDialog(getShell(), "New Profile",
                    "Enter a name for the new profile:", "My Profile", null);
                if (dlg.open() == Dialog.OK) {
                    // Create new custom profile
                }
            }
        });

        Button importBtn = new Button(bar, SWT.PUSH);
        importBtn.setText("Import...");

        Button exportBtn = new Button(bar, SWT.PUSH);
        exportBtn.setText("Export...");

        Button deleteBtn = new Button(bar, SWT.PUSH);
        deleteBtn.setText("Delete");

        // Auto-activate checkbox
        Button autoActivate = new Button(parent, SWT.CHECK);
        autoActivate.setText("Automatically activate new features after updates");
        autoActivate.setSelection(currentProfile.isAutoActivateNewRules());
        autoActivate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void createRulesTree(Composite parent) {
        Composite panel = new Composite(parent, SWT.NONE);
        panel.setLayout(new GridLayout(1, false));

        // Search field
        Text searchField = new Text(panel, SWT.BORDER | SWT.SEARCH);
        searchField.setMessage("Search rules...");
        searchField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        rulesTree = new Tree(panel, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL);
        rulesTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        populateRulesTree();

        rulesTree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.detail == SWT.CHECK) {
                    TreeItem item = (TreeItem) e.item;
                    if (item.getData() instanceof Rule rule) {
                        currentProfile.setRuleActive(rule.getID(), item.getChecked());
                    }
                } else {
                    TreeItem item = rulesTree.getSelection().length > 0
                        ? rulesTree.getSelection()[0] : null;
                    if (item != null && item.getData() instanceof Rule rule) {
                        showRuleDetails(rule);
                    }
                }
            }
        });
    }

    private void populateRulesTree() {
        rulesTree.removeAll();

        // Group by category
        for (RuleCategory category : RuleCategory.values()) {
            java.util.List<Rule> rules = RuleRegistry.getInstance()
                .getRulesByCategory(category);
            if (rules.isEmpty()) continue;

            TreeItem catItem = new TreeItem(rulesTree, SWT.NONE);
            catItem.setText(String.format("%s (%d)", category.getDisplayName(), rules.size()));

            for (Rule rule : rules) {
                TreeItem ruleItem = new TreeItem(catItem, SWT.NONE);
                ruleItem.setText(rule.getName());
                ruleItem.setChecked(currentProfile.isRuleActive(rule.getID()));
                ruleItem.setData(rule);
            }
            catItem.setExpanded(true);
        }
    }

    private void refreshRulesTree() {
        for (TreeItem catItem : rulesTree.getItems()) {
            for (TreeItem ruleItem : catItem.getItems()) {
                if (ruleItem.getData() instanceof Rule rule) {
                    ruleItem.setChecked(currentProfile.isRuleActive(rule.getID()));
                }
            }
        }
    }

    private void createRuleDetails(Composite parent) {
        Composite panel = new Composite(parent, SWT.NONE);
        panel.setLayout(new GridLayout(1, false));

        descriptionLabel = new Label(panel, SWT.WRAP);
        descriptionLabel.setText("Select a rule to see its details.");
        GridData descData = new GridData(SWT.FILL, SWT.TOP, true, false);
        descData.widthHint = 400;
        descriptionLabel.setLayoutData(descData);

        referencesLabel = new Label(panel, SWT.WRAP);
        referencesLabel.setText("");
        referencesLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        // Options area (dynamically populated)
        new Label(panel, SWT.SEPARATOR | SWT.HORIZONTAL)
            .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        optionsComposite = new Composite(panel, SWT.NONE);
        optionsComposite.setLayout(new GridLayout(2, false));
        optionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        // Example area
        new Label(panel, SWT.SEPARATOR | SWT.HORIZONTAL)
            .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        new Label(panel, SWT.NONE).setText("Example (before → after):");

        exampleText = new StyledText(panel, SWT.BORDER | SWT.V_SCROLL |
            SWT.H_SCROLL | SWT.READ_ONLY);
        exampleText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        Font mono = new Font(panel.getDisplay(), "Consolas", 9, SWT.NORMAL);
        exampleText.setFont(mono);
        exampleText.addDisposeListener(e -> mono.dispose());
    }

    private void showRuleDetails(Rule rule) {
        descriptionLabel.setText(
            rule.getName() + "\n\n" + rule.getDescription() +
            "\n\nSeverity: " + rule.getSeverity().getDisplayName() +
            (rule.isEssential() ? "  [ESSENTIAL]" : ""));

        // References
        StringBuilder refs = new StringBuilder("References:\n");
        for (RuleReference ref : rule.getReferences()) {
            refs.append("  • ").append(ref.toString()).append("\n");
        }
        referencesLabel.setText(refs.toString());

        // Options
        for (Control child : optionsComposite.getChildren()) {
            child.dispose();
        }
        for (ConfigValue<?> config : rule.getConfigValues()) {
            switch (config.getType()) {
                case BOOLEAN:
                    Button check = new Button(optionsComposite, SWT.CHECK);
                    check.setText(config.getDisplayName());
                    check.setSelection((Boolean) config.getValue());
                    check.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
                    break;
                case INTEGER:
                    new Label(optionsComposite, SWT.NONE).setText(config.getDisplayName());
                    Spinner spinner = new Spinner(optionsComposite, SWT.BORDER);
                    spinner.setSelection((Integer) config.getValue());
                    break;
                case STRING:
                    new Label(optionsComposite, SWT.NONE).setText(config.getDisplayName());
                    Text text = new Text(optionsComposite, SWT.BORDER);
                    text.setText((String) config.getValue());
                    text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
                    break;
            }
        }
        optionsComposite.layout();

        // Example
        String before = rule.getExampleBefore();
        String after = rule.getExampleAfter();
        if (!before.isEmpty()) {
            exampleText.setText("BEFORE:\n" + before + "\n\nAFTER:\n" + after);
        } else {
            exampleText.setText("(No example available)");
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
        createButton(parent, IDialogConstants.OK_ID, "Save Profiles and Exit", true);
    }

    @Override
    protected void okPressed() {
        // Save all profiles
        ProfileManager mgr = ProfileManager.getInstance();
        mgr.setActiveProfile(currentProfile);
        mgr.saveProfile(currentProfile);
        super.okPressed();
    }
}
