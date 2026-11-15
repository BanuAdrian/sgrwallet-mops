import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api';

function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setLoading(true);

    try {
      const { data } = await api.post('/auth/login', form);
      localStorage.setItem('sgr_token', data.token);
      localStorage.setItem('sgr_role', data.role);
      localStorage.setItem('sgr_email', data.email);
      navigate('/dashboard');
    } catch (error) {
      setError(error.response?.data?.message || 'Email sau parolă incorectă.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-layout">
      <section className="auth-panel">
        <p className="eyebrow">SGRWallet</p>
        <h1>Welcome back</h1>
        <p className="muted">Sign in to manage your deposit-return vouchers.</p>
        <form onSubmit={submit} className="form-card">
          {error && <p className="error">{error}</p>}
          <label>
            Email
            <input type="email" required value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })} />
          </label>
          <label>
            Password
            <input type="password" required value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })} />
          </label>
          <button disabled={loading}>{loading ? 'Signing in...' : 'Sign in'}</button>
        </form>
        <p className="switch-link">No account? <Link to="/register">Create one</Link></p>
      </section>
    </main>
  );
}

export default LoginPage;
