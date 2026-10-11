import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_BASE_URL } from '../../core/api.config';
import { FinancialTransaction, CreateFinancialTransactionRequest, UpdateFinancialTransactionRequest, TransactionFilters } from './transaction.models';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/transactions`;
  getAll(filters: TransactionFilters = {}) {
    let params = new HttpParams();
    if (filters.type) params = params.set('type', filters.type);
    if (filters.categoryId !== undefined) params = params.set('categoryId', filters.categoryId);
    return this.http.get<FinancialTransaction[]>(this.url, { params });
  }
  getOne(id: number) { return this.http.get<FinancialTransaction>(`${this.url}/${id}`); }
  create(request: CreateFinancialTransactionRequest) { return this.http.post<FinancialTransaction>(this.url, request); }
  update(id: number, request: UpdateFinancialTransactionRequest) { return this.http.put<FinancialTransaction>(`${this.url}/${id}`, request); }
  delete(id: number) { return this.http.delete<void>(`${this.url}/${id}`); }
}
