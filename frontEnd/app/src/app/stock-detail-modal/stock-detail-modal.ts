import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
  signal,
  computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StockService } from '../stock.service';
import {
  ApiError,
  StockDetail,
  TransactionType
} from '../stock.model';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-stock-detail-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './stock-detail-modal.html',
  styleUrl: './stock-detail-modal.css'
})
export class StockDetailModalComponent implements OnChanges {
  @Input({ required: true }) symbol!: string;
  @Output() closed = new EventEmitter<void>();

  private readonly stockService = inject(StockService);

  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly detail = signal<StockDetail | null>(null);
  readonly quantity = signal<number>(1);
  readonly actionBusy = signal<TransactionType | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly actionSuccess = signal<string | null>(null);
  readonly quantityError = signal<string | null>(null);

  readonly initial = computed(() => this.detail()?.name?.charAt(0).toUpperCase() ?? '?');
  readonly avatarColor = computed(() => this.colorForSector(this.detail()?.sector));

  readonly maxBuyQty = computed(() => {
    const d = this.detail();
    if (!d || d.currentPrice <= 0) return 0;
    return Math.floor(d.walletBalance / d.currentPrice);
  });

  readonly maxSellQty = computed(() => this.detail()?.userHolding?.quantity ?? 0);

  readonly estimatedTotal = computed(() => {
    const d = this.detail();
    const q = this.quantity();
    if (!d || !q || q < 1) return 0;
    return d.currentPrice * q;
  });

  readonly canBuy = computed(() => {
    const d = this.detail();
    const q = this.quantity();
    return !!d && !this.quantityError() && q > 0
        && this.estimatedTotal() <= d.walletBalance && !this.actionBusy();
  });

  readonly canSell = computed(() => {
    const q = this.quantity();
    return !this.quantityError() && q > 0 && q <= this.maxSellQty() && !this.actionBusy();
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['symbol'] && this.symbol) {
      this.load();
    }
  }

  close(): void {
    this.closed.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) this.close();
  }

  changeQty(delta: number): void {
    const next = Math.max(1, this.quantity() + delta);
    this.quantity.set(next);
    this.quantityError.set(null);
    this.clearFeedback();
  }

  onQtyInput(value: string): void {
    const n = parseInt(value, 10);
    if (Number.isNaN(n) || n < 1) {
      this.quantityError.set('Quantity must be a positive whole number');
    } else if (n > 10000) {
      this.quantityError.set('Quantity cannot exceed 10,000 per trade');
    } else {
      this.quantityError.set(null);
      this.quantity.set(n);
    }
    this.clearFeedback();
  }

  buy(): void {
    this.execute('BUY');
  }

  sell(): void {
    this.execute('SELL');
  }

  trackByLabel = (_: number, item: { label: string }) => item.label;

  private execute(type: TransactionType): void {
    const d = this.detail();
    const qty = this.quantity();
    if (!d || qty < 1) return;

    this.actionBusy.set(type);
    this.clearFeedback();

    this.stockService
      .executeTransaction({ symbol: d.symbol, type, quantity: qty })
      .subscribe({
        next: (txn) => {
          this.actionBusy.set(null);
          this.actionSuccess.set(
            `${type === 'BUY' ? 'Bought' : 'Sold'} ${qty} ${d.symbol} @ ₹${this.fmtRupees(
              txn.pricePerShare
            )} — new balance ₹${this.fmtRupees(txn.walletBalanceAfter)}`
          );
          this.quantity.set(1);
          this.load(false);
        },
        error: (err: HttpErrorResponse) => {
          this.actionBusy.set(null);
          const body = err.error as ApiError | { message?: string } | undefined;
          this.actionError.set(body?.message ?? 'Trade failed. Please try again.');
        }
      });
  }

  private load(showSpinner = true): void {
    if (showSpinner) this.loading.set(true);
    this.loadError.set(null);

    this.stockService.getStockDetail(this.symbol).subscribe({
      next: (d) => {
        this.detail.set(d);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        const body = err.error as ApiError | { message?: string } | undefined;
        this.loadError.set(body?.message ?? `Could not load ${this.symbol}`);
      }
    });
  }

  private clearFeedback(): void {
    this.actionError.set(null);
    this.actionSuccess.set(null);
  }

  fmtRupees(value: number | null | undefined): string {
    if (value == null) return '—';
    return new Intl.NumberFormat('en-IN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(value);
  }

  fmtPercent(value: number | null | undefined): string {
    if (value == null) return '—';
    const sign = value > 0 ? '+' : '';
    return `${sign}${value.toFixed(2)}%`;
  }

  private colorForSector(sector: string | undefined): string {
    const palette: Record<string, string> = {
      Technology: 'bg-indigo-500',
      Energy: 'bg-amber-500',
      Financials: 'bg-emerald-600',
      Telecom: 'bg-sky-500',
      Engineering: 'bg-orange-500',
      'Consumer Goods': 'bg-rose-500',
      Automobile: 'bg-red-500',
      Pharmaceuticals: 'bg-teal-500'
    };
    return (sector && palette[sector]) ?? 'bg-slate-500';
  }
}
