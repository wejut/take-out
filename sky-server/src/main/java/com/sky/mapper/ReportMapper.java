package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {
    @Select("select sum(amount) from orders where order_time<#{dayMax} and order_time>#{dayMin} and status=#{status}")
    Double sumAmountByDate(LocalDateTime dayMin, LocalDateTime dayMax, Integer status);

    Integer countUserIdByDate(Map map);

    Integer countOrdersByDateAndStatusDynamically(Map map);

    List<Map<String,Object>> getTop10ByDate(Map map);
}
