export interface Budget {
  id: number;
  categoryId: number;
  categoryName: string;
  year: number;
  month: number;
  amount: number;
  spent: number;
  remaining: number;
  usagePercent: number;
}

export interface CreateBudgetRequest { categoryId: number; year: number; month: number; amount: number; }
export interface UpdateBudgetRequest { categoryId: number; year: number; month: number; amount: number; }
export interface BudgetFilters { year?: number; month?: number; categoryId?: number; }
