export type TransactionType = 'BUY' | 'SELL';
export type TransactionStatus = 'EXECUTED' | 'FAILED';

export interface TransactionDTO {
  createdAt: string;
  stockName: string;
  type: TransactionType;
  status: TransactionStatus;
  quantity: number;
  pricePerShare: number;
  totalAmount: number;
}