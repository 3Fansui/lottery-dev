package com.itheima.prize.api.action;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itheima.prize.commons.config.RedisKeys;
import com.itheima.prize.commons.db.entity.CardUser;
import com.itheima.prize.commons.db.mapper.CardUserMapper;
import com.itheima.prize.commons.db.service.CardUserService;
import com.itheima.prize.commons.utils.ApiResult;
import com.itheima.prize.commons.utils.PasswordUtil;
import com.itheima.prize.commons.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping(value = "/api")
@Api(tags = {"登录模块"})
@Slf4j
public class LoginController {
    @Autowired
    private CardUserService userService;

    @Autowired
    private RedisUtil redisUtil;

    @PostMapping("/login")
    @ApiOperation(value = "登录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "account", value = "用户名", required = true),
            @ApiImplicitParam(name = "password", value = "密码", required = true)
    })
    public ApiResult login(HttpServletRequest request, @RequestParam String account, @RequestParam String password) {
        //TODO
        log.info("登录模块运行:{}", account);
        //检查登录尝试此时  RedisKeys.USERLOGINTIMES 是常量
        String loginAttemptKey = RedisKeys.USERLOGINTIMES + account;
        Integer loginAttempts = (Integer) redisUtil.get(loginAttemptKey);

        if (loginAttempts != null && loginAttempts >= 5) {
            return new ApiResult<>(0, "密码错误5次，请5分钟后再登陆", null);
        }

        //对密码加密
        password = PasswordUtil.encodePassword(password);

        //查数据库
        CardUser user = userService.getOne(new LambdaQueryWrapper<CardUser>().eq(CardUser::getUname, account), false);

        //另一种写法
        /*QueryWrapper<CardUser> wrapper = new QueryWrapper<>();
        wrapper.eq("uname",account).eq("passwd",PasswordUtil.encodePassword(password));
        List<CardUser> users = userService.list(wrapper);*/

        if (user != null) {
            //对比密码
            if (password.equals(user.getPasswd())) {
                // 登录成功
                // 清除登录尝试记录
                redisUtil.del(loginAttemptKey);

                // 设置session
                HttpSession session = request.getSession();
                CardUser safeUser = new CardUser();
                BeanUtils.copyProperties(user, safeUser);
                safeUser.setPasswd(null); // 脱敏处理
                safeUser.setIdcard(null);
                session.setAttribute("user", safeUser);

                return new ApiResult<>(1, "登录成功", safeUser);
            } else {
                //不对，redis计数，错误次数

                /*loginAttempts = loginAttempts == null ? 1 : loginAttempts + 1;
                redisUtil.set(loginAttemptKey, loginAttempts, 300);*/

                //另一种写法
                redisUtil.incr(loginAttemptKey,1);
                redisUtil.expire(loginAttemptKey,60 * 5);

                return new ApiResult<>(0, "账户名或密码错误", null);

            }
        }

        return new ApiResult<>(0, "用户不存在", null);
    }


    @GetMapping("/logout")
    @ApiOperation(value = "退出")
    public ApiResult logout(HttpServletRequest request) {
        //TODO
        log.info("退出登录模块运行:{}", request.getSession().getId());
        HttpSession session = request.getSession();
        if (session != null) {
            session.invalidate();
        }
        return new ApiResult<>(1, "退出登录成功", null);
    }
}