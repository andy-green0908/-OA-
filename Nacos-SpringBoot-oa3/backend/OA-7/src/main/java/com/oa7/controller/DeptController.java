package com.oa7.controller;

import com.oa7.service.DeptService;
import com.oa7.pojo.Department;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员端 - 部门管理控制器
 */
@RestController
@RequestMapping("/departments")
@CrossOrigin
public class DeptController {

    @Autowired
    private DeptService deptService;

    @GetMapping
    public RESP list(@RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                     @RequestParam(name = "pageSize", defaultValue = "1000") int pageSize) {
        return deptService.list(currentPage, pageSize);
    }

    @PostMapping
    public RESP add(@RequestBody Department department,
                    @RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                    @RequestParam(name = "pageSize", defaultValue = "8") int pageSize) {
        return deptService.add(department, currentPage, pageSize);
    }

    @PutMapping("/{deptId}")
    public RESP update(@PathVariable("deptId") int deptId,
                       @RequestBody Department department,
                       @RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                       @RequestParam(name = "pageSize", defaultValue = "8") int pageSize) {
        department.setDept_id(deptId);
        return deptService.update(department, currentPage, pageSize);
    }

    @DeleteMapping("/{deptId}")
    public RESP delete(@PathVariable("deptId") int deptId,
                       @RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                       @RequestParam(name = "pageSize", defaultValue = "8") int pageSize) {
        return deptService.delete(deptId, currentPage, pageSize);
    }
}
