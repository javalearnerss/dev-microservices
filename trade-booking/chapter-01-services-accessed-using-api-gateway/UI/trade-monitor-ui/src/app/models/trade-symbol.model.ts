

export interface TradeSymbol{
    symbol: string;
    isin: string;
    status: 'FOUND' | 'NOT_FOUND';
}