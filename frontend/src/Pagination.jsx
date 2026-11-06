export default function Pagination({ page, totalPages, onChange }) {
    if (totalPages <= 1) return null;
    return (
        <div className="mt-6 flex items-center justify-center gap-3 text-sm text-slate-600">
            <button className="btn" onClick={() => onChange(page - 1)} disabled={page === 0}>Previous</button>
            <span>Page {page + 1} of {totalPages}</span>
            <button className="btn" onClick={() => onChange(page + 1)}
                disabled={page + 1 >= totalPages}>Next</button>
        </div>
    );
}