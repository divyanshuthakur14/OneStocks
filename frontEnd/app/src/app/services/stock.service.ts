import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Stock } from '../models/stock.model';

/**
 * Thin wrapper around the backend stocks API.
 */
@Injectable({ providedIn: 'root' })
export class StockService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8082/api/stocks';

  /**
   * Fetches the current snapshot of all stocks.
   * Returns a cold Observable; the home component controls polling cadence.
   */
  getStocks(): Observable<Stock[]> {
    return this.http.get<Stock[]>(this.apiUrl);
  }
}
