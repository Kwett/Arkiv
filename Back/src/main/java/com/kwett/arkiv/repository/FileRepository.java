package com.kwett.arkiv.repository;

import com.kwett.arkiv.model.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileInfo, Integer> {
}
