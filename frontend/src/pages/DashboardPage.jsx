import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

function DashboardPage() {
  const [voucher, setVoucher] = useState({
    voucherCode: '',
    storeName: '',
    amount: '',
    issuedAt: '',
    expiresAt: ''
  });
  const [vouchers, setVouchers] = useState([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/vouchers')
      .then((response) => setVouchers(response.data || []))
      .catch(() => {});
  }, []);

  const addVoucher = async (event) => {
    event.preventDefault();
    setMessage('');
    setError('');
    try {
      const response = await api.post('/vouchers', { ...voucher, source: 'MANUAL' });
      setMessage('Bonul a fost adaugat cu succes.');
      setVouchers((prev) => [response.data, ...prev]);
      setVoucher({ voucherCode: '', storeName: '', amount: '', issuedAt: '', expiresAt: '' });
    } catch (err) {
      const serverMessage = err.response?.data?.message;
      setError(serverMessage || 'Bonul nu a putut fi adaugat. Verifica conexiunea la internet si datele introduse.');
    }
  };

  return (
    <main className="page">
      <header className="topbar">
        <strong>SGRWallet</strong>
        <button className="secondary" onClick={() => localStorage.removeItem('sgr_token')}>Sign out</button>
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
          <label>Store name<input required value={voucher.storeName}
            onChange={(event) => setVoucher({ ...voucher, storeName: event.target.value })} /></label>
          <label>Amount<input type="number" min="0.01" step="0.01" required value={voucher.amount}
            onChange={(event) => setVoucher({ ...voucher, amount: event.target.value })} /></label>
          <label>Issue date<input type="date" required value={voucher.issuedAt}
            onChange={(event) => setVoucher({ ...voucher, issuedAt: event.target.value })} /></label>
          <label>Expiration date<input type="date" required value={voucher.expiresAt}
            onChange={(event) => setVoucher({ ...voucher, expiresAt: event.target.value })} /></label>
          <button>Add voucher</button>
        </form>

        {vouchers.length > 0 && (
          <div className="form-card" style={{ marginTop: '1.5rem' }}>
            <h2>Saved vouchers</h2>
            <ul>
              {vouchers.map((item, idx) => (
                <li key={item.id ?? idx}>
                  <strong>{item.storeName}</strong> — {item.voucherCode} ({item.amount} RON)
                </li>
              ))}
            </ul>
          </div>
        )}

        <Link to="/" className="switch-link">Back to login</Link>
      </section>
    </main>
  );
}

export default DashboardPage;
