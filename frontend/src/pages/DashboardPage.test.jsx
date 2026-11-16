import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import api from '../services/api';
import DashboardPage from './DashboardPage';

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
    post: vi.fn()
  }
}));

const activeVoucher = {
  id: 1,
  storeName: 'Lidl',
  voucherCode: '201001502000000294104352',
  amount: 10.5,
  used: false
};

const usedVoucher = { ...activeVoucher, used: true };

function renderDashboard() {
  return render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>
  );
}

describe('DashboardPage voucher usage', () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    vi.clearAllMocks();
    api.get.mockResolvedValue({ data: [activeVoucher] });
    api.patch.mockResolvedValue({ data: usedVoucher });
  });

  it('displays the voucher barcode and available balance', async () => {
    renderDashboard();

    const barcode = await screen.findByRole('img', {
      name: `Cod de bare pentru bonul ${activeVoucher.voucherCode}`
    });
    expect(barcode.querySelectorAll('svg rect').length).toBeGreaterThan(0);
    expect(screen.getByText(activeVoucher.voucherCode)).toBeInTheDocument();
    expect(screen.getByText('Sold disponibil: 10.50 RON')).toBeInTheDocument();
  });

  it('marks a voucher used and deducts its value from the available balance', async () => {
    renderDashboard();
    const usedCheckbox = await screen.findByRole('checkbox', {
      name: `Marchează bonul ${activeVoucher.voucherCode} ca folosit`
    });

    fireEvent.click(usedCheckbox);

    await waitFor(() => {
      expect(api.patch).toHaveBeenCalledWith('/vouchers/1/used');
      expect(screen.getByText('Sold disponibil: 0.00 RON')).toBeInTheDocument();
    });
    expect(screen.getByRole('checkbox', {
      name: `Marchează bonul ${activeVoucher.voucherCode} ca folosit`
    })).toBeChecked();
  });
});
