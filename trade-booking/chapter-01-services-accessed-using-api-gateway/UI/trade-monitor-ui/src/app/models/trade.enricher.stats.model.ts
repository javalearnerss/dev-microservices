export interface TradeEnricherStats {
  received: number;
  processed: number;
  sent: number;
  failed: number;
  pendingProcessing: number;
  pendingSend: number;

  lastReceivedAt: string | null;
  lastProcessedAt: string | null;
  lastSentAt: string | null;
  lastFailedAt: string | null;
}
