
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { DecimalPipe, NgClass } from '@angular/common';
import { StockService } from '../../services/stock-service';
import { StockResponse } from '../../models/stock-response';
import { StockModal } from '../stock-modal/stock-modal';

@Component({
  selector: 'app-stock-list',
  standalone: true,
  imports: [DecimalPipe, NgClass, StockModal],
  templateUrl: './stock-list.html',
  styleUrl: './stock-list.css'
})
export class StockList implements OnInit {
  private stockService = inject(StockService);

  stocks = signal<StockResponse[]>([]);
  searchQuery = signal<string>('');
  selectedStock = signal<StockResponse | null>(null);
  isLoading = signal<boolean>(true);

  filteredStocks = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    if (!query) return this.stocks();

    return this.stocks().filter(stock => 
      stock.symbol.toLowerCase().includes(query) || 
      stock.companyName.toLowerCase().includes(query) ||
      stock.sector.toLowerCase().includes(query)
    );
  });

  ngOnInit(): void {
    this.fetchStocks();
  }

  fetchStocks(): void {
    this.isLoading.set(true);
    this.stockService.getAllStocks().subscribe({
      next: (data) => {
        this.stocks.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load stocks', err);
        this.isLoading.set(false);
      }
    });
  }

  onSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchQuery.set(input.value);
  }

  openChartModal(stock: StockResponse): void {
    this.selectedStock.set(stock);
  }

  closeChartModal(): void {
    this.selectedStock.set(null);
  }
}
