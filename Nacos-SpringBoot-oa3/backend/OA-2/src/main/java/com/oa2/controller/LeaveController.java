package com.oa2.controller;

import com.oa2.pojo.LeaveRequest;
import com.oa2.service.LeaveService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/leave")
@CrossOrigin
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @GetMapping("/my-records")
    public RESP myRecords(@RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                          @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                          HttpSession session) {
        return leaveService.listMine(currentPage, pageSize, session);
    }

    @PostMapping("/apply")
    public RESP apply(@RequestBody LeaveRequest leaveRequest, HttpSession session) {
        return leaveService.apply(leaveRequest, session);
    }

    @PutMapping("/{id}/cancel")
    public RESP cancel(@PathVariable("id") String id, HttpSession session) {
        return leaveService.cancel(id, session);
    }
}
