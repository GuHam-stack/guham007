package com.changgou.filter;

import com.changgou.utils.GetUrl;
import com.changgou.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关全局权限过滤器
 *
 * @author Steven
 * @description com.changgou.filter
 */
@Component
public class AuthorizeFilter implements GlobalFilter, Ordered {

    // 令牌的key
    private static final String AUTHORIZE_TOKEN = "Authorization";
    //登录url
    private static final String USER_LOGIN_URL = "http://localhost:9001/oauth/login";


    /**
     * 拦截请求过滤规则逻辑
     *
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // request response
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        //获取请求的url
        String url = request.getURI().getPath();
        //String url2 = request.getURI().toString();
        System.out.println(url);
        //System.out.println(url2);

        if (GetUrl.getUrl(url)) {
            // 登录请求 放行
            return chain.filter(exchange);
        }

        // 非登录状态其他请求
        // 获取头信息token
        String token = request.getHeaders().getFirst(AUTHORIZE_TOKEN);
        boolean falg = true;

        if (StringUtils.isEmpty(token)) {
            // 请求头里没有 请求参数里找
            token = request.getQueryParams().getFirst(AUTHORIZE_TOKEN);
            falg = false;
        }
        if (StringUtils.isEmpty(token)) {
            // 请求参数也没有 判断cookie
            HttpCookie cookie = request.getCookies().getFirst(AUTHORIZE_TOKEN);
            if (cookie != null) {
                token = cookie.getValue();
            }
        }
        if (StringUtils.isEmpty(token)) {
            // 没有令牌
            //response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
            response.setStatusCode(HttpStatus.SEE_OTHER);
            //拼接url地址
            String url2 = USER_LOGIN_URL + "?FROM=" + request.getURI();
            response.getHeaders().set("Location", url2);  //把请求地址带回去
            return response.setComplete();
        } else {
            // 有令牌
//            try {
//               // Claims claims = JwtUtil.parseJWT(token);
//                request.mutate().header(AUTHORIZE_TOKEN, "bearer " + token);
//            } catch (Exception e) {
//                e.printStackTrace();
//                //无效的认证
//                response.setStatusCode(HttpStatus.UNAUTHORIZED);
//                return response.setComplete();
//            }

//            if (!falg) {
//                if (!token.startsWith("bearer ") && !token.startsWith("Bearer ")) {
//                    token = "bearer " + token;
//                }
//
//                // 放入头中
//                request.mutate().header(AUTHORIZE_TOKEN, token);
//            }
            request.mutate().header(AUTHORIZE_TOKEN, "bearer " + token);
            return chain.filter(exchange);
        }
    }


    @Override
    public int getOrder() {
        return 0;
    }
}
