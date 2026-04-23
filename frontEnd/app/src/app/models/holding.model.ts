// src/app/models/holding.model.ts
export interface HoldingDTO {
  symbol: string;
  stockName: string;
  quantity: number;
  averageBuyPrice: number;
  currentPrice: number;
  currentValue: number;
  profitLoss: number;
  profitLossPercent: number;
}