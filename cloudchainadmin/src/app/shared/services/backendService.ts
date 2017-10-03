import { Injectable } from '@angular/core';
import { HttpClient, HttpClientModule, HttpRequest, HttpEventType, HttpResponse, HttpHeaders } from "@angular/common/http";
import 'rxjs/add/operator/toPromise';

import { User } from '../../models/user'
import { Register } from '../../models/register'
import { Md5 } from 'ts-md5/dist/md5';

@Injectable()
export class BackendService {
    serverUrl: string = "http://localhost:3500";
    uploadServerUrl: string = "http://localhost:3500";
    headers = new Headers({ 'Content-Type': 'application/json', 'charset': 'utf-8' });

    constructor(private http: HttpClient) { }

    private handleError(error: any): Promise<any> {
        console.error('An error occurred', error);
        alert('An error occurred : ' + error);
        return Promise.reject(error.message || error);
    }

    isLoggedIn() {
        return sessionStorage.getItem('isLoggedin');
    }

    getCurrentUser() {
        return JSON.parse(sessionStorage.getItem('loggedUser'));
    }

    checkLogin(userName: string, password: string) {
        return this.http.get(this.serverUrl + '/cloudchain/getuser/' + userName + '/' + Md5.hashStr(password).toString())
            .toPromise()
            .then((val) => {
                delete val["_id"];
                return val;
            });
    }

    checkEmail(email: string) {
        return this.http.get(this.serverUrl + '/cloudchain/userexists/' + email)
            .toPromise()
            .then((val:any) => {                
                return val.ok;
            });
    }

    createUser(register: Register) {
        let user: User = new User("", register.email, Md5.hashStr(register.password).toString());
        return this.http.post(this.serverUrl + '/cloudchain/createuser', user)
            .toPromise()
            .then(() => user)
            .catch(this.handleError);
    }

    getUserNode() {
        if (sessionStorage.getItem("loggedUser") != null) {
            let loggedUser: any = JSON.parse(sessionStorage.getItem("loggedUser"));            
            console.log(loggedUser);
            return this.http.post(this.serverUrl + '/cloudchain/getnode', loggedUser)
                .toPromise()
                .then((val) => {
                    if (val.toString() != "") {
                        sessionStorage.setItem("nodeId", val[0]._id.toString());
                        return val[0].nodes;
                    } else {
                        return [
                            {
                                id: 1,
                                name: '/',
                                type: "folder"
                            }
                        ];
                    }
                });
        } else {
            return null;
        }
    }

    setUserNode(nodes: any[]) {
        let loggedUser: any = JSON.parse(sessionStorage.getItem("loggedUser"));
        //let nodesRecord = { "email": loggedUser.email, "nodes": nodes };
        //let nodeId: any = sessionStorage.getItem("nodeId");
        let params : any;
        params.email = loggedUser.email;
        params.password = loggedUser.password;
        params.nodes = nodes;        
        this.http.post(this.serverUrl + '/cloudchain/setnodes', params)
        .toPromise()
        .catch(this.handleError);
    }

    fileUpload(file, nodeid) {
        let loggedUser: any = JSON.parse(sessionStorage.getItem("loggedUser"));
        let formData = new FormData();
        formData.append("file", file);
        formData.append("email",loggedUser.email);
        formData.append("password",loggedUser.password);
        formData.append("nodeid",nodeid);
        const req = new HttpRequest('POST', this.uploadServerUrl + '/upload', formData, {
            reportProgress: true,
        });
        return this.http.request(req);
    }

    createFile(fileInfo) {
     //   return this.http.post(this.serverUrl + '/cloudchain/files', fileInfo)
       //     .toPromise()
         //   .catch(this.handleError);
         return true;
    }

    getUserFilesCount(nodeid) {
        if (sessionStorage.getItem("loggedUser") != null) {
            let loggedUser: any = JSON.parse(sessionStorage.getItem("loggedUser"));
            return this.http.post(this.serverUrl + '/cloudchain/getfilescount/'+nodeid, loggedUser)
                .toPromise()
                .then((val) => {
                    return val;
                });
        } else {
            return null;
        }
    }
    getUserFiles(nodeid, skip, limit) {
        if (sessionStorage.getItem("loggedUser") != null) {
            let loggedUser: any = JSON.parse(sessionStorage.getItem("loggedUser"));
            return this.http.post(this.serverUrl + '/cloudchain/getfiles/'+nodeid+'/'+skip+'/'+limit, loggedUser)
                .toPromise()
                .then((val) => {
                    console.log(val);
                    return val;
                });
        } else {
            return null;
        }
    }

    downloadUserFile(file){

    }
}