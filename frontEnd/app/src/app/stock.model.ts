export interface StockSummary {
  id: number;
  symbol: string;
  name: string;
  sector: string;
  currentPrice: number;
  previousClose: number;
  dayChangePercent: number;
  updatedAt: string;
}

export interface HoldingInfo {
  quantity: number;
  averageBuyPrice: number;
  currentValue: number;
  profitLoss: number;
  profitLossPercent: number;
}

export interface StockDetail {
  symbol: string;
  name: string;
  sector: string;
  description: string;
  currentPrice: number;
  previousClose: number;
  dayChangePercent: number;
  userHolding: HoldingInfo | null;
  walletBalance: number;
}

export type TransactionType = 'BUY' | 'SELL';

export interface ExecuteTransactionRequest {
  symbol: string;
  type: TransactionType;
  quantity: number;
}

export interface TransactionResponse {
  id: number;
  symbol: string;
  stockName: string;
  type: TransactionType;
  quantity: number;
  pricePerShare: number;
  totalAmount: number;
  status: 'EXECUTED' | 'FAILED';
  createdAt: string;
  walletBalanceAfter: number;
}

export interface ApiError {
  error: string;
  message: string;
}
