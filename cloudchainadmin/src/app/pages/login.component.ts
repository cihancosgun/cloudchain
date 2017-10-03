import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { BackendService } from '../shared/services/backendService'
import { TranslateService } from '@ngx-translate/core';
import { User } from '../models/user';

@Component({
  templateUrl: 'login.component.html'
})
export class LoginComponent implements OnInit {
  user: User = new User("", "", "");
  isLoggedin: boolean = true;
  constructor(public router: Router, private backendService: BackendService) { }

  ngOnInit() {
  }

  public login() {
    this.backendService.checkLogin(this.user.email, this.user.password).then((val) => {
      this.isLoggedin = val.toString() != "";
      if (this.isLoggedin) {
        sessionStorage.setItem('isLoggedin', this.isLoggedin.toString());
        let userResult = val;
        sessionStorage.setItem("loggedUser", JSON.stringify(userResult));
      }
      this.router.navigateByUrl('');
    });

  }

  public register() {
    this.router.navigateByUrl('pages/register');
  }
}
