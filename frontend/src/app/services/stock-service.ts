import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PriceHistoryResponse, StockResponse } from '../models/stock-response';

@Injectable({
  providedIn: 'root',
})
export class StockService {
  private http = inject(HttpClient);
  // private BASE_URL = 'http://my-trading-app-env.eba-imuwyubr.us-east-2.elasticbeanstalk.com/api/'
   private BASE_URL = 'http://d33yqmoryj0bxp.cloudfront.net/api/'
  // private BASE_URL = 'http://localhost:8080/api'


  private getHeader(): HttpHeaders{
    const token = localStorage.getItem('jwt_token');
    return new HttpHeaders({
      'Authorization' : `Bearer ${token}`
    })
  }

  getAllStocks(): Observable<StockResponse[]> {
    return this.http.get<StockResponse[]>(`${this.BASE_URL}/stocks`, {
      headers : this.getHeader()
    })
  }

  getPriceHistory(symbol : string, range: string): Observable<PriceHistoryResponse[]> {
    const params = new HttpParams().set('range', range);

  return this.http.get<PriceHistoryResponse[]>(
    `${this.BASE_URL}/prices/history/${symbol}`, 
    { 
      headers: this.getHeader(),
      params: params 
    }
  );
  }

}
