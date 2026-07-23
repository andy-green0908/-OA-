package com.oa7.service.Impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.pojo.Department;
import com.oa7.service.DeptService;
import com.oa7.dao.DeptDao;
import com.oa7.util.RESP;
import com.oa7.util.JediPoolUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;

/**
 * @name: chenle
 * @Date: 2021/12/3 15:58
 * @Author: IAO
 * @Description: ...
 */

@Service
public class DeptServiceImpl implements DeptService {

    private static final String CACHE_KEY = "oa:admin:departments";

    @Autowired
    private DeptDao deptDao;

    @Autowired
    private JediPoolUtil jediPoolUtil;

    @Override
    @Transactional(readOnly = true)
    public RESP list(int currentPage, int pageSize) {
        List<Department> all = loadAllDepartments();
        return page(all, currentPage, pageSize);
    }

    @Override
    public RESP add(Department department, int currentPage, int pageSize) {
        if (department == null || department.getDept_name() == null || department.getDept_name().trim().isEmpty()) {
            return RESP.error(400, "部门名称不能为空");
        }
        department.setDept_name(department.getDept_name().trim());
        if (deptDao.selectByName(department) != null) {
            return RESP.error(400, "部门名称已存在");
        }
        int rows = deptDao.addDept(department);
        if (rows <= 0) {
            return RESP.error("添加部门失败");
        }
        evictCache();
        return list(currentPage, pageSize);
    }

    @Override
    public RESP update(Department department, int currentPage, int pageSize) {
        if (department == null || department.getDept_id() <= 0
                || department.getDept_name() == null || department.getDept_name().trim().isEmpty()) {
            return RESP.error(400, "部门参数不完整");
        }
        department.setDept_name(department.getDept_name().trim());
        Department sameName = deptDao.selectByName(department);
        if (sameName != null && sameName.getDept_id() != department.getDept_id()) {
            return RESP.error(400, "部门名称已存在");
        }
        int rows = deptDao.updateDeptNameById(department);
        if (rows <= 0) {
            return RESP.error("更新部门失败");
        }
        evictCache();
        return list(currentPage, pageSize);
    }

    @Override
    public RESP delete(int deptId, int currentPage, int pageSize) {
        if (deptId <= 0) {
            return RESP.error(400, "部门参数不完整");
        }
        if (deptDao.countEmployeesByDeptId(deptId) > 0) {
            return RESP.error(400, "该部门仍有在职人员，不能删除");
        }
        int rows = deptDao.deleteDeptById(deptId);
        if (rows <= 0) {
            return RESP.error("删除部门失败");
        }
        evictCache();
        return list(currentPage, pageSize);
    }

    private List<Department> loadAllDepartments() {
        String cached = getCache();
        if (cached != null && !cached.isEmpty()) {
            try {
                return JSON.parseObject(cached, new TypeReference<List<Department>>() {});
            } catch (Exception ignored) {
                evictCache();
            }
        }
        List<Department> list = deptDao.selectByPageHelper();
        setCache(list);
        return list;
    }

    private RESP page(List<Department> list, int currentPage, int pageSize) {
        if (list == null) {
            list = Collections.emptyList();
        }
        currentPage = Math.max(currentPage, 1);
        pageSize = Math.max(pageSize, 1);
        int total = list.size();
        int from = Math.min((currentPage - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        return RESP.ok(list.subList(from, to), currentPage, total);
    }

    private String getCache() {
        try (Jedis jedis = jediPoolUtil.getJedis()) {
            return jedis.get(CACHE_KEY);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void setCache(List<Department> list) {
        try (Jedis jedis = jediPoolUtil.getJedis()) {
            jedis.setex(CACHE_KEY, 300, JSON.toJSONString(list));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void evictCache() {
        try (Jedis jedis = jediPoolUtil.getJedis()) {
            jedis.del(CACHE_KEY);
        } catch (Exception ignored) {
        }
    }
}
