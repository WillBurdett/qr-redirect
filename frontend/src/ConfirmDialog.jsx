import { useEffect, useRef } from 'react';

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
            className="m-auto w-[min(26rem,90vw)] rounded-2xl bg-white p-6 text-slate-900 shadow-2xl backdrop:bg-slate-900/40"
            onCancel={(e) => { e.preventDefault(); if (!busy) onCancel(); }}   // Esc key
        >
            <h3 className="text-lg font-semibold">{title}</h3>
            <p className="mt-2 text-sm text-slate-600">{message}</p>
            <div className="mt-6 flex justify-end gap-2">
                <button type="button" className="btn" onClick={onCancel} disabled={busy}>Cancel</button>
                <button type="button" className="btn btn-danger-solid" onClick={onConfirm} disabled={busy}>
                    {busy ? 'Deleting…' : confirmLabel}
                </button>
            </div>
        </dialog>
    );
}