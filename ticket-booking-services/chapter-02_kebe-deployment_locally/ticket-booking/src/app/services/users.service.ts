import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API } from '../core/api.config';
import { Observable } from 'rxjs';

export interface User {
  userId: string;
  name: string;
  email: string;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class UsersService {
  constructor(private http: HttpClient) {}

  list(): Observable<User[]> {
    return this.http.get<User[]>(`${API.user}/all`);
  }

  get(userId: string): Observable<User> {
    return this.http.get<User>(`${API.user}/${encodeURIComponent(userId)}`);
  }
}
