package com.liyile.medication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyile.medication.entity.Alert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警表 Mapper 接口。
 *
 * @author Liyile
 */
@Mapper
public interface AlertMapper extends BaseMapper<Alert> {
}