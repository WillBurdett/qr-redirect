import { useEffect, useRef, useState } from 'react';

export default function EditCodeDialog({ code, busy = false, error = '', onSave, onCancel }) {
    const ref = useRef(null);
    const [url, setUrl] = useState(code.targetUrl);

    useEffect(() => {
        if (!ref.current.open) ref.current.showModal();
    }, []);

    return (
        <dialog
            ref={ref}
            className="m-auto w-[min(30rem,90vw)] rounded-2xl bg-white p-6 text-slate-900 shadow-2xl backdrop:bg-slate-900/40"
            onCancel={(e) => { e.preventDefault(); if (!busy) onCancel(); }}   // Esc key
        >
            <form onSubmit={(e) => { e.preventDefault(); onSave(url); }}>
                <h3 className="text-lg font-semibold">Edit QR code</h3>
                <p className="mt-1 text-sm text-slate-500">{code.companyName} · {code.shortCode}</p>

                <label className="mt-5 grid gap-1.5 text-sm font-medium text-slate-700">
                    Destination URL
                    <input type="url" className="input w-full font-normal" value={url}
                        onChange={(e) => setUrl(e.target.value)} required />
                </label>

                {error && <p className="mt-3 text-sm text-red-600">{error}</p>}

                <div className="mt-6 flex justify-end gap-2">
                    <button type="button" className="btn" onClick={onCancel} disabled={busy}>Cancel</button>
                    <button className="btn btn-primary" disabled={busy || url === code.targetUrl}>
                        {busy ? 'Saving…' : 'Save'}
                    </button>
                </div>
            </form>
        </dialog>
    );
}