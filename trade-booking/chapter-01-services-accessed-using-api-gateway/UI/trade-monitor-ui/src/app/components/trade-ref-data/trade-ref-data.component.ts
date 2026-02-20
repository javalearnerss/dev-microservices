import { Component, OnDestroy, OnInit } from '@angular/core';
import { TradeRefDataService } from '../../services/trade-ref-data.service';
import { interval, Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TradeSymbol as TradeRefData } from '../../models/trade-symbol.model';
import { RefdataStats } from '../../models/refdata.stats.model';

@Component({
  selector: 'app-trade-ref-data',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './trade-ref-data.component.html',
  styleUrl: './trade-ref-data.component.css'
})
export class TradeRefDataComponent implements OnInit,OnDestroy {

  symbol: string = 'RELIANCE, TCS, INFY';
  symbolCount: number = 0;
  items: TradeRefData[] = [];

  action: 'Start' | 'Pause' = 'Start';

  private sub = new Subscription();

  stats: RefdataStats = {
    received: 0,
    processed: 0,
    present: 0,
    missing: 0,
    pendingProcessing: 0
  };

  constructor(private refData: TradeRefDataService) {

  }

  ngOnInit(): void {
    this.loadStats();
  }

  retrieveIsin() {
    const symbolSub = this.refData.getIsin(this.symbol).subscribe({
      next: (response) => {
        this.symbolCount = response.count;
        this.items = response.data;
      },
      error: (error) => {
        console.log('An error occured when retrieving isin for symbol={}', this.symbol, error);
      }
    });
    this.sub.add(symbolSub);
  }


  ngOnDestroy(): void {
    if (this.sub) {
      this.sub.unsubscribe();
    }
  }

  

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
   const statsObs = this.refData.getStats().subscribe({
      next: (sts) => {
        this.stats = sts;
      },
      error: (error) => {
        console.log('Error occured while retrieving stats for ref Data service', error);
      }
    });
    this.sub.add(statsObs);
  }

}
