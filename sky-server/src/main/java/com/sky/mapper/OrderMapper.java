package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 新增订单
     * @param order
     */
    void insert(Orders order);

    /**
     * 查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单id查询订单
     * @return
     */
    @Select("select * from orders where id=#{id}")
    Orders getOrderById(long id);

    /**
     * 动态更新订单
     * @param order
     */
    void update(Orders order);

    /**
     * 数数各个状态订单数量
     * @return
     */
    @Select("select count(id) from orders where status=#{status}")
    Integer countStatus(Integer status);

    /**
     *查询订单By状态和时间
     * @param status
     * @param time
     * @return
     */
    @Select("select * from orders where status=#{status} and order_time < #{time}")
    List<Orders> getByOrderStatusAndTime(Integer status, LocalDateTime time);

    /**
     * 自创方法用于直接支付直接修改订单状态
     */
    @Select("SELECT id FROM orders where status = #{status} ORDER BY order_time DESC LIMIT 1")
    Long getLastOrderId(Integer status);
}
