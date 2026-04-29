package com.cleanabap.plugin.ui.dialogs;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Main preference page: Window → Preferences → ABAP Clean Code
 */
public class CleanupPreferencePage extends PreferencePage
        implements IWorkbenchPreferencePage {

    @Override
    public void init(IWorkbench workbench) { }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        new Label(container, SWT.NONE).setText(
            "ABAP Clean Code Tool — automated cleanup based on the SAP Clean ABAP Styleguide.");
        new Label(container, SWT.NONE).setText("");
        new Label(container, SWT.NONE).setText(
            "Use 'Profiles & Rules' sub-page to configure active rules and profiles.");
        new Label(container, SWT.NONE).setText("");

        Group shortcuts = new Group(container, SWT.NONE);
        shortcuts.setText("Keyboard Shortcuts");
        shortcuts.setLayout(new GridLayout(2, false));
        addShortcut(shortcuts, "Ctrl+4", "Clean Up with Automated ABAP Clean Code");
        addShortcut(shortcuts, "Ctrl+Shift+4", "Clean Up with Interactive ABAP Clean Code...");
        addShortcut(shortcuts, "Ctrl+Shift+5", "Show Read-Only Preview...");

        return container;
    }

    private void addShortcut(Composite parent, String key, String description) {
        Label keyLabel = new Label(parent, SWT.NONE);
        keyLabel.setText(key);
        // TODO: style bold
        new Label(parent, SWT.NONE).setText(description);
    }
}
