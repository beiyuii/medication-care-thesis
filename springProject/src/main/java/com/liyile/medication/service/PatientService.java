package com.liyile.medication.service;

import com.liyile.medication.entity.Patient;
import com.liyile.medication.entity.User;
import com.liyile.medication.vo.PatientSummaryVO;
import java.util.List;

/**
 * 患者服务接口。
 * <p>提供患者相关的查询与业务逻辑。</p>
 *
 * @author Liyile
 */
public interface PatientService {
  /**
   * 根据用户ID查询关联的患者列表。
   * <p>对于elder角色，返回自己的患者记录；对于caregiver角色，返回关联的所有患者列表（一对多）；对于child角色，返回关联的患者列表（通常只有一个，一对一）。</p>
   *
   * @param userId 用户ID
   * @param role 用户角色
   * @return 患者摘要列表
   */
  List<PatientSummaryVO> findPatientsByUserId(Long userId, String role);

  /**
   * 根据用户ID和角色查询当前关联的患者（用于child角色的一对一关系）。
   * <p>对于child角色，返回唯一关联的患者；对于其他角色返回null。</p>
   *
   * @param userId 用户ID
   * @param role 用户角色
   * @return 患者摘要VO，如果不存在或不是child角色则返回null
   */
  PatientSummaryVO findCurrentPatientByUserId(Long userId, String role);

  /**
   * 根据患者ID查询患者详情。
   *
   * @param id 患者ID
   * @return 患者实体或null
   */
  Patient findById(Long id);

  /**
   * 为 elder 账号确保存在一条患者档案。
   *
   * @param user 当前用户
   * @return 患者实体
   */
  Patient ensurePatientProfileForElder(User user);

  /**
   * 按老人用户名绑定当前 caregiver / child 到患者。
   *
   * @param currentUser 当前用户
   * @param currentRole 当前角色
   * @param elderUsername 老人账号用户名
   * @return 绑定后的患者摘要
   */
  PatientSummaryVO bindPatientByElderUsername(User currentUser, String currentRole, String elderUsername);
}
