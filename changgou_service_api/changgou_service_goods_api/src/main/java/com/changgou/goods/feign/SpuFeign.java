package com.changgou.goods.feign;

import com.changgou.goods.pojo.Spu;
import entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(name = "goods")
@RequestMapping("/spu")
public interface SpuFeign {
    /***
     * 根据id查找信息
     */
    @GetMapping("/{id}")
    Result<Spu> findById(@PathVariable("id") Long id);
}
