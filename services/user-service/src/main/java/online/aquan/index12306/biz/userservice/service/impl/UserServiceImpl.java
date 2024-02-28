package online.aquan.index12306.biz.userservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.userservice.dao.entity.UserDO;
import online.aquan.index12306.biz.userservice.dao.entity.UserDeletionDO;
import online.aquan.index12306.biz.userservice.dao.entity.UserMailDO;
import online.aquan.index12306.biz.userservice.dao.mapper.UserDeletionMapper;
import online.aquan.index12306.biz.userservice.dao.mapper.UserMailMapper;
import online.aquan.index12306.biz.userservice.dao.mapper.UserMapper;
import online.aquan.index12306.biz.userservice.dto.req.UserUpdateReqDTO;
import online.aquan.index12306.biz.userservice.dto.resp.UserQueryActualRespDTO;
import online.aquan.index12306.biz.userservice.dto.resp.UserQueryRespDTO;
import online.aquan.index12306.biz.userservice.service.UserService;
import online.aquan.index12306.framework.starter.common.toolkit.BeanUtil;
import online.aquan.index12306.framework.starter.convention.exception.ClientException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final UserDeletionMapper userDeletionMapper;
    private final UserMailMapper userMailMapper;
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

    @Override
    public void update(UserUpdateReqDTO requestParam) {
        //转化为UserDO对象直接更新
        UserDO userDO = BeanUtil.convert(requestParam, UserDO.class);
        LambdaUpdateWrapper<UserDO> userUpdateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername());
        userMapper.update(userDO, userUpdateWrapper);
        //如果传入的邮箱与之前的邮箱不同,那么需要修改mail路由表
        UserQueryRespDTO userQueryRespDTO = queryUserByUsername(requestParam.getUsername());
        if (StrUtil.isNotBlank(requestParam.getMail()) && !Objects.equals(requestParam.getMail(), userQueryRespDTO.getMail())) {
            LambdaUpdateWrapper<UserMailDO> updateWrapper = Wrappers.lambdaUpdate(UserMailDO.class)
                    .eq(UserMailDO::getMail, userQueryRespDTO.getMail());
            userMailMapper.delete(updateWrapper);
            UserMailDO userMailDO = UserMailDO.builder()
                    .mail(requestParam.getMail())
                    .username(requestParam.getUsername())
                    .build();
            userMailMapper.insert(userMailDO);
        }
    }
}
