package com.gatelms.service;

import com.gatelms.error.Exceptions.EntityNotFoundException;
import com.gatelms.error.Exceptions.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class NoteStorageService {

    public record StoredFileInfo(
        String relativePath,
        String originalFilename,
        String contentType,
        long sizeBytes
    ) {}

    private final String storageDirConfig;
    private final Map<String, List<String>> allowedTypes = Map.of(
        "application/pdf", List.of("pdf"),
        "text/markdown", List.of("md", "markdown"),
        "text/plain", List.of("txt"),
        "image/png", List.of("png"),
        "image/jpeg", List.of("jpg", "jpeg")
    );
    private final long maxSizeBytes = 50L * 1024 * 1024; // 50 MB

    public NoteStorageService(@Value("${app.notes.storage-dir:./docker-data/notes}") String storageDirConfig) {
        this.storageDirConfig = storageDirConfig;
    }

    public Path getStorageRoot() {
        Path root = Paths.get(storageDirConfig).toAbsolutePath().normalize();
        if (!Files.exists(root)) {
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                throw new RuntimeException("Could not create notes storage dir", e);
            }
        }
        return root;
    }

    public StoredFileInfo storeFile(UUID noteId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("File is empty", Map.of("file", "Uploaded file cannot be empty"));
        }

        if (file.getSize() > maxSizeBytes) {
            throw new ValidationException("File too large", Map.of("file", "File size exceeds 50 MB limit"));
        }

        String rawContentType = file.getContentType() != null ? file.getContentType().toLowerCase().trim() : "application/octet-stream";
        String baseContentType = rawContentType.split(";")[0].trim();

        List<String> allowedExtensions = allowedTypes.get(baseContentType);
        if (allowedExtensions == null) {
            throw new ValidationException(
                "Invalid content type",
                Map.of("file", "Content type '" + baseContentType + "' is not allowed. Whitelist: PDF, Markdown, Plain Text, PNG, JPEG")
            );
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        int lastDot = originalFilename.lastIndexOf('.');
        String ext = lastDot != -1 ? originalFilename.substring(lastDot + 1).toLowerCase() : "";
        if (ext.isEmpty() || !allowedExtensions.contains(ext)) {
            throw new ValidationException(
                "File extension mismatch",
                Map.of("file", "Extension '." + ext + "' does not match declared content-type '" + baseContentType + "'")
            );
        }

        Path root = getStorageRoot();
        Path noteDir = root.resolve(noteId.toString()).normalize();
        if (!noteDir.startsWith(root)) {
            throw new ValidationException("Security violation", Map.of("file", "Invalid storage path"));
        }
        if (!Files.exists(noteDir)) {
            try {
                Files.createDirectories(noteDir);
            } catch (IOException e) {
                throw new RuntimeException("Could not create note dir", e);
            }
        }

        String generatedFilename = UUID.randomUUID() + "." + ext;
        Path targetPath = noteDir.resolve(generatedFilename).normalize();
        if (!targetPath.startsWith(root)) {
            throw new ValidationException("Security violation", Map.of("file", "Path traversal detected"));
        }

        try {
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store note file", e);
        }

        String relativePath = (noteId + "/" + generatedFilename).replace('\\', '/');
        return new StoredFileInfo(relativePath, originalFilename, baseContentType, file.getSize());
    }

    public Resource loadFileAsResource(String relativePath) {
        if (relativePath.contains("..") || relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            throw new ValidationException("Path traversal rejected", Map.of("file", "Path traversal rejected"));
        }

        Path root = getStorageRoot();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new ValidationException("Path traversal rejected", Map.of("file", "Path traversal rejected"));
        }

        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            throw new EntityNotFoundException("Note file not found on disk");
        }

        try {
            return new UrlResource(resolved.toUri());
        } catch (Exception e) {
            throw new EntityNotFoundException("Could not read file: " + e.getMessage());
        }
    }

    public void deleteNoteFiles(UUID noteId) {
        Path root = getStorageRoot();
        Path noteDir = root.resolve(noteId.toString()).normalize();
        if (noteDir.startsWith(root) && Files.exists(noteDir)) {
            deleteRecursively(noteDir.toFile());
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) deleteRecursively(child);
            }
        }
        file.delete();
    }
}
