package com.changgou.order.service;

import com.changgou.order.pojo.OrderItem;

import java.util.List;

public interface CartService {

    /***
     * 添加购物车
     * @param num  商品的数量
     * @param skuId 商品的id
     * @param username 用户的名字
     */
    void add(Integer num,Long skuId,String username);

    /***
     * 查询用户的购物车数据
     * @param username
     * @return
     */
    List<OrderItem> list(String username);

}
