package online.aquan.index12306.biz.userservice.service;

import online.aquan.index12306.biz.userservice.dto.resp.PassengerRespDTO;

import java.util.List;

public interface PassengerService {
    /**
     * 根据用户名查询乘车人列表
     *
     * @param username 用户名
     * @return 乘车人返回列表
     */
    List<PassengerRespDTO> listPassengerQueryByUsername(String username);
}
