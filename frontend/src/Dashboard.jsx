import { useEffect, useState } from 'react';
import { api } from './api';
import Toggle from './Toggle';
import ConfirmDialog from './ConfirmDialog';
import EditCodeDialog from './EditCodeDialog';

const PAGE_SIZE = 10;
const NO_FILTERS = { companyId: '', codeId: '', status: '' };

export default function Dashboard() {
    const [companies, setCompanies] = useState([]);
    const [codes, setCodes] = useState([]);
    const [pageInfo, setPageInfo] = useState({ totalItems: 0, totalPages: 0 });
    const [filters, setFilters] = useState(NO_FILTERS);
    const [page, setPage] = useState(0);
    const [reloadKey, setReloadKey] = useState(0);
    const [loading, setLoading] = useState(false);

    const [companyId, setCompanyId] = useState('');
    const [targetUrl, setTargetUrl] = useState('');
    const [newCompany, setNewCompany] = useState('');
    const [togglingId, setTogglingId] = useState(null);
    const [pendingDelete, setPendingDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const [error, setError] = useState('');

    const [editing, setEditing] = useState(null);
    const [editError, setEditError] = useState('');
    const [saving, setSaving] = useState(false);

    const reload = () => setReloadKey((k) => k + 1);
    const filtersActive = filters.companyId !== '' || filters.codeId !== '' || filters.status !== '';

    useEffect(() => {
        api.listCompanies()
            .then((cs) => {
                setCompanies(cs);
                if (cs.length) setCompanyId(String(cs[0].id));
            })
            .catch((err) => setError(err.message));
    }, []);

    useEffect(() => {
        let ignore = false;
        const timer = setTimeout(async () => {
            setLoading(true);
            try {
                const res = await api.listCodes({
                    companyId: filters.companyId,
                    id: filters.codeId,
                    active: filters.status,
                    page,
                    size: PAGE_SIZE,
                });
                if (ignore) return;
                if (res.items.length === 0 && page > 0) {
                    setPage(Math.max(0, res.totalPages - 1));
                    return;
                }
                setCodes(res.items);
                setPageInfo({ totalItems: res.totalItems, totalPages: res.totalPages });
            } catch (err) {
                if (!ignore) setError(err.message);
            } finally {
                if (!ignore) setLoading(false);
            }
        }, 250);
        return () => { ignore = true; clearTimeout(timer); };
    }, [filters, page, reloadKey]);

    function changeFilter(name, value) {
        setFilters((f) => ({ ...f, [name]: value }));
        setPage(0);
    }

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
            await api.createCode(Number(companyId), targetUrl);
            setTargetUrl('');
            setFilters(NO_FILTERS);
            setPage(0);
            reload();
        } catch (err) {
            setError(err.message);
        }
    }

    function openEdit(qr) {
        setEditError('');
        setEditing(qr);
    }

    async function saveEdit(url) {
        setEditError('');
        setSaving(true);
        try {
            await api.updateCode(editing.id, url);
            setEditing(null);
            reload();
        } catch (err) {
            setEditError(err.message);
        } finally {
            setSaving(false);
        }
    }

    async function toggleActive(qr) {
        setError('');
        setTogglingId(qr.id);
        try {
            await api.codeStatus(qr.id, !qr.active);
            reload();
        } catch (err) {
            setError(err.message);
        } finally {
            setTogglingId(null);
        }
    }

    async function downloadQr(code) {
        setError('');
        try {
            const blob = await api.downloadCodeZip(code.id);
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `qr-${code.shortCode}.zip`;
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);
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
            reload();
        } catch (err) {
            setError(err.message);
        } finally {
            setDeleting(false);
            setPendingDelete(null);
        }
    }

    return (
        <div className="space-y-8">
            {error && (
                <div role="alert" className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                    {error}
                </div>
            )}

            <div className="grid gap-6 md:grid-cols-2">
                <section className="card">
                    <h2 className="text-base font-semibold">Add company</h2>
                    <form onSubmit={addCompany} className="mt-4 flex gap-2">
                        <input className="input min-w-0 flex-1" value={newCompany}
                            onChange={(e) => setNewCompany(e.target.value)}
                            placeholder="New company name" required maxLength={128} />
                        <button className="btn btn-primary">Add</button>
                    </form>
                </section>

                <section className="card">
                    <h2 className="text-base font-semibold">New QR code</h2>
                    <form onSubmit={addCode} className="mt-4 grid gap-2">
                        <select className="input w-full" value={companyId}
                            onChange={(e) => setCompanyId(e.target.value)} required>
                            {companies.length === 0 && <option value="">Add a company first</option>}
                            {companies.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                        </select>
                        <input type="url" className="input w-full" value={targetUrl}
                            onChange={(e) => setTargetUrl(e.target.value)}
                            placeholder="https://example.com/menu" required />
                        <button className="btn btn-primary" disabled={!companyId}>Create QR code</button>
                    </form>
                </section>
            </div>

            <section>
                <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
                    <div>
                        <h2 className="text-lg font-semibold">Your codes</h2>
                        <p className="text-sm text-slate-500">
                            {pageInfo.totalItems} {pageInfo.totalItems === 1 ? 'code' : 'codes'}
                            {filtersActive && ' match'}
                        </p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                        <select className="input" value={filters.companyId}
                            onChange={(e) => changeFilter('companyId', e.target.value)}>
                            <option value="">All companies</option>
                            {companies.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                        </select>
                        <input type="number" min="1" placeholder="QR id" className="input w-24"
                            value={filters.codeId} onChange={(e) => changeFilter('codeId', e.target.value)} />
                        <select className="input" value={filters.status}
                            onChange={(e) => changeFilter('status', e.target.value)}>
                            <option value="">Active + inactive</option>
                            <option value="true">Active only</option>
                            <option value="false">Inactive only</option>
                        </select>
                        <button type="button" className="btn" disabled={!filtersActive}
                            onClick={() => { setFilters(NO_FILTERS); setPage(0); }}>
                            Clear filters
                        </button>
                    </div>
                </div>

                <div className={`grid gap-4 transition-opacity ${loading ? 'opacity-60' : ''}`}>
                    {codes.length === 0 && !loading && (
                        <div className="card text-center text-sm text-slate-500">No QR codes found.</div>
                    )}
                    {codes.map((qr) => (
                        <article key={qr.id} className="card flex flex-col gap-5 sm:flex-row sm:items-center">
                            <img
                                src={`/api/codes/${qr.id}/image?format=png&size=320&label=true`}
                                alt={qr.shortCode}
                                width={160}
                                className={`w-40 shrink-0 self-center ${qr.active ? '' : 'opacity-50'}`}
                            />
                            <div className={`min-w-0 flex-1 ${qr.active ? '' : 'opacity-50'}`}>
                                <h3 className="font-semibold">{qr.companyName}</h3>
                                <p className="mt-1 break-all text-sm text-slate-600">{qr.targetUrl}</p>
                                <p className="mt-1 font-mono text-xs text-slate-400">{qr.shortUrl}</p>
                            </div>

                            <div className="flex flex-wrap items-center gap-2 sm:justify-end">
                                <button className="btn" onClick={() => openEdit(qr)}>Edit</button>
                                <button className="btn" title="Downloads a zip with PNG and SVG"
                                    onClick={() => downloadQr(qr)}>
                                    Download
                                </button>
                            </div>

                            <p className="mt-2 text-sm text-slate-500">
                                <span className="font-semibold text-slate-900">
                                    {qr.scanCount}
                                </span>{' '}
                                {qr.scanCount === 1 ? 'hit' : 'hits'}
                            </p>

                            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                                    <button className="btn" onClick={() => openEdit(qr)}>Edit</button>
                                    <button className="btn" title="Downloads a zip with PNG and SVG" onClick={() => downloadQr(qr)}>
                                        Download
                                    </button>
                                    <Toggle
                                        checked={qr.active}
                                        onChange={() => toggleActive(qr)}
                                        disabled={togglingId === qr.id}
                                        label={`${qr.active ? 'Deactivate' : 'Activate'} code ${qr.shortCode}`}
                                    />
                                    <span className={`w-[4.5rem] text-center text-sm font-semibold ${qr.active ? 'text-emerald-600' : 'text-red-600'}`}>
                                        {qr.active ? 'Active' : 'Inactive'}
                                    </span>
                                    <button className="btn btn-danger" onClick={() => setPendingDelete(qr)}>Delete</button>
                                </div>
                            </div>
                        </article>
                    ))}
                </div>

                {pageInfo.totalPages > 1 && (
                    <div className="mt-6 flex items-center justify-center gap-3 text-sm text-slate-600">
                        <button className="btn" onClick={() => setPage(page - 1)} disabled={page === 0}>Previous</button>
                        <span>Page {page + 1} of {pageInfo.totalPages}</span>
                        <button className="btn" onClick={() => setPage(page + 1)}
                            disabled={page + 1 >= pageInfo.totalPages}>Next</button>
                    </div>
                )}
            </section>

            <ConfirmDialog
                open={pendingDelete !== null}
                title="Delete this QR code?"
                message={pendingDelete && `${pendingDelete.companyName} (${pendingDelete.shortCode}). Printed copies will stop working. This can't be undone.`}
                busy={deleting}
                onConfirm={confirmDelete}
                onCancel={() => setPendingDelete(null)}
            />
            {editing && (
                <EditCodeDialog
                    key={editing.id}
                    code={editing}
                    busy={saving}
                    error={editError}
                    onSave={saveEdit}
                    onCancel={() => setEditing(null)}
                />
            )}
        </div>
    );
}