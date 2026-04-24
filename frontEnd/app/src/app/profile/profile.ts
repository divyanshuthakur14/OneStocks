import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Subject, timer } from 'rxjs';
import { switchMap, takeUntil } from 'rxjs/operators';
import { AuthService } from '../auth';
import { HoldingService } from '../services/holding.service';
import { HoldingDTO } from '../models/holding.model';
import { TransactionDTO } from '../models/transaction.model';
import { TransactionService } from '../services/transaction.service';
import { WalletService } from '../services/wallet.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class ProfileComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly holdingService = inject(HoldingService);
  private readonly transactionService = inject(TransactionService);
  private readonly walletService = inject(WalletService);

  private readonly destroy$ = new Subject<void>();
  

  username: string | null = null;
  readonly holdings = signal<HoldingDTO[]>([]);
  readonly transactions = signal<TransactionDTO[]>([]);
  readonly balance = signal<number | null>(null);

  ngOnInit(): void {
    this.username = this.authService.getUsername();

    if (this.username) {
      timer(0, 5000)
        .pipe(
          switchMap(() => this.holdingService.getMyHoldings(this.username!)),
          takeUntil(this.destroy$)
        )
        .subscribe({
          next: (data) => this.holdings.set(data),
          error: () => console.error('Failed to fetch holdings')
        });

        this.transactionService.getMyTransactions(this.username)
        .pipe(takeUntil(this.destroy$))
        .subscribe({ next: (data) => this.transactions.set(data) 
        });

        this.walletService.getBalance()
        .pipe(takeUntil(this.destroy$))
        .subscribe({ next: (data) => this.balance.set(data.balance) 
        });



    }
  }

    get totalProfitLoss() : number {
    return this.holdings().reduce((sum, h)=>sum+h.profitLoss, 0);
  }

  get totalProfitLossPercent() : number {
    const totalCost = this.holdings().reduce(
      (sum, h)=>sum+ (h.averageBuyPrice + h.quantity),0
    );
    if(totalCost == 0) return 0;
    return (this.totalProfitLoss / totalCost) * 100;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}