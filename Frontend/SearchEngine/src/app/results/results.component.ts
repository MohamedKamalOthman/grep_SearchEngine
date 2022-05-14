import { Component, OnInit, NgZone } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { faCaretRight, faCaretLeft } from '@fortawesome/free-solid-svg-icons';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { map, Observable, startWith } from 'rxjs';
import { FindService } from '../services/find.service';
import { QueriesService } from '../services/queries.service';
import {
  defaultFindChunks,
  defaultSanitize,
  findAll,
} from '../utils/ngx-highlight-words.utils';
import { Chunk } from '../utils/ngx-highlight-words.utils';
import { ThemePalette } from '@angular/material/core';

declare const annyang: any;

@Component({
  selector: 'app-results',
  templateUrl: './results.component.html',
  styleUrls: ['./results.component.scss'],
})
export class ResultsComponent implements OnInit {
  loading = true;
  color: ThemePalette = 'warn';
  resultsCount = 0;
  resultstime = 0;
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private findService: FindService,
    private queriesService: QueriesService,
    private ngZone: NgZone
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
  options!: string[];
  filteredOptions!: Observable<string[]>;
  //Functions

  search(): void {
    if (!this.q.value || !this.q?.value.trim()) return;
    this.router.routeReuseStrategy.shouldReuseRoute = () => false;
    this.router.onSameUrlNavigation = 'reload';
    this.router.navigate(['/result', { q: this.q.value }]);
    this.options.push(this.q.value);
  }
  ngOnInit(): void {
    const routeParams = this.route.snapshot.paramMap;
    const q = routeParams.get('q');
    if (q == null) {
      this.router.navigate(['/search']);
    }
    this.query = q;
    this.searchWords = this.query?.split(/[ ,]+/) as string[];
    this.findService.Find(q).subscribe((data) => {
      this.results = data as any;
      this.pages = Math.ceil(this.results.length / 10.0);
      this.pageNumbers = Array.from(Array(this.pages).keys());
      this.loading = false;
      //ugly code
      this.findService.stats().subscribe((data) => {
        this.resultstime = data.time;
        this.resultsCount = data.results;
        console.log(data);
      });
    });
    this.queriesService.Queries().subscribe((data) => {
      this.options = data as string[];
      this.filteredOptions = this.q.valueChanges.pipe(
        startWith(''),
        map((value) => this._filter(value))
      );
    });
    this.q.setValue(this.query);
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
    // another ugly code
    var diffrence = page - this.page;
    if (diffrence > 0)
      for (let index = 0; index < diffrence; index++) {
        this.nextPage();
      }
    else
      for (let index = 0; index < -diffrence; index++) {
        this.prevPage();
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

  //! disclaimer I don't own the rights to claim any work related to "ngx-highlight-words.utils"
  //! library was incombatble with anguler 13 so I had to copy necessary parts from it
  textToHighlight: string = '';
  searchWords: string[] = [];
  highlightClassName = 'highlight';
  autoEscape = true;
  caseSensitive = false;
  findChunks = defaultFindChunks;
  sanitize = defaultSanitize;

  chunks(index: number): Chunk[] {
    this.textToHighlight = this.results[index].p.slice(0, 320);
    return findAll({
      textToHighlight: this.textToHighlight,
      searchWords: this.searchWords,
      autoEscape: this.autoEscape,
      caseSensitive: this.caseSensitive,
      findChunks: this.findChunks,
      sanitize: this.sanitize,
    });
  }
  // ============================== voice search section ==============================
  voiceActiveSectionDisabled: boolean = true;
  voiceActiveSectionError: boolean = false;
  voiceActiveSectionSuccess: boolean = false;
  voiceActiveSectionListening: boolean = false;
  voiceText: any;
  initializeVoiceRecognitionCallback(): void {
    annyang.addCallback('error', (err: any) => {
      if (err.error === 'network') {
        this.voiceText = 'Internet is require';
        annyang.abort();
        this.ngZone.run(() => (this.voiceActiveSectionSuccess = true));
      } else if (this.voiceText === undefined) {
        this.ngZone.run(() => (this.voiceActiveSectionError = true));
        annyang.abort();
      }
    });

    annyang.addCallback('soundstart', (res: any) => {
      this.ngZone.run(() => (this.voiceActiveSectionListening = true));
    });

    annyang.addCallback('end', () => {
      if (this.voiceText === undefined) {
        this.ngZone.run(() => (this.voiceActiveSectionError = true));
        annyang.abort();
      }
    });

    annyang.addCallback('result', (userSaid: any) => {
      this.ngZone.run(() => (this.voiceActiveSectionError = false));

      let queryText: any = userSaid[0];

      annyang.abort();

      this.voiceText = queryText;

      this.ngZone.run(() => (this.voiceActiveSectionListening = false));
      this.ngZone.run(() => (this.voiceActiveSectionSuccess = true));
      console.log(queryText);
      this.q.setValue(queryText);
      this.voiceRecognitionOn = false;
    });
  }

  startVoiceRecognition(): void {
    this.voiceActiveSectionDisabled = false;
    this.voiceActiveSectionError = false;
    this.voiceActiveSectionSuccess = false;
    this.voiceText = undefined;
    this.voiceRecognitionOn = true;
    if (annyang) {
      let commands = {
        'demo-annyang': () => {},
      };

      annyang.addCommands(commands);

      this.initializeVoiceRecognitionCallback();

      annyang.start({ autoRestart: false });
    }
  }
  closeVoiceRecognition(): void {
    this.voiceActiveSectionDisabled = true;
    this.voiceActiveSectionError = false;
    this.voiceActiveSectionSuccess = false;
    this.voiceActiveSectionListening = false;
    this.voiceText = undefined;
    this.voiceRecognitionOn = false;
    if (annyang) {
      annyang.abort();
    }
  }

  voiceRecognitionOn = false;
  voiceRecognition() {
    if (this.voiceRecognitionOn) this.closeVoiceRecognition();
    else this.startVoiceRecognition();
  }
}
