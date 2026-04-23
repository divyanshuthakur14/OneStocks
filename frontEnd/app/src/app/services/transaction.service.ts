import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { TransactionDTO } from '../models/transaction.model';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8082/api/transactions';

  getMyTransactions(username: string): Observable<TransactionDTO[]> {
    return this.http.get<TransactionDTO[]>(`${this.apiUrl}/my?username=${username}`);
  }
}