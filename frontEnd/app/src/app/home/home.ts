import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../auth';
import { StockService } from '../stock.service';
import { StockSummary } from '../stock.model';
import { StockDetailModalComponent } from '../stock-detail-modal/stock-detail-modal';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, StockDetailModalComponent],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class HomeComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly stockService = inject(StockService);

  username: string | null = '';
  readonly stocks = signal<StockSummary[]>([]);
  readonly stocksLoading = signal(false);
  readonly stocksError = signal<string | null>(null);
  readonly selectedSymbol = signal<string | null>(null);

  ngOnInit(): void {
    this.username = this.authService.getUsername();
    this.loadStocks();
  }

  openStock(symbol: string): void {
    this.selectedSymbol.set(symbol);
  }

  closeModal(): void {
    this.selectedSymbol.set(null);
    this.loadStocks();
  }

  logout(): void {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }

  fmtRupees(n: number): string {
    return new Intl.NumberFormat('en-IN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(n);
  }

  private loadStocks(): void {
    this.stocksLoading.set(true);
    this.stocksError.set(null);
    this.stockService.listStocks().subscribe({
      next: (s) => {
        this.stocks.set(s);
        this.stocksLoading.set(false);
      },
      error: () => {
        this.stocksLoading.set(false);
        this.stocksError.set('Could not load stocks');
      }
    });
  }
}
