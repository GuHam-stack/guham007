package com.changgou.controller;

import com.changgou.file.FastDFSFile;
import com.changgou.util.FastDFSClient;
import entity.Result;
import entity.StatusCode;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@RestController
@CrossOrigin
public class FileController {
    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public Result upload(@RequestParam(value = "file") MultipartFile file) throws IOException {
//       System.out.println(file.getOriginalFilename());
        //包装上传文件
        FastDFSFile dfsFile = new FastDFSFile(file.getOriginalFilename(), // 文件原来的名字
                file.getBytes(),                                           //文件字节数组
                StringUtils.getFilenameExtension(file.getOriginalFilename()) //获取文件后缀名
        );
        //调用fastdfs上传文件
        String[] upload = FastDFSClient.upload(dfsFile);
        //返回上传结果
//        String url = "http://192.168.211.132:8080/"+upload[0]+"/"+upload[1];
        String url = FastDFSClient.getTrackerUrl()+upload[0]+"/"+upload[1];
        return new Result(true, StatusCode.OK, "上传文件成功",url);
    }
}
