export default function QrFilters({ filters, companies, totalItems, onChange, onClear }) {
    const active = filters.companyId !== '' || filters.codeId !== '' || filters.status !== '';

    return (
        <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
            <div>
                <h2 className="text-lg font-semibold">Your QR codes</h2>
                <p className="text-sm text-slate-500">
                    {totalItems} {totalItems === 1 ? 'code' : 'codes'}
                    {active && ' match'}
                </p>
            </div>
            <div className="flex flex-wrap gap-2">
                <select className="input" value={filters.companyId}
                    onChange={(e) => onChange('companyId', e.target.value)}>
                    <option value="">All companies</option>
                    {companies.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
                <input type="number" min="1" placeholder="QR id" className="input w-24"
                    value={filters.codeId} onChange={(e) => onChange('codeId', e.target.value)} />
                <select className="input" value={filters.status}
                    onChange={(e) => onChange('status', e.target.value)}>
                    <option value="">Active + inactive</option>
                    <option value="true">Active only</option>
                    <option value="false">Inactive only</option>
                </select>
                <button type="button" className="btn" disabled={!active} onClick={onClear}>
                    Clear filters
                </button>
            </div>
        </div>
    );
}