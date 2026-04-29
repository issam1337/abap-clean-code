package com.cleanabap.plugin.ui.handlers;

import com.cleanabap.plugin.integration.EditorHelper;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Shared utilities for cleanup command handlers.
 */
public class CleanupHandlerUtil {

    /**
     * Get ABAP source code from the active editor selection,
     * the full editor document, or the clipboard.
     */
    public static String getSourceFromEditorOrClipboard(ExecutionEvent event) {
        // Try editor first
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        if (editor instanceof ITextEditor textEditor) {
            IDocument document = textEditor.getDocumentProvider()
                .getDocument(textEditor.getEditorInput());

            if (document != null) {
                ITextSelection selection = EditorHelper.getSelection(textEditor);
                if (selection != null && selection.getLength() > 0) {
                    return selection.getText();
                }
                return document.get();
            }
        }

        // Fall back to clipboard
        return getClipboardText();
    }

    /**
     * Get text from the system clipboard.
     */
    public static String getClipboardText() {
        Display display = Display.getDefault();
        Clipboard clipboard = new Clipboard(display);
        try {
            Object data = clipboard.getContents(TextTransfer.getInstance());
            return (data instanceof String) ? (String) data : null;
        } finally {
            clipboard.dispose();
        }
    }

    /**
     * Set text to the system clipboard.
     */
    public static void setClipboardText(String text) {
        Display display = Display.getDefault();
        Clipboard clipboard = new Clipboard(display);
        try {
            clipboard.setContents(
                new Object[] { text },
                new org.eclipse.swt.dnd.Transfer[] { TextTransfer.getInstance() }
            );
        } finally {
            clipboard.dispose();
        }
    }
}
