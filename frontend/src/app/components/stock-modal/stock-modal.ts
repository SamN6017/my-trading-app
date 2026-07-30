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
import 'chartjs-adapter-luxon';
import { CandlestickController, CandlestickElement } from 'chartjs-chart-financial';

// Register standard Chart.js components + Candlestick plugins
Chart.register(...registerables, CandlestickController, CandlestickElement);

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

  chart: Chart | null = null;
  selectedRange = signal<TimeRange>('1w');
  isLoading = signal<boolean>(false);

  readonly timeRanges: { label: string; value: TimeRange }[] = [
    { label: '1 Day (Live)', value: '1d' },
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

    if (range === '1d') {
      this.stockService.getTodaysPrice(symbol).subscribe({
        next: (data) => {
          const parsedData = data.map(d => {
            const timestampStr = d.timestamp.endsWith('Z') ? d.timestamp : `${d.timestamp}Z`;
            return {
              x: new Date(timestampStr).getTime(),
              y: d.price ?? 0
            };
          }).filter(item => new Date(item.x).getHours() >= 8);

          this.renderLineChart(parsedData);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('Failed to load intraday prices', err);
          this.isLoading.set(false);
        }
      });
    } else {
      this.stockService.getPriceHistory(symbol, range).subscribe({
        next: (data) => {
          // Map backend history into OHLC candlestick format { x, o, h, l, c }
          const candleData = data.map(h => ({
            x: new Date(h.recordedDate).getTime(),
            o: h.openPrice ?? h.closePrice,
            h: h.highPrice ?? h.closePrice,
            l: h.lowPrice ?? h.closePrice,
            c: h.closePrice
          }));

          this.renderCandlestickChart(candleData);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('Failed to load price history', err);
          this.isLoading.set(false);
        }
      });
    }
  }

  private renderCandlestickChart(candleData: Array<{ x: number; o: number; h: number; l: number; c: number }>): void {
    if (this.chart) {
      this.chart.destroy();
    }

    if (!this.chartCanvas) return;

    this.chart = new Chart(this.chartCanvas.nativeElement, {
      type: 'candlestick',
      data: {
        datasets: [{
          label: `${this.stock()?.symbol} Price`,
          data: candleData as any,
          // Standard property names for chartjs-chart-financial datasets:
          color: {
            up: '#198754',
            down: '#dc3545',
            unchanged: '#6c757d'
          },
          borderColor: {
            up: '#198754',
            down: '#dc3545',
            unchanged: '#6c757d'
          }
        } as any] // Cast dataset to 'any' to bypass strict ChartDataset union checks
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx: any) => {
                const raw = ctx.raw;
                if (!raw) return '';
                return [
                  `Open:  $${raw.o?.toFixed(2)}`,
                  `High:  $${raw.h?.toFixed(2)}`,
                  `Low:   $${raw.l?.toFixed(2)}`,
                  `Close: $${raw.c?.toFixed(2)}`
                ];
              }
            }
          }
        },
        scales: {
          x: {
            type: 'time',
            time: {
              unit: 'day'
            },
            grid: { display: false }
          },
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

  private renderLineChart(dataPoints: Array<{ x: number; y: number }>): void {
    if (this.chart) {
      this.chart.destroy();
    }

    if (!this.chartCanvas) return;

    const prices = dataPoints.map(p => p.y);
    const isPositive = prices.length > 1 ? prices[prices.length - 1] >= prices[0] : true;

    this.chart = new Chart(this.chartCanvas.nativeElement, {
      type: 'line',
      data: {
        datasets: [{
          label: `${this.stock()?.symbol} Price ($)`,
          data: dataPoints as any,
          borderColor: isPositive ? '#198754' : '#dc3545',
          backgroundColor: isPositive ? 'rgba(25, 135, 84, 0.1)' : 'rgba(220, 53, 69, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.2,
          pointRadius: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: {
            type: 'time',
            time: { unit: 'hour' },
            grid: { display: false }
          },
          y: {
            grid: { color: '#e9ecef' },
            ticks: { callback: (val) => `$${val}` }
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