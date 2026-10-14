import { useEffect, useState } from 'react';
import { api } from './api';

export default function Dashboard() {
    const [companies, setCompanies] = useState([]);
    const [codes, setCodes] = useState([]);
    const [companyId, setCompanyId] = useState('');
    const [targetUrl, setTargetUrl] = useState('');
    const [newCompany, setNewCompany] = useState('');
    const [error, setError] = useState('');

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

            <h2>Your codes</h2>
            <div style={{ display: 'grid', gap: 16 }}>
                {codes.map((qr) => (
                    <div key={qr.id} style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
                        <img src={`/api/codes/${qr.id}/image?format=png&size=160&label=true`} alt={qr.shortCode} width={160} height={160} />
                        <div>
                            <strong>{qr.companyName}</strong>
                            <div>{qr.targetUrl}</div>
                            <small>{qr.shortUrl}</small>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}