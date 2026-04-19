package com.liyile.medication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyile.medication.entity.IntakeEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服药事件表 Mapper 接口。
 *
 * @author Liyile
 */
@Mapper
public interface IntakeEventMapper extends BaseMapper<IntakeEvent> {
}