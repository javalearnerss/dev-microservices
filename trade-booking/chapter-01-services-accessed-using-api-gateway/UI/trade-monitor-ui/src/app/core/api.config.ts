import { environment } from "../../environments/environment";

export const API = {
    baseUrl: environment.url,
    exchangeUrl: `${environment.url}/exchange`,
    tradeEnricherUrl: `${environment.url}/trade-enricher`,
    tradeProcessorUrl: `${environment.url}/trade-processor`,
    refdataProviderUrl: `${environment.url}/reference`
};