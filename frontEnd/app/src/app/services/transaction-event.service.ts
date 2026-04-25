import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TransactionEventService {
  private transactionCompleted$ = new Subject<void>();
  
  readonly transactionCompleted = this.transactionCompleted$.asObservable();

  emit(): void {
    this.transactionCompleted$.next();
  }
}