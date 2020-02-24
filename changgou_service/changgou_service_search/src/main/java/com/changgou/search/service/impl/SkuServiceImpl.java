package com.changgou.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.search.dao.SkuEsMapper;
import com.changgou.search.pojo.SkuInfo;
import com.changgou.search.service.SkuService;
import entity.Result;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.util.StringUtil;

import java.util.*;

@Service
public class SkuServiceImpl implements SkuService {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private SkuFeign skuFeign;
    @Autowired
    private SkuEsMapper skuEsMapper;
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate; // 可以实现索引库增删改查 [高级搜索]

    @Override
    public void importSku() {
        //调用goods微服的feign得到响应数据
        Result<List<Sku>> result = skuFeign.findByStatus("1");
        //变更数据格式
        List<SkuInfo> skuInfos = JSON.parseArray(JSON.toJSONString(result.getData()), SkuInfo.class);

        for (SkuInfo skuInfo : skuInfos) {
            //调用参数属性
            Map<String, Object> map = JSON.parseObject(skuInfo.getSpec(), Map.class);
            //如果要生成的动态的域,只需将域存放在Map<String,Object>,该Map<String,Object>的key会生成一个key,域的名字为该map的key
            skuInfo.setSpecMap(map);
        }
        //存入es
        skuEsMapper.saveAll(skuInfos);
    }

    /***
     * 多条件搜索
     * @param searchMap 搜索条件
     * @return
     */
    @Override
    public Map search(Map<String, String> searchMap) {
        // 搜索条件封装
        NativeSearchQueryBuilder builder = builderBasicQuery(searchMap);

        // 条件搜索
        Map<String, Object> map = searchList(builder);

        // 分组搜索
        Map<String, Object> searchGroup = searchGroup(builder, searchMap);
        map.putAll(searchGroup);

        //Pageable pageable = builder.build().getPageable();


        return map;
    }

    /**
     * 所有分组
     *
     * @param builder
     * @return
     */
    private Map<String,Object> searchGroup(NativeSearchQueryBuilder builder, Map<String, String> searchMap) {
        // 定义一个map接收
        Map<String,Object> map = new HashMap<String,Object>();
        /***
         * 所有分组查询
         * addAggregation 添加一个聚合操作
         * 1) 取别名
         * 2) 表示根据哪个域进行分组查询
         */
        if (StringUtil.isEmpty(searchMap.get("category"))) {
            // 没有分类数据
            builder.addAggregation(AggregationBuilders.terms("skuCategory").field("categoryName"));
        }
        if (StringUtil.isEmpty(searchMap.get("brand"))) {
            // 没有品牌数据
            builder.addAggregation(AggregationBuilders.terms("skuBrand").field("brandName"));
        }
        // 添加规格分组
        builder.addAggregation(AggregationBuilders.terms("skuSpec").field("spec.keyword").size(10000));
        //执行查询
        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(builder.build(), SkuInfo.class);

        /***
         * 获取分组数据
         * aggregatedPage.getAggregations() 得到的是集合,可以是多个域进行分组
         * get("skuCategory");获取指定域的集合
         */
        // 分类
        if (StringUtil.isEmpty(searchMap.get("category"))) {
            StringTerms skuCategory = aggregatedPage.getAggregations().get("skuCategory");
            // 调用封装成集合
            List<String> categoryList = getGroupList(skuCategory);
            map.put("categoryList", categoryList);
        }
        // 品牌
        if (StringUtil.isEmpty(searchMap.get("brand"))) {
            StringTerms skuBrand = aggregatedPage.getAggregations().get("skuBrand");
            // 调用
            List<String> brandList = getGroupList(skuBrand);
            map.put("brandList", brandList);
        }
        // 规格
        StringTerms skuSpec = aggregatedPage.getAggregations().get("skuSpec");
        List<String> specList = getGroupList(skuSpec);
        // 解析规格,封装成map类型
        Map<String, Set<String>> specMap = getSetMap(specList);

        map.put("specMap", specMap);

        return map;
    }

    /***
     * 将指定传入的对象解析成 List
     * @param stringTerms
     * @return
     */
    private List<String> getGroupList(StringTerms stringTerms) {
        List<String> groupList = new ArrayList();
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            groupList.add(bucket.getKeyAsString());
        }
        return groupList;
    }

    /**
     * 查询条件构建
     *
     * @param searchMap 用户传入的参数
     * @return
     */
    private NativeSearchQueryBuilder builderBasicQuery(Map<String, String> searchMap) {
        // 创建查询条件构建器
        NativeSearchQueryBuilder builder = new NativeSearchQueryBuilder();
        // 组装条件
        if (searchMap != null && searchMap.size() > 0) {
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            // 关键字搜索
            String keyword = searchMap.get("keywords");
            if (!StringUtils.isEmpty(keyword)) {
                // 有输入关键词
                // builder.withQuery(QueryBuilders.matchQuery("name", keyword));
                boolQueryBuilder.must(QueryBuilders.matchQuery("name", keyword));
            }
            // 分类搜索
            String category = searchMap.get("category");
            if (StringUtil.isNotEmpty(category)) {
                // 分类有词
                boolQueryBuilder.must(QueryBuilders.termQuery("categoryName", category));
            }

            // 品牌搜索
            String brand = searchMap.get("brand");
            if (StringUtil.isNotEmpty(brand)) {
                // 分类有词
                boolQueryBuilder.must(QueryBuilders.termQuery("brandName", brand));
            }

            // 规格搜索
            // 读取用户传入的所有参数的key
            for (String key : searchMap.keySet()) {
                // 识别规格
                if (key.startsWith("spec_")) {
                    System.out.println(searchMap.get(key));
                    String replace = searchMap.get(key).replace("\\", "");
                    // 规格域
                    boolQueryBuilder.must(QueryBuilders.termQuery("specMap." + key.substring(5) + ".keyword", searchMap.get(key)));
                }
            }

            // 价格区间查询
            String price = searchMap.get("price");
            if (StringUtil.isNotEmpty(price)) {
                // 0-500元..3000元以上 过滤
                price = price.replace("元", "").replace("以上", "");
                // - 过滤
                String[] split = price.split("-");
                // 判断还有没有数据
                if (split.length > 0 && split != null) {
                    // 第一个数 gt 大于
                    boolQueryBuilder.must(QueryBuilders.rangeQuery("price").gt(split[0]));
                    if (split.length == 2) {
                        // 有第二个数 lte  小于等于
                        boolQueryBuilder.must(QueryBuilders.rangeQuery("price").lte(split[1]));
                    }
                }
            }

            // 追加多条件匹配搜索
            builder.withQuery(boolQueryBuilder);



            /***
             *  排序 综合 销量 新品 评价 价格
             *  sortField 用于排序的域
             *  sortRole  排序的规则 ASC 升序 DESC 降序
             */
            String sortField = searchMap.get("sortField");
            String sortRole = searchMap.get("sortRole");
            if (StringUtil.isNotEmpty(sortField) && StringUtil.isNotEmpty(sortRole)) {
                builder.withSort(new FieldSortBuilder(sortField).order(SortOrder.valueOf(sortRole)));
            }


        }
        // 设置分页
        Integer pageNum = coverterPage(searchMap);  // 当前页页码
        Integer pageSize = 30;  // 每页查询显示数
        // 调用构建方法加入分页构建
        // 1) 从第几页开始 (索引从0开始)
        // 2) 每页显示记录数
        builder.withPageable(PageRequest.of(pageNum - 1, pageSize));

        return builder;
    }

    private Integer coverterPage(Map<String, String> searchMap) {
        if (searchMap.size() > 0 && searchMap != null) {
            try {
                Integer pageNum = Integer.valueOf(searchMap.get("pageNum"));
                return pageNum;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 1;
    }

    /**
     * 条件搜索
     *
     * @param builder
     * @return
     */
    private Map<String, Object> searchList(NativeSearchQueryBuilder builder) {
        /**
         * 构建需要高亮显示的域
         * .preTags 设置高亮前缀
         * .postTags 设置高亮后缀
         * .fragmentSize 设置碎片长度
         */
        HighlightBuilder.Field field = new HighlightBuilder.Field("name");
        field.preTags("<em style = 'color:red;'>");
        field.postTags("</em>");
        field.fragmentSize(100);
        // 追加高亮查询信息
        builder.withHighlightFields(field);


        //获取搜索条件对象
        NativeSearchQuery query = builder.build();
        //执行搜索得到结果集
        AggregatedPage<SkuInfo> page = elasticsearchTemplate.queryForPage(query, SkuInfo.class, new SearchResultMapper() {
            /**
             * 进行高亮数据的替换
             * @param response 查询的结果
             * @param clazz    数据列表的类型
             * @param pageable 分页选项
             * @param <T>
             * @return
             */
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                // 创建一个接收替换高亮数据后的容器
                List<T> highLightSku = new ArrayList<T>();
                // 遍历查询所有高亮数据
                for (SearchHit hit : response.getHits()) {
                    // 先获取当次的无高亮数据并封装成对象
                    SkuInfo skuInfo = JSON.parseObject(hit.getSourceAsString(), SkuInfo.class);
                    // 获取指定域的高亮数据
                    HighlightField name = hit.getHighlightFields().get("name");
                    if (name != null && name.getFragments() != null) {
                        // 得到碎片数据
                        Text[] fragments = name.getFragments();
                        // 创建一个接收容器
                        StringBuffer stringBuffer = new StringBuffer();
                        //System.out.println(name.toString());
                        for (Text fragment : fragments) {
                            stringBuffer.append(fragment);
                        }
                        // 替换高亮数据
                        skuInfo.setName(stringBuffer.toString());
                    }
                    // 添加到容器中
                    highLightSku.add((T) skuInfo);
                }

                return new AggregatedPageImpl<T>(highLightSku, pageable, response.getHits().getTotalHits());
            }
        });

        // 封装结果
        Map<String, Object> map = new HashMap();
        map.put("rows", page.getContent());
        map.put("total", page.getTotalElements());
        map.put("totalPages", page.getTotalPages());


        Pageable pageable = page.getPageable();
        // 获取当前页
        int pageNum = pageable.getPageNumber();

        //获取当前分页每页记录数
        int pageSize = pageable.getPageSize();

        map.put("pageNum", pageNum);
        map.put("pageSize", pageSize);

        return map;
    }

    /**
     * 分类分组
     *
     * @param builder
     * @return
     */
    private List<String> searchCategoryList(NativeSearchQueryBuilder builder) {
        /***
         * 分组分类查询
         * addAggregation 添加一个聚合操作
         * 1) 取别名
         * 2) 表示根据哪个域进行分组查询
         */
        builder.addAggregation(AggregationBuilders.terms("skuCategory").field("categoryName"));
        //执行查询
        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(builder.build(), SkuInfo.class);

        /***
         * 获取分组数据
         * aggregatedPage.getAggregations() 得到的是集合,可以是多个域进行分组
         * get("skuCategory");获取指定域的集合
         */
        StringTerms stringTerms = aggregatedPage.getAggregations().get("skuCategory");

        List<String> categoryList = new ArrayList();
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            categoryList.add(bucket.getKeyAsString());
        }
        return categoryList;
    }

    /**
     * 品牌分组
     *
     * @param builder
     * @return
     */
    private List<String> searchBrandList(NativeSearchQueryBuilder builder) {
        /***
         * 品牌分组查询
         * addAggregation 添加一个聚合操作
         * 1) 取别名
         * 2) 表示根据哪个域进行分组查询
         */
        builder.addAggregation(AggregationBuilders.terms("skuBrand").field("brandName"));
        //执行查询
        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(builder.build(), SkuInfo.class);

        /***
         * 获取分组数据
         * aggregatedPage.getAggregations() 得到的是集合,可以是多个域进行分组
         * get("skuCategory");获取指定域的集合
         */
        StringTerms stringTerms = aggregatedPage.getAggregations().get("skuBrand");

        List<String> brandList = new ArrayList();
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            brandList.add(bucket.getKeyAsString());
        }
        return brandList;
    }

    /***
     * 规格分组
     * @param builder 查询条件构建
     * @return
     */
    private Map<String, Set<String>> searchSpecList(NativeSearchQueryBuilder builder) {
        /***
         * 规格分组查询
         * builder构建一个查询条件,添加一个聚合操作
         * AggregationBuilders分组条件构建
         * terms 别名
         * field 域名
         */
        builder.addAggregation(AggregationBuilders.terms("skuSpec").field("spec.keyword").size(10000));

        //调用elasticsearchTemplate对构建好的条件对象进行查询
        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(builder.build(), SkuInfo.class);

        /**
         * 调用aggregatedPage获得数据集合并得到指定域的集合
         * 用StringTerms类型接收
         */
        StringTerms stringTerms = aggregatedPage.getAggregations().get("skuSpec");
        // 遍历得到数据
        List<String> specList = new ArrayList<String>();
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            specList.add(bucket.getKeyAsString());
        }
        Map<String, Set<String>> specMap = getSetMap(specList);

        return specMap;
    }

    private Map<String, Set<String>> getSetMap(List<String> specList) {
        /***
         * 集合拆分封装成map对象返回
         * set 无序去重集合
         */
        Map<String, Set<String>> specMap = new HashMap<String, Set<String>>();
        for (String spec : specList) {
            Map<String, String> map = JSON.parseObject(spec, Map.class);
            for (Map.Entry<String, String> entry : map.entrySet()) {

                String key = entry.getKey();
                String value = entry.getValue();
                Set<String> set = specMap.get(key);
                if (set == null) {
                    set = new HashSet<String>();
                }
                set.add(value);
                specMap.put(key, set);
            }
        }
        return specMap;
    }
}
