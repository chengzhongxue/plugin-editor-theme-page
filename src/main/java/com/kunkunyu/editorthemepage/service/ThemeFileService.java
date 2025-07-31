package com.kunkunyu.editorthemepage.service;


import java.nio.file.Path;
import com.kunkunyu.editorthemepage.model.ThemeFile;
import com.kunkunyu.editorthemepage.model.UploadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ThemeFileService {

    Mono<Path> upload(UploadContext uploadContext);

    Flux<ThemeFile> listFiles(String themeName, String directoryPath);

    Mono<Boolean> deleteFile(String themeName, String path);

    Mono<String> readFileContent(String themeName, String path);

    Mono<Void> writeContent(String themeName, String path, String content);

    Mono<Path> createFile(String themeName, String path, boolean dir);
}
