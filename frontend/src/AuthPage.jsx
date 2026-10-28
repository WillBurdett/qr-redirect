import { useState } from 'react';
import { useAuth } from './AuthContext';

export default function AuthPage() {
    const { login } = useAuth();
    const [form, setForm] = useState({ email: '', password: '' });
    const [error, setError] = useState('');
    const [busy, setBusy] = useState(false);

    const update = (e) => setForm({ ...form, [e.target.name]: e.target.value });

    async function submit(e) {
        e.preventDefault();
        setError('');
        setBusy(true);
        try {
            await login(form.email, form.password);
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    }

    return (
        <div className="flex min-h-screen items-center justify-center px-4">
            <form onSubmit={submit} className="card w-full max-w-sm space-y-4 p-8">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Log in</h1>
                    <p className="mt-1 text-sm text-slate-500">QR-Redirect admin</p>
                </div>

                <input name="email" type="email" placeholder="Email" autoComplete="username"
                    className="input w-full" value={form.email} onChange={update} required />
                <input name="password" type="password" placeholder="Password" autoComplete="current-password"
                    className="input w-full" value={form.password} onChange={update} required />

                {error && <p className="text-sm text-red-600">{error}</p>}

                <button className="btn btn-primary w-full" disabled={busy}>
                    {busy ? 'Logging in…' : 'Log in'}
                </button>
            </form>
        </div>
    );
}