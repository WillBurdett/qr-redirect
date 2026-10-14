async function request(path, options = {}) {
    const res = await fetch(path, {
        headers: { 'Content-Type': 'application/json' },
        ...options,
    });
    if (res.status === 204) return null;

    const body = await res.json().catch(() => null);
    if (!res.ok) {
        const err = new Error(body?.message || body?.error || 'Something went wrong');
        err.status = res.status;
        throw err;
    }
    return body;
}

// cookies are automatically sent with fetch requests, so we don't need to do anything special to include them
export const api = {
    me: () => request('/auth/me'),
    login: (email, password) =>
        request('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
    logout: () => request('/auth/logout', { method: 'POST' }),
    listCompanies: () => request('/api/companies'),
    createCompany: (name) =>
        request('/api/companies', { method: 'POST', body: JSON.stringify({ name }) }),
    listCodes: () => request('/api/codes'),
    createCode: (companyId, targetUrl) =>
        request('/api/codes', { method: 'POST', body: JSON.stringify({ companyId, targetUrl }) }),
};