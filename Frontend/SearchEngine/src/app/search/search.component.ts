import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, Validators, FormControl } from '@angular/forms';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss'],
})
export class SearchComponent implements OnInit {
  constructor(private route: Router, private fb: FormBuilder) {}

  Form = this.fb.group({
    control: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.filteredOptions = this.q.valueChanges.pipe(
      startWith(''),
      map((value) => this._filter(value))
    );
  }

  search(): void {
    console.log(this.q);
    if (!this.q.value.trim()) return;
    this.route.navigate(['/result', { q: this.q.value }]);
  }
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
