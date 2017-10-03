import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable'
import 'rxjs/add/operator/map'; 
import { Socket } from 'ng-socket-io';

@Injectable()
export class SocketService {

    constructor(private socket: Socket) {}

    getMessage(subject:string) {
        return this.socket        
            .fromEvent<any>(subject)
            .map(data => data.msg);
    }

    sendMessage(subject:string, msg: string) {
        this.socket
            .emit(subject, msg);
    }
}