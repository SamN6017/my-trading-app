import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { AuthenticationRequest, AuthenticationResponse, RegisterRequest } from '../../models/auth-models';
import { Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private readonly API_URL = 'https://d33yqmoryj0bxp.cloudfront.net/api/auth'
  // private readonly API_URL = 'http://localhost:8080/api/auth'

  isLoggedIn = signal<boolean>(this.hasToken());

  private hasToken(): boolean {
    return !!localStorage.getItem('jwt_token');
  }

  register(data : RegisterRequest): Observable<any>{
    return this.http.post(`${this.API_URL}/register`, data);
  }

  login(data : AuthenticationRequest): Observable<AuthenticationResponse> {
      return this.http.post<AuthenticationResponse>(`${this.API_URL}/login`, data).pipe(
        tap( response => {
        if(response?.token){
          localStorage.setItem('jwt_token', response.token);
          this.isLoggedIn.set(true);
        }
      }))
  }

  logout(){
    localStorage.removeItem('jwt_token');
  }

  getToken(): string | null{
    return localStorage.getItem('jwt_token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
}
