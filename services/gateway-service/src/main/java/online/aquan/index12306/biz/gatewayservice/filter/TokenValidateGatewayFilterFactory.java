/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package online.aquan.index12306.biz.gatewayservice.filter;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import online.aquan.index12306.biz.gatewayservice.config.Config;
import online.aquan.index12306.biz.gatewayservice.toolkit.JWTUtil;
import online.aquan.index12306.biz.gatewayservice.toolkit.UserInfoDTO;
import online.aquan.index12306.framework.starter.bases.constant.UserConstant;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * SpringCloud Gateway Token 拦截器
 *
 */

@Component
public class TokenValidateGatewayFilterFactory extends AbstractGatewayFilterFactory<Config> {

    public TokenValidateGatewayFilterFactory() {
        super(Config.class);
    }

    /**
     * 注销用户时需要传递 Token
     */
    public static final String DELETION_PATH = "/api/user-service/deletion";

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String requestPath = request.getPath().toString();
            //如果是黑名单里面的路径,即需要token的路径
            if (isPathInBlackPreList(requestPath, config.getBlackPathPre())) {
                //那么需要获取到token然后解析出用户的信息
                String token = request.getHeaders().getFirst("Authorization");
                // TODO 需要验证 Token 是否有效，有可能用户注销了账户，但是 Token 有效期还未过
                UserInfoDTO userInfo = JWTUtil.parseJwtToken(token);
                if (!validateToken(userInfo)) {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return response.setComplete();
                }
                //在请求头中填入用户的相关信息
                ServerHttpRequest.Builder builder = exchange.getRequest().mutate().headers(httpHeaders -> {
                    httpHeaders.set(UserConstant.USER_ID_KEY, userInfo.getUserId());
                    httpHeaders.set(UserConstant.USER_NAME_KEY, userInfo.getUsername());
                    httpHeaders.set(UserConstant.REAL_NAME_KEY, URLEncoder.encode(userInfo.getRealName(), StandardCharsets.UTF_8));
                    if (Objects.equals(requestPath, DELETION_PATH)) {
                        httpHeaders.set(UserConstant.USER_TOKEN_KEY, token);
                    }
                });
                //放行
                return chain.filter(exchange.mutate().request(builder.build()).build());
            }
            return chain.filter(exchange);
        };
    }

    private boolean isPathInBlackPreList(String requestPath, List<String> blackPathPre) {
        if (CollectionUtils.isEmpty(blackPathPre)) {
            return false;
        }
        return blackPathPre.stream().anyMatch(requestPath::startsWith);
    }

    private boolean validateToken(UserInfoDTO userInfo) {
        return userInfo != null;
    }
}

/*
 * ServerWebExchange:
 * ServerWebExchange 是 Spring Web 中的一个接口，它代表了一个 HTTP 请求-响应交换。
 * 它包含了 HTTP 请求的所有信息，比如请求头、请求体、路径、参数等，并且提供了一些方法来操作响应，比如添加响应头、修改状态码等。
 * 在 Gateway 中，ServerWebExchange 代表了当前正在处理的 HTTP 请求-响应交换。
 * 
 * GatewayFilterChain:
 * GatewayFilterChain 是一个过滤器链，它包含了一系列的网关过滤器，用于对 HTTP 请求进行处理。
 * 在 Gateway 中，当一个 HTTP 请求到达时，会经过一系列的过滤器，每个过滤器都有机会对请求进行修改或者添加额外的逻辑。
 * GatewayFilterChain 负责管理这些过滤器，并依次调用它们来处理请求。
 * 在自定义网关过滤器时，通常会使用 ServerWebExchange 对请求进行检查或修改，
 * 然后调用 GatewayFilterChain 的 filter() 方法将请求传递给下一个过滤器。
 * 这样可以构建一个过滤器链，每个过滤器都可以对请求进行一些操作，最终将请求发送到目标服务或者返回给客户端。
 * 在上一个示例中，exchange 就代表了当前的 ServerWebExchange，而 chain 则代表了当前的 GatewayFilterChain。
 * 你可以在自定义的过滤器中使用 exchange 对请求进行检查或修改，并调用 chain.filter(exchange) 将请求传递给下一个过滤器。
 */