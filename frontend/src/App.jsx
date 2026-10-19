import { useAuth } from './AuthContext';
import AuthPage from './AuthPage';
import Dashboard from './Dashboard';

export default function App() {
  const { user, loading, logout } = useAuth();

  if (loading) return <p>Loading…</p>;
  if (!user) return <AuthPage />;

  return (
    <div style={{ maxWidth: 640, margin: '2rem auto' }}>
      <header style={{ display: 'flex', justifyContent: 'space-between' }}>
        <span>{user.name} ({user.email})</span>
        <button onClick={logout}>Log out</button>
      </header>
      <Dashboard />
    </div>
  );
}