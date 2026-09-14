package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.user.api.ImageAssetVO;
import com.boxai.user.application.PublicImageAssetService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/assets")
public class AdminAssetController {

    private final PublicImageAssetService publicImageAssetService;

    public AdminAssetController(PublicImageAssetService publicImageAssetService) {
        this.publicImageAssetService = publicImageAssetService;
    }

    @PostMapping("/images")
    public Result<ImageAssetVO> upload(@RequestPart("file") MultipartFile file) {
        return Result.success(publicImageAssetService.upload(file));
    }
}
