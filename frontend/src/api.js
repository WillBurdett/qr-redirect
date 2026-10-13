async function request(path, options = {}) {
    const res = await fetch(path, {
        headers: { 'Content-Type': 'application/json' },
        ...options,
    });
    if (res.status === 204) return null;

    const body = await res.json().catch(() => null);
    if (!res.ok) {
        const err = new Error(body?.message || 'Something went wrong');
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
};