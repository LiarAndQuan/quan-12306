package online.aquan.index12306.biz.userservice.service;

import jakarta.validation.constraints.NotEmpty;
import online.aquan.index12306.biz.userservice.dto.resp.UserQueryRespDTO;

public interface UserService {

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户详细信息
     */
    UserQueryRespDTO queryUserByUsername(@NotEmpty String username);
}
