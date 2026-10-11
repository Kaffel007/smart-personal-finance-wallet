import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_BASE_URL } from '../../core/api.config';
import { Category, CategoryType, CreateCategoryRequest, UpdateCategoryRequest } from './category.models';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/categories`;
  getAll(type?: CategoryType) {
    const params = type ? new HttpParams().set('type', type) : new HttpParams();
    return this.http.get<Category[]>(this.url, { params });
  }
  getOne(id: number) { return this.http.get<Category>(`${this.url}/${id}`); }
  create(request: CreateCategoryRequest) { return this.http.post<Category>(this.url, request); }
  update(id: number, request: UpdateCategoryRequest) { return this.http.put<Category>(`${this.url}/${id}`, request); }
  delete(id: number) { return this.http.delete<void>(`${this.url}/${id}`); }
}
