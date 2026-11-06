import { useState } from 'react';

export default function AddQrCodeForm({ companies, onSubmit }) {
    const [selectedId, setSelectedId] = useState('');
    const [url, setUrl] = useState('');

    // Fall back to the first company until the user picks one
    const companyId = companies.some((c) => String(c.id) === selectedId)
        ? selectedId
        : String(companies[0]?.id ?? '');

    async function handleSubmit(e) {
        e.preventDefault();
        if (await onSubmit(companyId, url)) setUrl('');
    }

    return (
        <section className="card">
            <h2 className="text-base font-semibold">New QR code</h2>
            <form onSubmit={handleSubmit} className="mt-4 grid gap-2">
                <select className="input w-full" value={companyId}
                    onChange={(e) => setSelectedId(e.target.value)} required>
                    {companies.length === 0 && <option value="">Add a company first</option>}
                    {companies.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
                <input type="url" className="input w-full" value={url}
                    onChange={(e) => setUrl(e.target.value)}
                    placeholder="https://example.com/menu" required />
                <button className="btn btn-primary" disabled={!companyId}>Create QR code</button>
            </form>
        </section>
    );
}