package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.user.api.ImageAssetVO;
import com.boxai.user.application.PublicImageAssetService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1")
public class PublicAssetController {

    private final PublicImageAssetService publicImageAssetService;

    public PublicAssetController(PublicImageAssetService publicImageAssetService) {
        this.publicImageAssetService = publicImageAssetService;
    }

    @PostMapping("/assets/images")
    public Result<ImageAssetVO> upload(@RequestPart("file") MultipartFile file) {
        return Result.success(publicImageAssetService.upload(file));
    }

    @GetMapping("/public-assets/{fileName}")
    public ResponseEntity<InputStreamResource> get(@PathVariable String fileName) {
        PublicImageAssetService.LoadedImage image = publicImageAssetService.load(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(new InputStreamResource(image.stream()));
    }
}
