package com.liyile.medication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyile.medication.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper 接口。
 *
 * @author Liyile
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}