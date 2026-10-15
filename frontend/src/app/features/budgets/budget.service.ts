import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_BASE_URL } from '../../core/api.config';
import { Budget, BudgetFilters, CreateBudgetRequest, UpdateBudgetRequest } from './budget.models';

@Injectable({ providedIn: 'root' })
export class BudgetService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/budgets`;
  getAll(filters: BudgetFilters = {}) {
    let params = new HttpParams();
    if (filters.year !== undefined) params = params.set('year', filters.year);
    if (filters.month !== undefined) params = params.set('month', filters.month);
    if (filters.categoryId !== undefined) params = params.set('categoryId', filters.categoryId);
    return this.http.get<Budget[]>(this.url, { params });
  }
  getOne(id: number) { return this.http.get<Budget>(`${this.url}/${id}`); }
  create(request: CreateBudgetRequest) { return this.http.post<Budget>(this.url, this.payload(request)); }
  update(id: number, request: UpdateBudgetRequest) { return this.http.put<Budget>(`${this.url}/${id}`, this.payload(request)); }
  delete(id: number) { return this.http.delete<void>(`${this.url}/${id}`); }
  private payload(request: CreateBudgetRequest): CreateBudgetRequest {
    const { categoryId, year, month, amount } = request;
    return { categoryId, year, month, amount };
  }
}
