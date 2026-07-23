package com.oa7.controller;

import com.oa7.service.LeaveService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leave")
@CrossOrigin
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @GetMapping
    public RESP list(@RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                     @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                     @RequestParam(name = "status", required = false) String status,
                     @RequestParam(name = "keyword", required = false) String keyword) {
        return leaveService.list(currentPage, pageSize, status, keyword);
    }

    @PutMapping("/{id}/approve")
    public RESP approve(@PathVariable("id") String id) {
        return leaveService.approve(id);
    }

    @PutMapping("/{id}/reject")
    public RESP reject(@PathVariable("id") String id) {
        return leaveService.reject(id);
    }
}
