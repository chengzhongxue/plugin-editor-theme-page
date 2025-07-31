package com.kunkunyu.editorthemepage.service.impl;

import static com.kunkunyu.editorthemepage.utils.FileUtils.checkDirectoryTraversal;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import com.kunkunyu.editorthemepage.model.ThemeFile;
import com.kunkunyu.editorthemepage.model.UploadContext;
import com.kunkunyu.editorthemepage.service.PageFileManager;
import com.kunkunyu.editorthemepage.service.ThemeFileService;
import com.kunkunyu.editorthemepage.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.BackupRootGetter;

@Component
@RequiredArgsConstructor
public class ThemeFileServiceImpl implements ThemeFileService {

    private final ReactiveExtensionClient client;
    private final BackupRootGetter backupRootGetter;
    private final PageFileManager pageFileManager;


    private static String getType(File file) {
        String name = file.getName();
        if (file.isDirectory()) {
            return "directory";
        } else {
            int lastIndexOf = name.lastIndexOf(".");
            if (lastIndexOf == -1) {
                // empty extension
                return "";
            }
            return name.substring(lastIndexOf + 1);
        }
    }

    private static Flux<ThemeFile> doListFiles(Path pathToList) {
        return Flux.<ThemeFile>create(sink -> {
                File directory = pathToList.toFile();
                if (!directory.exists()) {
                    sink.complete();
                    return;
                }
                if (!directory.isDirectory()) {
                    sink.error(new ServerWebInputException("The path is not a directory."));
                    return;
                }
                File[] files = directory.listFiles();
                if (files == null) {
                    sink.complete();
                    return;
                }
                for (File file : files) {
                    if (sink.isCancelled()) {
                        break;
                    }
                    var projectFile = new ThemeFile()
                        .setPath(file.getAbsolutePath())
                        .setDirectory(file.isDirectory())
                        .setName(file.getName())
                        .setSize(file.length())
                        .setType(getType(file))
                        .setCanRead(file.canRead())
                        .setCanWrite(file.canWrite())
                        .setLastModifiedTime(Instant.ofEpochMilli(file.lastModified()));
                    sink.next(projectFile);
                }
                sink.complete();
            })
            .subscribeOn(Schedulers.boundedElastic());
    }

    static Path concatPath(Path root, String... segments) {
        if (segments.length == 0) {
            return root;
        }
        var path = Paths.get(root.toString(), segments);
        checkDirectoryTraversal(root, path);
        return path;
    }

    @NonNull
    static String[] pathSegments(String dir) {
        if (StringUtils.isBlank(dir)) {
            return new String[0];
        }
        return StringUtils.split(dir, "/");
    }

    @Override
    public Mono<Path> upload(UploadContext uploadContext) {
        Path storePath = extractThemeFilePath(uploadContext.getName(), uploadContext.getDir());
        return writeToFile(storePath, uploadContext);
    }

    @Override
    public Flux<ThemeFile> listFiles(String themeName, String directoryPath) {
        Path filePath = extractThemeFilePath(themeName, directoryPath);
        return doListFiles(filePath);
    }

    @Override
    public Mono<Boolean> deleteFile(String themeName, String path) {
        Path filePath = extractThemeFilePath(themeName, path);
        return Mono.fromCallable(
                () -> FileSystemUtils.deleteRecursively(filePath))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<String> readFileContent(String themeName, String path) {
        Path filePath = extractThemeFilePath(themeName, path);
        return pageFileManager.readString(filePath);
    }

    @Override
    public Mono<Void> writeContent(String themeName, String path, String content) {
        Path filePath = extractThemeFilePath(themeName, path);
        return pageFileManager.writeString(filePath, content);
    }

    @Override
    public Mono<Path> createFile(String themeName, String path, boolean dir) {
        Path filePath = extractThemeFilePath(themeName, path);
        return pageFileManager.createFile(filePath, dir).thenReturn(filePath);
    }

    private Mono<String> getActivationTheme() {
        return Mono.just("theme-earth");
    }


    Path extractThemeFilePath(String themePath, String extractPath) {
        var segments = pathSegments(extractPath);
        return concatPath(determineProjectPath(themePath), segments);
    }

    private Path determineProjectPath(String projectDir) {
        Assert.notNull(projectDir, "The projectDir must not be null.");
        return concatPath(getStaticRootPath(), pathSegments(projectDir));
    }

    private Mono<Path> writeToFile(Path storePath, UploadContext uploadContext) {
        return Mono.fromCallable(() -> {
                try {
                    Files.createDirectories(storePath);
                    return storePath;
                } catch (IOException e) {
                    throw Exceptions.propagate(e);
                }
            })
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(rootPath -> {
                if (uploadContext.isUnzip()) {
                    return FileUtils.unzipTo(uploadContext.getFilePart().content(), rootPath)
                        .thenReturn(rootPath);
                }
                var filePath = rootPath.resolve(uploadContext.getFilePart().filename());
                return writeToFile(uploadContext.getFilePart().content(), filePath);
            });
    }

    private Mono<Path> writeToFile(Flux<DataBuffer> content, Path targetPath) {
        return Mono.defer(() -> DataBufferUtils.write(content, targetPath, CREATE))
            .thenReturn(targetPath);
    }

    private Path getStaticRootPath() {
        return backupRootGetter.get().getParent().resolve("themes");
    }
}
