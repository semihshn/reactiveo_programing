package com.reactive.crud.file;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
public class FileService {

    private static final Logger LOG = Logger.getLogger(FileService.class);
    private static final String EXPORT_DIR = "exports";

    @Inject
    Vertx vertx;

    public Uni<String> readFile(String filePath) {
        LOG.debugf("Reading file: %s", filePath);
        return vertx.fileSystem().readFile(filePath)
                .onItem().transform(Buffer::toString)
                .invoke(() -> LOG.infof("Successfully read file: %s", filePath))
                .onFailure().invoke(failure ->
                        LOG.errorf("Failed to read file %s: %s", filePath, failure.getMessage())
                );
    }

    public Uni<Void> writeFile(String filePath, String content) {
        LOG.debugf("Writing to file: %s", filePath);
        return vertx.fileSystem().writeFile(filePath, Buffer.buffer(content))
                .invoke(() -> LOG.infof("Successfully wrote to file: %s", filePath))
                .onFailure().invoke(failure ->
                        LOG.errorf("Failed to write file %s: %s", filePath, failure.getMessage())
                );
    }

    public Uni<Void> createDirectory(String dirPath) {
        LOG.debugf("Creating directory: %s", dirPath);
        return vertx.fileSystem().mkdirs(dirPath)
                .invoke(() -> LOG.infof("Successfully created directory: %s", dirPath))
                .onFailure().invoke(failure ->
                        LOG.errorf("Failed to create directory %s: %s", dirPath, failure.getMessage())
                );
    }

    public Uni<Boolean> fileExists(String filePath) {
        return vertx.fileSystem().exists(filePath);
    }

    public Uni<Void> deleteFile(String filePath) {
        LOG.debugf("Deleting file: %s", filePath);
        return vertx.fileSystem().delete(filePath)
                .invoke(() -> LOG.infof("Successfully deleted file: %s", filePath))
                .onFailure().invoke(failure ->
                        LOG.errorf("Failed to delete file %s: %s", filePath, failure.getMessage())
                );
    }

    public Uni<String> exportProductsToFile(String jsonContent) {
        String fileName = "products_export_" + System.currentTimeMillis() + ".json";
        Path exportPath = Paths.get(EXPORT_DIR, fileName);

        return createDirectory(EXPORT_DIR)
                .replaceWith(exportPath.toString())
                .chain(path -> writeFile(path, jsonContent)
                        .replaceWith(path)
                )
                .invoke(path -> LOG.infof("Products exported to: %s", path));
    }

    public Uni<String> readProductsExport(String fileName) {
        Path exportPath = Paths.get(EXPORT_DIR, fileName);
        return readFile(exportPath.toString());
    }
}
