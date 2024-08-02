package com.itheima.prize.api.action;

import ch.qos.logback.core.joran.util.beans.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.prize.commons.db.entity.*;
import com.itheima.prize.commons.db.mapper.ViewCardUserHitMapper;
import com.itheima.prize.commons.db.mapper.ViewGameProductnumMapper;
import com.itheima.prize.commons.db.service.GameLoadService;
import com.itheima.prize.commons.db.service.ViewCardUserHitService;
import com.itheima.prize.commons.db.service.ViewGameHitnumService;
import com.itheima.prize.commons.db.service.ViewGameProductnumService;
import com.itheima.prize.commons.utils.ApiResult;
import com.itheima.prize.commons.utils.PageBean;
import com.itheima.prize.commons.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping(value = "/api/user")
@Api(tags = {"用户模块"})
@Slf4j
public class UserController {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ViewCardUserHitService hitService;
    @Autowired
    private GameLoadService loadService;


    @GetMapping("/info")
    @ApiOperation(value = "用户信息")
    public ApiResult info(HttpServletRequest request) {
        //TODO

        log.info("用户信息模块：{}", request.getSession().getId());

        // 获取当前请求的session对象，如果没有session则创建一个新的
        HttpSession session = request.getSession(false);
        // 从session中获取名为"user"的属性，并将其转换为CardUser对象
        CardUser user = (CardUser) session.getAttribute("user");

        // 检查用户信息是否存在
        if (user != null) {
            // 创建一个新的CardUserDto对象
            CardUserDto userDto = new CardUserDto();

            // 将CardUser对象的属性复制到CardUserDto对象中
            BeanUtils.copyProperties(user, userDto);

            Integer gamesNumByUserId = loadService.getGamesNumByUserId(user.getId());
            userDto.setGames(gamesNumByUserId);

            Integer prizesNumByUserId = loadService.getPrizesNumByUserId(user.getId());
            userDto.setProducts(prizesNumByUserId);

            //另一种写法
            /*CardUserDto dto = new CardUserDto(user);
            dto.setGames(loadService.getGamesNumByUserId(user.getId()));
            dto.setProducts(loadService.getPrizesNumByUserId(user.getId()));*/


            // 返回成功的ApiResult对象，并包含用户信息
            return new ApiResult(1, "成功", userDto);
        }


        // 如果session不存在或用户信息不存在，返回登录超时的ApiResult对象
        return new ApiResult(0, "登陆超时", null);
    }

    @GetMapping("/hit/{gameid}/{curpage}/{limit}")
    @ApiOperation(value = "我的奖品")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "gameid", value = "活动id（-1=全部）", dataType = "int", example = "1", required = true),
            @ApiImplicitParam(name = "curpage", value = "第几页", defaultValue = "1", dataType = "int", example = "1"),
            @ApiImplicitParam(name = "limit", value = "每页条数", defaultValue = "10", dataType = "int", example = "3")
    })
    public ApiResult hit(@PathVariable int gameid, @PathVariable int curpage, @PathVariable int limit, HttpServletRequest request) {
        //TODO

        log.info("我的奖品模块：{}", request.getSession().getId());
        // 获取当前登录用户
        HttpSession session = request.getSession(false);

        CardUser user = (CardUser) session.getAttribute("user");


        //lambda 分页查询
        Page<ViewCardUserHit> page = hitService.lambdaQuery().eq(ViewCardUserHit::getUserid, user.getId())
                .eq(gameid != -1, ViewCardUserHit::getGameid, gameid)
                .page(new Page<>(curpage, limit));


        // 使用 PageBean 封装结果
        return new ApiResult(1, "查询成功", new PageBean<ViewCardUserHit>(page));
    }


}