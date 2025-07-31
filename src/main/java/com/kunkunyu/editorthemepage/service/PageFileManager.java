package com.kunkunyu.editorthemepage.service;

import java.nio.file.Path;
import reactor.core.publisher.Mono;

public interface PageFileManager {

    Mono<String> readString(Path path);

    Mono<Void> writeString(Path path, String content);

    Mono<Void> createFile(Path filePath, boolean dir);

    Mono<Void> move(Path source, Path target);
}
