export type TransactionType = 'BUY' | 'SELL';
export type TransactionStatus = 'EXECUTED' | 'FAILED';

export interface TransactionDTO {
  id: number;
  symbol: string;
  stockName: string;
  type: TransactionType;
  status: TransactionStatus;
  quantity: number;
  pricePerShare: number;
  totalAmount: number;
  createdAt: string;
  walletBalanceAfter: number;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;   // current page (0-indexed)
  size: number;
  first: boolean;
  last: boolean;
}
