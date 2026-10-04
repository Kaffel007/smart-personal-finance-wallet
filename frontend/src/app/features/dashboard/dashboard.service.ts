import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../../core/api.config';
import { DashboardSummary } from './dashboard-summary.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  getSummary(year: number, month: number) {
    return this.http.get<DashboardSummary>(`${API_BASE_URL}/dashboard/summary`, {
      params: { year, month },
    });
  }
}
