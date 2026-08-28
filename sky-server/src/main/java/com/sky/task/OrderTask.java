package com.sky.task;

import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    //每分钟检查是否有超时15分钟未支付的订单，取消该订单
    @Autowired
    private OrderMapper orderMapper;

    @Scheduled(cron = "0 * * * * ? ")
    public void processTimeOut(){
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> ordersList = orderMapper.getByOrderStatusAndTime(Orders.PENDING_PAYMENT,time);
        if (ordersList!=null && ordersList.size()>0) {
            for (Orders order: ordersList){
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason("订单超时，取消订单");
                orderMapper.update(order);
            }
        }
    }
    //每天凌晨一点检查前一天所有订单是否有一直处于派送中订单，状态改为完成
    @Scheduled(cron="0 0 1 * * ? ")
    public void processDeliveryToCompleted(){
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> ordersList = orderMapper.getByOrderStatusAndTime(Orders.DELIVERY_IN_PROGRESS,time);
        if (ordersList!=null && ordersList.size()>0) {
            for (Orders order: ordersList){
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }
}
