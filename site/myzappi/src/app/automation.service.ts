import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Automation, AutomationOptions, AutomationsResponse } from './automation.interface';

@Injectable({ providedIn: 'root' })
export class AutomationService {
  private readonly baseUrl = 'https://api.myzappiunofficial.com';

  constructor(private http: HttpClient) {}

  getOptions(bearerToken: string): Observable<AutomationOptions> {
    return this.http.get<AutomationOptions>(`${this.baseUrl}/automations/options`, this.options(bearerToken));
  }

  list(bearerToken: string): Observable<AutomationsResponse> {
    return this.http.get<AutomationsResponse>(`${this.baseUrl}/automations`, this.options(bearerToken));
  }

  create(bearerToken: string, automation: Partial<Automation>): Observable<Automation> {
    return this.http.post<Automation>(`${this.baseUrl}/automations`, automation, this.options(bearerToken));
  }

  setActive(bearerToken: string, automationId: string, active: boolean): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/automations/${automationId}`, { active }, this.options(bearerToken));
  }

  reorder(bearerToken: string, automationIds: string[]): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/automations/priorities`, { automationIds }, this.options(bearerToken));
  }

  delete(bearerToken: string, automationId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/automations/${automationId}`, this.options(bearerToken));
  }

  private options(bearerToken: string) {
    return {
      headers: new HttpHeaders({ 'Content-Type': 'application/json', 'Authorization': bearerToken }),
      withCredentials: true
    };
  }
}
