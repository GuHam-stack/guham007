package com.changgou.canal.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.changgou.content.feign.ContentFeign;
import com.changgou.content.pojo.Content;
import com.xpand.starter.canal.annotation.*;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

/**
 * 实现数据库变更监听
 * @description com.changgou.canal.listener
 */
@CanalEventListener
public class CanalDataEventListener {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private ContentFeign contentFeign;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
//    /**
//     * 新增监听
//     * @InsertListenPoint：新增监听
//     * CanalEntry.EventType:变更操作类型
//     * CanalEntry.RowData：此次变更的数据
//     */
//    @InsertListenPoint
//    public void onEventInsert(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
//        //rowData.getBeforeColumnsList():数据变更前的内容
//        //rowData.getAfterColumnsList()：数据变更后的内容
//        System.out.println("---------新增监听--------");
//        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
//            System.out.println(column.getName() + ":" + column.getValue());
//        }
//    }
//    /**
//     * 修改监听
//     * @UpdateListenPoint：更新监听
//     * CanalEntry.EventType:变更操作类型
//     * CanalEntry.RowData：此次变更的数据
//     */
//    @UpdateListenPoint
//    public void onEventUpdate(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
//        //rowData.getBeforeColumnsList():数据变更前的内容
//        //rowData.getAfterColumnsList()：数据变更后的内容
//        System.out.println("---------更新监听--------");
//        int i = 0;
//        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
//            //获取修改前数据
//            CanalEntry.Column beforeColumns = rowData.getBeforeColumns(i);
//            //如果修改了字段
//            if(!beforeColumns.getValue().equals(column.getValue())) {
//                System.out.print("修改了字段:" + column.getName() + "   ");
//                System.out.println(beforeColumns.getValue() + "-->" + column.getValue());
//            }
//            i++;
//        }
//    }
//    /**
//     * 删除监听
//     * @DeleteListenPoint：删除监听
//     * CanalEntry.EventType:变更操作类型
//     * CanalEntry.RowData：此次变更的数据
//     */
//    @DeleteListenPoint
//    public void onEventDelete(CanalEntry.EventType eventType, CanalEntry.RowData rowData){
//        //rowData.getBeforeColumnsList():数据变更前的内容
//        //rowData.getAfterColumnsList()：数据变更后的内容
//        System.out.println("---------删除监听--------");
//        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
//            System.out.println(column.getName() + ":" + column.getValue());
//        }
//    }
//
//    /**
//     * 自定义监听
//     * @ListenPoint：自定义监听
//     * destination：必须使用canal.properties配置文件中canal.destinations属性的名字
//     * schema：监听的数据库
//     * table：监听的表
//     * eventType：监听的操作类型
//     */
//    @ListenPoint(destination = "example",
//            schema = "changgou_content",
//            table = "tb_content",
//            eventType = CanalEntry.EventType.DELETE)
//    public void onEventCustomUpdate(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
//        System.out.println("---------自定义监听--------");
//        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
//            System.out.println(column.getName() + ":" + column.getValue());
//        }
//    }

    /****
     * 广告内容监听
     * 自定义监听
     * destination: 必须使用canal.properties配置文件中的canal.destination属性的名字
     * schema : 数据库的名字
     * table : 数据库表的名字
     * CanalEntry.EventType 需要监听的操作 可以多个
     * @param eventType 监听的操作类型
     * @param rowData   监听得到的数据
     */
    @ListenPoint(destination = "example",
            schema = "changgou_content",
            table = "tb_content",
            eventType = {CanalEntry.EventType.INSERT,CanalEntry.EventType.UPDATE,CanalEntry.EventType.DELETE})
    public void onEventContent(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
        System.out.println("-----广告开始监听------");
        // 定义分类id
        String categoryId = "";
        /**
         * 判断监听到的操作类型CanalEntry.EventType
         * 根据得到的数据获得id
         */
        if (eventType == CanalEntry.EventType.INSERT){
            // 是增加类型
            categoryId = rowData.getAfterColumns(1).getValue();
        }else if (eventType == CanalEntry.EventType.UPDATE){
            // 是修改类型
            categoryId = rowData.getAfterColumns(1).getValue();
            // 判断是否修改了分类id
            String beforeId = rowData.getBeforeColumns(1).getValue();
            if (! categoryId.equals(beforeId)){
                // 不相同 调用feign获得数据并更新redis缓存
                // 注意需要修改的是通过之前id查找的数据
                Result<List<Content>> byCategory = contentFeign.findByCategory(Long.parseLong(beforeId));
                if (byCategory.getData() != null && byCategory.getData().size() > 0){
                    stringRedisTemplate.boundValueOps("content_"+beforeId).set(JSON.toJSONString(byCategory.getData()));
                }
            }
        }else {
            //删除类型
            categoryId = rowData.getBeforeColumns(1).getValue();
        }
        Result<List<Content>> result = contentFeign.findByCategory(Long.parseLong(categoryId));
        if (result.getData() != null && result.getData().size() > 0){
            stringRedisTemplate.boundValueOps("content_"+categoryId).set(JSON.toJSONString(result.getData()));
        }

        System.out.println("--------广告监听结束---------");
    }
}
