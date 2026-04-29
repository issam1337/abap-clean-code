package com.cleanabap.plugin.ui.handlers;

import com.cleanabap.core.config.CleanupEngine;
import com.cleanabap.core.config.CleanupEngine.CleanupSession;
import com.cleanabap.core.profiles.CleanupProfile;
import com.cleanabap.plugin.integration.ProfileManager;
import com.cleanabap.plugin.integration.EditorHelper;
import com.cleanabap.plugin.ui.dialogs.InteractiveCleanupDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Handler for the "Clean Up with Interactive ABAP Clean Code..." command.
 *
 * <p>Triggered by Ctrl+Shift+4. Opens the interactive diff dialog where
 * the user can review changes, toggle individual rules per statement,
 * select a profile, and then apply or discard.</p>
 */
public class CleanInteractiveHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        if (!(editor instanceof ITextEditor)) {
            MessageDialog.openError(null, "ABAP Clean Code",
                "No active ABAP editor found.");
            return null;
        }

        ITextEditor textEditor = (ITextEditor) editor;
        IDocument document = textEditor.getDocumentProvider()
            .getDocument(textEditor.getEditorInput());
        if (document == null) return null;

        // Get source
        String source;
        int replaceOffset = 0;
        int replaceLength;

        ITextSelection selection = EditorHelper.getSelection(textEditor);
        if (selection != null && selection.getLength() > 0) {
            source = selection.getText();
            replaceOffset = selection.getOffset();
            replaceLength = selection.getLength();
        } else {
            source = document.get();
            replaceLength = source.length();
        }

        // Run initial cleanup
        CleanupProfile profile = ProfileManager.getInstance().getActiveProfile();
        CleanupEngine engine = new CleanupEngine();
        CleanupSession session = engine.clean(source, profile);

        // Open interactive dialog
        Shell shell = HandlerUtil.getActiveShell(event);
        InteractiveCleanupDialog dialog = new InteractiveCleanupDialog(
            shell, source, session, profile);

        if (dialog.open() == Window.OK) {
            // User confirmed — apply changes
            String cleanedSource = dialog.getAcceptedSource();
            if (cleanedSource != null && !cleanedSource.equals(source)) {
                try {
                    document.replace(replaceOffset, replaceLength, cleanedSource);
                    EditorHelper.setStatusMessage(
                        "ABAP Clean Code: Changes applied successfully");
                } catch (Exception e) {
                    MessageDialog.openError(shell, "ABAP Clean Code",
                        "Failed to apply changes: " + e.getMessage());
                }
            }
        }
        // else: user cancelled — no changes

        return null;
    }
}
