package com.oa7.service;

import com.oa7.pojo.Admin;
import com.oa7.util.RESP;

import jakarta.servlet.http.HttpSession;

/**
 * @name: chenle
 * @Date: 2021/12/3 14:43
 * @Author: IAO
 * @Description: ...
 */
public interface AdmService {

    RESP login(Admin admin, HttpSession session);

    String register(Admin admin);
}
