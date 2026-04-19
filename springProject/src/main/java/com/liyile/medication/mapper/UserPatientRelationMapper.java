package com.liyile.medication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyile.medication.entity.UserPatientRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户患者关联表 Mapper 接口。
 *
 * @author Liyile
 */
@Mapper
public interface UserPatientRelationMapper extends BaseMapper<UserPatientRelation> {}

