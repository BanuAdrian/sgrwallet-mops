import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api';
import VoucherList from '../components/VoucherList';

function calculateExpiryDate(issueDate) {
  if (!issueDate) {
    return '';
  }

  const expiryDate = new Date(`${issueDate}T00:00:00Z`);
  if (Number.isNaN(expiryDate.getTime())) {
    return '';
  }

  expiryDate.setUTCFullYear(expiryDate.getUTCFullYear() + 1);
  return expiryDate.toISOString().slice(0, 10);
}

function DashboardPage() {
  const [voucher, setVoucher] = useState({
    storeName: '',
    voucherCode: '',
    amount: '',
    issuedAt: '',
    expiresAt: ''
  });
  const [voucherSource, setVoucherSource] = useState('MANUAL');
  const [vouchers, setVouchers] = useState([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [image, setImage] = useState(null);
  const [scanning, setScanning] = useState(false);
  const [scanPreview, setScanPreview] = useState(null);
  const [pendingVoucherId, setPendingVoucherId] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    api.get('/vouchers')
      .then((response) => setVouchers(response.data || []))
      .catch(() => {});
  }, []);

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
        storeName: data.store_name || '',
        voucherCode: data.barcode_number || '',
        amount: data.voucher_value ?? '',
        issuedAt: data.print_date || '',
        expiresAt: calculateExpiryDate(data.print_date)
      });
      setVoucherSource('OCR');
    } catch (scanError) {
      setError(scanError.name === 'AbortError'
        ? 'OCR scan timed out. Check the OCR logs or try a clearer image.'
        : 'OCR scan failed. Check that the OCR service is running.');
    } finally {
      clearTimeout(timeout);
      setScanning(false);
    }
  };

  const signOut = () => {
    localStorage.removeItem('sgr_token');
    localStorage.removeItem('sgr_role');
    localStorage.removeItem('sgr_email');
    navigate('/', { replace: true });
  };

  const updateIssuedAt = (event) => {
    const issuedAt = event.target.value;
    setVoucher((previous) => ({
      ...previous,
      issuedAt,
      expiresAt: calculateExpiryDate(issuedAt)
    }));
  };

  const addVoucher = async (event) => {
    event.preventDefault();
    setMessage('');
    setError('');
    try {
      const response = await api.post('/vouchers', { ...voucher, source: voucherSource });
      setMessage('Bonul a fost adaugat cu succes.');
      setVouchers((prev) => [response.data, ...prev]);
      setVoucher({ storeName: '', voucherCode: '', amount: '', issuedAt: '', expiresAt: '' });
      setVoucherSource('MANUAL');
    } catch (err) {
      const serverMessage = err.response?.data?.message;
      if (serverMessage) {
        setError(serverMessage);
      } else if (!err.response) {
        setError('Serverul backend (http://localhost:8080) nu raspunde. Asigura-te ca PostgreSQL si Spring Boot sunt pornite.');
      } else {
        setError(`Bonul nu a putut fi adaugat (HTTP ${err.response.status}).`);
      }
    }
  };

  const markVoucherUsed = async (voucherId) => {
    setMessage('');
    setError('');
    setPendingVoucherId(voucherId);
    try {
      const response = await api.patch(`/vouchers/${voucherId}/used`);
      setVouchers((previous) => previous.map((item) => (
        item.id === voucherId ? response.data : item
      )));
      setMessage('Bonul a fost marcat ca folosit.');
    } catch (err) {
      const serverMessage = err.response?.data?.message;
      setError(serverMessage || `Bonul nu a putut fi marcat ca folosit${err.response
        ? ` (HTTP ${err.response.status}).`
        : '.'}`);
    } finally {
      setPendingVoucherId(null);
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
        <form onSubmit={addVoucher} className="form-card voucher-form">
          {message && <p className="success">{message}</p>}
          {error && <p className="error">{error}</p>}
          <label>
            Nume magazin *
            <input required name="storeName" placeholder="ex: Lidl, Kaufland, Carrefour"
              value={voucher.storeName}
              onChange={(event) => setVoucher({ ...voucher, storeName: event.target.value })} />
          </label>
          <label>
            Valoare (RON) *
            <input type="number" min="0.01" step="0.01" required name="amount"
              placeholder="ex: 0.50" value={voucher.amount}
              onChange={(event) => setVoucher({ ...voucher, amount: event.target.value })} />
          </label>
          <label>
            Data emiterii
            <input type="date" name="issuedAt" value={voucher.issuedAt}
              onChange={updateIssuedAt} />
          </label>
          <label>
            Data expirării *
            <input type="date" required name="expiresAt" value={voucher.expiresAt}
              onChange={(event) => setVoucher({ ...voucher, expiresAt: event.target.value })} />
          </label>
          <label>
            Cod voucher *
            <input required name="voucherCode" placeholder="ex: 201001502000000294104352"
              value={voucher.voucherCode}
              onChange={(event) => setVoucher({ ...voucher, voucherCode: event.target.value })} />
          </label>
          <button>{voucherSource === 'OCR' ? 'Confirmă voucherul' : 'Adaugă voucher'}</button>
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

        {vouchers.length > 0 && (
          <VoucherList
            vouchers={vouchers}
            onMarkUsed={markVoucherUsed}
            pendingVoucherId={pendingVoucherId}
          />
        )}

        <Link to="/" className="switch-link">Back to login</Link>
      </section>
    </main>
  );
}

export default DashboardPage;
