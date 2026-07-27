export interface StockResponse {
  symbol: string;
  companyName: string;
  sector: string;
  currentPrice: number;
  previousClose: number;
  change: number;
  changePercent: number;
  isActive?: boolean;
}

export interface PriceHistoryResponse {
  recordedDate: string; 
  closePrice: number;
  openPrice?: number;
  highPrice?: number;
  lowPrice?: number;
  historyId?: number;
  stock?: StockResponse;
}

export type TimeRange = '1d' | '1w' | '1m' | '6m' | '1y';

export interface TodaysPrice {
  id?: number;
  symbol: string;
  price: number;
  volume: number;
  timestamp: string;
}