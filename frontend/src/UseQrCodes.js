import { useEffect, useState } from 'react';
import { api } from './api';

const PAGE_SIZE = 10;

function useDebounced(value, ms) {
    const [debounced, setDebounced] = useState(value);
    useEffect(() => {
        const t = setTimeout(() => setDebounced(value), ms);
        return () => clearTimeout(t);
    }, [value, ms]);
    return debounced;
}

export function useQrCodes(filters, page, setPage) {
    const [qrCodes, setQrCodes] = useState([]);
    const [pageInfo, setPageInfo] = useState({ totalItems: 0, totalPages: 0 });
    const [loading, setLoading] = useState(false);
    const [loadError, setLoadError] = useState('');
    const [reloadKey, setReloadKey] = useState(0);

    const codeId = useDebounced(filters.codeId, 250);
    const reload = () => setReloadKey((k) => k + 1);

    useEffect(() => {
        let ignore = false;
        (async () => {
            setLoading(true);
            try {
                const res = await api.listQrCodes({
                    companyId: filters.companyId,
                    id: codeId,
                    active: filters.status,
                    page,
                    size: PAGE_SIZE,
                });
                if (ignore) return;
                if (res.items.length === 0 && page > 0) {
                    setPage(Math.max(0, res.totalPages - 1));
                    return;
                }
                setLoadError('');
                setQrCodes(res.items);
                setPageInfo({ totalItems: res.totalItems, totalPages: res.totalPages });
            } catch (err) {
                if (!ignore) setLoadError(err.message);
            } finally {
                if (!ignore) setLoading(false);
            }
        })();
        return () => { ignore = true; };
    }, [filters.companyId, filters.status, codeId, page, reloadKey, setPage]);

    return { qrCodes, pageInfo, loading, loadError, reload };
}