package com.sky.controller.user;

import com.alibaba.fastjson.JSON;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController("UserOrderController")
@Api(value = "用户订单模块")
@RequestMapping("/user/order")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private OrderMapper orderMapper;


    @PostMapping("/submit")
    @ApiOperation(value = "用户下单")
    Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户正在下单，参数为：{}",ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submit(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) {

        OrderPaymentVO vo = OrderPaymentVO.builder()
                .nonceStr("fake_nonce_" + System.currentTimeMillis())
                .paySign("fake_sign_123456")
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .signType("MD5")
                .packageStr("prepay_id=fake_prepay_123")
                .build();
        Long lastOrderId = orderMapper.getLastOrderId(Orders.PAID);
        Orders order = new Orders();
        order.setId(lastOrderId);
        log.info("777777777777777777777777:"+lastOrderId);
        order.setPayStatus(Orders.PAID);
        order.setStatus(Orders.TO_BE_CONFIRMED);
        orderMapper.update(order);
        Map map =new HashMap<>();
        map.put("type", 1);//根据微信支付时序图得知，此处是在返回支付参数，再由用户端直接向微信服务器发真正的支付请求
        map.put("orderId",lastOrderId);//订单达不到接单状态，所以此处直接把接单提醒改成用户下单提醒，作为websocket的入门案例
        map.put("content","订单号："+ ordersPaymentDTO.getOrderNumber());//qwq
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
        return Result.success(vo);
    }

    @GetMapping("/historyOrders")
    @ApiOperation("查询历史订单")
    public Result<PageResult> page(int page, int pageSize, Integer status){
        PageResult pageResult = orderService.pageQuery4user(page, pageSize, status);
        return Result.success(pageResult);
    }

    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> details(@PathVariable long id){
        OrderVO orderVO = orderService.getOrderVoById(id);
        return Result.success(orderVO);
    }

    @PutMapping("cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancelOrder(@PathVariable long id){
        orderService.cancel(id);
        return Result.success();
    }
    @PostMapping("repetition/{id}")
    @ApiOperation("再来一单")
    public Result onemore(@PathVariable long id){
        orderService.onemore(id);
        return Result.success();
    }
    @GetMapping("reminder/{id}")
    @ApiOperation(value = "用户催单")
    public Result reminder(@PathVariable long id){
        orderService.reminder(id);
        return Result.success();
    }
}
