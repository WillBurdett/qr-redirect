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
    const [updateId, setUpdateId] = useState('');
    const [updateUrl, setUpdateUrl] = useState('');
    const [togglingId, setTogglingId] = useState(null);
    const [pendingDelete, setPendingDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const [error, setError] = useState('');

    const [editing, setEditing] = useState(null);     // the code being edited
    const [editError, setEditError] = useState('');
    const [saving, setSaving] = useState(false);

    const reload = () => setReloadKey((k) => k + 1);
    const filtersActive = filters.companyId !== '' || filters.codeId !== '' || filters.status !== '';

    // Companies, once
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
        const timer = setTimeout(async () => {      // short delay so typing an id doesn't fire per keystroke
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
                    setPage(Math.max(0, res.totalPages - 1));   // e.g. deleted the last code on this page
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
        setPage(0);                                  // a new filter always starts from page 1
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
            setFilters(NO_FILTERS);                  // make sure the new code isn't hidden by a filter
            setPage(0);
            reload();
        } catch (err) {
            setError(err.message);
        }
    }

    async function updateCode(e) {
        e.preventDefault();
        setError('');
        try {
            await api.updateTarget(Number(updateId), updateUrl);
            setUpdateId('');
            setUpdateUrl('');
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
            setEditError(err.message);     // stays open so they can fix the URL
        } finally {
            setSaving(false);
        }
    }

    async function toggleActive(qr) {
        setError('');
        setTogglingId(qr.id);
        try {
            await api.codeStatus(qr.id, !qr.active);
            reload();      // reload, not patch: with an "Active" filter on, a deactivated code should drop out
        } catch (err) {
            setError(err.message);
        } finally {
            setTogglingId(null);
        }
    }

    async function downloadQr(qr, format) {
        const params = new URLSearchParams({ format, ec: "Q" });
        if (format === "png") params.set("size", "2048");

        const res = await fetch(`/api/qr-codes/${qr.id}/image?${params}`, {
            credentials: "include",
        });
        if (!res.ok) throw new Error(`Download failed (${res.status})`);

        const url = URL.createObjectURL(await res.blob());
        const a = document.createElement("a");
        a.href = url;
        a.download = `${qr.shortCode}.${format}`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
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
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 12 }}>
                <select value={filters.companyId} onChange={(e) => changeFilter('companyId', e.target.value)}>
                    <option value="">All companies</option>
                    {companies.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
                <input type="number" min="1" placeholder="QR id" style={{ width: 90 }}
                    value={filters.codeId} onChange={(e) => changeFilter('codeId', e.target.value)} />
                <select value={filters.status} onChange={(e) => changeFilter('status', e.target.value)}>
                    <option value="">Active + inactive</option>
                    <option value="true">Active only</option>
                    <option value="false">Inactive only</option>
                </select>
                <button type="button" disabled={!filtersActive}
                    onClick={() => { setFilters(NO_FILTERS); setPage(0); }}>
                    Clear filters
                </button>
            </div>

            <p style={{ color: '#666', margin: '0 0 12px' }}>
                {pageInfo.totalItems} {pageInfo.totalItems === 1 ? 'code' : 'codes'}
                {filtersActive && ' match'}
            </p>

            <div style={{ display: 'grid', gap: 16, opacity: loading ? 0.6 : 1, transition: 'opacity 0.15s' }}>
                {codes.length === 0 && !loading && <p>No QR codes found.</p>}
                {codes.map((qr) => (
                    <div key={qr.id} style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
                        <img src={`/api/codes/${qr.id}/image?format=png&size=160&label=true`}
                            alt={qr.shortCode} width={160} style={{ opacity: qr.active ? 1 : 0.5 }} />
                        <div style={{ opacity: qr.active ? 1 : 0.5 }}>
                            <strong>{qr.companyName}</strong>
                            <div>{qr.targetUrl}</div>
                            <small>{qr.shortUrl}</small>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <button
                                onClick={() => openEdit(qr)}
                                style={{ background: 'none', border: '1px solid #888', borderRadius: 6, padding: '4px 10px', cursor: 'pointer' }}
                            >
                                Edit
                            </button>
                            <button onClick={() => downloadQr(qr, "svg")}>Download SVG</button>
                            <button onClick={() => downloadQr(qr, "png")}>Download PNG</button>
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
                    </div>
                ))}
            </div>

            {pageInfo.totalPages > 1 && (
                <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginTop: 16 }}>
                    <button onClick={() => setPage(page - 1)} disabled={page === 0}>Previous</button>
                    <span>Page {page + 1} of {pageInfo.totalPages}</span>
                    <button onClick={() => setPage(page + 1)} disabled={page + 1 >= pageInfo.totalPages}>Next</button>
                </div>
            )}

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