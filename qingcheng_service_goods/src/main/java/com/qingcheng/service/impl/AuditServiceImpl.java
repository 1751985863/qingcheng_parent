package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.AuditMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.Audit;
import com.qingcheng.service.goods.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.Map;

@Service
public class AuditServiceImpl implements AuditService {

    @Autowired
    private AuditMapper auditMapper;

    /**
     * 返回全部记录
     * @return
     */
    public List<Audit> findAll() {
        return auditMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Audit> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Audit> audits = (Page<Audit>) auditMapper.selectAll();
        return new PageResult<Audit>(audits.getTotal(),audits.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Audit> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return auditMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Audit> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Audit> audits = (Page<Audit>) auditMapper.selectByExample(example);
        return new PageResult<Audit>(audits.getTotal(),audits.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Audit findById(String id) {
        return auditMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param audit
     */
    public void add(Audit audit) {
        auditMapper.insert(audit);
    }

    /**
     * 修改
     * @param audit
     */
    public void update(Audit audit) {
        auditMapper.updateByPrimaryKeySelective(audit);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(String id) {
        auditMapper.deleteByPrimaryKey(id);
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Audit.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 主键
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andLike("id","%"+searchMap.get("id")+"%");
            }
            // 对应商品的id
            if(searchMap.get("spuId")!=null && !"".equals(searchMap.get("spuId"))){
                criteria.andLike("spuId","%"+searchMap.get("spuId")+"%");
            }
            // 1通过 2未通过
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andLike("status","%"+searchMap.get("status")+"%");
            }
            // 操作员
            if(searchMap.get("operator")!=null && !"".equals(searchMap.get("operator"))){
                criteria.andLike("operator","%"+searchMap.get("operator")+"%");
            }
            // 反馈详情
            if(searchMap.get("message")!=null && !"".equals(searchMap.get("message"))){
                criteria.andLike("message","%"+searchMap.get("message")+"%");
            }


        }
        return example;
    }

}
