package com.smartproperty.platform.file.controller;

import com.smartproperty.platform.file.service.FloorPlanService;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/properties/{propertyUnitId}/floor-plan")
public class FloorPlanController {
    private final FloorPlanService service;

    public FloorPlanController(FloorPlanService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FloorPlanService.FileView upload(@PathVariable Long propertyUnitId,
                                            @RequestPart("file") MultipartFile file,
                                            @AuthenticationPrincipal Jwt jwt) {
        return service.upload(propertyUnitId, file, Long.valueOf(jwt.getSubject()));
    }

    @GetMapping
    public ResponseEntity<InputStreamResource> download(@PathVariable Long propertyUnitId) {
        FloorPlanService.DownloadPayload payload = service.download(propertyUnitId);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(payload.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .contentLength(payload.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(payload.stream()));
    }
}
