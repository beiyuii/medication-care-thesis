package com.liyile.medication.service;

import com.liyile.medication.entity.User;

/**
 * 用户服务接口。
 * <p>提供用户相关的查询与业务逻辑。</p>
 *
 * @author Liyile
 */
public interface UserService {
  /**
   * 根据用户名查询用户。
   *
   * @param username 用户名
   * @return 用户实体或 null
   */
  User findByUsername(String username);

  /**
   * 保存用户信息。
   *
   * @param user 待保存的用户实体
   * @return 持久化后的用户实体
   */
  User save(User user);

  /**
   * 根据用户ID查询用户。
   *
   * @param id 用户ID
   * @return 用户实体或null
   */
  User findById(Long id);
}
