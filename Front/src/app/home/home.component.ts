import { Component, OnInit } from '@angular/core';
import { FileInfo } from '../models/fileInfo.model';
import { FileService } from '../services/fileService.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {

  protected files: FileInfo[] = [
    {name: 'INTRODUCTION - Robin DICKINSON.mp4', size: 72, date: '17/11/2024 - 11:00'},
    {name: 'INTRODUCTION - Robin DICKINSON.mp4', size: 72, date: '17/11/2024 - 11:00'}
  ];

  constructor(private fileService: FileService) {}

  ngOnInit(): void {

    console.log('files :', this.files);
    this.fileService.getFiles().subscribe({
      next: (data) => {
        this.files = data;
        console.log('data :', data);
      },
      error: (err) => {
        console.error('Erreur lors de la recuperation des fichiers :', err)
      }
    });
  }
}
