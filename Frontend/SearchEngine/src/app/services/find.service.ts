import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from 'src/environments/environment';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FindService {

  constructor(private http : HttpClient) { }

  Find (q : string | null): Observable<any> {
    return this.http.get(`${environment.baseUrl}/grep/${q}`);
  }
  stats (): Observable<any>{
    return this.http.get(`${environment.baseUrl}/stats`);
  }
}
