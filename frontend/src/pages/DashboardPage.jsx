import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api';

function DashboardPage() {
  const [voucher, setVoucher] = useState({
    voucherCode: '',
    amount: '',
    issuedAt: '',
    expiresAt: ''
  });
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const signOut = () => {
    localStorage.removeItem('sgr_token');
    localStorage.removeItem('sgr_role');
    localStorage.removeItem('sgr_email');
    navigate('/', { replace: true });
  };

  const addVoucher = async (event) => {
    event.preventDefault();
    setMessage('');
    setError('');
    try {
      await api.post('/vouchers', { ...voucher, source: 'MANUAL' });
      setMessage('Voucher added.');
      setVoucher({ voucherCode: '', amount: '', issuedAt: '', expiresAt: '' });
    } catch {
      setError('Voucher could not be added. The backend endpoint may not be implemented yet.');
    }
  };

  return (
    <main className="page">
      <header className="topbar">
        <strong>SGRWallet</strong>
        <button className="secondary" onClick={signOut}>Sign out</button>
      </header>
      <section className="content">
        <p className="eyebrow">Voucher wallet</p>
        <h1>Add a voucher</h1>
        <p className="muted">This screen is the starting point for US-04, US-05A/05B, US-08 and US-16.</p>
        <form onSubmit={addVoucher} className="form-card voucher-form">
          {message && <p className="success">{message}</p>}
          {error && <p className="error">{error}</p>}
          <label>Voucher code<input required value={voucher.voucherCode}
            onChange={(event) => setVoucher({ ...voucher, voucherCode: event.target.value })} /></label>
          <label>Amount<input type="number" min="0" step="0.01" required value={voucher.amount}
            onChange={(event) => setVoucher({ ...voucher, amount: event.target.value })} /></label>
          <label>Issue date<input type="date" required value={voucher.issuedAt}
            onChange={(event) => setVoucher({ ...voucher, issuedAt: event.target.value })} /></label>
          <label>Expiration date<input type="date" required value={voucher.expiresAt}
            onChange={(event) => setVoucher({ ...voucher, expiresAt: event.target.value })} /></label>
          <button>Add voucher</button>
        </form>
        <Link to="/" className="switch-link">Back to login</Link>
      </section>
    </main>
  );
}

export default DashboardPage;
