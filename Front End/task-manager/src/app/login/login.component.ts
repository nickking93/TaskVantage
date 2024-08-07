import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
clickEvent($event: MouseEvent) {
throw new Error('Method not implemented.');
}
hide: any;
signin: FormGroup<any> | undefined;
passwordInput: any;

  constructor() { }

  ngOnInit(): void {
  }

}
