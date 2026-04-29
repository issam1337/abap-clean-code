package com.cleanabap.plugin.integration;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Utility class for interacting with the ADT/Eclipse text editor.
 */
public class EditorHelper {

    /**
     * Get the current text selection from the editor.
     */
    public static ITextSelection getSelection(ITextEditor editor) {
        if (editor == null) return null;
        ISelection selection = editor.getSelectionProvider().getSelection();
        if (selection instanceof ITextSelection) {
            return (ITextSelection) selection;
        }
        return null;
    }

    /**
     * Get the currently active text editor.
     */
    public static ITextEditor getActiveTextEditor() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) return null;
        IWorkbenchPage page = window.getActivePage();
        if (page == null) return null;
        IEditorPart editor = page.getActiveEditor();
        if (editor instanceof ITextEditor) {
            return (ITextEditor) editor;
        }
        return null;
    }

    /**
     * Display a message in the Eclipse status bar.
     */
    public static void setStatusMessage(String message) {
        try {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window != null) {
                window.getShell().getDisplay().asyncExec(() -> {
                    IWorkbenchPage page = window.getActivePage();
                    if (page != null) {
                        // Use the action bars status line
                        IEditorPart editor = page.getActiveEditor();
                        if (editor != null) {
                            editor.getEditorSite().getActionBars()
                                .getStatusLineManager().setMessage(message);
                        }
                    }
                });
            }
        } catch (Exception e) {
            // Silently ignore status bar errors
        }
    }
}
