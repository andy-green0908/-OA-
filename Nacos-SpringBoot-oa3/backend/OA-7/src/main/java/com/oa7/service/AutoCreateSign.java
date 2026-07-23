package com.oa7.service;

import com.oa7.dao.EmpDao;
import com.oa7.pojo.Emp;
import com.oa7.pojo.Sign;
import com.oa7.repository.SignElasticsearchRepository;
import com.oa7.util.DU;
import com.oa7.util.JediPoolUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * @name: chenle
 * @Date: 2021/12/25 5:24
 * @Author: IAO
 * @Description: 定时自动更新员工考勤
 */
@Configuration
//开启定时任务
@EnableScheduling
public class AutoCreateSign {
    private static final Logger log = LoggerFactory.getLogger(AutoCreateSign.class);
    private static final String CREATE_JOB_LOCK_KEY = "oa:job:attendance:create";
    private static final String CLEANUP_JOB_LOCK_KEY = "oa:job:attendance:cleanup";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private EmpDao empDao;

    @Autowired
    private SignElasticsearchRepository signRepository;

    @Autowired
    private JediPoolUtil jediPoolUtil;

    @Value("${attendance.retention-months:12}")
    private int retentionMonths;

    //每日凌晨零点执行，生成员工签到任务
    @Scheduled(cron = "0 0 0 * * ?")
    public void create() {
        String lockValue = UUID.randomUUID().toString();
        if (!tryLock(CREATE_JOB_LOCK_KEY, lockValue, 1800)) {
            log.info("考勤生成任务已被其他实例执行");
            return;
        }
        try {
            String today = DU.getNowSortString();
            List<Integer> list = empDao.selectAllEmpNumber();
            for (int n : list) {
                List<Sign> todayRecords = signRepository.findByNumberAndDateOnly(n, today);
                if (findByType(todayRecords, "a") == null) {
                    upsertAttendance(createEsSign(n, DU.getNowAM(), "a", today), today);
                }
                if (findByType(todayRecords, "p") == null) {
                    upsertAttendance(createEsSign(n, DU.getNowPM(), "p", today), today);
                }
            }
        } finally {
            unlock(CREATE_JOB_LOCK_KEY, lockValue);
        }
    }

    @Scheduled(cron = "0 15 0 1 * ?")
    public void cleanupExpiredAttendance() {
        String lockValue = UUID.randomUUID().toString();
        if (!tryLock(CLEANUP_JOB_LOCK_KEY, lockValue, 1800)) {
            log.info("考勤清理任务已被其他实例执行");
            return;
        }
        try {
            LocalDate cutoffDate = LocalDate.now().minusMonths(Math.max(1, retentionMonths));
            String cutoff = cutoffDate.format(DATE_FORMATTER);
            List<Sign> expired = signRepository.findAllByOrderByTimestampDesc().stream()
                    .filter(sign -> sign.getDateOnly() != null
                            && !sign.getDateOnly().isBlank()
                            && sign.getDateOnly().compareTo(cutoff) < 0)
                    .toList();
            if (!expired.isEmpty()) {
                signRepository.deleteAll(expired);
                log.info("已清理 {} 条过期考勤记录，保留截止日期 {}", expired.size(), cutoff);
            }
        } finally {
            unlock(CLEANUP_JOB_LOCK_KEY, lockValue);
        }
    }

    private Sign createEsSign(int number, String signDate, String type, String today) {
        Sign sign = new Sign();
        sign.setId(attendanceId(number, today, type));
        sign.setSignDate(signDate);
        sign.setNumber(number);
        sign.setState("未签到");
        sign.setType(type);
        sign.setDateOnly(today);
        sign.setTimestamp(System.currentTimeMillis());
        sign.setTag(1);
        try {
            Emp emp = empDao.selectByEmpNumber(number);
            if (emp != null) {
                sign.setName(emp.getName());
                sign.setDept_name(emp.getDept_name());
            }
        } catch (Exception ignored) {
        }
        return sign;
    }

    private Sign findByType(List<Sign> records, String type) {
        if (records == null || type == null) {
            return null;
        }
        for (Sign sign : records) {
            if (type.equals(sign.getType())) {
                return sign;
            }
        }
        return null;
    }

    private void upsertAttendance(Sign sign, String dateOnly) {
        if (sign == null) {
            return;
        }
        sign.setId(attendanceId(sign.getNumber(), dateOnly, sign.getType()));
        sign.setDateOnly(dateOnly);
        signRepository.save(sign);
    }

    private String attendanceId(int number, String dateOnly, String type) {
        return number + ":" + dateOnly + ":" + type;
    }

    private boolean tryLock(String key, String value, int seconds) {
        try (Jedis jedis = jediPoolUtil.getJedis()) {
            return "OK".equals(jedis.set(key, value, SetParams.setParams().nx().ex(seconds)));
        } catch (Exception e) {
            log.warn("获取考勤锁失败: {}", e.getMessage());
            return false;
        }
    }

    private void unlock(String key, String value) {
        try (Jedis jedis = jediPoolUtil.getJedis()) {
            if (value.equals(jedis.get(key))) {
                jedis.del(key);
            }
        } catch (Exception e) {
            log.warn("释放考勤锁失败: {}", e.getMessage());
        }
    }
}
