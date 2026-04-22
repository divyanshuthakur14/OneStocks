import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Subject, Subscription, timer } from 'rxjs';
import { switchMap, takeUntil } from 'rxjs/operators';

import { Stock } from '../models/stock.model';
import { StockService } from '../services/stock.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class HomeComponent implements OnInit, OnDestroy {
  private readonly stockService = inject(StockService);
  private readonly destroy$ = new Subject<void>();
  private pollSubscription?: Subscription;

  readonly stocks = signal<Stock[]>([]);
  readonly lastError = signal<string | null>(null);

  ngOnInit(): void {
    this.pollSubscription = timer(0, 5000)
      .pipe(
        switchMap(() => this.stockService.getStocks()),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (data) => {
          this.stocks.set(data);
          this.lastError.set(null);
        },
        error: (err) => {
          this.lastError.set('Unable to reach the backend. Retrying...');
          console.error('Stock fetch failed', err);
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.pollSubscription?.unsubscribe();
  }

  priceChange(stock: Stock): number {
    return stock.currentPrice - stock.previousClose;
  }

  percentChange(stock: Stock): number {
    if (!stock.previousClose) {
      return 0;
    }
    return (this.priceChange(stock) / stock.previousClose) * 100;
  }

  formatTime(iso: string): string {
    if (!iso) {
      return '';
    }
    const date = new Date(iso);
    return date.toLocaleTimeString('en-GB', { hour12: false });
  }
}
