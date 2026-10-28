import { useAuth } from './AuthContext';
import AuthPage from './AuthPage';
import Dashboard from './Dashboard';

export default function App() {
  const { user, loading, logout } = useAuth();

  if (loading) return <p className="p-8 text-center text-slate-500">Loading…</p>;
  if (!user) return <AuthPage />;

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <span className="text-lg font-semibold tracking-tight">QR-Redirect</span>
          <div className="flex items-center gap-3 text-sm text-slate-600">
            <span className="hidden sm:inline">{user.name} ({user.email})</span>
            <button className="btn" onClick={logout}>Log out</button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Dashboard />
      </main>
    </div>
  );
}