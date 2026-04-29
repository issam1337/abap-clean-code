package com.cleanabap.plugin.ui.handlers;

import com.cleanabap.core.config.CleanupEngine;
import com.cleanabap.core.config.CleanupEngine.CleanupSession;
import com.cleanabap.core.profiles.CleanupProfile;
import com.cleanabap.plugin.integration.ProfileManager;
import com.cleanabap.plugin.ui.dialogs.InteractiveCleanupDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for "Show Read-Only Preview with ABAP Clean Code..."
 * Ctrl+Shift+5. Same as interactive but in read-only mode.
 */
public class CleanPreviewHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        // Get code from editor or clipboard
        String source = CleanupHandlerUtil.getSourceFromEditorOrClipboard(event);
        if (source == null || source.isBlank()) return null;

        CleanupProfile profile = ProfileManager.getInstance().getActiveProfile();
        CleanupEngine engine = new CleanupEngine();
        CleanupSession session = engine.clean(source, profile);

        // Open dialog in read-only mode
        InteractiveCleanupDialog dialog = new InteractiveCleanupDialog(
            shell, source, session, profile);
        dialog.setReadOnly(true);
        dialog.open();

        return null;
    }
}
