import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router, ActivatedRoute, ParamMap } from '@angular/router';
import {
  faCaretRight,
  faCaretLeft,
  faMicrophone,
} from '@fortawesome/free-solid-svg-icons';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { map, Observable, startWith } from 'rxjs';

@Component({
  selector: 'app-results',
  templateUrl: './results.component.html',
  styleUrls: ['./results.component.scss'],
})
export class ResultsComponent implements OnInit {
  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder
  ) {}
  // font awsome
  faCaretRight = faCaretRight;
  faCaretLeft = faCaretLeft;
  // vars
  results!: any[];
  page = 1;
  pageSize = 10;
  pages = 0;
  pageNumbers!: number[];
  nextBuffer = 10;
  backBuffer = 0;
  query!: string | null;
  //search bar
  Form = this.fb.group({
    q: ['', [Validators.required]],
  });
  q = new FormControl();
  //this will be loaded from data base
  options: string[] = [
    'Champs-Élysées',
    'Lombard Street',
    'Abbey Road',
    'Fifth Avenue',
    'Champs-Élysées',
    'Lombard Street',
    'Abbey Road',
    'Fifth Avenue',
  ];
  filteredOptions!: Observable<string[]>;
  //Functions

  search(): void {
    console.log(this.q);
    if (!this.q?.value.trim()) return;
    this.router.routeReuseStrategy.shouldReuseRoute = () => false;
    this.router.onSameUrlNavigation = 'reload';
    this.router.navigate(['/result', { q: this.q.value }]);
  }
  ngOnInit(): void {
    const routeParams = this.route.snapshot.paramMap;
    const q = routeParams.get('q');
    if (q == null) {
      this.router.navigate(['/search']);
    }
    this.query = q;
    let response = this.http.get('http://localhost:8080/api/GoFind/' + q);
    response.subscribe((data) => {
      this.results = data as any;
      this.pages = Math.ceil(this.results.length / 10.0);
      this.pageNumbers = Array.from(Array(this.pages).keys());
      console.log(this.results);
    });
    this.filteredOptions = this.q.valueChanges.pipe(
      startWith(''),
      map((value) => this._filter(value))
    );
  }
  nextPage() {
    if (this.pages < this.page) return;
    this.page++;
    if (this.page > 5 && this.nextBuffer < this.pages) {
      this.backBuffer++;
      this.nextBuffer++;
    }
  }
  prevPage() {
    if (this.page < 1) return;
    this.page--;
    if (this.page < this.pages - 5 && this.backBuffer > 0) {
      this.backBuffer--;
      this.nextBuffer--;
    }
  }
  setPage(page: number) {
    this.page = page;
    // if (this.page < this.pages - 5 && this.page > 5) {
    //   this.backBuffer = page - 5;
    //   this.nextBuffer = page + 5;
    // } else if (this.page < this.pages - 5) {
    //   this.backBuffer = this.pages - 10;
    //   this.nextBuffer = this.pages;
    // } else if(this.page > 5) {
    //   this.backBuffer = 0;
    //   this.nextBuffer = 10;
    // }
  }
  public highlight(i: number) {
    let words = this.query?.split(/[ ,]+/);
    if (this.query) {
      console.log(words);
      for (let index = 0; index < words!.length; index++) {
        this.results[i].p = this.results[i].p.replace(
          new RegExp(words![index], 'gi'),
          (match: string) => {
            return '<b>' + match + '</b>';
          }
        );
      }
      return this.results[i].p;
    }
  }

  private _filter(value: string): string[] {
    const filterValue = this._normalizeValue(value);
    return this.options.filter((option) =>
      this._normalizeValue(option).includes(filterValue)
    );
  }

  private _normalizeValue(value: string): string {
    return value.toLowerCase().replace(/\s/g, '');
  }
}
