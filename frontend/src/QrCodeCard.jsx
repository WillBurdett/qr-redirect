import Toggle from './Toggle';

export default function QrCodeCard({ qr, toggling, onEdit, onDownload, onToggle, onDelete }) {
    const dim = qr.active ? '' : 'opacity-50';
    return (
        <article className="card flex flex-col gap-5 sm:flex-row sm:items-center">
            <img
                src={`/api/codes/${qr.id}/image?format=png&size=320&label=true`}
                alt={qr.shortQrId}
                width={160}
                className={`w-40 shrink-0 self-center ${dim}`}
            />
            <div className={`min-w-0 flex-1 ${dim}`}>
                <h3 className="font-semibold">{qr.companyName}</h3>
                <p className="mt-1 break-all text-sm text-slate-600">{qr.targetUrl}</p>
                <p className="mt-1 font-mono text-xs text-slate-400">{qr.shortUrl}</p>
            </div>

            <div className="flex flex-wrap items-center gap-2 sm:justify-end">
                <button className="btn" onClick={() => onEdit(qr)}>Edit</button>
                <button className="btn" title="Downloads a zip with PNG and SVG" onClick={() => onDownload(qr)}>
                    Download
                </button>
            </div>

            <p className="mt-2 text-sm text-slate-500">
                <span className="font-semibold text-slate-900">{qr.scanCount}</span>{' '}
                {qr.scanCount === 1 ? 'hit' : 'hits'}
            </p>

            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <Toggle
                    checked={qr.active}
                    onChange={() => onToggle(qr)}
                    disabled={toggling}
                    label={`${qr.active ? 'Deactivate' : 'Activate'} code ${qr.shortQrId}`}
                />
                <span className={`w-[4.5rem] text-center text-sm font-semibold ${qr.active ? 'text-emerald-600' : 'text-red-600'}`}>
                    {qr.active ? 'Active' : 'Inactive'}
                </span>
                <button className="btn btn-danger" onClick={() => onDelete(qr)}>Delete</button>
            </div>
        </article>
    );
}