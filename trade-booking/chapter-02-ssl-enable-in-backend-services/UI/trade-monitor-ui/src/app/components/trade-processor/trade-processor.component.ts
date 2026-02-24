import { Component, OnDestroy, OnInit } from '@angular/core';
import { TradeProcessorService } from '../../services/trade-processor.service';
import { interval, Subscription } from 'rxjs';
import { TradeEnricherStats } from '../../models/trade.enricher.stats.model';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-trade-processor',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './trade-processor.component.html',
  styleUrl: './trade-processor.component.css'
})
export class TradeProcessorComponent implements OnInit, OnDestroy {

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

  constructor(private tradeProcessor: TradeProcessorService) { }

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
    const statsObs = this.tradeProcessor.fetchEnricherStats().subscribe({
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
