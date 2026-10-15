import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../../core/api.config';
import { CreateSavingsGoalRequest, SavingsGoal, UpdateSavingsGoalRequest } from './savings-goal.models';

@Injectable({ providedIn: 'root' })
export class SavingsGoalService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/savings-goals`;
  getAll() { return this.http.get<SavingsGoal[]>(this.url); }
  getOne(id: number) { return this.http.get<SavingsGoal>(`${this.url}/${id}`); }
  create(request: CreateSavingsGoalRequest) {
    const { name, targetAmount, savedAmount, targetDate } = request;
    return this.http.post<SavingsGoal>(this.url, { name, targetAmount, savedAmount, targetDate });
  }
  update(id: number, request: UpdateSavingsGoalRequest) {
    const { name, targetAmount, savedAmount, targetDate } = request;
    return this.http.put<SavingsGoal>(`${this.url}/${id}`, { name, targetAmount, savedAmount, targetDate });
  }
  delete(id: number) { return this.http.delete<void>(`${this.url}/${id}`); }
}
