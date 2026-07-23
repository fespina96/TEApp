import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NoConnectionComponent } from './shared/no-connection/no-connection.component';
import { ConnectivityService } from './core/services/connectivity.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NoConnectionComponent],
  template: `
    <router-outlet></router-outlet>
    <app-no-connection></app-no-connection>
  `
})
export class AppComponent implements OnInit {
  constructor(private connectivity: ConnectivityService) {}

  ngOnInit(): void {
    this.connectivity.check();
  }
}
