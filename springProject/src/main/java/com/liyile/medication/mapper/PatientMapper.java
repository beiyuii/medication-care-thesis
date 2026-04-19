package com.liyile.medication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyile.medication.entity.Patient;
import org.apache.ibatis.annotations.Mapper;

/**
 * 患者表 Mapper 接口。
 *
 * @author Liyile
 */
@Mapper
public interface PatientMapper extends BaseMapper<Patient> {
}