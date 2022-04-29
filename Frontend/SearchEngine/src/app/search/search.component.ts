import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, ParamMap } from '@angular/router';
@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {

  constructor(private route: Router) { 

  }
  q:any;
  ngOnInit(): void {
  }
  search():void{
    this.route.navigate(['/result',{q:this.q}]);
  }
}
