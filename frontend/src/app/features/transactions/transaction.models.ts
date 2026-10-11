import { CategoryType } from '../categories/category.models';

export interface FinancialTransaction {
  id: number;
  amount: number;
  type: CategoryType;
  transactionDate: string;
  description: string | null;
  categoryId: number;
  categoryName: string;
}
export interface CreateFinancialTransactionRequest {
  categoryId: number;
  amount: number;
  transactionDate: string;
  description: string | null;
}
export interface UpdateFinancialTransactionRequest {
  categoryId: number;
  amount: number;
  transactionDate: string;
  description: string | null;
}
export interface TransactionFilters { type?: CategoryType; categoryId?: number; }
