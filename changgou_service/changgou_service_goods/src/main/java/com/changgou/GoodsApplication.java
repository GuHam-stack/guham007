package com.changgou;

import entity.IdWorker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@EnableDiscoveryClient
//开启通用mapper DAO包扫描
@MapperScan(basePackages = "com.changgou.goods.dao")
public class GoodsApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoodsApplication.class,args);
    }

    /**
     * 注册IdWorker实例对象
     * @return
     */
    @Bean
    public IdWorker getIdWorker(){
        //IdWorker(工作机器id,数据中心id)
        return new IdWorker(0, 0);
    }

}
