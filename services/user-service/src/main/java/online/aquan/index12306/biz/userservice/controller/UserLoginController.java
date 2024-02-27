package online.aquan.index12306.biz.userservice.controller;


import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.userservice.dto.req.UserLoginReqDTO;
import online.aquan.index12306.biz.userservice.dto.resp.UserLoginRespDTO;
import online.aquan.index12306.biz.userservice.service.UserLoginService;
import online.aquan.index12306.framework.starter.convention.result.Result;
import online.aquan.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserLoginController {

    private final UserLoginService userLoginService;

    /**
     * 用户登录
     */
    @PostMapping("/api/user-service/v1/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        return Results.success(userLoginService.login(requestParam));
    }
}
