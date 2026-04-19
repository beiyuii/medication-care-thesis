package com.liyile.medication.common;

/**
 * 通用常量定义类。
 * <p>集中管理系统中使用的字符串常量，避免魔法值。</p>
 *
 * @author Liyile
 */
public final class CommonConstants {

  /** Bearer 令牌前缀（包含空格）。 */
  public static final String BEARER_PREFIX = "Bearer ";

  /** 点号字符常量。 */
  public static final char DOT = '.';

  /** BCrypt 哈希前缀：$2a$。 */
  public static final String BCRYPT_PREFIX_2A = "$2a$";
  /** BCrypt 哈希前缀：$2b$。 */
  public static final String BCRYPT_PREFIX_2B = "$2b$";
  /** BCrypt 哈希前缀：$2y$。 */
  public static final String BCRYPT_PREFIX_2Y = "$2y$";

  private CommonConstants() {}
}