import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api';

function RegisterPage() {
  const [form, setForm] = useState({
    email: '', password: '', confirmPassword: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    if (form.password !== form.confirmPassword) {
      setError('Parolele nu coincid.');
      return;
    }
    setLoading(true);
    try {
      await api.post('/auth/register', {
        email: form.email,
        password: form.password
      });
      navigate('/', { state: { registered: true } });
    } catch (error) {
      setError(error.response?.data?.message || 'Înregistrarea a eșuat. Încearcă din nou.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-layout">
      <section className="auth-panel">
        <p className="eyebrow">SGRWallet</p>
        <h1>Create an account</h1>
        <p className="muted">Start managing your vouchers in one place.</p>
        <form onSubmit={submit} className="form-card">
          {error && <p className="error">{error}</p>}
          <label>
            Email
            <input type="email" required value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })} />
          </label>
          <label>
            Password
            <input type="password" minLength="8" required value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })} />
          </label>
          <label>
            Confirm password
            <input type="password" minLength="8" required value={form.confirmPassword}
              onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })} />
          </label>
          <button disabled={loading}>{loading ? 'Creating...' : 'Create account'}</button>
        </form>
        <p className="switch-link">Already registered? <Link to="/">Sign in</Link></p>
      </section>
    </main>
  );
}

export default RegisterPage;
