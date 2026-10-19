import { useState } from 'react';
import { useAuth } from './AuthContext';

export default function AuthPage() {
    const { login } = useAuth();
    const [form, setForm] = useState({ name: '', email: '', password: '' });
    const [error, setError] = useState('');
    const [busy, setBusy] = useState(false);

    const update = (e) => setForm({ ...form, [e.target.name]: e.target.value });

    async function submit(e) {
        e.preventDefault();
        setError('');
        setBusy(true);
        try {
            login(form.email, form.password);
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    }

    return (
        <form onSubmit={submit} style={{ maxWidth: 320, margin: '4rem auto', display: 'grid', gap: 12 }}>
            <h1>{'Log in'}</h1>

            <input name="email" type="email" placeholder="Email" value={form.email} onChange={update} required />
            <input
                name="password"
                type="password"
                placeholder="Password"
                value={form.password}
                onChange={update}
                required
            />

            {error && <p style={{ color: 'crimson', margin: 0 }}>{error}</p>}

            <button disabled={busy}>{'Log in'}</button>
        </form>
    );
}