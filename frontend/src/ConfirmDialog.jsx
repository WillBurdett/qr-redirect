import { useEffect, useRef } from 'react';
import './css/ConfirmDialog.css';

export default function ConfirmDialog({
    open, title, message, confirmLabel = 'Delete', busy = false, onConfirm, onCancel,
}) {
    const ref = useRef(null);

    useEffect(() => {
        const dialog = ref.current;
        if (open && !dialog.open) dialog.showModal();
        if (!open && dialog.open) dialog.close();
    }, [open]);

    return (
        <dialog
            ref={ref}
            className="confirm"
            onCancel={(e) => { e.preventDefault(); if (!busy) onCancel(); }}   // Esc key
        >
            <h3>{title}</h3>
            <p>{message}</p>
            <div className="confirm__actions">
                <button type="button" onClick={onCancel} disabled={busy}>Cancel</button>
                <button type="button" className="confirm__danger" onClick={onConfirm} disabled={busy}>
                    {busy ? 'Deleting…' : confirmLabel}
                </button>
            </div>
        </dialog>
    );
}