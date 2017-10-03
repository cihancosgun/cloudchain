import { Component, OnInit, TemplateRef, NgModule, ViewChild, Renderer, ElementRef } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FileDropModule, UploadEvent, UploadFile } from 'ngx-file-drop/lib/ngx-drop';;
import { TranslateModule } from "@ngx-translate/core";
import { BsModalService } from 'ngx-bootstrap/modal';
import { BsModalRef } from 'ngx-bootstrap/modal/modal-options.class';
import { TreeComponent, TreeNode } from 'angular-tree-component';
import { BackendService } from '../shared/services/backendService'
import { TranslateService } from '@ngx-translate/core';
import { HttpClient, HttpClientModule, HttpRequest, HttpEventType, HttpResponse, HttpHeaders } from "@angular/common/http";
import { SocketService } from '../shared/services/socketService';


@Component({
  templateUrl: 'files.component.html',
  providers : [SocketService]
})
export class FilesComponent implements OnInit {
  public modalRef: BsModalRef;
  folderName: string;
  editingNode: any;
  maxNodeId: number = 1;
  fileUploadPercent: number = 50;
  fileUploadFileName: string = "deneme.txt";
  userFiles: any[] = [];

  public maxSize: number = 10;
  public bigTotalItems: number = 175;
  public bigCurrentPage: number = 1;
  public numPages: number = 0;

  @ViewChild('tree') treeComponent: TreeComponent;
  @ViewChild('fileUploadModal') fileUploadModal: TemplateRef<any>;
  @ViewChild('popUpMenuFolder') popUpMenuFolder: ElementRef;


  public files: any[] = [];
  nodes: any[] = [
    {
      id: 1,
      name: '/',
      type: "folder"
    }
  ];

  constructor(private modalService: BsModalService,
    private backendService: BackendService,
    private translateService: TranslateService, private renderer: Renderer, private socketService : SocketService) { }

  public addFolder(modal: TemplateRef<any>, parentNode: any) {
    this.folderName = "";
    this.editingNode = null;
    this.treeComponent.treeModel.setFocusedNode(parentNode);
    if (this.treeComponent.treeModel.getFocusedNode() == null) {
      this.translateService.get('Please select a parent folder.', null).toPromise().then((val) => {
        alert(val);
      })
      return;
    }
    this.modalRef = this.modalService.show(modal);
  }
  public closeModal() {
    this.modalRef.hide();
  }

  private getMaxNodeId(node: any[]) {
    node.forEach((node) => {
      if (node.id > this.maxNodeId) {
        this.maxNodeId = node.id;
      }
      if (node.children != null) {
        this.getMaxNodeId(node.children);
      }
    })
  }

  public saveFolder() {
    if (this.folderName != "") {

      if (this.editingNode == null) {
        if (this.treeComponent.treeModel.getFocusedNode().data.children == undefined) {
          this.treeComponent.treeModel.getFocusedNode().data.children = [];
        }
        this.getMaxNodeId(this.nodes);
        this.maxNodeId++;
        this.treeComponent.treeModel.getFocusedNode().data.children.push({ id: this.maxNodeId, name: this.folderName, type: "folder" });
      } else {
        this.treeComponent.treeModel.setFocusedNode(this.editingNode);
        this.treeComponent.treeModel.getFocusedNode().data.name = this.folderName;
      }
      this.nodes = this.treeComponent.treeModel.nodes;
      this.treeComponent.treeModel.update();
      this.modalRef.hide();
      this.backendService.setUserNode(this.treeComponent.treeModel.nodes);
      this.treeComponent.treeModel.getFocusedNode().expandAll();
    }

  }

  editFolder(modal: TemplateRef<any>, node: any) {
    this.folderName = node.data.name;
    this.editingNode = node;
    this.modalRef = this.modalService.show(modal);
  }

  removeFolder(node: any) {
    this.translateService.get("Are you sure delete this folder ?", null).toPromise().then((val) => {
      if (confirm(val)) {
        node.parent.data.children.splice(node.parent.data.children.findIndex(item => item.id == node.data.id), 1);
        this.nodes = this.getNodeRoot(node).data.children;
        this.treeComponent.treeModel.nodes = this.nodes;
        this.treeComponent.treeModel.update();
        this.backendService.setUserNode(this.treeComponent.treeModel.nodes);
      }
    });
  }

  getNodeRoot(node: any) {
    if (node.parent != null) {
      return this.getNodeRoot(node.parent);
    } else {
      return node;
    }
  }


  onFileSelect(event) {
    var files: any[] = [];
    for (let i = 0; i < event.srcElement.files.length; i++) {
      files.push(event.srcElement.files.item(i));
    }
    this.addFiles(files);
  }

  addFiles(files) {
    if (this.treeComponent.treeModel.getFocusedNode() == null) {
      this.treeComponent.treeModel.setFocusedNode(this.treeComponent.treeModel.getFirstRoot());
    }
    if (files != null) {
      this.files = files;
      this.recursiveFileUpload(this.files[0], 0);
    }
  }

  recursiveFileUpload(currentFile, currentFileIndex: number) {
    if (currentFile) {
      this.fileUploadFileName = currentFile.name;
      this.fileUploadPercent = 0;
      if (currentFileIndex == 0) {
        this.modalRef = this.modalService.show(this.fileUploadModal);
      }
      this.backendService.fileUpload(currentFile, this.treeComponent.treeModel.getFocusedNode().id).subscribe((event) => {
        if (event.type === HttpEventType.UploadProgress) {
          this.fileUploadPercent = Math.round(100 * event.loaded / event.total);
        } else if (event instanceof HttpResponse) {
          let result = <any>event.body;
          let serverSideFileName = result.path;
          this.saveFileToDb(serverSideFileName, currentFile);
          currentFileIndex++;
          if (currentFileIndex < this.files.length) {
            setTimeout(() => {
              this.recursiveFileUpload(this.files[currentFileIndex], currentFileIndex);
            }, 1000);
          } else {
            setTimeout(() => {
              this.modalRef.hide();
              this.files = [];

              this.backendService.getUserFilesCount(this.treeComponent.treeModel.getFocusedNode().id).then((count) => {
                this.bigTotalItems = (<any>count).count;
                this.bigCurrentPage = 1;
                this.backendService.getUserFiles(this.treeComponent.treeModel.getFocusedNode().id, 0, this.maxSize).then((val) => {
                  this.userFiles = <any[]>val;
                });
              });

            }, 1000);
          }
        }
      });
    }
  }

  saveFileToDb(serverSideFileName, file) {
    let fileInfo: any = {
      "name": file.name, "uploadfilepath": serverSideFileName, "type": file.type, "size": file.size,
      "lastModifiedDate": file.lastModifiedDate, "lastModified": file.lastModified, mail: this.backendService.getCurrentUser().email,
      "nodeid": this.treeComponent.treeModel.getFocusedNode().id
    };
    this.backendService.createFile(fileInfo);
  }

  onNodeActivate(event) {
    this.backendService.getUserFilesCount(event.node.id).then((count) => {
      this.bigTotalItems = (<any>count).count;
      this.bigCurrentPage = 1;
      this.backendService.getUserFiles(event.node.id, 0, this.maxSize).then((val) => {
        this.userFiles = <any[]>val;
      });
    });
  }

  public pageChanged(event: any): void {
    this.backendService.getUserFiles(this.treeComponent.treeModel.getFocusedNode().id, this.maxSize * (event.page - 1), this.maxSize).then((val) => {
      this.userFiles = <any[]>val;
    });
  }

  download(file) {
    let loggedUser: any = JSON.parse(sessionStorage.getItem("loggedUser"));
    window.location.replace(this.backendService.uploadServerUrl + "/download?file=" + file.uploadfilepath + "&email=" + loggedUser.email + "&password="+loggedUser.password);
  }


  dropFiles(event) {
    event.preventDefault();
    if (this.treeComponent.treeModel.getFocusedNode() == null) {
      this.treeComponent.treeModel.setFocusedNode(this.treeComponent.treeModel.getFirstRoot());
    }
    this.files = event.dataTransfer.files;
    this.recursiveFileUpload(this.files[0], 0);
  }

  allowDrop(event) {
    event.preventDefault();
  }

  openContextMenu(event) {
    event.preventDefault();
    if (event.button > 0) {
      //console.log(event);
      this.popUpMenuFolder.nativeElement.style.visibility = 'visible';
      this.popUpMenuFolder.nativeElement.style.top = event.screenY;
      this.popUpMenuFolder.nativeElement.style.left = event.screenX;
    }
  }

  ngOnInit(): void {
    let loggedUser: any = JSON.parse(sessionStorage.getItem("loggedUser"));
    this.backendService.getUserNode().then((val) => {
      this.nodes = val;
    });
    window.addEventListener("contextmenu", (e) => {
      e.preventDefault();
    });

    this.socketService.sendMessage("joinroom",loggedUser.email);
    this.socketService
    .getMessage("updatefiles")
    .subscribe(msg => {
      setTimeout(() => {      
        this.backendService.getUserFilesCount(this.treeComponent.treeModel.getFocusedNode().id).then((count) => {
          this.bigTotalItems = (<any>count).count;
          this.bigCurrentPage = 1;
          this.backendService.getUserFiles(this.treeComponent.treeModel.getFocusedNode().id, 0, this.maxSize).then((val) => {
            this.userFiles = <any[]>val;
          });
        });

      }, 1000);
    });

    this.socketService
    .getMessage("nodeupdate")
    .subscribe(msg => {
      this.backendService.getUserNode().then((val) => {
        this.nodes = val;
      });
    });
  }

}
