import { useEffect, useRef, useState } from 'react';
import './ConfirmDialog.css';

export default function EditCodeDialog({ code, busy = false, error = '', onSave, onCancel }) {
    const ref = useRef(null);
    const [url, setUrl] = useState(code.targetUrl);   // starts from the code being edited

    useEffect(() => {
        if (!ref.current.open) ref.current.showModal();
    }, []);

    return (
        <dialog
            ref={ref}
            className="confirm"
            style={{ width: 'min(480px, 90vw)' }}
            onCancel={(e) => { e.preventDefault(); if (!busy) onCancel(); }}   // Esc key
        >
            <form onSubmit={(e) => { e.preventDefault(); onSave(url); }}>
                <h3>Edit QR code</h3>
                <p style={{ marginBottom: 12 }}>{code.companyName} · {code.shortCode}</p>

                <label>
                    Destination URL
                    <input type="url" value={url} onChange={(e) => setUrl(e.target.value)} required />
                </label>

                {error && <p style={{ color: '#d92d20', marginBottom: 12 }}>{error}</p>}

                <div className="confirm__actions">
                    <button type="button" onClick={onCancel} disabled={busy}>Cancel</button>
                    <button className="confirm__primary" disabled={busy || url === code.targetUrl}>
                        {busy ? 'Saving…' : 'Save'}
                    </button>
                </div>
            </form>
        </dialog>
    );
}