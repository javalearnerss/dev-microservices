import { Component } from '@angular/core';
import { RouterOutlet, RouterLinkActive, RouterModule } from '@angular/router';
import { ExchangeComponent } from './components/exchange/exchange.component';
import { TradeEnricherComponent } from './components/trade-enricher/trade-enricher.component';
import { TradeRefDataComponent } from './components/trade-ref-data/trade-ref-data.component';
import { TradeProcessorComponent } from './components/trade-processor/trade-processor.component';
import { OpsDashboardComponent } from './components/ops-dashboard/ops-dashboard.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterModule,RouterOutlet, ExchangeComponent, TradeEnricherComponent, TradeRefDataComponent, TradeProcessorComponent, OpsDashboardComponent, RouterOutlet, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'trade-monitor-ui';
}
