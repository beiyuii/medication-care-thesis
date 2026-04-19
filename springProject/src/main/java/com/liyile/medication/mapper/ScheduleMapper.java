package com.liyile.medication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyile.medication.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 计划表 Mapper 接口。
 *
 * @author Liyile
 */
@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {
}