package com.changgou.goods.feign;

import com.changgou.goods.pojo.Sku;
import entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient("goods")
@RequestMapping("sku")
public interface SkuFeign {
    /***
     * 根据审核状态查询Sku
     * @param status
     * @return
     */
    @GetMapping("/status/{status}")
    Result<List<Sku>> findByStatus(@PathVariable("status") String status);

    /**
     * 根据id查询sku信息
     */
    @GetMapping("/{id}")
    Result<Sku> findById(@PathVariable("id") Long id);

    /***
     * 库存递减
     * @param username 注意，由于我们此方法由order调起，所以此处的参数可以从service-order传入
     * @return
     */
    @PostMapping(value = "/decr/count/{username}")
    public Result decrCount(@PathVariable("username") String username);


}
