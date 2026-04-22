import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { Subject, Subscription, timer } from 'rxjs';
import { switchMap, takeUntil } from 'rxjs/operators';
import { AuthService } from '../auth';
import { StockService } from '../stock.service';
import { StockSummary } from '../stock.model';
import { StockDetailModalComponent } from '../stock-detail-modal/stock-detail-modal';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, DecimalPipe, StockDetailModalComponent],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class HomeComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly stockService = inject(StockService);
  private readonly destroy$ = new Subject<void>();
  private pollSubscription?: Subscription;

  username: string | null = '';
  readonly stocks = signal<StockSummary[]>([]);
  readonly lastError = signal<string | null>(null);
  readonly selectedSymbol = signal<string | null>(null);


  ngOnInit(): void {
    this.username = this.authService.getUsername();
    this.pollSubscription = timer(0, 5000)
      .pipe(
        switchMap(() => this.stockService.listStocks()),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (data) => {
          this.stocks.set(data);
          this.lastError.set(null);
        },
        error: () => {
          this.lastError.set('Unable to reach the backend. Retrying…');
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.pollSubscription?.unsubscribe();
  }

  openStock(symbol: string): void {
    this.selectedSymbol.set(symbol);
  }

  closeModal(): void {
    this.selectedSymbol.set(null);
  }

  // logout(): void {
  //   this.authService.logout().subscribe(() => {
  //     this.router.navigate(['/login']);
  //   });
  // }

  priceChange(stock: StockSummary): number {
    return stock.currentPrice - stock.previousClose;
  }

  percentChange(stock: StockSummary): number {
    if (!stock.previousClose) return 0;
    return (this.priceChange(stock) / stock.previousClose) * 100;
  }

  formatTime(iso: string): string {
    if (!iso) return '';
    return new Date(iso).toLocaleTimeString('en-GB', { hour12: false });
  }

  fmtRupees(n: number): string {
    return new Intl.NumberFormat('en-IN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(n);
  }
}
