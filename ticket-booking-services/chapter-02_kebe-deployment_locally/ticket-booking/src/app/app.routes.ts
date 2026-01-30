import { RouterModule, Routes } from '@angular/router';
import { UsersComponent } from './components/users/users.component';
import { InventoryComponent } from './components/inventory/inventory.component';
import { BookingsComponent } from './components/bookings/bookings.component';
import { PaymentsComponent } from './components/payments/payments.component';
import { NgModule } from '@angular/core';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'bookings' },
  { path: 'users', component: UsersComponent },
  { path: 'inventory', component: InventoryComponent },
  { path: 'bookings', component: BookingsComponent },
  { path: 'payments', component: PaymentsComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}