package org.example.springdemo.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    // 文件保存目录：项目根目录下的 uploads
    private final Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

    public FileUploadController() throws IOException {
        Files.createDirectories(uploadDir);
    }

    /**
     * 单文件上传
     * 请求方式：POST
     * 请求地址：/api/files/upload
     * 表单参数名：file
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "文件为空"));
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String ext = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 使用 UUID 重新生成文件名，避免重名和路径穿越
            String storedFilename = UUID.randomUUID() + ext;
            Path target = uploadDir.resolve(storedFilename).normalize();

            // 防止路径穿越
            if (!target.getParent().equals(uploadDir)) {
                return ResponseEntity.badRequest().body(Map.of("message", "非法文件名"));
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "上传成功");
            result.put("originalFilename", originalFilename);
            result.put("storedFilename", storedFilename);
            result.put("size", file.getSize());
            result.put("url", "/api/files/download/" + storedFilename);

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "上传失败: " + e.getMessage()));
        }
    }

    /**
     * 文件下载
     * 请求方式：GET
     * 请求地址：/api/files/download/{filename}
     */
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        try {
            Path filePath = uploadDir.resolve(filename).normalize();

            // 防止路径穿越
            if (!filePath.getParent().equals(uploadDir)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}