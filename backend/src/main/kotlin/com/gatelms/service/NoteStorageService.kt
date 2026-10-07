package com.gatelms.service

import com.gatelms.error.EntityNotFoundException
import com.gatelms.error.ValidationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class NoteStorageService(
    @Value("\${app.notes.storage-dir:./docker-data/notes}")
    private val storageDirConfig: String
) {
    private val allowedTypes = mapOf(
        "application/pdf" to listOf("pdf"),
        "text/markdown" to listOf("md", "markdown"),
        "text/plain" to listOf("txt"),
        "image/png" to listOf("png"),
        "image/jpeg" to listOf("jpg", "jpeg")
    )

    private val maxSizeBytes = 50L * 1024 * 1024 // 50 MB

    fun getStorageRoot(): Path {
        val root = Paths.get(storageDirConfig).toAbsolutePath().normalize()
        if (!Files.exists(root)) {
            Files.createDirectories(root)
        }
        return root
    }

    data class StoredFileInfo(
        val relativePath: String,
        val originalFilename: String,
        val contentType: String,
        val sizeBytes: Long
    )

    fun storeFile(noteId: UUID, file: MultipartFile): StoredFileInfo {
        if (file.isEmpty) {
            throw ValidationException("File is empty", mapOf("file" to "Uploaded file cannot be empty"))
        }

        if (file.size > maxSizeBytes) {
            throw ValidationException("File too large", mapOf("file" to "File size exceeds 50 MB limit"))
        }

        val rawContentType = file.contentType?.lowercase()?.trim() ?: "application/octet-stream"
        val baseContentType = rawContentType.split(";").first().trim()

        val allowedExtensions = allowedTypes[baseContentType]
            ?: throw ValidationException(
                "Invalid content type",
                mapOf("file" to "Content type '$baseContentType' is not allowed. Whitelist: PDF, Markdown, Plain Text, PNG, JPEG")
            )

        val originalFilename = file.originalFilename ?: "file"
        val ext = originalFilename.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty() || !allowedExtensions.contains(ext)) {
            throw ValidationException(
                "File extension mismatch",
                mapOf("file" to "Extension '.$ext' does not match declared content-type '$baseContentType'")
            )
        }

        val root = getStorageRoot()
        val noteDir = root.resolve(noteId.toString()).normalize()
        if (!noteDir.startsWith(root)) {
            throw ValidationException("Security violation", mapOf("file" to "Invalid storage path"))
        }
        if (!Files.exists(noteDir)) {
            Files.createDirectories(noteDir)
        }

        val generatedFilename = "${UUID.randomUUID()}.$ext"
        val targetPath = noteDir.resolve(generatedFilename).normalize()
        if (!targetPath.startsWith(root)) {
            throw ValidationException("Security violation", mapOf("file" to "Path traversal detected"))
        }

        file.inputStream.use { inputStream ->
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }

        val relativePath = "${noteId}/$generatedFilename".replace('\\', '/')
        return StoredFileInfo(
            relativePath = relativePath,
            originalFilename = originalFilename,
            contentType = baseContentType,
            sizeBytes = file.size
        )
    }

    fun loadFileAsResource(relativePath: String): Resource {
        if (relativePath.contains("..") || relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            throw ValidationException("Path traversal rejected", mapOf("file" to "Path traversal rejected"))
        }

        val root = getStorageRoot()
        val resolved = root.resolve(relativePath).normalize()
        if (!resolved.startsWith(root)) {
            throw ValidationException("Path traversal rejected", mapOf("file" to "Path traversal rejected"))
        }

        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            throw EntityNotFoundException("Note file not found on disk")
        }

        return UrlResource(resolved.toUri())
    }

    fun deleteNoteFiles(noteId: UUID) {
        val root = getStorageRoot()
        val noteDir = root.resolve(noteId.toString()).normalize()
        if (noteDir.startsWith(root) && Files.exists(noteDir)) {
            noteDir.toFile().deleteRecursively()
        }
    }
}
