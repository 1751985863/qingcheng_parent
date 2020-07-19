package com.qingcheng.dao;

import com.qingcheng.pojo.order.CategoryReport;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CategoryReportMapper extends Mapper<CategoryReport> {
    @Select("SELECT oi.`category_id1` categoryId1,oi.`category_id2` categoryId2,oi.`category_id3` categoryId3,DATE_FORMAT( o.`pay_time`,'%Y-%m-%d') countDate,SUM(oi.`num`) num,SUM(oi.`pay_money`) money " +
            "FROM tb_order_item oi,tb_order o " +
            "WHERE oi.`order_id`=o.`id` AND o.`pay_status`='1' AND o.`is_delete`='0'  AND  DATE_FORMAT( o.`pay_time`,'%Y-%m-%d')=#{date} " +
            "GROUP BY oi.`category_id1`,oi.`category_id2`,oi.`category_id3`,DATE_FORMAT( o.`pay_time`,'%Y-%m-%d')")
    public List<CategoryReport> categoryReport(@Param("date") LocalDate date);//自定义方法，注意这里需要将下划线格式转化为驼峰格式。

    @Select("SELECT  category_id1 categoryId1,v.name name,SUM(num) num ,SUM(money) money " +
            "FROM tb_category_report o,v_tb_category v " +
            "WHERE count_date>=#{date1} AND count_date<=#{date2} AND o.category_id1=v.id " +
            "GROUP BY category_id1")
    public List<Map> category1Count(@Param("date1") String date1,@Param("date2") String date2);
}
