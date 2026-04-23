import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, timer } from 'rxjs';
import { switchMap, takeUntil } from 'rxjs/operators';
import { AuthService } from '../auth';
import { HoldingService } from '../services/holding.service';
import { HoldingDTO } from '../models/holding.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class ProfileComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly holdingService = inject(HoldingService);
  private readonly destroy$ = new Subject<void>();

  username: string | null = null;
  readonly holdings = signal<HoldingDTO[]>([]);

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
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}