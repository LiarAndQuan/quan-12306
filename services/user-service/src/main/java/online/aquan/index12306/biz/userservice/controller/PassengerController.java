package online.aquan.index12306.biz.userservice.controller;

import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.userservice.dto.resp.PassengerRespDTO;
import online.aquan.index12306.biz.userservice.service.PassengerService;
import online.aquan.index12306.framework.starter.convention.result.Result;
import online.aquan.index12306.framework.starter.web.Results;
import online.aquan.index12306.frameworks.starter.user.core.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PassengerController {
    
    private final PassengerService passengerService;
    
    
    /**
     * 根据用户名查询乘车人列表
     */
    @GetMapping("/api/user-service/passenger/query")
    public Result<List<PassengerRespDTO>> listPassengerQueryByUsername() {
        return Results.success(passengerService.listPassengerQueryByUsername(UserContext.getUsername()));
    }
}
