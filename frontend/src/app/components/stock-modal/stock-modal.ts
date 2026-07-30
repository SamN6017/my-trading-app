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
import { StockResponse, TimeRange, PriceHistoryResponse, TodaysPrice } from '../../models/stock-response';
import { Chart, registerables } from 'chart.js';
import 'chartjs-adapter-luxon';
import { CandlestickController, CandlestickElement } from 'chartjs-chart-financial';

Chart.register(...registerables, CandlestickController, CandlestickElement);

interface CandlePoint {
  x: number;
  o: number;
  h: number;
  l: number;
  c: number;
}

@Component({
  selector: 'app-stock-modal',
  standalone: true,
  imports: [DecimalPipe, NgClass],
  templateUrl: './stock-modal.html',
  styleUrl: './stock-modal.css'
})
export class StockModalComponent {
  stock = input<StockResponse | null>(null);
  closeModal = output<void>();

  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;

  private stockService = inject(StockService);

  chart: Chart | null = null;
  selectedRange = signal<TimeRange>('1d');
  isLoading = signal<boolean>(false);

  readonly timeRanges: { label: string; value: TimeRange }[] = [
    { label: '1 Day (Live Candlesticks)', value: '1d' },
    { label: '1 Week', value: '1w' },
    { label: '1 Month', value: '1m' },
    { label: '6 Months', value: '6m' },
    { label: '1 Year', value: '1y' }
  ];

  constructor() {
    effect(() => {
      const currentStock = this.stock();
      if (currentStock) {
        this.selectedRange.set('1d');
        this.loadChartData(currentStock.symbol, '1d');
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
      // 1D INTRADAY: Aggregate raw minute entries into 5-minute Candlesticks
      this.stockService.getTodaysPrice(symbol).subscribe({
        next: (data) => {
          const candleData = this.aggregateToIntradayCandles(data, 5); // 5-minute interval
          this.renderCandlestickChart(candleData);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('Failed to load intraday prices', err);
          this.isLoading.set(false);
        }
      });
    } else {
      // HISTORICAL: Render standard smooth Line Chart
      this.stockService.getPriceHistory(symbol, range).subscribe({
        next: (data) => {
          const labels = data.map(h => new Date(h.recordedDate).toLocaleDateString());
          const prices = data.map(h => h.closePrice ?? 0);

          // Append Today's Live Price if available
          const currentStock = this.stock();
          if (currentStock) {
            const todayFormatted = new Date().toLocaleDateString();
            if (labels.length === 0 || labels[labels.length - 1] !== todayFormatted) {
              labels.push(todayFormatted);
              prices.push(currentStock.currentPrice);
            }
          }

          this.renderLineChart(labels, prices);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('Failed to load price history', err);
          this.isLoading.set(false);
        }
      });
    }
  }

  /**
   * Helper function: Aggregates high-frequency raw minute prices into N-minute OHLC Candlestick bars
   */
  private aggregateToIntradayCandles(rawPrices: TodaysPrice[], intervalMinutes: number = 5): CandlePoint[] {
    if (!rawPrices || rawPrices.length === 0) return [];

    // Parse UTC timestamps correctly and filter pre-market (< 8 AM local)
    const validPoints = rawPrices
      .map(p => {
        const timestampStr = p.timestamp.endsWith('Z') ? p.timestamp : `${p.timestamp}Z`;
        return {
          time: new Date(timestampStr),
          price: p.price ?? 0
        };
      })
      .filter(p => p.time.getHours() >= 8)
      .sort((a, b) => a.time.getTime() - b.time.getTime());

    const bucketMs = intervalMinutes * 60 * 1000;
    const buckets = new Map<number, number[]>();

    // Group prices into bucket timestamps
    validPoints.forEach(p => {
      const bucketTime = Math.floor(p.time.getTime() / bucketMs) * bucketMs;
      if (!buckets.has(bucketTime)) {
        buckets.set(bucketTime, []);
      }
      buckets.get(bucketTime)!.push(p.price);
    });

    // Compute OHLC for each bucket
    const candles: CandlePoint[] = [];
    buckets.forEach((prices, bucketTime) => {
      if (prices.length > 0) {
        candles.push({
          x: bucketTime,
          o: prices[0],
          h: Math.max(...prices),
          l: Math.min(...prices),
          c: prices[prices.length - 1]
        });
      }
    });

    return candles;
  }

  private renderCandlestickChart(candleData: CandlePoint[]): void {
    if (this.chart) {
      this.chart.destroy();
    }

    if (!this.chartCanvas) return;

    this.chart = new Chart(this.chartCanvas.nativeElement, {
      type: 'candlestick',
      data: {
        datasets: [{
          label: `${this.stock()?.symbol} Live Intraday`,
          data: candleData as any,
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
        } as any]
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
              unit: 'minute',
              displayFormats: {
                minute: 'hh:mm a'
              }
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

  private renderLineChart(labels: string[], prices: number[]): void {
    if (this.chart) {
      this.chart.destroy();
    }

    if (!this.chartCanvas) return;

    const firstPrice = prices[0] ?? 0;
    const lastPrice = prices[prices.length - 1] ?? 0;
    const isPositive = prices.length > 1 ? lastPrice >= firstPrice : true;

    this.chart = new Chart(this.chartCanvas.nativeElement, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: `${this.stock()?.symbol} Price ($)`,
          data: prices,
          borderColor: isPositive ? '#198754' : '#dc3545',
          backgroundColor: isPositive ? 'rgba(25, 135, 84, 0.1)' : 'rgba(220, 53, 69, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.2,
          pointRadius: prices.length > 50 ? 0 : 3
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false } },
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