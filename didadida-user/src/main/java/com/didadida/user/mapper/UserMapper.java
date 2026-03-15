package com.didadida.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.didadida.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 */
@Mapper // 这个注解也可以不加，因为启动类加了 @MapperScan
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据手机号查询用户
     */
    User selectByPhone(String phone);
}