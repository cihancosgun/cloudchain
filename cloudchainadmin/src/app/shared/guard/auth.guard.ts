import { Injectable } from '@angular/core';
import { CanActivate } from '@angular/router';
import { Router } from '@angular/router';
import { BackendService } from '../services/backendService';

@Injectable()
export class AuthGuard implements CanActivate {

    constructor(private router: Router, private backendService: BackendService) { }

    canActivate() {
        if (this.backendService.isLoggedIn()) {
            return true;
        }

        this.router.navigate(['pages/login']);
        return false;
    }
}
