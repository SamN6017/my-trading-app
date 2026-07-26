import {
  Component,
  ElementRef,
  ViewChild,
  inject,
  input,
  output,
  signal,
  effect
} from '@angular/core';
import { DecimalPipe, NgClass } from '@angular/common';
import { StockService } from '../../services/stock-service';
import { StockResponse, TimeRange, PriceHistoryResponse } from '../../models/stock-response';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-stock-modal',
  standalone: true,
  imports: [DecimalPipe, NgClass],
  templateUrl: './stock-modal.html',
  styleUrl: './stock-modal.css'
})
export class StockModal {
  stock = input<StockResponse | null>(null);
  closeModal = output<void>();

  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;

  private stockService = inject(StockService);

  chart: Chart<'line'> | null = null;
  selectedRange = signal<TimeRange>('1w');
  isLoading = signal<boolean>(false);

  readonly timeRanges: { label: string; value: TimeRange }[] = [
    { label: '1 Week', value: '1w' },
    { label: '1 Month', value: '1m' },
    { label: '6 Months', value: '6m' },
    { label: '1 Year', value: '1y' }
  ];

  constructor() {
    effect(() => {
      const currentStock = this.stock();
      if (currentStock) {
        this.selectedRange.set('1w');
        this.loadChartData(currentStock.symbol, '1w');
      }
    });
  }

  onRangeSelect(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    const range = selectElement.value as TimeRange;
    this.selectedRange.set(range);

    const currentStock = this.stock();
    if (currentStock) {
      this.loadChartData(currentStock.symbol, range);
    }
  }

  loadChartData(symbol: string, range: TimeRange): void {
    this.isLoading.set(true);
    this.stockService.getPriceHistory(symbol, range).subscribe({
      next: (data) => {
        this.renderChart(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load price history', err);
        this.isLoading.set(false);
      }
    });
  }

  private renderChart(history: PriceHistoryResponse[]): void {
    if (this.chart) {
      this.chart.destroy();
    }

    if (!this.chartCanvas) return;

    const labels = history.map(h => new Date(h['recordedDate']).toLocaleDateString());

    // Ensure prices array is strictly number[] (not number | undefined)
    const prices: number[] = history.map(h => h.closePrice ?? 0);

    // Safe array access with nullish coalescing
    const firstPrice = prices[0] ?? 0;
    const lastPrice = prices[prices.length - 1] ?? 0;
    const isPositive = prices.length > 1 ? lastPrice >= firstPrice : true;

    const strokeColor = isPositive ? '#198754' : '#dc3545';
    const fillColor = isPositive ? 'rgba(25, 135, 84, 0.1)' : 'rgba(220, 53, 69, 0.1)';

    this.chart = new Chart(this.chartCanvas.nativeElement, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: `${this.stock()?.symbol} Price ($)`,
          data: prices,
          borderColor: strokeColor,
          backgroundColor: fillColor,
          borderWidth: 2,
          fill: true,
          tension: 0.2,
          pointRadius: history.length > 50 ? 0 : 3
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false }
        },
        scales: {
          x: { grid: { display: false } },
          y: {
            grid: { color: '#e9ecef' },
            ticks: {
              callback: (value) => `$${value}`
            }
          }
        }
      }
    });
  }

  onClose(): void {
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
    }
    this.closeModal.emit();
  }
}