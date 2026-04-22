import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ExecuteTransactionRequest,
  StockDetail,
  StockSummary,
  TransactionResponse
} from './stock.model';

@Injectable({ providedIn: 'root' })
export class StockService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8082/api';

  listStocks(): Observable<StockSummary[]> {
    return this.http.get<StockSummary[]>(`${this.baseUrl}/stocks`);
  }

  getStockDetail(symbol: string): Observable<StockDetail> {
    return this.http.get<StockDetail>(`${this.baseUrl}/stocks/${symbol}`);
  }

  executeTransaction(req: ExecuteTransactionRequest): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.baseUrl}/transactions`, req);
  }

  getWalletBalance(): Observable<{ balance: number }> {
    return this.http.get<{ balance: number }>(`${this.baseUrl}/wallet/balance`);
  }
}
