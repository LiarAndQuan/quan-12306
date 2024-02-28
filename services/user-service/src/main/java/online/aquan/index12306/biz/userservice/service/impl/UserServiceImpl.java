package online.aquan.index12306.biz.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.userservice.dao.entity.UserDO;
import online.aquan.index12306.biz.userservice.dao.entity.UserDeletionDO;
import online.aquan.index12306.biz.userservice.dao.mapper.UserDeletionMapper;
import online.aquan.index12306.biz.userservice.dao.mapper.UserMapper;
import online.aquan.index12306.biz.userservice.dto.resp.UserQueryActualRespDTO;
import online.aquan.index12306.biz.userservice.dto.resp.UserQueryRespDTO;
import online.aquan.index12306.biz.userservice.service.UserService;
import online.aquan.index12306.framework.starter.common.toolkit.BeanUtil;
import online.aquan.index12306.framework.starter.convention.exception.ClientException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final UserDeletionMapper userDeletionMapper;
    @Override
    public UserQueryRespDTO queryUserByUsername(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        UserDO userDO = userMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException("用户不存在，请检查用户名是否正确");
        }
        return BeanUtil.convert(userDO, UserQueryRespDTO.class);
    }

    @Override
    public UserQueryActualRespDTO queryActualUserByUsername(String username) {
        return BeanUtil.convert(queryUserByUsername(username), UserQueryActualRespDTO.class);
    }

    public Integer queryUserDeletionNum(Integer idType, String idCard) {
        LambdaQueryWrapper<UserDeletionDO> queryWrapper = Wrappers.lambdaQuery(UserDeletionDO.class)
                .eq(UserDeletionDO::getIdType, idType)
                .eq(UserDeletionDO::getIdCard, idCard);
        // TODO 此处应该先查缓存
        //返回记录的条数
        Long deletionCount = userDeletionMapper.selectCount(queryWrapper);
        return Optional.ofNullable(deletionCount).map(Long::intValue).orElse(0);
    }
}
