package com.changgou.content.feign;

import com.changgou.content.pojo.Content;
import entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient("content")
@RequestMapping("/content")
public interface ContentFeign {
    /***
     * 根据广告的分类id获取广告信息集合
     * @param id
     * @return
     */
    @GetMapping("/list/category/{id}")
    Result<List<Content>> findByCategory(@PathVariable("id") Long id);

}
