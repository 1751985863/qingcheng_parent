package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.*;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.*;
import com.qingcheng.service.goods.SpuService;
import com.qingcheng.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service(interfaceClass = SpuService.class)//写上这句话才能使得事务生效
public class SpuServiceImpl implements SpuService {

    @Autowired
    private SpuMapper spuMapper;

    /**
     * 返回全部记录
     * @return
     */
    public List<Spu> findAll() {
        return spuMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Spu> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectAll();
        return new PageResult<Spu>(spus.getTotal(),spus.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Spu> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return spuMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Spu> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectByExample(example);
        return new PageResult<Spu>(spus.getTotal(),spus.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Spu findById(String id) {
        return spuMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param spu
     */
    public void add(Spu spu) {
        spuMapper.insert(spu);
    }

    /**
     * 修改
     * @param spu
     */
    public void update(Spu spu) {
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(String id) {
        spuMapper.deleteByPrimaryKey(id);
    }

    /**
     * 保存商品spu以及sku列
     * @param goods
     */
    @Autowired
    private IdWorker idWorker;//注入雪花算法实例
    @Autowired
    private CategoryMapper categoryMapper;//注入分类dao
    @Autowired
    private BrandMapper brandMapper;//注入品牌dao
    @Autowired
    private SkuMapper skuMapper;//注入sku dao
    @Autowired
    private CategoryBrandMapper categoryBrandMapper;
    //当多个mapper进行操作时要开启事务
    @Transactional
    public void save(Goods goods) {
        /*增加spu*/
        Spu spu = goods.getSpu();
        if (spu.getId()==null){//新增
            spu.setId(idWorker.nextId()+"");//设置id
            spuMapper.insert(spu);
        }else {//修改
            //先删除之前的sku列表
            Example example = new Example(Sku.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("spuId", spu.getId());
            skuMapper.deleteByExample(example);//删除完毕后再更新操作
            spuMapper.updateByPrimaryKey(spu);//

        }


        /*增加sku*/
        Category category = categoryMapper.selectByPrimaryKey(spu.getCategory3Id());//获取三级分类对象
        Brand brand = brandMapper.selectByPrimaryKey(spu.getBrandId());//获取品牌对象
        Date date=new Date();//获取现在的时间
        List<Sku> skuList = goods.getSkuList();
        for (Sku sku : skuList) {
            //设置id
            if (sku.getId()==null) {//新增
                sku.setId(idWorker.nextId() + "");
                sku.setCreateTime(date);
            }
            if (sku.getSpec()==null||"".equals(sku.getSpec())){//未启用sku规格的容错处理
                sku.setSpec("{}");
            }

            //设置更新时间
            sku.setUpdateTime(date);
            //设置spu_id
            sku.setSpuId(spu.getId());
            //设置分类id、名称
            sku.setCategoryId(spu.getCategory3Id());
            sku.setCategoryName(category.getName());
            //设置品牌相关
            sku.setBrandName(brand.getName());
            //设置销量
            sku.setSaleNum(0);
            //设置评论数
            sku.setCommentNum(0);
            //设置名字
            Map<String,String> jsonObject = JSON.parseObject(sku.getSpec(),Map.class);//把字符串转化为json,再转化为map
            String name = spu.getName();//获取spu的名字
            for (String value : jsonObject.values()) {
                name+=" "+value;
            }
            sku.setName(name);//设置sku的名字

            skuMapper.insert(sku);

            //建立品牌和分类的关系
            CategoryBrand categoryBrand=new CategoryBrand();
            categoryBrand.setBrandId(spu.getBrandId());
            categoryBrand.setCategoryId(spu.getCategory3Id());
            int count = categoryBrandMapper.selectCount(categoryBrand);
            if (count==0){
                categoryBrandMapper.insert(categoryBrand);
            }


        }


    }

    public Goods findGoodsById(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        Example example=new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("spuId",id);
        List<Sku> skus = skuMapper.selectByExample(example);
        Goods goods=new Goods();
        goods.setSkuList(skus);
        goods.setSpu(spu);
        return goods;
    }
    @Autowired
    private AuditMapper auditMapper;
    @Transactional
    public void audit(String id, String status, String message) {
        //审核商品
        Spu spu=new Spu();
        spu.setId(id);
        spu.setStatus(status);
        if (id=="1"){//如果审核通过，则自动上架
            spu.setIsMarketable("1");
        }
        spuMapper.updateByPrimaryKeySelective(spu);
        //商品审核记录
        Audit audit=new Audit();
        audit.setId(idWorker.nextId()+"");
        audit.setOperator("admin");
        audit.setStatus(status);
        if (status=="1"){
            audit.setMessage("");
        }else {
            audit.setMessage(message);
        }
        audit.setSpuId(id);
        audit.setAuditTime(new Date());
        auditMapper.insert(audit);
        //商品日志表
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 主键
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andLike("id","%"+searchMap.get("id")+"%");
            }
            // 货号
            if(searchMap.get("sn")!=null && !"".equals(searchMap.get("sn"))){
                criteria.andLike("sn","%"+searchMap.get("sn")+"%");
            }
            // SPU名
            if(searchMap.get("name")!=null && !"".equals(searchMap.get("name"))){
                criteria.andLike("name","%"+searchMap.get("name")+"%");
            }
            // 副标题
            if(searchMap.get("caption")!=null && !"".equals(searchMap.get("caption"))){
                criteria.andLike("caption","%"+searchMap.get("caption")+"%");
            }
            // 图片
            if(searchMap.get("image")!=null && !"".equals(searchMap.get("image"))){
                criteria.andLike("image","%"+searchMap.get("image")+"%");
            }
            // 图片列表
            if(searchMap.get("images")!=null && !"".equals(searchMap.get("images"))){
                criteria.andLike("images","%"+searchMap.get("images")+"%");
            }
            // 售后服务
            if(searchMap.get("saleService")!=null && !"".equals(searchMap.get("saleService"))){
                criteria.andLike("saleService","%"+searchMap.get("saleService")+"%");
            }
            // 介绍
            if(searchMap.get("introduction")!=null && !"".equals(searchMap.get("introduction"))){
                criteria.andLike("introduction","%"+searchMap.get("introduction")+"%");
            }
            // 规格列表
            if(searchMap.get("specItems")!=null && !"".equals(searchMap.get("specItems"))){
                criteria.andLike("specItems","%"+searchMap.get("specItems")+"%");
            }
            // 参数列表
            if(searchMap.get("paraItems")!=null && !"".equals(searchMap.get("paraItems"))){
                criteria.andLike("paraItems","%"+searchMap.get("paraItems")+"%");
            }
            // 是否上架
            if(searchMap.get("isMarketable")!=null && !"".equals(searchMap.get("isMarketable"))){
                criteria.andLike("isMarketable","%"+searchMap.get("isMarketable")+"%");
            }
            // 是否启用规格
            if(searchMap.get("isEnableSpec")!=null && !"".equals(searchMap.get("isEnableSpec"))){
                criteria.andLike("isEnableSpec","%"+searchMap.get("isEnableSpec")+"%");
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andLike("isDelete","%"+searchMap.get("isDelete")+"%");
            }
            // 审核状态
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andLike("status","%"+searchMap.get("status")+"%");
            }

            // 品牌ID
            if(searchMap.get("brandId")!=null ){
                criteria.andEqualTo("brandId",searchMap.get("brandId"));
            }
            // 一级分类
            if(searchMap.get("category1Id")!=null ){
                criteria.andEqualTo("category1Id",searchMap.get("category1Id"));
            }
            // 二级分类
            if(searchMap.get("category2Id")!=null ){
                criteria.andEqualTo("category2Id",searchMap.get("category2Id"));
            }
            // 三级分类
            if(searchMap.get("category3Id")!=null ){
                criteria.andEqualTo("category3Id",searchMap.get("category3Id"));
            }
            // 模板ID
            if(searchMap.get("templateId")!=null ){
                criteria.andEqualTo("templateId",searchMap.get("templateId"));
            }
            // 运费模板id
            if(searchMap.get("freightId")!=null ){
                criteria.andEqualTo("freightId",searchMap.get("freightId"));
            }
            // 销量
            if(searchMap.get("saleNum")!=null ){
                criteria.andEqualTo("saleNum",searchMap.get("saleNum"));
            }
            // 评论数
            if(searchMap.get("commentNum")!=null ){
                criteria.andEqualTo("commentNum",searchMap.get("commentNum"));
            }

        }
        return example;
    }

}
