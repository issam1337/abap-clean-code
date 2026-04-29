package com.cleanabap.plugin.ui.handlers;

import com.cleanabap.core.config.CleanupEngine;
import com.cleanabap.core.config.CleanupEngine.CleanupSession;
import com.cleanabap.core.profiles.CleanupProfile;
import com.cleanabap.plugin.integration.ProfileManager;
import com.cleanabap.plugin.integration.EditorHelper;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Handler for the "Clean Up with Automated ABAP Clean Code" command.
 *
 * <p>Triggered by Ctrl+4 or Source Code menu. Applies all active rules
 * from the current profile automatically — no interactive UI.</p>
 *
 * <p>This mirrors the behavior of ABAP Cleaner's automated mode.</p>
 */
public class CleanAutomatedHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        // Get the active editor
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        if (!(editor instanceof ITextEditor)) {
            showError("No active ABAP editor found.");
            return null;
        }

        ITextEditor textEditor = (ITextEditor) editor;
        IDocument document = textEditor.getDocumentProvider()
            .getDocument(textEditor.getEditorInput());
        if (document == null) {
            showError("Could not access the document.");
            return null;
        }

        // Get source code (entire document or selection)
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

        // Load the active profile
        CleanupProfile profile = ProfileManager.getInstance().getActiveProfile();

        // Run cleanup
        CleanupEngine engine = new CleanupEngine();
        CleanupSession session = engine.clean(source, profile);

        if (!session.isModified()) {
            showInfo("No changes needed — code is already clean according to the active profile.");
            return null;
        }

        // Apply changes to the document
        try {
            document.replace(replaceOffset, replaceLength, session.getCleanedSource());
        } catch (Exception e) {
            showError("Failed to apply changes: " + e.getMessage());
            return null;
        }

        // Show summary in status bar
        String summary = String.format(
            "ABAP Clean Code: %d rules applied, %d changes (%dms)",
            session.getAppliedRuleCount(),
            session.getChangedLineCount(),
            session.getElapsedMs());
        EditorHelper.setStatusMessage(summary);

        return null;
    }

    private void showError(String message) {
        MessageDialog.openError(null, "ABAP Clean Code", message);
    }

    private void showInfo(String message) {
        MessageDialog.openInformation(null, "ABAP Clean Code", message);
    }
}
