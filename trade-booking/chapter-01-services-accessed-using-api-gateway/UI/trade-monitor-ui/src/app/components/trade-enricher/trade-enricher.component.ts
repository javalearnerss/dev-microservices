import { Component, OnDestroy, OnInit } from '@angular/core';
import { TradeEnricherStats } from '../../models/trade.enricher.stats.model';
import { TradeEnricherService } from '../../services/trade-enricher.service';
import { interval, Subscription } from 'rxjs';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-trade-enricher',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './trade-enricher.component.html',
  styleUrl: './trade-enricher.component.css'
})
export class TradeEnricherComponent implements OnInit, OnDestroy {

  stats: TradeEnricherStats = {
    received: 0,
    processed: 0,
    sent: 0,
    failed: 0,
    pendingProcessing: 0,
    pendingSend: 0,
    lastReceivedAt: '',
    lastProcessedAt: '',
    lastSentAt: '',
    lastFailedAt: ''
  };

  action: 'Start' | 'Pause' = 'Start';

  constructor(private tradeEnricher: TradeEnricherService) { }

  ngOnInit(): void {
    this.loadStats();
  }

  private sub = new Subscription();

  onStartPause() {
    if (this.action == 'Start') {
      this.action = 'Pause';
      this.statsOn();
    } else {
      this.action = 'Start';
      this.sub.unsubscribe();
      this.sub = new Subscription();  
    }
  }

  statsOn() {
    const stats = interval(500).subscribe(() => {
      this.loadStats()
    });
    this.sub.add(stats);
  }

  loadStats() {
    const statsObs = this.tradeEnricher.fetchEnricherStats().subscribe({
      next: (response) => {
        this.stats = response;
      },
      error: (error) => {
        console.log('Error occured while retrieving stats from enricher', error);
      },
      complete: () => console.log('✅ request complete')
    });
    this.sub.add(statsObs);
  }

  ngOnDestroy(): void {
    if (this.sub)
      this.sub.unsubscribe();
  }

}
