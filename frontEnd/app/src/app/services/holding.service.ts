import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { HoldingDTO } from '../models/holding.model';

@Injectable({ providedIn: 'root' })
export class HoldingService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8082/api/holdings';

  getMyHoldings(username: String): Observable<HoldingDTO[]> {
    return this.http.get<HoldingDTO[]>(`${this.apiUrl}/my?username=${username}`);
  }
}