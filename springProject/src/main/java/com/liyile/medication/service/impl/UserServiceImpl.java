package com.liyile.medication.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyile.medication.entity.User;
import com.liyile.medication.mapper.UserMapper;
import com.liyile.medication.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类。
 *
 * @author Liyile
 */
@Service
public class UserServiceImpl implements UserService {

  /** 用户表数据访问对象 */
  private final UserMapper userMapper;

  /** 构造方法注入依赖。 */
  public UserServiceImpl(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  @Override
  public User findByUsername(String username) {
    return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
  }

  /**
   * 保存用户。
   *
   * @param user 用户实体
   * @return 持久化后的用户实体
   */
  @Override
  public User save(User user) {
    userMapper.insert(user);
    return user;
  }

  @Override
  public User findById(Long id) {
    return userMapper.selectById(id);
  }
}
