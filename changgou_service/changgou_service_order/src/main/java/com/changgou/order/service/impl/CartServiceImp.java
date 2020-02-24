package com.changgou.order.service.impl;

import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.feign.SpuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.goods.pojo.Spu;
import com.changgou.order.pojo.OrderItem;
import com.changgou.order.service.CartService;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("ALL")
@Service
public class CartServiceImp implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SkuFeign skuFeign;

    @Autowired
    private SpuFeign spuFeign;

    @Override
    public void add(Integer num, Long skuId, String username) {
        if (num <= 0){
            redisTemplate.boundHashOps("Cart_"+username).delete(skuId);
            return;
        }

        // 查找商品信息
        Result<Sku> result = skuFeign.findById(skuId);

        if (result.isFlag() && result.getData()!=null){
            // 获得sku
            Sku sku = result.getData();
            // 获得spu
            Spu spu = spuFeign.findById(sku.getSpuId()).getData();

            // 信息封装
            OrderItem orderItem = new OrderItem();
            orderItem.setCategoryId1(spu.getCategory1Id());
            orderItem.setCategoryId2(spu.getCategory2Id());
            orderItem.setCategoryId3(spu.getCategory3Id());
            orderItem.setSpuId(spu.getId());
            orderItem.setSkuId(sku.getId());
            orderItem.setName(sku.getName());
            orderItem.setPrice(sku.getPrice());
            orderItem.setNum(num);
            orderItem.setMoney(orderItem.getPrice()*num);
            orderItem.setImage(sku.getImage());
            orderItem.setWeight(sku.getWeight()*num);

            // 存入redis
            redisTemplate.boundHashOps("Cart_"+username).put(skuId, orderItem);

        }
    }

    @Override
    public List<OrderItem> list(String username) {

        List list = redisTemplate.boundHashOps("Cart_" + username).values();
        return list;
    }
}
