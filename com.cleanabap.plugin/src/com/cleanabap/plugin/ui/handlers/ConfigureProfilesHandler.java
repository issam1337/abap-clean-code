package com.cleanabap.plugin.ui.handlers;

import com.cleanabap.plugin.ui.dialogs.ProfileConfigDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for "Configure ABAP Clean Code Profiles..."
 * Opens the profile/rules configuration dialog.
 */
public class ConfigureProfilesHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);
        ProfileConfigDialog dialog = new ProfileConfigDialog(shell);
        dialog.open();
        return null;
    }
}
