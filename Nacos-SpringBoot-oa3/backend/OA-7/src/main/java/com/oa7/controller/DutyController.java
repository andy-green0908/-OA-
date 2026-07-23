package com.oa7.controller;

import com.oa7.service.DutyService;
import com.oa7.pojo.Duty;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员端 - 职务管理控制器
 */
@RestController
@RequestMapping("/duties")
@CrossOrigin
public class DutyController {

    @Autowired
    private DutyService dutyService;

    @GetMapping
    public RESP list(@RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                     @RequestParam(name = "pageSize", defaultValue = "1000") int pageSize) {
        return dutyService.list(currentPage, pageSize);
    }

    @PostMapping
    public String add(@RequestBody Duty duty) {
        return dutyService.add(duty);
    }

    @PutMapping("/{dutyId}")
    public String update(@PathVariable("dutyId") int dutyId, @RequestBody Duty duty) {
        duty.setDuty_id(dutyId);
        return dutyService.update(duty);
    }

    @DeleteMapping("/{dutyId}")
    public String delete(@PathVariable("dutyId") int dutyId) {
        return dutyService.delete(dutyId);
    }
}
