import { useEffect, useState } from 'react';
import { api } from './api';
import Toggle from './Toggle';
import ConfirmDialog from './ConfirmDialog';

export default function Dashboard() {
    const [companies, setCompanies] = useState([]);
    const [codes, setCodes] = useState([]);
    const [companyId, setCompanyId] = useState('');
    const [targetUrl, setTargetUrl] = useState('');
    const [newCompany, setNewCompany] = useState('');
    const [id, setId] = useState('');
    const [togglingId, setTogglingId] = useState(null);
    const [error, setError] = useState('');
    const [pendingDelete, setPendingDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const [updateUrl, setUpdateUrl] = useState('');

    useEffect(() => {
        Promise.all([api.listCompanies(), api.listCodes()])
            .then(([cs, qs]) => {
                setCompanies(cs);
                setCodes(qs);
                if (cs.length) setCompanyId(String(cs[0].id));
            })
            .catch((err) => setError(err.message));
    }, []);

    async function addCompany(e) {
        e.preventDefault();
        setError('');
        try {
            const company = await api.createCompany(newCompany);
            setCompanies([...companies, company]);
            setCompanyId(String(company.id));
            setNewCompany('');
        } catch (err) {
            setError(err.message);
        }
    }

    async function addCode(e) {
        e.preventDefault();
        setError('');
        try {
            const qr = await api.createCode(Number(companyId), targetUrl);
            setCodes([qr, ...codes]);
            setTargetUrl('');
        } catch (err) {
            setError(err.message);
        }
    }

    async function updateCode(e) {
        e.preventDefault();
        setError('');
        try {
            await api.updateCode(Number(id), updateUrl);
            setCodes(await api.listCodes());
        } catch (err) {
            setError(err.message);
        }
    }

    async function confirmDelete() {
        const target = pendingDelete;
        setError('');
        setDeleting(true);
        try {
            await api.deleteCode(target.id);
            setCodes((current) => current.filter((c) => c.id !== target.id));
        } catch (err) {
            setError(err.message);
        } finally {
            setDeleting(false);
            setPendingDelete(null);
        }
    }

    async function toggleActive(qr) {
        setError('');
        setTogglingId(qr.id);
        try {
            const updated = await api.codeStatus(qr.id, { active: !qr.active });
            setCodes(codes.map((c) => (c.id === updated.id ? updated : c)));
        } catch (err) {
            setError(err.message);
        } finally {
            setTogglingId(null);
        }
    }

    return (
        <div>
            {error && <p style={{ color: 'crimson' }}>{error}</p>}

            <h2>Companies</h2>
            <form onSubmit={addCompany} style={{ display: 'flex', gap: 8 }}>
                <input value={newCompany} onChange={(e) => setNewCompany(e.target.value)}
                    placeholder="New company name" required maxLength={128} />
                <button>Add company</button>
            </form>

            <h2>New QR code</h2>
            <form onSubmit={addCode} style={{ display: 'grid', gap: 8, maxWidth: 400 }}>
                <select value={companyId} onChange={(e) => setCompanyId(e.target.value)} required>
                    {companies.length === 0 && <option value="">Add a company first</option>}
                    {companies.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
                <input type="url" value={targetUrl} onChange={(e) => setTargetUrl(e.target.value)}
                    placeholder="https://example.com/menu" required />
                <button disabled={!companyId}>Create QR code</button>
            </form>

            <h2>Update QR code</h2>
            <form onSubmit={updateCode} style={{ display: 'grid', gap: 8, maxWidth: 400 }}>
                <input type="id" value={id} onChange={(e) => setId(e.target.value)}
                    placeholder="1" required />
                <input type="url" value={updateUrl} onChange={(e) => setUpdateUrl(e.target.value)}
                    placeholder="https://example.com/menu" required />
                <button disabled={!companyId}>Update QR code</button>
            </form>

            <h2>Your codes</h2>
            <div style={{ display: 'grid', gap: 16 }}>
                {codes.map((qr) => (
                    <div key={qr.id} style={{ display: 'flex', gap: 16, alignItems: 'center', opacity: qr.active ? 1 : 0.5 }}>
                        <img src={`/api/codes/${qr.id}/image?format=png&size=160&label=true`} alt={qr.shortCode} width={160} />
                        <div>
                            <strong>{qr.companyName}</strong>
                            <div>{qr.targetUrl}</div>
                            <small>{qr.shortUrl}</small>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <Toggle
                                checked={qr.active}
                                onChange={() => toggleActive(qr)}
                                disabled={togglingId === qr.id}
                                label={`${qr.active ? 'Deactivate' : 'Activate'} code ${qr.shortCode}`}
                            />
                            <span style={{ color: qr.active ? '#1f9d4a' : '#d92d20', fontWeight: 600 }}>
                                {qr.active ? 'Active' : 'Inactive'}
                            </span>

                            <button
                                onClick={() => setPendingDelete(qr)}
                                style={{ color: '#d92d20', background: 'none', border: '1px solid #d92d20', borderRadius: 6, padding: '4px 10px', cursor: 'pointer' }}
                            >
                                Delete
                            </button>
                        </div>
                        <ConfirmDialog
                            open={pendingDelete !== null}
                            title="Delete this QR code?"
                            message={pendingDelete && `${pendingDelete.companyName} (${pendingDelete.shortCode}). Printed copies will stop working. This can't be undone.`}
                            busy={deleting}
                            onConfirm={confirmDelete}
                            onCancel={() => setPendingDelete(null)}
                        />
                    </div>
                ))}
            </div>
        </div>
    );
}
