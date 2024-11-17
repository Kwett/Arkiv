import { HttpClient } from "@angular/common/http";
import { FileInfo } from "../models/fileInfo.model";
import { Observable } from "rxjs";

import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})

export class FileService {

    private url: string = 'http://localhost:8080/files';
    constructor(private http: HttpClient) {}
    
    public getFiles(): Observable<FileInfo[]> {
        return this.http.get<FileInfo[]>(this.url);
      }
}