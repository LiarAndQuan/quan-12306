package online.aquan.index12306.biz.ticketservice.controller;


import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import online.aquan.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import online.aquan.index12306.biz.ticketservice.service.TicketService;
import online.aquan.index12306.framework.starter.convention.result.Result;
import online.aquan.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TicketController {
    
    private final TicketService ticketService;

    
    /**
     * 根据条件查询车票
     */
    @GetMapping("/api/ticket-service/ticket/query")
    public Result<TicketPageQueryRespDTO> pageListTicketQuery(TicketPageQueryReqDTO requestParam) {
        return Results.success(ticketService.pageListTicketQueryV1(requestParam));
    }

}
