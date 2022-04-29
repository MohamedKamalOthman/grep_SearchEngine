import {Component, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Router, ActivatedRoute, ParamMap} from '@angular/router';

@Component({
  selector: 'app-results',
  templateUrl: './results.component.html',
  styleUrls: ['./results.component.scss'],
})
export class ResultsComponent implements OnInit {
  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router
  ) {
  }

  results: any;

  ngOnInit(): void {
    const routeParams = this.route.snapshot.paramMap;
    const q = routeParams.get('q');
    if (q == null) {
      this.router.navigate(['/search']);
    }
    let response = this.http.get('http://localhost:8080/api/GoFind/' + q);
    response.subscribe((data) => (this.results = data));
  }
}
