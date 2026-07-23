package com.oa7.service.Impl;

import com.oa7.dao.EmpDao;
import com.oa7.pojo.Emp;
import com.oa7.pojo.O;
import com.oa7.pojo.Sign;
import com.oa7.repository.SignElasticsearchRepository;
import com.oa7.service.SignService;
import com.oa7.util.DU;
import com.oa7.util.LocationUtil;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.UUID;


@Service
public class SignServiceElasticsearchImpl implements SignService {

    @Autowired
    private SignElasticsearchRepository signRepository;

    @Autowired
    private EmpDao empDao;

    private static final String STATE_SIGNED = "已签到";
    private static final String STATE_UNSIGNED = "未签到";
    private static final long ORPHAN_CLEANUP_INTERVAL_MS = 60_000L;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private volatile long lastOrphanCleanupAt = 0L;

    @Override
    @Transactional(readOnly = true)
    public RESP signed(int currentPage, int pageSize) {
        cleanupDeletedEmployeeSignsIfDue();
        Pageable pageable = pageRequest(currentPage, pageSize);
        Page<Sign> page = signRepository.findByStateOrderByTimestampDesc(STATE_SIGNED, pageable);
        return pageResp(page, currentPage);
    }

    @Override
    @Transactional(readOnly = true)
    public RESP unsigned(int currentPage, int pageSize) {
        return unsigned(currentPage, pageSize, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public RESP unsigned(int currentPage, int pageSize, String date, String signType) {
        return unsigned(currentPage, pageSize, date, signType, null);
    }

    @Override
    @Transactional(readOnly = true)
    public RESP unsigned(int currentPage, int pageSize, String date, String signType, String keyword) {
        cleanupDeletedEmployeeSignsIfDue();
        Pageable pageable = pageRequest(currentPage, pageSize);
        String day = normalize(date);
        String type = normalizeSignType(signType);
        Integer number = parseNumber(keyword);
        Page<Sign> page;
        if (number != null && !day.isEmpty() && !type.isEmpty()) {
            page = signRepository.findByNumberAndDateOnlyAndStateAndTypeOrderByTimestampDesc(number, day, STATE_UNSIGNED, type, pageable);
        } else if (number != null && !day.isEmpty()) {
            page = signRepository.findByNumberAndDateOnlyAndStateOrderByTimestampDesc(number, day, STATE_UNSIGNED, pageable);
        } else if (number != null) {
            page = signRepository.findByNumberAndStateOrderByTimestampDesc(number, STATE_UNSIGNED, pageable);
        } else if (!day.isEmpty() && !type.isEmpty()) {
            page = signRepository.findByDateOnlyAndStateAndTypeOrderByTimestampDesc(day, STATE_UNSIGNED, type, pageable);
        } else if (!day.isEmpty()) {
            page = signRepository.findByDateOnlyAndStateOrderByTimestampDesc(day, STATE_UNSIGNED, pageable);
        } else {
            page = signRepository.findByStateOrderByTimestampDesc(STATE_UNSIGNED, pageable);
        }
        return pageResp(page, currentPage);
    }

    @Override
    @Transactional(readOnly = true)
    public RESP todaySigned(int currentPage, int pageSize) {
        cleanupDeletedEmployeeSignsIfDue();
        String today = DU.getNowSortString();
        Pageable pageable = pageRequest(currentPage, pageSize);
        Page<Sign> page = signRepository.findByDateOnlyAndStateOrderByTimestampDesc(today, STATE_SIGNED, pageable);
        return pageResp(page, currentPage);
    }

    @Override
    @Transactional(readOnly = true)
    public RESP todayUnsigned(int currentPage, int pageSize) {
        return todayUnsigned(currentPage, pageSize, null);
    }

    @Override
    @Transactional(readOnly = true)
    public RESP todayUnsigned(int currentPage, int pageSize, String signType) {
        return todayUnsigned(currentPage, pageSize, signType, null);
    }

    @Override
    @Transactional(readOnly = true)
    public RESP todayUnsigned(int currentPage, int pageSize, String signType, String keyword) {
        ensureTodayTasks();
        cleanupDeletedEmployeeSignsIfDue();
        String today = DU.getNowSortString();
        Pageable pageable = pageRequest(currentPage, pageSize);
        String type = normalizeSignType(signType);
        Integer number = parseNumber(keyword);
        Page<Sign> page;
        if (number != null && !type.isEmpty()) {
            page = signRepository.findByNumberAndDateOnlyAndStateAndTypeOrderByTimestampDesc(number, today, STATE_UNSIGNED, type, pageable);
        } else if (number != null) {
            page = signRepository.findByNumberAndDateOnlyAndStateOrderByTimestampDesc(number, today, STATE_UNSIGNED, pageable);
        } else if (!type.isEmpty()) {
            page = signRepository.findByDateOnlyAndStateAndTypeOrderByTimestampDesc(today, STATE_UNSIGNED, type, pageable);
        } else {
            page = signRepository.findByDateOnlyAndStateOrderByTimestampDesc(today, STATE_UNSIGNED, pageable);
        }
        return pageResp(page, currentPage);
    }

    @Override
    @Transactional(readOnly = true)
    public RESP approve(String id) {
        Optional<Sign> optional = signRepository.findById(id);
        if (!optional.isPresent()) {
            return RESP.error(404, "考勤记录不存在");
        }
        Sign sign = optional.get();
        if (!STATE_UNSIGNED.equals(sign.getState())) {
            return RESP.error(400, "该记录不是未签到状态");
        }
        String today = DU.getNowSortString();
        String dateOnly = dateOnly(sign);
        if (!today.equals(dateOnly)) {
            return RESP.error(400, "只能补签今日记录");
        }
        sign.setState(STATE_SIGNED);
        sign.setSignDate(DU.formatDateToString(new Date()));
        sign.setTimestamp(System.currentTimeMillis());
        sign.setDateOnly(today);
        sign.setTag(0);
        if (!enrich(sign)) {
            signRepository.delete(sign);
            return RESP.error(400, "员工已不存在，已清理该考勤记录");
        }
        upsertAttendance(sign, today);
        return RESP.ok("补签成功");
    }

    @Override
    @Transactional(readOnly = true)
    public RESP dailyStatistics(int currentPage, int pageSize) {
        cleanupDeletedEmployeeSignsIfDue();
        List<Sign> all = enrichActive(signRepository.findAllByOrderByTimestampDesc());
        Map<String, int[]> grouped = new TreeMap<>(Collections.reverseOrder());
        for (Sign sign : all) {
            String date = dateOnly(sign);
            if (date == null || date.isEmpty()) {
                continue;
            }
            int[] counts = grouped.computeIfAbsent(date, k -> new int[2]);
            if (STATE_SIGNED.equals(sign.getState())) {
                counts[0]++;
            } else if (STATE_UNSIGNED.equals(sign.getState())) {
                counts[1]++;
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : grouped.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", entry.getKey());
            row.put("yc", entry.getValue()[0]);
            row.put("nc", entry.getValue()[1]);
            row.put("ne", entry.getValue()[0] + entry.getValue()[1]);
            rows.add(row);
        }
        return pageList(rows, currentPage, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public RESP dailyDetails(String date) {
        if (date == null || date.trim().isEmpty()) {
            return RESP.error(400, "日期不能为空");
        }
        String day = date.trim();
        cleanupDeletedEmployeeSignsIfDue();
        Map<String, Object> details = new HashMap<>();
        int morningSigned = enrichActive(signRepository.findByDateOnlyAndStateAndType(day, STATE_SIGNED, "a")).size();
        int morningUnsigned = enrichActive(signRepository.findByDateOnlyAndStateAndType(day, STATE_UNSIGNED, "a")).size();
        int eveningSigned = enrichActive(signRepository.findByDateOnlyAndStateAndType(day, STATE_SIGNED, "p")).size();
        int eveningUnsigned = enrichActive(signRepository.findByDateOnlyAndStateAndType(day, STATE_UNSIGNED, "p")).size();
        details.put("morningSignedCount", morningSigned);
        details.put("morningUnsignedCount", morningUnsigned);
        details.put("eveningSignedCount", eveningSigned);
        details.put("eveningUnsignedCount", eveningUnsigned);
        details.put("totalSignedCount", morningSigned + eveningSigned);
        details.put("totalUnsignedCount", morningUnsigned + eveningUnsigned);
        return RESP.ok(details);
    }

    @Override
    @Transactional(readOnly = true)
    public RESP statisticsChart() {
        ensureTodayTasks();
        cleanupDeletedEmployeeSignsIfDue();
        List<Sign> all = enrichActive(signRepository.findAllByOrderByTimestampDesc());
        Map<String, int[]> grouped = new TreeMap<>();
        for (Sign sign : all) {
            String date = dateOnly(sign);
            if (date == null || date.isEmpty()) {
                continue;
            }
            int[] counts = grouped.computeIfAbsent(date, k -> new int[6]);
            boolean isCheckIn = "a".equals(sign.getType());
            boolean isCheckOut = "p".equals(sign.getType());
            if (!isCheckIn && !isCheckOut) {
                continue;
            }
            int offset = isCheckIn ? 0 : 3;
            if (STATE_SIGNED.equals(sign.getState())) {
                counts[offset]++;
            } else if (STATE_UNSIGNED.equals(sign.getState())) {
                counts[offset + 1]++;
            }
            counts[offset + 2]++;
        }
        List<String> dates = new ArrayList<>(grouped.keySet());
        if (dates.size() > 5) {
            dates = dates.subList(dates.size() - 5, dates.size());
        }
        Map<String, Object> checkIn = new HashMap<>();
        Map<String, Object> checkOut = new HashMap<>();
        List<Integer> checkInSigned = new ArrayList<>();
        List<Integer> checkInUnsigned = new ArrayList<>();
        List<Integer> checkInTotal = new ArrayList<>();
        List<Integer> checkOutSigned = new ArrayList<>();
        List<Integer> checkOutUnsigned = new ArrayList<>();
        List<Integer> checkOutTotal = new ArrayList<>();
        for (String date : dates) {
            int[] counts = grouped.get(date);
            checkInSigned.add(counts[0]);
            checkInUnsigned.add(counts[1]);
            checkInTotal.add(counts[2]);
            checkOutSigned.add(counts[3]);
            checkOutUnsigned.add(counts[4]);
            checkOutTotal.add(counts[5]);
        }
        checkIn.put("signed", checkInSigned);
        checkIn.put("unsigned", checkInUnsigned);
        checkIn.put("total", checkInTotal);
        checkOut.put("signed", checkOutSigned);
        checkOut.put("unsigned", checkOutUnsigned);
        checkOut.put("total", checkOutTotal);

        Map<String, Object> payload = new HashMap<>();
        payload.put("dates", dates);
        payload.put("checkIn", checkIn);
        payload.put("checkOut", checkOut);
        return RESP.ok(payload);
    }

    private RESP pageResp(Page<Sign> page, int currentPage) {
        List<Sign> list = enrichActive(page.getContent());
        return RESP.ok(list, currentPage, (int) page.getTotalElements());
    }

    private Pageable pageRequest(int currentPage, int pageSize) {
        currentPage = Math.max(currentPage, 1);
        pageSize = Math.max(pageSize, 1);
        return PageRequest.of(currentPage - 1, pageSize);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeSignType(String signType) {
        String type = normalize(signType);
        return "a".equals(type) || "p".equals(type) ? type : "";
    }

    private Integer parseNumber(String keyword) {
        String value = normalize(keyword);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private <T> RESP pageList(List<T> list, int currentPage, int pageSize) {
        currentPage = Math.max(currentPage, 1);
        pageSize = Math.max(pageSize, 1);
        int total = list.size();
        int from = Math.min((currentPage - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        return RESP.ok(list.subList(from, to), currentPage, total);
    }

    private void ensureTodayTasks() {
        String today = DU.getNowSortString();
        List<Integer> numbers = empDao.selectAllEmpNumber();
        for (Integer number : numbers) {
            List<Sign> todayRecords = signRepository.findByNumberAndDateOnly(number, today);
            boolean hasMorning = false;
            boolean hasEvening = false;
            for (Sign sign : todayRecords) {
                if ("a".equals(sign.getType())) {
                    hasMorning = true;
                } else if ("p".equals(sign.getType())) {
                    hasEvening = true;
                }
            }
            if (!hasMorning) {
                Sign morningSign = createTask(number, DU.getNowAM(), "a", today);
                upsertAttendance(morningSign, today);
            }
            if (!hasEvening) {
                Sign eveningSign = createTask(number, DU.getNowPM(), "p", today);
                upsertAttendance(eveningSign, today);
            }
        }
    }

    private Sign createTask(int number, String signDate, String type, String dateOnly) {
        Sign sign = new Sign();
        sign.setId(attendanceId(number, dateOnly, type));
        sign.setNumber(number);
        sign.setSignDate(signDate);
        sign.setState(STATE_UNSIGNED);
        sign.setType(type);
        sign.setDateOnly(dateOnly);
        sign.setTimestamp(parseMillis(signDate));
        sign.setTag(1);
        enrich(sign);
        return sign;
    }

    private List<Sign> enrichActive(List<Sign> signs) {
        Map<Integer, Emp> activeEmployees = activeEmployeeMap();
        List<Sign> activeSigns = new ArrayList<>();
        List<Sign> orphanSigns = new ArrayList<>();
        for (Sign sign : signs) {
            Emp emp = activeEmployees.get(sign.getNumber());
            if (emp == null) {
                orphanSigns.add(sign);
                continue;
            }
            applyEmployee(sign, emp);
            sign.setTag(DU.getNowSortString().equals(dateOnly(sign)) && STATE_UNSIGNED.equals(sign.getState()) ? 1 : 0);
            activeSigns.add(sign);
        }
        deleteOrphanSigns(orphanSigns);
        return activeSigns;
    }

    private boolean enrich(Sign sign) {
        Emp emp = empDao.selectByEmpNumber(sign.getNumber());
        if (emp == null) {
            return false;
        }
        applyEmployee(sign, emp);
        return true;
    }

    private void applyEmployee(Sign sign, Emp emp) {
        sign.setName(emp.getName());
        sign.setDept_name(emp.getDept_name());
        if (sign.getDateOnly() == null || sign.getDateOnly().isEmpty()) {
            sign.setDateOnly(dateOnly(sign));
        }
        if (sign.getTimestamp() == null) {
            sign.setTimestamp(parseMillis(sign.getSignDate()));
        }
    }

    private Map<Integer, Emp> activeEmployeeMap() {
        Map<Integer, Emp> activeEmployees = new HashMap<>();
        for (Emp emp : empDao.selectAllEmpWithDept()) {
            activeEmployees.put(emp.getNumber(), emp);
        }
        return activeEmployees;
    }

    private void cleanupDeletedEmployeeSignsIfDue() {
        long now = System.currentTimeMillis();
        if (now - lastOrphanCleanupAt < ORPHAN_CLEANUP_INTERVAL_MS) {
            return;
        }
        synchronized (this) {
            if (now - lastOrphanCleanupAt < ORPHAN_CLEANUP_INTERVAL_MS) {
                return;
            }
            Map<Integer, Emp> activeEmployees = activeEmployeeMap();
            List<Sign> orphanSigns = new ArrayList<>();
            for (Sign sign : signRepository.findAllByOrderByTimestampDesc()) {
                if (!activeEmployees.containsKey(sign.getNumber())) {
                    orphanSigns.add(sign);
                }
            }
            deleteOrphanSigns(orphanSigns);
            lastOrphanCleanupAt = now;
        }
    }

    private void deleteOrphanSigns(List<Sign> orphanSigns) {
        if (!orphanSigns.isEmpty()) {
            signRepository.deleteAll(orphanSigns);
        }
    }

    private void upsertAttendance(Sign sign, String dateOnly) {
        if (sign == null) {
            return;
        }
        List<Sign> sameSlotRecords = signRepository.findByNumberAndDateOnly(sign.getNumber(), dateOnly);
        sameSlotRecords.removeIf(record -> !sign.getType().equals(record.getType()));
        if (!sameSlotRecords.isEmpty()) {
            signRepository.deleteAll(sameSlotRecords);
        }
        sign.setId(attendanceId(sign.getNumber(), dateOnly, sign.getType()));
        sign.setDateOnly(dateOnly);
        signRepository.save(sign);
    }

    private String attendanceId(int number, String dateOnly, String type) {
        return number + ":" + dateOnly + ":" + type;
    }

    private String dateOnly(Sign sign) {
        if (sign.getDateOnly() != null && !sign.getDateOnly().isEmpty()) {
            return sign.getDateOnly();
        }
        String signDate = sign.getSignDate();
        if (signDate != null && signDate.length() >= 10) {
            return signDate.substring(0, 10);
        }
        return "";
    }

    private long parseMillis(String signDate) {
        if (signDate == null) {
            return System.currentTimeMillis();
        }
        try {
            return DU.parseDate(signDate).getTime();
        } catch (Exception ignored) {
            return System.currentTimeMillis();
        }
    }
}
