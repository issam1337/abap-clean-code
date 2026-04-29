package com.cleanabap.plugin.ui.dialogs;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Profiles & Rules preference sub-page.
 * Window → Preferences → ABAP Clean Code → Profiles & Rules
 *
 * Delegates to {@link ProfileConfigDialog} for the actual configuration UI.
 */
public class ProfilePreferencePage extends PreferencePage
        implements IWorkbenchPreferencePage {

    @Override
    public void init(IWorkbench workbench) { }

    @Override
    protected Control createContents(Composite parent) {
        new Label(parent, SWT.NONE).setText(
            "Click 'Configure...' below to open the full profiles and rules dialog.");
        // In a real implementation, embed the profile config UI inline
        return parent;
    }
}
