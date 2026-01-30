import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InventoryService, ShowInfo } from '../../services/inventory.service';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './inventory.component.html',
})
export class InventoryComponent implements OnInit {

  // Create/Update show
  showId = 'SHOW-101';
  totalTickets = 10;

  // Get show
  getShowId = 'SHOW-101';

  // ✅ All shows for first time load
  shows: ShowInfo[] = [];

  // Output
  resp: any = null;
  err = '';

  constructor(private api: InventoryService) {}

  ngOnInit(): void {
    this.loadAllShows(); // ✅ first time load
  }

  /**
   * Loads all available shows for initial UI load.
   */
  loadAllShows() {
    this.err = '';
    this.api.getAllShows().subscribe({
      next: (x) => (this.shows = x),
      error: (e) => (this.err = e?.error?.error || e?.message || 'Failed to load shows')
    });
  }

  /**
   * Create or update a show inventory (total tickets).
   */
  createOrUpdate() {
    this.err = '';
    this.resp = null;

    this.api.createOrUpdateShow(this.showId, this.totalTickets).subscribe({
      next: (x) => {
        this.resp = x;
        this.loadAllShows(); // ✅ refresh list after update
      },
      error: (e) => (this.err = e?.error?.error || e?.message || 'Request failed')
    });
  }

  /**
   * Get current remaining tickets for a show.
   */
  getShow() {
    this.err = '';
    this.resp = null;

    this.api.getShow(this.getShowId).subscribe({
      next: (x) => (this.resp = x),
      error: (e) => (this.err = e?.error?.error || e?.message || 'Request failed')
    });
  }
}
