package com.qingcheng.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.business.Ad;
import com.qingcheng.service.business.AdService;
import com.qingcheng.service.goods.BrandService;
import com.qingcheng.service.goods.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {
    @Reference
    private AdService adService;
/*    @Reference
    private BrandService brandService;*/

    @Reference
    private CategoryService categoryService;

    @GetMapping("/index")
    public String index(Model model){
        //得到首页广告轮播图列表
        List<Ad> index_lb = adService.findByPosition("web_index_lb");
        List<Map> categoryTree = categoryService.findCategoryTree();
        model.addAttribute("tree",categoryTree);
        return "index";



    }
    @GetMapping("/test")
    @ResponseBody
    public List<Map> test(){
        //得到首页广告轮播图列表
      /*  List<Ad> index_lb = adService.findByPosition("web_index_lb");*/
        List<Map> categoryTree = categoryService.findCategoryTree();
        return categoryTree;



    }
    @GetMapping("/findAll")
    @ResponseBody
    public List<Ad> findAll(){
        //得到首页广告轮播图列表
        List<Ad> all = adService.findAll();

        return all;

    }
}
