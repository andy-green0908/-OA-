package com.oa7.controller;

import com.oa7.service.SignService;
import com.oa7.pojo.Sign;
import com.oa7.util.DU;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员端 - 考勤管理控制器
 */
@RestController
@RequestMapping("/attendance")
@CrossOrigin
public class SignController {

    @Autowired
    private SignService signService;

    @GetMapping("/signed")
    public RESP signed(@RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                       @RequestParam(name = "pageSize", defaultValue = "8") int pageSize) {
        return signService.signed(currentPage, pageSize);
    }

    @GetMapping("/unsigned")
    public RESP unsigned(@RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                         @RequestParam(name = "pageSize", defaultValue = "8") int pageSize,
                         @RequestParam(name = "date", required = false) String date,
                         @RequestParam(name = "signType", required = false) String signType,
                         @RequestParam(name = "keyword", required = false) String keyword) {
        return signService.unsigned(currentPage, pageSize, date, signType, keyword);
    }

    @GetMapping("/today/signed")
    public RESP todaySigned(@RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                            @RequestParam(name = "pageSize", defaultValue = "8") int pageSize) {
        return signService.todaySigned(currentPage, pageSize);
    }

    @GetMapping("/today/unsigned")
    public RESP todayUnsigned(@RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                              @RequestParam(name = "pageSize", defaultValue = "8") int pageSize,
                              @RequestParam(name = "signType", required = false) String signType,
                              @RequestParam(name = "keyword", required = false) String keyword) {
        return signService.todayUnsigned(currentPage, pageSize, signType, keyword);
    }

    @PutMapping("/{id}/approve")
    public RESP approve(@PathVariable("id") String id) {
        return signService.approve(id);
    }

    @GetMapping("/daily-statistics")
    public RESP dailyStatistics(@RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                                @RequestParam(name = "pageSize", defaultValue = "8") int pageSize) {
        return signService.dailyStatistics(currentPage, pageSize);
    }

    @GetMapping("/daily-details")
    public RESP dailyDetails(@RequestParam("date") String date) {
        return signService.dailyDetails(date);
    }

    @GetMapping("/statistics/chart")
    public RESP statisticsChart() {
        return signService.statisticsChart();
    }
}
