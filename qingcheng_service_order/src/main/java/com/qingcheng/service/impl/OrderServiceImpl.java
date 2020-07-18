package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.OrderConfigMapper;
import com.qingcheng.dao.OrderItemMapper;
import com.qingcheng.dao.OrderLogMapper;
import com.qingcheng.dao.OrderMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.*;
import com.qingcheng.service.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service(interfaceClass = OrderService.class)
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 返回全部记录
     * @return
     */
    public List<Order> findAll() {
        return orderMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Order> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Order> orders = (Page<Order>) orderMapper.selectAll();
        return new PageResult<Order>(orders.getTotal(),orders.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Order> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return orderMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Order> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Order> orders = (Page<Order>) orderMapper.selectByExample(example);
        return new PageResult<Order>(orders.getTotal(),orders.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Order findById(String id) {
        return orderMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param order
     */
    public void add(Order order) {
        orderMapper.insert(order);
    }

    /**
     * 修改
     * @param order
     */
    public void update(Order order) {
        orderMapper.updateByPrimaryKeySelective(order);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(String id) {
        orderMapper.deleteByPrimaryKey(id);
    }
    @Autowired
    private OrderItemMapper orderItemMapper;

    /*查询详细订单*/
    public OrderAll findOrderAll(String id) {
        OrderAll orderAll = new OrderAll();
        Order order = orderMapper.selectByPrimaryKey(id);
        orderAll.setOrder(order);
        Example example=new Example(OrderItem.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("orderId",id);
        List<OrderItem> orderItems = orderItemMapper.selectByExample(example);
        orderAll.setOrderItemList(orderItems);
        return orderAll;

    }

    /**
     * 根据ids数组批量查询 order信息
     * @param ids
     * @return
     */
    public List<Order> findListByIds(String [] ids){
        Example example=new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", Arrays.asList(ids));
        List<Order> orders = orderMapper.selectByExample(example);
        return  orders;
    }

    /**
     * 批量发货
     * @param orderList
     */
    public void batchSend(List<Order> orderList) {
        for (Order order : orderList) {//大坑  如果为空则，必然有问题
            if (order.getShippingCode()==null||order.getShippingName()==null){
                throw new RuntimeException("请填写快递单号和快递名称");
            }
        }
        for (Order order : orderList) {
            order.setOrderStatus("3");//订单状态 已发货
            order.setConsignStatus("2");//发货状态 已发货
            order.setConsignTime(new Date());
            orderMapper.updateByPrimaryKeySelective(order);
            /*记录日志暂时不做了，有机会补上*/
        }

    }

    @Autowired
    private OrderConfigMapper orderConfigMapper;//注入定时任务配置dao

    private OrderLogMapper orderLogMapper;

    public void orderTimeOutLogic() {
        OrderConfig orderConfig = orderConfigMapper.selectByPrimaryKey(1);

        Integer orderTimeout = orderConfig.getOrderTimeout();//获取过期时间
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(orderTimeout);//获得超过时间点
        Example example=new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLessThan("createTime",localDateTime);//超时了
        criteria.andEqualTo("orderStatus","0");//未付款的
        criteria.andEqualTo("isDelete","0");//未删除的
        List<Order> orders = orderMapper.selectByExample(example);
        for (Order order : orders) {
            OrderLog orderLog=new OrderLog();
            orderLog.setOperater("system");// 系统
            orderLog.setOperateTime(new Date());//当前日期
            orderLog.setOrderStatus("4");
            orderLog.setPayStatus(order.getPayStatus());
            orderLog.setConsignStatus(order.getConsignStatus());
            orderLog.setRemarks("超时订单，系统自动关闭");
            orderLog.setOrderId(Long.valueOf(order.getId()));
            orderLogMapper.insert(orderLog);
            order.setOrderStatus("4");
            order.setCloseTime(new Date());//关闭日期
            orderMapper.updateByPrimaryKeySelective(order);
        }


    }

    /**
     * 订单合并
     * @param orderId1
     * @param orderId2
     */
    @Override
    @Transactional
    public void merge(String orderId1, String orderId2) {

        Order order1 = orderMapper.selectByPrimaryKey(orderId1);
        Order order2 = orderMapper.selectByPrimaryKey(orderId2);
        if (order1==null||order2==null){
            throw new RuntimeException("合并对象不存在");
        }
        //把从订单，订单详情的orderId改为主订单的id。(订单详情合并)
        Example example=new Example(OrderItem.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("orderId",order2.getId());
        List<OrderItem> orderItems = orderItemMapper.selectByExample(example);
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(orderId1);//转移
            orderItemMapper.updateByPrimaryKeySelective(orderItem);//保存
        }
        //订单概要表合并
        order1.setTotalNum(order1.getTotalNum()+order2.getTotalNum());//合并数量
        order1.setTotalMoney(order1.getTotalMoney()+order2.getTotalMoney());//合并总价钱
        order1.setPayMoney(order1.getPayMoney()+order2.getPayMoney());//合并支付价格
        order1.setPreMoney(order1.getPreMoney()+order2.getPreMoney());//合并预支付价格
        orderMapper.updateByPrimaryKeySelective(order1);//保存主订单
        order2.setIsDelete("1");//将从订单逻辑删除
        orderMapper.updateByPrimaryKeySelective(order2);//保存从订单
        //记录日志


    }


    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 订单id
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andLike("id","%"+searchMap.get("id")+"%");
            }
            // 支付类型，1、在线支付、0 货到付款
            if(searchMap.get("payType")!=null && !"".equals(searchMap.get("payType"))){
                criteria.andLike("payType","%"+searchMap.get("payType")+"%");
            }
            // 物流名称
            if(searchMap.get("shippingName")!=null && !"".equals(searchMap.get("shippingName"))){
                criteria.andLike("shippingName","%"+searchMap.get("shippingName")+"%");
            }
            // 物流单号
            if(searchMap.get("shippingCode")!=null && !"".equals(searchMap.get("shippingCode"))){
                criteria.andLike("shippingCode","%"+searchMap.get("shippingCode")+"%");
            }
            // 用户名称
            if(searchMap.get("username")!=null && !"".equals(searchMap.get("username"))){
                criteria.andLike("username","%"+searchMap.get("username")+"%");
            }
            // 买家留言
            if(searchMap.get("buyerMessage")!=null && !"".equals(searchMap.get("buyerMessage"))){
                criteria.andLike("buyerMessage","%"+searchMap.get("buyerMessage")+"%");
            }
            // 是否评价
            if(searchMap.get("buyerRate")!=null && !"".equals(searchMap.get("buyerRate"))){
                criteria.andLike("buyerRate","%"+searchMap.get("buyerRate")+"%");
            }
            // 收货人
            if(searchMap.get("receiverContact")!=null && !"".equals(searchMap.get("receiverContact"))){
                criteria.andLike("receiverContact","%"+searchMap.get("receiverContact")+"%");
            }
            // 收货人手机
            if(searchMap.get("receiverMobile")!=null && !"".equals(searchMap.get("receiverMobile"))){
                criteria.andLike("receiverMobile","%"+searchMap.get("receiverMobile")+"%");
            }
            // 收货人地址
            if(searchMap.get("receiverAddress")!=null && !"".equals(searchMap.get("receiverAddress"))){
                criteria.andLike("receiverAddress","%"+searchMap.get("receiverAddress")+"%");
            }
            // 订单来源：1:web，2：app，3：微信公众号，4：微信小程序  5 H5手机页面
            if(searchMap.get("sourceType")!=null && !"".equals(searchMap.get("sourceType"))){
                criteria.andLike("sourceType","%"+searchMap.get("sourceType")+"%");
            }
            // 交易流水号
            if(searchMap.get("transactionId")!=null && !"".equals(searchMap.get("transactionId"))){
                criteria.andLike("transactionId","%"+searchMap.get("transactionId")+"%");
            }
            // 订单状态
            if(searchMap.get("orderStatus")!=null && !"".equals(searchMap.get("orderStatus"))){
                criteria.andLike("orderStatus","%"+searchMap.get("orderStatus")+"%");
            }
            // 支付状态
            if(searchMap.get("payStatus")!=null && !"".equals(searchMap.get("payStatus"))){
                criteria.andLike("payStatus","%"+searchMap.get("payStatus")+"%");
            }
            // 发货状态
            if(searchMap.get("consignStatus")!=null && !"".equals(searchMap.get("consignStatus"))){
                criteria.andLike("consignStatus","%"+searchMap.get("consignStatus")+"%");
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andLike("isDelete","%"+searchMap.get("isDelete")+"%");
            }

            // 数量合计
            if(searchMap.get("totalNum")!=null ){
                criteria.andEqualTo("totalNum",searchMap.get("totalNum"));
            }
            // 金额合计
            if(searchMap.get("totalMoney")!=null ){
                criteria.andEqualTo("totalMoney",searchMap.get("totalMoney"));
            }
            // 优惠金额
            if(searchMap.get("preMoney")!=null ){
                criteria.andEqualTo("preMoney",searchMap.get("preMoney"));
            }
            // 邮费
            if(searchMap.get("postFee")!=null ){
                criteria.andEqualTo("postFee",searchMap.get("postFee"));
            }
            // 实付金额
            if(searchMap.get("payMoney")!=null ){
                criteria.andEqualTo("payMoney",searchMap.get("payMoney"));
            }

        }
        return example;
    }

}
