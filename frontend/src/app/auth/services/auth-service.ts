import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { AuthenticationRequest, AuthenticationResponse, RegisterRequest } from '../../models/auth-models';
import { Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private readonly API_URL = 'http://my-trading-app-env.eba-imuwyubr.us-east-2.elasticbeanstalk.com/api/auth'
  // private readonly API_URL = 'http://localhost:8080/api/auth'

  register(data : RegisterRequest): Observable<any>{
    return this.http.post(`${this.API_URL}/register`, data);
  }

  login(data : AuthenticationRequest): Observable<AuthenticationResponse> {
      return this.http.post<AuthenticationResponse>(`${this.API_URL}/login`, data).pipe(
        tap( response => {
        if(response?.token){
          localStorage.setItem('jwt_token', response.token);
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
