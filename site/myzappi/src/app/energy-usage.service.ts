import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EnergyUsageResolution, EnergyUsageResponse } from './energyUsage.interface';

@Injectable({
  providedIn: 'root'
})
export class EnergyUsageService {
  private readonly energyUsageUrl = 'https://api.myzappiunofficial.com/energy-usage';

  constructor(private readonly http: HttpClient) { }

  getEnergyUsage(
    bearerToken: string | null | undefined,
    date: string,
    zoneId: string,
    resolution: EnergyUsageResolution
  ): Observable<EnergyUsageResponse> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': bearerToken ?? ''
    });
    const params = new HttpParams()
      .set('date', date)
      .set('zoneId', zoneId)
      .set('resolution', resolution);

    return this.http.get<EnergyUsageResponse>(this.energyUsageUrl, {
      headers,
      params,
      withCredentials: true
    });
  }
}
