package com.changgou.search.controller;

import com.changgou.search.feign.SkuFeign;
import entity.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/search")
public class SkuController {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private SkuFeign skuFeign;

    /**
     * 搜索商品
     * 注意此处的@GetMapping()要添加list的url请求，不然会跟SkuFeign中的请求url冲突
     */
    @GetMapping("/list")
    public String search(@RequestParam(required = false) Map<String,String> searchMap, Model model) {
        // 替换特殊字符
        if (searchMap != null) {
            handlerSearchMap(searchMap);
        }
        // 查询数据
        Map<String, Object> result = skuFeign.search(searchMap);
        // 返回结果集
        model.addAttribute("result", result);

        // 返回查询条件
        model.addAttribute("searchMap", searchMap);

        //获得url
        String[] url = getUrl(searchMap);
        model.addAttribute("url", url[0]);
        model.addAttribute("sortUrl", url[1]);

        System.out.println(result.get("pageSize"));
        System.out.println(result.get("pageNum"));

        Page page = new Page(
                new Long(result.get("total").toString()),
                Integer.valueOf(result.get("pageNum").toString()) + 1,
                new Integer(result.get("pageSize").toString()));

        model.addAttribute("page", page);
        // 响应视图
        return "search";
    }

    private void handlerSearchMap(Map<String,String> searchMap) {
        for (Map.Entry<String, String> entry : searchMap.entrySet()) {
            if (entry.getKey().startsWith("spec_")){
               // System.out.println(entry.getValue());
                entry.setValue(entry.getValue().replace("+", "%2B"));
               // System.out.println(entry.getValue());
            }
        }
    }

    private String[] getUrl(Map<String, String> searchMap) {
        String url = "/search/list";
        String sortUrl = "/search/list";
        if (searchMap.size() > 0 && searchMap != null) {
            url += "?";
            sortUrl += "?";
            for (Map.Entry<String, String> entry : searchMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("pageNum")) {
                    continue;
                }
                url += entry.getKey() + "=" + entry.getValue() + "&";
                if (entry.getKey().equalsIgnoreCase("sortField") ||
                        entry.getKey().equalsIgnoreCase("sortRole")) {
                    continue;
                }
                sortUrl += entry.getKey() + "=" + entry.getValue() + "&";
            }
            url.substring(0, url.length() - 1); // 0 1 2 3
            sortUrl.substring(0, sortUrl.length() - 1);
        }
        return new String[]{url, sortUrl};
    }
}
