package com.example.kitechin.service;

import com.example.kitechin.exception.ApiException;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileService {

    private static final Pattern SAFE_NAME = Pattern.compile(
            "^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.(jpg|jpeg|png|webp|pdf)$");
    private static final Set<String> IMAGES = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> IDENTITY = Set.of("jpg", "jpeg", "png", "pdf");
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "pdf", "application/pdf"
    );

    private final GridFsTemplate gridFsTemplate;
    private final Path uploadDir;
    private final long maxBytes;

    public FileService(GridFsTemplate gridFsTemplate,
                       @Value("${app.upload.dir:uploads}") String uploadDir,
                       @Value("${app.upload.max-bytes:5242880}") long maxBytes) {
        this.gridFsTemplate = gridFsTemplate;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    public String storeImage(MultipartFile file) {
        return store(file, IMAGES);
    }

    public String storeIdentity(MultipartFile file) {
        return store(file, IDENTITY);
    }

    public StoredFile load(String filename) {
        String safeName = requireSafeName(filename);
        GridFSFile gridFile = gridFsTemplate.findOne(Query.query(Criteria.where("filename").is(safeName)));
        if (gridFile != null) {
            try {
                GridFsResource resource = gridFsTemplate.getResource(gridFile);
                try (InputStream input = resource.getInputStream()) {
                    return new StoredFile(input.readAllBytes(), contentTypeOf(gridFile, safeName));
                }
            } catch (IOException exception) {
                throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Could not read the file");
            }
        }
        return readLocal(safeName);
    }

    public StoredFile loadPublicImage(String filename) {
        StoredFile file = load(filename);
        if (file.contentType() == null || !file.contentType().startsWith("image/")) {
            throw ApiException.notFound("Image not found");
        }
        return file;
    }

    private String store(MultipartFile file, Set<String> allowedExtensions) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("File is empty");
        }
        if (file.getSize() > maxBytes) {
            throw ApiException.badRequest("File is too large. Maximum size is 5 MB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!allowedExtensions.contains(extension)) {
            throw ApiException.badRequest("This file type is not allowed");
        }
        String declared = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!declared.isBlank() && !contentTypeMatches(extension, declared)) {
            throw ApiException.badRequest("File type does not match its extension");
        }

        String filename = UUID.randomUUID() + "." + extension;
        try (InputStream input = file.getInputStream()) {
            gridFsTemplate.store(input, filename, CONTENT_TYPES.get(extension));
        } catch (IOException exception) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the file");
        }
        return filename;
    }

    private StoredFile readLocal(String filename) {
        Path path = uploadDir.resolve(filename).normalize();
        if (!path.startsWith(uploadDir) || !Files.exists(path)) {
            throw ApiException.notFound("File not found");
        }
        try {
            return new StoredFile(Files.readAllBytes(path), contentTypeFromName(filename));
        } catch (IOException exception) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Could not read the file");
        }
    }

    private String requireSafeName(String filename) {
        if (filename == null || !SAFE_NAME.matcher(filename.toLowerCase(Locale.ROOT)).matches()) {
            throw ApiException.badRequest("Invalid file name");
        }
        return filename.toLowerCase(Locale.ROOT);
    }

    private String extensionOf(String originalName) {
        if (originalName == null || !originalName.contains(".")) {
            throw ApiException.badRequest("File must have an extension");
        }
        String extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (extension.isBlank() || extension.contains("/") || extension.contains("\\")) {
            throw ApiException.badRequest("File must have an extension");
        }
        return extension;
    }

    private boolean contentTypeMatches(String extension, String declared) {
        if (("jpg".equals(extension) || "jpeg".equals(extension))
                && (declared.equals("image/jpeg") || declared.equals("image/jpg") || declared.equals("image/pjpeg"))) {
            return true;
        }
        return CONTENT_TYPES.get(extension).equals(declared);
    }

    private String contentTypeOf(GridFSFile gridFile, String filename) {
        Document metadata = gridFile.getMetadata();
        if (metadata != null) {
            Object stored = metadata.get("_contentType");
            if (stored == null) {
                stored = metadata.get("contentType");
            }
            if (stored != null && !stored.toString().isBlank()) {
                return stored.toString();
            }
        }
        return contentTypeFromName(filename);
    }

    private String contentTypeFromName(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }
}
