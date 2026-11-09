import { useEffect, useState } from 'react';
import { api } from './api';
import ConfirmDialog from './ConfirmDialog';
import EditCodeDialog from './EditQrCodeDialog';
import QrCodeCard from './QrCodeCard';
import { useQrCodes } from './UseQrCodes';
import AddCompanyForm from './AddCompanyForm';
import AddQrCodeForm from './AddQrCodeForm';
import QrFilters from './QrFilters';
import Pagination from './Pagination';

const NO_FILTERS = { companyId: '', codeId: '', status: '' };

export default function Dashboard() {
    const [companies, setCompanies] = useState([]);
    const [filters, setFilters] = useState(NO_FILTERS);
    const [page, setPage] = useState(0);
    const { qrCodes, pageInfo, loading, loadError, reload } = useQrCodes(filters, page, setPage);

    const [companyId, setCompanyId] = useState('');
    const [togglingId, setTogglingId] = useState(null);
    const [pendingDelete, setPendingDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const [error, setError] = useState('');

    const [editing, setEditing] = useState(null);
    const [editError, setEditError] = useState('');
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        api.listCompanies()
            .then((cs) => {
                setCompanies(cs);
                if (cs.length) setCompanyId(String(cs[0].id));
            })
            .catch((err) => setError(err.message));
    }, []);

    function changeFilter(name, value) {
        setFilters((f) => ({ ...f, [name]: value }));
        setPage(0);
    }

    async function addCompany(name, planType) {
        return run(async () => {
            const company = await api.createCompany(name, planType);
            setCompanies((cs) => [...cs, company]);
            return true;
        });
    }

    async function addQrCode(companyId, url) {
        return run(async () => {
            await api.createQrCode(Number(companyId), url);
            setFilters(NO_FILTERS);
            setPage(0);
            reload();
            return true;
        });
    }

    function openEdit(qr) {
        setEditError('');
        setEditing(qr);
    }

    async function saveEdit(url) {
        setEditError('');
        setSaving(true);
        await run(async () => {
            await api.updateQrCode(editing.id, url);
            setEditing(null);
            reload();
        }, { onError: setEditError });
        setSaving(false);
    }

    async function toggleActive(qr) {
        setTogglingId(qr.id);
        await run(async () => {
            await api.qrCodeStatus(qr.id, !qr.active);
            reload();
        });
        setTogglingId(null);
    }

    async function downloadQr(qrCode) {
        console.log(qrCode, Object.keys(qrCode));
        await run(async () => {
            const blob = await api.downloadQrCodeZip(qrCode.id);
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `qr-${qrCode.shortQrId}.zip`;
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);
        });
    }

    async function confirmDelete() {
        const target = pendingDelete;
        setDeleting(true);
        await run(async () => {
            await api.deleteQrCode(target.id);
            reload();
        })
        setDeleting(false);
        setPendingDelete(null);
    }

    async function run(action, { onError = setError } = {}) {
        setError('');
        try { return await action(); }
        catch (err) { onError(err.message); }
    }

    function clearFilters() {
        setFilters(NO_FILTERS);
        setPage(0);
    }

    return (
        <div className="space-y-8">
            {(error || loadError) && (
                <div role="alert" className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                    {error || loadError}
                </div>
            )}


            <div className="grid gap-6 md:grid-cols-2">
                <AddCompanyForm onSubmit={addCompany} />
                <AddQrCodeForm companies={companies} onSubmit={addQrCode} />
            </div>

            <section>
                <QrFilters
                    filters={filters}
                    companies={companies}
                    totalItems={pageInfo.totalItems}
                    onChange={changeFilter}
                    onClear={clearFilters}
                />

                <div className={`grid gap-4 transition-opacity ${loading ? 'opacity-60' : ''}`}>
                    {qrCodes.length === 0 && !loading && (
                        <div className="card text-center text-sm text-slate-500">No QR codes found.</div>
                    )}
                    {qrCodes.map((qr) => (
                        <QrCodeCard
                            key={qr.id}
                            qr={qr}
                            toggling={togglingId === qr.id}
                            onEdit={openEdit}
                            onDownload={downloadQr}
                            onToggle={toggleActive}
                            onDelete={setPendingDelete}
                        />
                    ))}
                </div>

                <Pagination page={page} totalPages={pageInfo.totalPages} onChange={setPage} />
            </section>

            <ConfirmDialog
                open={pendingDelete !== null}
                title="Delete this QR code?"
                message={pendingDelete && `${pendingDelete.companyName} (${pendingDelete.shortQrId}). Printed copies will stop working. This can't be undone.`}
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