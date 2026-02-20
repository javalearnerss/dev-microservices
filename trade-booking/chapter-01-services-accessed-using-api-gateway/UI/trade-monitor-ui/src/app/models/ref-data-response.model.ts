import { TradeSymbol } from "./trade-symbol.model";

export interface RefDataResponse{
  count: number;
  data: TradeSymbol[];
}