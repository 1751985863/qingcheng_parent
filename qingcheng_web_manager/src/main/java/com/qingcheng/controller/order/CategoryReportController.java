package com.qingcheng.controller.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.service.order.CategoryReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categoryReport")
public class CategoryReportController {
    @Reference
    private CategoryReportService categoryReportService;

    /**
     * 昨天的销售数据统计
     * @return
     */
    @GetMapping("/yesterday")
    public List<CategoryReport> yesterday(){
        LocalDate localDate=LocalDate.now().minusDays(1);//得到昨天的时间
        return categoryReportService.categoryReport(localDate);
    }

    /**
     * 根据时间范围获取 categoryId1的销量总和
     * @param date1
     * @param date2
     * @return
     */
    @GetMapping("/category1Count")
    public List<Map> category1Count(String date1,String date2){
        return categoryReportService.category1Count(date1,date2);

    }
}
