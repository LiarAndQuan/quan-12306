package online.aquan.index12306.biz.userservice.service;

import online.aquan.index12306.biz.userservice.dto.resp.PassengerActualRespDTO;
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

    /**
     * 根据乘车人 ID 集合查询乘车人列表
     *
     * @param username 用户名
     * @param ids      乘车人 ID 集合
     * @return 乘车人返回列表
     */
    List<PassengerActualRespDTO> listPassengerQueryByIds(String username, List<Long> ids);

}
