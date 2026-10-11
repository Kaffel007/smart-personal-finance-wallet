import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../../core/api.config';
import { CategoryService } from './category.service';

const category = { id: 1, name: 'Transport', type: 'EXPENSE' as const };
const url = `${API_BASE_URL}/categories`;

describe('CategoryService', () => {
  let service: CategoryService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CategoryService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('gets all categories', () => {
    service.getAll().subscribe(value => expect(value).toEqual([category]));
    const request = http.expectOne(url);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]);
    request.flush([category]);
  });
  it.each(['INCOME', 'EXPENSE'] as const)('sends the optional type filter %s', type => {
    service.getAll(type).subscribe();
    const request = http.expectOne(`${url}?type=${type}`);
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
  it('gets one category', () => {
    service.getOne(1).subscribe(value => expect(value).toEqual(category));
    const request = http.expectOne(`${url}/1`);
    expect(request.request.method).toBe('GET');
    request.flush(category);
  });
  it('posts the exact create DTO', () => {
    const dto = { name: category.name, type: category.type };
    service.create(dto).subscribe(value => expect(value).toEqual(category));
    const request = http.expectOne(url);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(dto);
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush(category);
  });
  it('puts the exact update DTO', () => {
    const dto = { name: 'Transport public', type: 'EXPENSE' as const };
    service.update(1, dto).subscribe();
    const request = http.expectOne(`${url}/1`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(dto);
    request.flush({ ...category, ...dto });
  });
  it('deletes a category with a 204 response', () => {
    service.delete(1).subscribe();
    const request = http.expectOne(`${url}/1`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
  });
});
