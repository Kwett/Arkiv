package com.kwett.arkiv.controller;

import com.kwett.arkiv.model.FileInfo;
import com.kwett.arkiv.repository.FileRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileRepository fileRepository;

    public FileController(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @GetMapping
    public List<FileInfo> getFiles() {
        return fileRepository.findAll();
    }
}
