package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Api(tags ="通用接口")
@Slf4j
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    @Value("${my.upload.local-path}")
    private String localPath;

    @PostMapping("/upload")
    @ApiOperation(value="文件上传")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传,file:{}", file);
        try {
            //构建新文件名防重名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + extension;
            String filePath = aliOssUtil.upload(file.getBytes(), newFileName);
            // 2. 本地也存一份
            File localDir = new File(localPath);
            if (!localDir.exists()) localDir.mkdirs();
            file.transferTo(new File(localDir, newFileName));

            // 3. 返回本地 URL 给前端
            return Result.success("http://localhost:8080/upload/" + newFileName);
        } catch (IOException e) {
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
    }

}
