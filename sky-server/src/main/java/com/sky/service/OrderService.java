package com.sky.service;

import com.github.pagehelper.Page;
import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.stereotype.Service;

@Service
public interface OrderService {
    /**
     * 下单
     * @param ordersSubmitDTO
     * @return
     */
     OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);


    /**
     * 查询历史订单
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    PageResult pageQuery4user(int page, int pageSize, Integer status);

    /**
     * 根据订单id查询订单detail
     * @param id
     * @return
     */
    OrderVO getOrderVoById(long id);

    /**
     * 取消订单
     * @param id
     */
    void cancel(long id);

    /**
     * 再来一单
     * @param id
     */
    void onemore(long id);

    /**
     ******************************************************************管理端********************************************************************
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 查询各个状态订单
     * @return
     */
    OrderStatisticsVO getOrdersStatusStatistics();

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);
    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    /**
     * 商家取消订单
     *
     * @param ordersCancelDTO
     */
    void cancelOrder(OrdersCancelDTO ordersCancelDTO) throws Exception;
    /**
     * 派送订单
     *
     * @param id
     */
    void delivery(Long id);
    /**
     * 完成订单
     *
     * @param id
     */
    void complete(Long id);

    /**
     * 用户催单
     * @param id
     */
    void reminder(long id);
}
