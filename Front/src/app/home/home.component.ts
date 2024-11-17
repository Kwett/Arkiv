import { Component } from '@angular/core';
import { FileInfo } from '../models/fileInfo.model';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {

  protected files: FileInfo[] = [
    {name: 'INTRODUCTION - Robin DICKINSON.mp4', size: 72, date: '17/11/2024 - 11:00'},
    {name: 'INTRODUCTION - Robin DICKINSON.mp4', size: 72, date: '17/11/2024 - 11:00'}
  ];
}
