package online.aquan.index12306.biz.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import online.aquan.index12306.biz.userservice.dao.entity.UserDO;
import online.aquan.index12306.biz.userservice.dto.req.UserLoginReqDTO;
import online.aquan.index12306.biz.userservice.dto.resp.UserLoginRespDTO;

public interface UserLoginService extends IService<UserDO> {

    /**
     * 用户登录接口
     *
     * @param requestParam 用户登录入参
     * @return 用户登录返回结果
     */
    UserLoginRespDTO login(UserLoginReqDTO requestParam);


    /**
     * 通过 Token 检查用户是否登录
     *
     * @param accessToken 用户登录 Token 凭证
     * @return 用户是否登录返回结果
     */
    UserLoginRespDTO checkLogin(String accessToken);

    /**
     * 用户退出登录
     *
     * @param accessToken 用户登录 Token 凭证
     */
    void logout(String accessToken);

    /**
     * 用户名是否存在
     *
     * @param username 用户名
     * @return 用户名是否存在返回结果
     */
    Boolean hasUsername(String username);
}
