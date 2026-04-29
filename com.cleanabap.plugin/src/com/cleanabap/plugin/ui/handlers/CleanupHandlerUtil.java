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
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Shared utilities for cleanup command handlers.
 */
public class CleanupHandlerUtil {

    /**
     * Returns the active text editor for the given execution event,
     * unwrapping multi-page editors used by ADT.
     *
     * <p>ADT's ABAP source editor is a {@link MultiPageEditorPart};
     * {@link HandlerUtil#getActiveEditor} returns the outer container, not
     * the inner text page. We try several strategies:</p>
     * <ol>
     *   <li>If the active editor is already an {@link ITextEditor}, return it.</li>
     *   <li>Try {@code getAdapter(ITextEditor.class)} — most ADT editors support this.</li>
     *   <li>If it's a {@code MultiPageEditorPart}, fetch the currently selected
     *       page and re-apply the same checks.</li>
     * </ol>
     *
     * @return the wrapped {@link ITextEditor}, or {@code null} if none can be found
     */
    public static ITextEditor getActiveTextEditor(ExecutionEvent event) {
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        return resolveTextEditor(editor);
    }

    private static ITextEditor resolveTextEditor(IEditorPart editor) {
        if (editor == null) return null;

        if (editor instanceof ITextEditor te) {
            return te;
        }

        // Most ADT editors expose ITextEditor via the adapter framework.
        Object adapted = editor.getAdapter(ITextEditor.class);
        if (adapted instanceof ITextEditor te) {
            return te;
        }

        // Multi-page editor: drill into the currently selected page.
        if (editor instanceof MultiPageEditorPart multi) {
            try {
                Object selected = multi.getSelectedPage();
                if (selected instanceof IEditorPart inner) {
                    ITextEditor te = resolveTextEditor(inner);
                    if (te != null) return te;
                }
            } catch (Exception ignored) {
                // fall through to null
            }
        }

        return null;
    }

    /**
     * Get ABAP source code from the active editor selection,
     * the full editor document, or the clipboard.
     */
    public static String getSourceFromEditorOrClipboard(ExecutionEvent event) {
        ITextEditor textEditor = getActiveTextEditor(event);
        if (textEditor != null) {
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
