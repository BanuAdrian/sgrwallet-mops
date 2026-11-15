import { useState } from 'react';
import { Link } from 'react-router-dom';
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
  const [image, setImage] = useState(null);
  const [scanning, setScanning] = useState(false);
  const [scanPreview, setScanPreview] = useState(null);

  const scanVoucher = async () => {
    if (!image) {
      setError('Select a voucher image first.');
      return;
    }

    setError('');
    setMessage('');
    setScanning(true);
    const formData = new FormData();
    formData.append('file', image);
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 60000);

    try {
      const response = await fetch(
        `${import.meta.env.VITE_OCR_URL || 'http://localhost:8000/api'}/scan-voucher`,
        { method: 'POST', body: formData, signal: controller.signal }
      );
      if (!response.ok) {
        throw new Error('OCR request failed');
      }
      const data = await response.json();
      setScanPreview(data);
      setVoucher({
        voucherCode: data.barcode_number || '',
        amount: data.voucher_value ?? '',
        issuedAt: data.print_date || '',
        expiresAt: ''
      });
    } catch (scanError) {
      setError(scanError.name === 'AbortError'
        ? 'OCR scan timed out. Check the OCR logs or try a clearer image.'
        : 'OCR scan failed. Check that the OCR service is running.');
    } finally {
      clearTimeout(timeout);
      setScanning(false);
    }
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
          <label>Amount<input type="number" min="0" step="0.01" required value={voucher.amount}
            onChange={(event) => setVoucher({ ...voucher, amount: event.target.value })} /></label>
          <label>Issue date<input type="date" required value={voucher.issuedAt}
            onChange={(event) => setVoucher({ ...voucher, issuedAt: event.target.value })} /></label>
          <label>Expiration date<input type="date" required value={voucher.expiresAt}
            onChange={(event) => setVoucher({ ...voucher, expiresAt: event.target.value })} /></label>
          <button>Add voucher</button>
        </form>
        <section className="form-card voucher-form">
          <h2>Scan voucher image</h2>
          <p className="muted">OCR fills the fields below. Review and correct them before saving.</p>
          <input type="file" accept="image/*" capture="environment"
            onChange={(event) => setImage(event.target.files?.[0] || null)} />
          <button type="button" onClick={scanVoucher} disabled={scanning}>
            {scanning ? 'Scanning...' : 'Scan with OCR'}
          </button>
          {scanPreview && <p className="success">Review the extracted values, then add the voucher.</p>}
        </section>
        <Link to="/" className="switch-link">Back to login</Link>
      </section>
    </main>
  );
}

export default DashboardPage;
