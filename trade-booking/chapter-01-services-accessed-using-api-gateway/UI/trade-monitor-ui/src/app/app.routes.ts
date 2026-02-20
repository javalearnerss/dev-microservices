import { Routes } from '@angular/router';
import { ExchangeComponent } from './components/exchange/exchange.component';
import { OpsDashboardComponent } from './components/ops-dashboard/ops-dashboard.component';
import { TradeEnricherComponent } from './components/trade-enricher/trade-enricher.component';
import { TradeProcessorComponent } from './components/trade-processor/trade-processor.component';
import { TradeRefDataComponent } from './components/trade-ref-data/trade-ref-data.component';

export const routes: Routes = [
    {
        path: 'ops-dashboard',
        component: OpsDashboardComponent
    },
    {
        path: 'exchange',
        component: ExchangeComponent
    },
    {
        path: 'trade-enricher',
        component: TradeEnricherComponent
    },
    {
        path: 'trade-processor',
        component: TradeProcessorComponent
    },
    {
        path: 'refdata-provide',
        component: TradeRefDataComponent
    }
];
