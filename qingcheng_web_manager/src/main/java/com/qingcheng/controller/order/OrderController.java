package com.qingcheng.controller.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.PageResult;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderAll;
import com.qingcheng.service.order.OrderService;
import org.aspectj.weaver.ast.Or;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Reference
    private OrderService orderService;

    @GetMapping("/findAll")
    public List<Order> findAll(){
        return orderService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult<Order> findPage(int page, int size){
        return orderService.findPage(page, size);
    }

    @PostMapping("/findList")
    public List<Order> findList(@RequestBody Map<String,Object> searchMap){
        return orderService.findList(searchMap);
    }

    @PostMapping("/findPage")
    public PageResult<Order> findPage(@RequestBody Map<String,Object> searchMap,int page, int size){
        return  orderService.findPage(searchMap,page,size);
    }

    @GetMapping("/findById")
    public Order findById(String id){
        return orderService.findById(id);
    }


    @PostMapping("/add")
    public Result add(@RequestBody Order order){
        orderService.add(order);
        return new Result();
    }

    @PostMapping("/update")
    public Result update(@RequestBody Order order){
        orderService.update(order);
        return new Result();
    }

    @GetMapping("/delete")
    public Result delete(String id){
        orderService.delete(id);
        return new Result();
    }

    @GetMapping("/findOrderAll")
    public OrderAll findOrderAll(String id){
        return orderService.findOrderAll(id);
    }

    /**
     * 批量查询
     * @param ids
     * @return
     */
    @PostMapping("/findListByIds")
    public List<Order> findListByIds(@RequestBody String [] ids){
        return orderService.findListByIds(ids);
    }

    /**
     * 批量发货
     * @param orderList
     * @return
     */
    @PostMapping("/batchSend")
    public Result batchSend(@RequestBody List<Order> orderList){
        orderService.batchSend(orderList);
        return new Result();
    }

    @GetMapping("/merge")
    public Result merge(String orderId1,String orderId2){
        orderService.merge(orderId1,orderId2);
        return new Result();

    }





}
