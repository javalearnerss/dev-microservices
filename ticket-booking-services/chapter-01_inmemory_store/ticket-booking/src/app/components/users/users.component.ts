import { Component } from '@angular/core';
import { User, UsersService } from '../../services/users.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './users.component.html',
  styleUrl: './users.component.css'
})
export class UsersComponent {
users: User[] = [];
  userId = 'U-1001';
  user?: User;
  err = '';

  constructor(private api: UsersService) {}

  load() {
    this.err = '';
    this.api.list().subscribe({
      next: (x) => (this.users = x),
      error: (e) => (this.err = e?.error?.error || e.message)
    });
  }

  get() {
    this.err = '';
    this.api.get(this.userId).subscribe({
      next: (x) => (this.user = x),
      error: (e) => (this.err = e?.error?.error || e.message)
    });
  }
}
