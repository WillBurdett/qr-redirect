import { useAuth } from './AuthContext';
import AuthPage from './AuthPage';

export default function App() {
  const { user, loading, logout } = useAuth();

  if (loading) return <p>Loading…</p>;
  if (!user) return <AuthPage />;

  return (
    <div style={{ maxWidth: 480, margin: '4rem auto' }}>
      <h1>Hi {user.name}</h1>
      <p>{user.email}</p>
      <button onClick={logout}>Log out</button>
    </div>
  );
}