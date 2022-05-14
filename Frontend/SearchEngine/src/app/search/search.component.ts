import { Component, NgZone, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, Validators, FormControl } from '@angular/forms';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { QueriesService } from '../services/queries.service';

declare const annyang: any;

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss'],
})
export class SearchComponent implements OnInit {
  constructor(
    private route: Router,
    private fb: FormBuilder,
    private queriesService: QueriesService,
    private ngZone: NgZone
  ) {}

  Form = this.fb.group({
    control: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.queriesService.Queries().subscribe((data) => {
      this.options = data as string[];
      this.filteredOptions = this.q.valueChanges.pipe(
        startWith(''),
        map((value) => this._filter(value))
      );
      console.log(data);
    });
  }

  search(): void {
    if (!this.q.value || !this.q.value.trim()) return;
    this.route.navigate(['/result', { q: this.q.value }]);
  }
  q = new FormControl();
  //this will be loaded from data base
  options!: string[];
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
