import { Component, OnInit, OnDestroy } from '@angular/core';
import { ExchangeService } from '../../services/exchange.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-exchange',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './exchange.component.html',
  styleUrl: './exchange.component.css'
})
export class ExchangeComponent implements OnInit, OnDestroy {

  feedStatus: string = 'UNKNOWN';
  feedEntries: [string, number][] = [];

  duration: number = 1000;
  message: string = 'Feed is OFF';

  // ✅ UI state
  loadingStatus = false;
  loadingDetails = false;
  loadingStartStop = false;

  // ✅ table interactions
  feedSearch = '';
  sortDir: 'asc' | 'desc' = 'desc';

  // ✅ toast
  toastVisible = false;
  toastText = '';

  private autoRefreshSub?: Subscription;

  private subs = new Subscription();

  constructor(private exchangeService: ExchangeService) { }

  ngOnInit(): void {
    this.updateFeedStatus();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
    this.stopAutoRefresh();
  }

  startAutoRefresh() {
    if (this.autoRefreshSub) return;

    this.autoRefreshSub = interval(1000).subscribe(() => {
      this.getFeedDetails();
    });
  }

  stopAutoRefresh() {
    if (this.autoRefreshSub) {
      this.autoRefreshSub.unsubscribe();
      this.autoRefreshSub = undefined;
    }
  }

  // ---------- Derived UI helpers ----------
  get filteredFeeds(): [string, number][] {
    const q = (this.feedSearch || '').toLowerCase().trim();

    const arr = (this.feedEntries || []).filter(([name]) =>
      (name || '').toLowerCase().includes(q)
    );

    arr.sort((a, b) => {
      const av = Number(a?.[1] ?? 0);
      const bv = Number(b?.[1] ?? 0);
      return this.sortDir === 'desc' ? (bv - av) : (av - bv);
    });

    return arr;
  }

  get totalTrades(): number {
    return (this.feedEntries || []).reduce((sum, [, count]) => sum + Number(count ?? 0), 0);
  }

  toggleSort(): void {
    this.sortDir = this.sortDir === 'desc' ? 'asc' : 'desc';
  }

  setIntervalPreset(v: number): void {
    this.duration = v;
  }

  trackByFeedName = (_: number, item: [string, number]) => item?.[0];

  // ---------- API Calls ----------
  updateFeedStatus(): void {
    this.loadingStatus = true;

    const s = this.exchangeService.getExchangeFeedStatus()
      .pipe(finalize(() => (this.loadingStatus = false)))
      .subscribe({
        next: (result) => {
          this.feedStatus = result ?? 'UNKNOWN';

          // ✅ keep message consistent with status
          if ((this.feedStatus || '').toUpperCase().includes('ON') ) {
            this.message = this.message || 'Feed is ON';
          } else if ((this.feedStatus || '').toUpperCase().includes('OFF') ) {
            this.message = this.message || 'Feed is OFF';
          }
        },
        error: (error) => {
         // console.log('Error occurred while retrieving feed status', error);
         // this.showToast('Failed to refresh status ❌');
        }
      });

    this.subs.add(s);
  }

  getFeedDetails(): void {
    this.loadingDetails = true;

    const s = this.exchangeService.getFeedDetails()
      .pipe(finalize(() => (this.loadingDetails = false)))
      .subscribe({
        next: (result) => {
          // service returns object -> convert to tuple list
          this.feedEntries = Object.entries(result || {}) as [string, number][];
        },
        error: (error) => {
         // console.log('Error occurred while retrieving feed details', error);
         // this.showToast('Failed to load details ❌');
        }
      });

    this.subs.add(s);
  }

  onFeedStart(): void {
    // ✅ guardrails
    const safe = this.clampduration(this.duration);

    this.loadingStartStop = true;

    const s = this.exchangeService.startFeed(safe)
      .pipe(finalize(() => (this.loadingStartStop = false)))
      .subscribe({
        next: (res) => {
          this.message = res || 'Feed started';
          this.showToast(this.message+' ✅');
          this.updateFeedStatus();
          if (this.feedStatus === 'ON') {
            this.startAutoRefresh();
          } 
        },
        error: (error) => {
          console.log('Error occurred while starting feed', error);
          this.showToast('Start failed');
        }
      });

    this.subs.add(s);
  }

  onFeedStop(): void {
    this.loadingStartStop = true;

    const s = this.exchangeService.stopFeed()
      .pipe(finalize(() => (this.loadingStartStop = false)))
      .subscribe({
        next: (res) => {
          this.message = res || 'Feed stopped';
          this.showToast(this.message+' 🛑');
          this.updateFeedStatus();
          this.getFeedDetails();
          if (this.feedStatus === 'OFF') {
            this.stopAutoRefresh();
          } 
        },
        error: (error) => {
          console.log('Error occurred while stopping feed', error);
          this.showToast('Stop failed');
        }
      });

    this.subs.add(s);
  }

  // ---------- Clipboard + Toast ----------
  copyMessage(): void {
    if (!this.message) return;

    navigator.clipboard.writeText(this.message)
      .then(() => this.showToast('Copied to clipboard ✅'))
      .catch(() => this.showToast('Copy failed ❌'));
  }

  showToast(text: string): void {
    this.toastText = text;
    this.toastVisible = true;

    // auto-hide
    window.setTimeout(() => (this.toastVisible = false), 2500);
  }

  private clampduration(v: number): number {
    const n = Number(v ?? 1000);
    if (Number.isNaN(n)) return 1000;
    return Math.min(60000, Math.max(50, n));
  }
}
