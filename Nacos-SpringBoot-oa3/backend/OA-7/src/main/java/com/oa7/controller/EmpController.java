package com.oa7.controller;

import com.oa7.pojo.Emp;
import com.oa7.service.EmpService;
import com.oa7.dao.EmpDao;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员端 - 员工管理控制器
 */
@RestController
@RequestMapping("/employees")
@CrossOrigin
public class EmpController {

    @Autowired
    private EmpService empService;

    @GetMapping
    public RESP list(@RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                     @RequestParam(name = "pageSize", defaultValue = "8") int pageSize,
                     @RequestParam(name = "keyword", required = false) String keyword) {
        return empService.list(currentPage, pageSize, keyword);
    }

    @GetMapping("/departments")
    public RESP departments() {
        return empService.departments();
    }

    @GetMapping("/duties")
    public RESP duties() {
        return empService.duties();
    }

    @PostMapping
    public String add(@RequestBody Emp emp) {
        return empService.add(emp);
    }

    @PutMapping("/{number}")
    public RESP update(@PathVariable("number") int number,
                       @RequestBody Emp emp,
                       @RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                       @RequestParam(name = "pageSize", defaultValue = "8") int pageSize,
                       @RequestParam(name = "keyword", required = false) String keyword) {
        emp.setNumber(number);
        return empService.update(emp, currentPage, pageSize, keyword);
    }

    @DeleteMapping("/{number}")
    public String delete(@PathVariable("number") int number) {
        Emp emp = new Emp();
        emp.setNumber(number);
        return empService.delete(emp);
    }
}
