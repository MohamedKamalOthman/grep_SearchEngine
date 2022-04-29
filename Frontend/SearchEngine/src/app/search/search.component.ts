import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute, ParamMap} from '@angular/router';
import {FormBuilder, Validators} from '@angular/forms';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {

  constructor(private route: Router, private fb: FormBuilder) {
  }

  get q() {
    return this.Form.get('q');
  }

  Form = this.fb.group(
    {
      q: ['', [Validators.required]]
    }
  );

  ngOnInit(): void {
  }

  search(): void {
    console.log(this.q);
    this.route.navigate(['/result', {q: this.q?.value}]);
  }
}
