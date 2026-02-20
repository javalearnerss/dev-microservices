import { Component, OnDestroy, OnInit } from '@angular/core';
import { RefdataStats } from '../../models/refdata.stats.model';
import { TradeEnricherStats } from '../../models/trade.enricher.stats.model';
import { ExchangeService } from '../../services/exchange.service';
import { interval, Subscription } from 'rxjs';
import { DatePipe } from '@angular/common';
import { TradeEnricherService } from '../../services/trade-enricher.service';
import { TradeProcessorService } from '../../services/trade-processor.service';
import { TradeRefDataService } from '../../services/trade-ref-data.service';

@Component({
  selector: 'app-ops-dashboard',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './ops-dashboard.component.html',
  styleUrl: './ops-dashboard.component.css'
})
export class OpsDashboardComponent implements OnInit, OnDestroy {

  statsDummy: TradeEnricherStats = {
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

  refDataStats: RefdataStats = {
    received: 0,
    processed: 0,
    present: 0,
    missing: 0,
    pendingProcessing: 0
  };

  enricherStats: TradeEnricherStats = this.statsDummy;
  processorStats: TradeEnricherStats = this.statsDummy;
  feedEntries: [string, number][] = [];
  feedStatus: string = 'STOPPED';

  constructor(private exchangeService: ExchangeService,
    private tradeEnricher: TradeEnricherService,
    private tradeProcessor: TradeProcessorService,
    private refData: TradeRefDataService) { }

  private subs = new Subscription();

  lastFeedDate: Date = new Date();
  lastRefDataDate: Date = new Date();
  lastProcessorDate: Date = new Date();

  action: 'Start' | 'Pause' = 'Start';

  ngOnInit(): void {
      this.loadStats();
  }


  loadStats() {
    this.loadFeedStatus();
    this.loadFeedDetails();
    this.loadEnricherStats();
    this.loadProcessorStats();
    this.loadRefdataStats();
  }

  get totalTrades(): number {
    return (this.feedEntries || []).reduce((sum, [, count]) => sum + Number(count ?? 0), 0);
  }

  loadFeedDetails(): void {
    const s = this.exchangeService.getFeedDetails()
      .subscribe({
        next: (result) => {
          this.feedEntries = Object.entries(result || {}) as [string, number][];
          this.lastFeedDate = new Date();
        },
        error: (error) => {
          console.log('Error occurred while retrieving feed details', error);
        }
      });
    this.subs.add(s);
  }


  loadFeedStatus(): void {
    const s = this.exchangeService.getExchangeFeedStatus()
      .subscribe({
        next: (result) => {
          this.feedStatus = (result ?? 'STOPPED') == 'ON' ? 'RUNNING' : 'STOPPED';
        },
        error: (error) => {
          console.log('Error occurred while retrieving feed status', error);
        }
      });

    this.subs.add(s);
  }

  loadEnricherStats() {
    const statsObs = this.tradeEnricher.fetchEnricherStats().subscribe({
      next: (response) => {
        this.enricherStats = response;
      },
      error: (error) => {
        console.log('Error occured while retrieving stats from enricher', error);
      },
      complete: () => console.log('✅ request complete')
    });
    this.subs.add(statsObs);
  }

  loadProcessorStats() {
    const statsObs = this.tradeProcessor.fetchEnricherStats().subscribe({
      next: (response) => {
        this.lastProcessorDate = new Date();
        this.processorStats = response;
      },
      error: (error) => {
        console.log('Error occured while retrieving stats from enricher', error);
      },
      complete: () => console.log('✅ request complete')
    });
    this.subs.add(statsObs);
  }


  loadRefdataStats() {
    const statsObs = this.refData.getStats().subscribe({
      next: (sts) => {
        this.lastRefDataDate = new Date();
        this.refDataStats = sts;
      },
      error: (error) => {
        console.log('Error occured while retrieving stats for ref Data service', error);
      }
    });
    this.subs.add(statsObs);
  }


  onStartPause() {
    if (this.action == 'Start') {
      this.action = 'Pause';
      this.statsOn();
    } else {
      this.action = 'Start';
      this.subs.unsubscribe();
      this.subs = new Subscription();
    }
  }

  statsOn() {
    const stats = interval(1000).subscribe(() => {
      this.loadStats();
    });
    this.subs.add(stats);
  }

  ngOnDestroy(): void {
    if (this.subs)
      this.subs.unsubscribe();
  }


}
