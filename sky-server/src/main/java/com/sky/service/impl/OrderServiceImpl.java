package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.MapUtil;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private MapUtil mapUtil;
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //用户下单检查地址和购物车是否为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        String userAddress= addressBook.getCityName()+addressBook.getDistrictName();
        //--------------------------------------------------------------------------------------------------
        HashMap latAndLong = mapUtil.getLatAndLong(userAddress);
        if (latAndLong==null|| latAndLong.isEmpty()){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_NOT_FOUND);
        }
        HashMap tooFar = mapUtil.isTooFar((double) latAndLong.get("lng"), (double) latAndLong.get("lat"));
        if (tooFar==null|| tooFar.isEmpty()){
            throw new AddressBookBusinessException(MessageConstant.ROUTE_IS_WORRIED);
        }
        if (!(boolean)tooFar.get("0F1T")){
            throw new AddressBookBusinessException(MessageConstant.TOO_FAR);
        }
        //----------------------------------------------------------------------------------------------------
        ShoppingCart cart = new ShoppingCart();
        Long userid = BaseContext.getCurrentId();
        cart.setUserId(userid);
        List<ShoppingCart> carts = shoppingCartMapper.search(cart);
        if (carts==null || carts.size()==0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //搞个order对象插入order表里
        //把用户购物车当前数据导入到orderDetail里依旧是一条购物车数据一条orderDetail数据，所以detail表和order表是多对一关系
        //批量插入order_detail
        //删除对应用户的购物车数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);
        order.setUserId(userid);
        order.setPayStatus(Orders.UN_PAID);
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setOrderTime(LocalDateTime.now());
        //注意到这里username用的收货人名字
        order.setUserName(addressBook.getConsignee());
        order.setAddress(addressBook.getDetail());
        order.setPhone(addressBook.getPhone());
        order.setConsignee(addressBook.getConsignee());
        orderMapper.insert(order);
        List<OrderDetail> orderDetails = new ArrayList<>();
        carts.forEach(Cart ->
                {
                    OrderDetail orderDetail = new OrderDetail();
                    BeanUtils.copyProperties(Cart,orderDetail);
                    orderDetail.setId(null);
                    orderDetail.setOrderId(order.getId());
                    orderDetails.add(orderDetail);
                }
        );
        orderDetailMapper.insertBatch(orderDetails);
        shoppingCartMapper.deleteAllByUserId(userid);
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();
        return orderSubmitVO;
    }


    public PageResult pageQuery4user(int pageNum, int pageSize, Integer status) {
        PageHelper.startPage(pageNum,pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> list= new ArrayList<>();
        if (page!=null && page.size()>0){
        for (Orders order: page){
            Long orderId = order.getId();
            List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order,orderVO);
            orderVO.setOrderDetailList(orderDetails);
            list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(),list);
    }


    public OrderVO getOrderVoById(long id) {
        OrderVO orderVO = new OrderVO();
        Orders order = orderMapper.getOrderById(id);
        List<OrderDetail> details = orderDetailMapper.getByOrderId(id);
        BeanUtils.copyProperties(order,orderVO);
        orderVO.setOrderDetailList(details);
        return orderVO;
    }


    public void cancel(long id) {
        Orders order = orderMapper.getOrderById(id);
        if (order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (order.getStatus()>2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //对status为2，待接单的order进行退款
        //对订单状态进行修改
        order.setStatus(6);
        order.setCancelReason("用户取消");
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    public void onemore(long id) {
        List<OrderDetail> details = orderDetailMapper.getByOrderId(id);
        List<ShoppingCart> carts = new ArrayList<>();
        for (OrderDetail DT:details){
            ShoppingCart cart = new ShoppingCart();
            BeanUtils.copyProperties(DT,cart);
            cart.setId(null);
            cart.setUserId(BaseContext.getCurrentId());
            cart.setCreateTime(LocalDateTime.now());
            carts.add(cart);
        }
        shoppingCartMapper.insertBatch(carts);
    }


    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        if (page==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //先取出所有订单，封装进VOList中，
        List<OrderVO> orderVOList =page.stream().map(
                O->{
                    OrderVO orderVO = new OrderVO();
                    BeanUtils.copyProperties(O,orderVO);
                    String orderDish = orderDishesToStr(O);
                    orderVO.setOrderDishes(orderDish);
                    return orderVO;
                }).collect(Collectors.toList());
        return new PageResult(page.getTotal(),orderVOList);
    }



    //VO中有新字段OrderDishes，做字符串拼接方法
    private String orderDishesToStr(Orders OR){
        List<OrderDetail> details = orderDetailMapper.getByOrderId(OR.getId());
        List<String> orderDishList=details.stream().map(de->{
            String dish= de.getName()+"*"+de.getNumber()+";";
            return dish;
        }).collect(Collectors.toList());
        return String.join("",orderDishList);
    }


    public OrderStatisticsVO getOrdersStatusStatistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(orderMapper.countStatus(Orders.CONFIRMED));
        orderStatisticsVO.setToBeConfirmed(orderMapper.countStatus(Orders.TO_BE_CONFIRMED));
        orderStatisticsVO.setDeliveryInProgress(orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS));
        return orderStatisticsVO;
    }

    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
    }

    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
//        商家拒单其实就是将订单状态修改为“已取消”
//        只有订单处于“待接单”状态时可以执行拒单操作
//        商家拒单时需要指定拒单原因
//        商家拒单时，如果用户已经完成了支付，需要为用户退款
        // 根据id查询订单
        Orders ordersDB = orderMapper.getOrderById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

//        //支付状态
//        Integer payStatus = ordersDB.getPayStatus();
//        if (payStatus == Orders.PAID) {
//            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
//        }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    public void cancelOrder(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getOrderById(ordersCancelDTO.getId());

//        //支付状态
//        Integer payStatus = ordersDB.getPayStatus();
//        if (payStatus == 1) {
//            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
//        }

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getOrderById(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getOrderById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }


    public void reminder(long id) {
        Orders order = orderMapper.getOrderById(id);
        if (order==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Map map= new HashMap<>();
        map.put("type",2);//1为来单提醒，2为用户催单
        map.put("OrderId",order.getId());
        map.put("content","订单号"+order.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }
}
