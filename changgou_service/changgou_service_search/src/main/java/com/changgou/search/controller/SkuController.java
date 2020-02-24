package com.changgou.search.controller;

import com.changgou.search.service.SkuService;
import entity.Result;
import entity.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/search")
public class SkuController {

    @Autowired
    private SkuService skuService;

    /**
     * 导入数据
     * @return
     */
    @GetMapping("import")
    public Result importData(){
        skuService.importSku();
        return new Result(true, StatusCode.OK, "导入数据成功!");
    }

    /**
     * 搜索商品
     * @return
     */
    @GetMapping
    public Map search(@RequestParam(required = false) Map<String,String> searchMap){
        return  skuService.search(searchMap);
    }

}
