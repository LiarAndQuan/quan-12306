package online.aquan.index12306.biz.ticketservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("online.aquan.index12306.biz.ticketservice.dao.mapper")
public class TicketServiceApplication {
}
