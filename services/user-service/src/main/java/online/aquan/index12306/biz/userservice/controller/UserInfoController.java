package online.aquan.index12306.biz.userservice.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.userservice.dto.req.UserRegisterReqDTO;
import online.aquan.index12306.biz.userservice.dto.req.UserUpdateReqDTO;
import online.aquan.index12306.biz.userservice.dto.resp.UserQueryActualRespDTO;
import online.aquan.index12306.biz.userservice.dto.resp.UserQueryRespDTO;
import online.aquan.index12306.biz.userservice.dto.resp.UserRegisterRespDTO;
import online.aquan.index12306.biz.userservice.service.UserLoginService;
import online.aquan.index12306.biz.userservice.service.UserService;
import online.aquan.index12306.framework.starter.convention.result.Result;
import online.aquan.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserInfoController {
    
    private final UserService userService;
    private final UserLoginService userLoginService;
    
    /**
     * 根据用户名查询用户脱敏信息
     */
    @GetMapping("/api/user-service/query")
    public Result<UserQueryRespDTO> queryUserByUsername(@RequestParam("username") @NotEmpty String username) {
        return Results.success(userService.queryUserByUsername(username));
    }

    /**
     * 根据用户名查询用户无脱敏信息
     */
    @GetMapping("/api/user-service/actual/query")
    public Result<UserQueryActualRespDTO> queryActualUserByUsername(@RequestParam("username") @NotEmpty String username) {
        return Results.success(userService.queryActualUserByUsername(username));
    }

    /**
     * 检查用户名是否已存在
     */
    @GetMapping("/api/user-service/has-username")
    public Result<Boolean> hasUsername(@RequestParam("username") @NotEmpty String username) {
        return Results.success(userLoginService.hasUsername(username));
    }

    /**
     * 注册用户
     */
    @PostMapping("/api/user-service/register")
    public Result<UserRegisterRespDTO> register(@RequestBody @Valid UserRegisterReqDTO requestParam) {
        return Results.success(userLoginService.register(requestParam));
    }

    /**
     * 修改用户
     */
    @PostMapping("/api/user-service/update")
    public Result<Void> update(@RequestBody @Valid UserUpdateReqDTO requestParam) {
        userService.update(requestParam);
        return Results.success();
    }
    
}
