import { Component, OnInit, NgModule, TemplateRef } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgClass, CommonModule } from '@angular/common';
import { FormGroup } from '@angular/forms';
import { BsModalService } from 'ngx-bootstrap/modal';
import { BsModalRef } from 'ngx-bootstrap/modal/modal-options.class';
import { Register } from '../models/register'
import { ValidationManager } from "ng2-validation-manager";
import { BackendService } from "../shared/services/backendService";
import { User } from "../models/user";
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';


@Component({
  templateUrl: 'register.component.html'
})
export class RegisterComponent implements OnInit {
  public modalRef: BsModalRef;
  form: ValidationManager;
  fg: FormGroup;
  registerError: string = null;
  register: Register = new Register("", "", "", "");

  constructor(private modalService: BsModalService, private backendService: BackendService, public router: Router, private translate: TranslateService) {
  }

  ngOnInit() {
    this.form = new ValidationManager({    
      'email': 'required|email',     
      'password': 'required|rangeLength:8,50',
      'passwordConfirm': 'required|equalTo:password'
    });
    this.fg = this.form.getForm();

  }
 
  onChangeEmail() {
    this.backendService.checkEmail(this.form.getData().email).then((val) => {
      if (!val) {
        this.translate.get('email is using by another user, please change.', { value: '' }).subscribe((res: string) => {
          this.registerError = this.register.email + " " + res;
          this.fg.controls["email"].setValue("");
          this.register.email = "";
        });
      }
    });
  }

  save(registerModal: TemplateRef<any>) {
    this.registerError = null;
    if (this.form.isValid()) {
      let user: User;
      this.backendService.createUser(<Register>this.form.getData())
        .then((val) => {
          user = val;
          this.openModal(registerModal);
          this.form.reset();
        })
        .catch((error) => {
          this.registerError = error;
        });
    }
  }

  public openModal(registerModal: TemplateRef<any>) {
    this.modalRef = this.modalService.show(registerModal);
  }
  public closeModal(registerModal: TemplateRef<any>) {
    this.modalRef.hide();
    this.router.navigateByUrl('pages/login');
  }

}
