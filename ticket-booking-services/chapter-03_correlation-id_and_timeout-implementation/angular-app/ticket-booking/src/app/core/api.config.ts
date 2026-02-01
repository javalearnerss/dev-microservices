import { environment } from '../../environments/environment';

export const API = {
  base: environment.gatewayBaseUrl,
  booking: `${environment.gatewayBaseUrl}/bookings`,
  inventory: `${environment.gatewayBaseUrl}/inventory`,
  user: `${environment.gatewayBaseUrl}/users`,
  payment: `${environment.gatewayBaseUrl}/payments`,
};
