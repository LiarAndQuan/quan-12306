package online.aquan.index12306.biz.aggregationservice;


import cn.crane4j.spring.boot.annotation.EnableCrane4j;
import cn.hippo4j.core.enable.EnableDynamicThreadPool;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;


@SpringBootApplication(scanBasePackages = {
        "online.aquan.index12306.biz.userservice",
        "online.aquan.index12306.biz.ticketservice",
        "online.aquan.index12306.biz.orderservice",
        "online.aquan.index12306.biz.payservice"
})
@MapperScan(value = {
        "online.aquan.index12306.biz.userservice.dao.mapper",
        "online.aquan.index12306.biz.ticketservice.dao.mapper",
        "online.aquan.index12306.biz.orderservice.dao.mapper",
        "online.aquan.index12306.biz.payservice.dao.mapper"
})
/*
* @EnableRetry 是一个 Spring Framework 提供的注解，用于启用方法级别的重试机制。
* 当我们在 Spring 应用中的某些方法上标记 @Retryable 注解时，Spring 会尝试在方法执行失败时自动重试。
* */
@EnableRetry
@EnableFeignClients(value = {
        "online.aquan.index12306.biz.ticketservice.remote",
        "online.aquan.index12306.biz.orderservice.remote",
})
//todo 注释
@EnableDynamicThreadPool
@EnableCrane4j(enumPackages = "online.aquan.index12306.biz.orderservice.common.enums")
public class AggregationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AggregationServiceApplication.class, args);
    }
    
}
