import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from 'src/environments/environment';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class QueriesService {

  constructor(private http : HttpClient) { }

  Queries (): Observable<string[]> {
    return this.http.get<string[]>(`${environment.baseUrl}/prevQueries/`);
  }

}
