/**
 * Shape of a Stock record as returned by GET /api/stocks.
 * Mirrors the Spring Boot Stock entity.
 */
export interface Stock {
  id: number;
  symbol: string;
  name: string;
  sector: string | null;
  description: string | null;
  currentPrice: number;
  previousClose: number;
  updatedAt: string; // ISO-8601 string from Jackson (Instant)
}
