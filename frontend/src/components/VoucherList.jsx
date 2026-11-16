import Barcode from 'react-barcode';
import PropTypes from 'prop-types';

function VoucherList({ vouchers, onMarkUsed, pendingVoucherId }) {
  const availableBalance = vouchers.reduce(
    (total, voucher) => voucher.used ? total : total + Number(voucher.amount),
    0
  );

  return (
    <section className="form-card voucher-list">
      <div className="voucher-list-heading">
        <h2>Bonuri salvate</h2>
        <p aria-live="polite">Sold disponibil: {availableBalance.toFixed(2)} RON</p>
      </div>
      <ul className="voucher-items">
        {vouchers.map((voucher) => (
          <li className="voucher-item" key={voucher.id}>
            <div className="voucher-summary">
              <strong>{voucher.storeName}</strong>
              <span>{voucher.amount} RON</span>
              <span>Status: {voucher.used ? 'Folosit' : 'Activ'}</span>
            </div>
            <p className="voucher-code">{voucher.voucherCode}</p>
            <div
              className="voucher-barcode"
              role="img"
              aria-label={`Cod de bare pentru bonul ${voucher.voucherCode}`}
            >
              <Barcode
                value={voucher.voucherCode}
                format="CODE128"
                width={1.5}
                height={64}
                displayValue={false}
                margin={0}
              />
            </div>
            <label className="voucher-used-toggle">
              <input
                type="checkbox"
                aria-label={`Marchează bonul ${voucher.voucherCode} ca folosit`}
                checked={voucher.used}
                disabled={voucher.used || pendingVoucherId === voucher.id}
                onChange={(event) => {
                  if (event.target.checked) {
                    onMarkUsed(voucher.id);
                  }
                }}
              />
              {pendingVoucherId === voucher.id ? 'Se marchează...' : 'Marchează ca folosit'}
            </label>
          </li>
        ))}
      </ul>
    </section>
  );
}

VoucherList.propTypes = {
  vouchers: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
    storeName: PropTypes.string.isRequired,
    voucherCode: PropTypes.string.isRequired,
    amount: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
    used: PropTypes.bool.isRequired
  })).isRequired,
  onMarkUsed: PropTypes.func.isRequired,
  pendingVoucherId: PropTypes.oneOfType([PropTypes.number, PropTypes.string])
};

export default VoucherList;
