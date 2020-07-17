package com.qingcheng.pojo.order;

import com.sun.org.apache.xpath.internal.operations.Or;

import java.io.Serializable;
import java.util.List;

public class OrderAll implements Serializable {
    private Order order;

    private List<OrderItem> orderItemList;

    public List<OrderItem> getOrderItemList() {
        return orderItemList;
    }

    public void setOrderItemList(List<OrderItem> orderItemList) {
        this.orderItemList = orderItemList;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }




}
