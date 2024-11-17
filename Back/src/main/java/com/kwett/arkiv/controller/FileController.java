package com.kwett.arkiv.controller;

import com.kwett.arkiv.model.FileInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    @GetMapping
    public List<FileInfo> getFiles() {
        return Arrays.asList(
                new FileInfo("INTRODUCTION - Robin DICKINSON.mp4", "72", "17/11/2024 - 11:00"),
                new FileInfo("VIDEO TUTORIEL.mp4", "55", "16/11/2024 - 14:30"),
                new FileInfo("DOCUMENTAIRE.mp4", "100", "15/11/2024 - 18:45")
        );
    }
}
