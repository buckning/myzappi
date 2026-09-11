import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, Input, OnDestroy, ViewChild } from '@angular/core';
import { EnergyUsageResponse } from '../energyUsage.interface';
import { EnergyUsageService } from '../energy-usage.service';

@Component({
  selector: 'app-energy-usage-panel',
  templateUrl: './energy-usage-panel.component.html',
  styleUrls: ['./energy-usage-panel.component.css'],
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: false
})
export class EnergyUsagePanelComponent implements AfterViewInit, OnDestroy {

  private static readonly MINUTES_PER_DAY = 24 * 60;

  @Input() public bearerToken: string | null = null;
  @ViewChild('energyUsageCanvas') private energyUsageCanvas?: ElementRef<HTMLCanvasElement>;
  selectedDate = this.toDateInputValue(new Date());
  energyUsage: EnergyUsageResponse | null = null;
  isLoadingEnergyUsage = true;
  energyUsageError = false;
  private energyUsageChart?: import('chart.js').Chart<'line'>;

  constructor(private readonly energyUsageService: EnergyUsageService) {}

  ngAfterViewInit(): void {
    this.loadEnergyUsage();
  }

  ngOnDestroy(): void {
    this.energyUsageChart?.destroy();
  }

  setDate(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    if (value && value <= this.todayDate) {
      this.selectedDate = value;
      this.loadEnergyUsage();
    }
  }

  moveDay(days: number): void {
    const [year, month, day] = this.selectedDate.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    date.setDate(date.getDate() + days);
    const nextDate = this.toDateInputValue(date);

    if (nextDate > this.todayDate) {
      return;
    }

    this.selectedDate = nextDate;
    this.loadEnergyUsage();
  }

  isTodaySelected(): boolean {
    return this.selectedDate === this.todayDate;
  }

  private loadEnergyUsage(): void {
    this.isLoadingEnergyUsage = true;
    this.energyUsageError = false;
    this.energyUsage = null;
    this.energyUsageChart?.destroy();
    this.energyUsageChart = undefined;

    const zoneId = Intl.DateTimeFormat().resolvedOptions().timeZone;
    this.energyUsageService.getEnergyUsage(
      this.bearerToken,
      this.selectedDate,
      zoneId,
      'minute'
    ).subscribe({
      next: data => {
        this.energyUsage = data;
        this.isLoadingEnergyUsage = false;
        void this.renderEnergyUsageChart();
      },
      error: error => {
        console.log('Failed to get energy usage ' + error.status);
        this.isLoadingEnergyUsage = false;
        this.energyUsageError = true;
      }
    });
  }

  private async renderEnergyUsageChart(): Promise<void> {
    const canvas = this.energyUsageCanvas?.nativeElement;
    const usage = this.energyUsage;
    if (!canvas || !usage) {
      return;
    }

    const { Chart, registerables } = await import('chart.js');
    Chart.register(...registerables);
    if (this.energyUsage !== usage || !this.energyUsageCanvas) {
      return;
    }

    this.energyUsageChart?.destroy();
    const labels = this.createDayLabels();
    const readingsByMinute = this.indexReadingsByMinute(usage);

    this.energyUsageChart = new Chart(canvas, {
      type: 'line',
      data: {
        labels,
        datasets: [
          // Chart.js draws higher-order datasets first, so the lower-order consumed
          // series is drawn last and remains visible over the other areas.
          this.createDataset('Imported', readingsByMinute.map(reading => reading?.importedKW ?? null), '#ff0000', 3),
          this.createDataset('Exported', readingsByMinute.map(reading => reading ? -reading.exportedKW : null), '#ffff00', 2),
          this.createDataset('Consumed', readingsByMinute.map(reading => reading?.consumedKW ?? null), '#00ff00', 1)
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
          mode: 'index',
          intersect: false
        },
        plugins: {
          legend: {
            position: 'top',
            align: 'end',
            labels: {
              color: '#d3d3d3'
            }
          },
          tooltip: {
            backgroundColor: '#202020',
            titleColor: '#d3d3d3',
            bodyColor: '#d3d3d3',
            callbacks: {
              label: context => `${context.dataset.label}: ${(context.parsed.y ?? 0).toFixed(1)} kW`
            }
          }
        },
        scales: {
          x: {
            ticks: {
              color: '#d3d3d3',
              maxTicksLimit: 12,
              callback: (_value, index) => labels[index]
            },
            grid: {
              display: false
            },
            border: {
              display: false
            }
          },
          y: {
            title: {
              display: true,
              text: 'Power (kW)',
              color: '#d3d3d3'
            },
            ticks: {
              color: '#d3d3d3',
              callback: value => {
                const numericValue = typeof value === 'number' ? value : Number(value);
                return Number.isInteger(numericValue) ? `${numericValue.toFixed(1)}kW` : '';
              }
            },
            grid: {
              display: false
            },
            border: {
              display: false
            }
          }
        }
      }
    });
  }

  private createDayLabels(): string[] {
    return Array.from({ length: EnergyUsagePanelComponent.MINUTES_PER_DAY }, (_value, minute) => {
      const hour = Math.floor(minute / 60);
      const minuteWithinHour = minute % 60;
      return `${String(hour).padStart(2, '0')}:${String(minuteWithinHour).padStart(2, '0')}`;
    });
  }

  private indexReadingsByMinute(usage: EnergyUsageResponse): Array<EnergyUsageResponse['readings'][number] | null> {
    const readingsByMinute: Array<EnergyUsageResponse['readings'][number] | null> =
      new Array(EnergyUsagePanelComponent.MINUTES_PER_DAY).fill(null);
    const dateTimeFormatter = new Intl.DateTimeFormat('en-GB', {
      timeZone: usage.zoneId,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23'
    });

    for (const reading of usage.readings) {
      const parts = Object.fromEntries(dateTimeFormatter.formatToParts(new Date(reading.timestamp))
        .map(part => [part.type, part.value]));
      const localDate = `${parts['year']}-${parts['month']}-${parts['day']}`;
      if (localDate !== usage.date) {
        continue;
      }

      const minute = Number(parts['hour']) * 60 + Number(parts['minute']);
      if (minute >= 0 && minute < EnergyUsagePanelComponent.MINUTES_PER_DAY) {
        readingsByMinute[minute] = reading;
      }
    }

    return readingsByMinute;
  }

  private createDataset(label: string, data: Array<number | null>, color: string, order: number) {
    return {
      label,
      data,
      borderColor: color,
      backgroundColor: color,
      order,
      borderWidth: 2,
      pointRadius: 0,
      pointHitRadius: 8,
      tension: 0.25,
      fill: true,
      spanGaps: false
    };
  }

  private toDateInputValue(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  get todayDate(): string {
    return this.toDateInputValue(new Date());
  }
}
