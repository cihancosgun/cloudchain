import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartsModule } from 'ng2-charts/ng2-charts';
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';
import { ProgressbarModule } from 'ngx-bootstrap/progressbar';
import { FilesComponent } from './files.component';
import { FilesRoutingModule } from './files-routing.module';
import { FileDropModule } from 'ngx-file-drop/lib/ngx-drop';
import { TreeModule } from 'angular-tree-component';
import { TranslateModule } from "@ngx-translate/core";
import { ModalModule } from 'ngx-bootstrap/modal';
import { AlertModule } from 'ngx-bootstrap/alert';
import { PaginationModule } from 'ngx-bootstrap/pagination';
import { SocketIoModule, SocketIoConfig } from 'ng-socket-io';

import { FormsModule, ReactiveFormsModule } from '@angular/forms';

const config: SocketIoConfig = { url: 'http://192.168.134.85:3500', options: {} };


@NgModule({
  imports: [
    FilesRoutingModule,
    ChartsModule,    
    BsDropdownModule,
    FileDropModule,
    CommonModule,
    TreeModule,
    TranslateModule,
    FormsModule,
    ReactiveFormsModule,
    ProgressbarModule.forRoot(), 
    PaginationModule.forRoot(),   
    ModalModule.forRoot(),
    AlertModule.forRoot(),
    SocketIoModule.forRoot(config)
  ],
  declarations: [FilesComponent]
})
export class FilesModule { }
