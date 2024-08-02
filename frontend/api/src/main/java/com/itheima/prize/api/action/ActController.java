package com.itheima.prize.api.action;

import com.alibaba.fastjson.JSON;
import com.itheima.prize.api.config.LuaScript;
import com.itheima.prize.commons.config.RabbitKeys;
import com.itheima.prize.commons.config.RedisKeys;
import com.itheima.prize.commons.db.entity.*;
import com.itheima.prize.commons.db.mapper.CardGameMapper;
import com.itheima.prize.commons.db.service.CardGameService;
import com.itheima.prize.commons.utils.ApiResult;
import com.itheima.prize.commons.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/act")
@Api(tags = {"抽奖模块"})
public class ActController {

    private static final Logger log = LoggerFactory.getLogger(ActController.class);
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private LuaScript luaScript;

    @GetMapping("/limits/{gameid}")
    @ApiOperation(value = "剩余次数")
    @ApiImplicitParams({
            @ApiImplicitParam(name="gameid",value = "活动id",example = "1",required = true)
    })
    public ApiResult<Object> limits(@PathVariable int gameid, HttpServletRequest request){
        //获取活动基本信息
        CardGame game = (CardGame) redisUtil.get(RedisKeys.INFO+gameid);
        if (game == null){
            return new ApiResult<>(-1,"活动未加载",null);
        }
        //获取当前用户
        HttpSession session = request.getSession();
        CardUser user = (CardUser) session.getAttribute("user");
        if (user == null){
            return new ApiResult(-1,"未登陆",null);
        }
        //用户可抽奖次数
        Integer enter = (Integer) redisUtil.get(RedisKeys.USERENTER+gameid+"_"+user.getId());
        log.info("用户抽奖次数：{}",enter);
        if (enter == null){
            enter = 0;
        }
        //根据会员等级，获取本活动允许的最大抽奖次数
        Integer maxenter = (Integer) redisUtil.hget(RedisKeys.MAXENTER+gameid,user.getLevel()+"");
        //如果没设置，默认为0，即：不限制次数
        maxenter = maxenter==null ? 0 : maxenter;

        //用户已中奖次数
        Integer count = (Integer) redisUtil.get(RedisKeys.USERHIT+gameid+"_"+user.getId());
        if (count == null){
            count = 0;
        }
        //根据会员等级，获取本活动允许的最大中奖数
        Integer maxcount = (Integer) redisUtil.hget(RedisKeys.MAXGOAL+gameid,user.getLevel()+"");
        //如果没设置，默认为0，即：不限制次数
        maxcount = maxcount==null ? 0 : maxcount;

        //幸运转盘类，先给用户随机剔除，再获取令牌，有就中，没有就说明抢光了
        //一般这种情况会设置足够的商品，卡在随机上
        Integer randomRate = (Integer) redisUtil.hget(RedisKeys.RANDOMRATE+gameid,user.getLevel()+"");
        if (randomRate == null){
            randomRate = 100;
        }

        Map map = new HashMap();
        map.put("maxenter",maxenter);
        map.put("enter",enter);
        map.put("maxcount",maxcount);
        map.put("count",count);
        map.put("randomRate",randomRate);

        return new ApiResult<>(1,"成功",map);
    }
    @GetMapping("/go/{gameid}")
    @ApiOperation(value = "抽奖")
    @ApiImplicitParams({
            @ApiImplicitParam(name="gameid",value = "活动id",example = "1",required = true)
    })
    public ApiResult<Object> act(@PathVariable int gameid, HttpServletRequest request){
        //TODO
        // 获取当前用户
        HttpSession session = request.getSession();
        CardUser user = (CardUser) session.getAttribute("user");
        if (user == null) {
            return new ApiResult<>(-1, "未登录", null);
        }

        // 获取活动基本信息
        CardGame game = (CardGame) redisUtil.get(RedisKeys.INFO + gameid);
        if (game == null) {
            return new ApiResult<>(-1, "活动未加载", null);
        }

        // 检查活动是否在有效期内
        long now = System.currentTimeMillis();
        if (now < game.getStarttime().getTime() || now > game.getEndtime().getTime()) {
            return new ApiResult<>(-1, "活动未开始或已结束", null);
        }

        // 获取用户当前抽奖次数
        Integer enter = (Integer) redisUtil.get(RedisKeys.USERENTER + gameid + "_" + user.getId());
        enter = enter == null ? 0 : enter;

        // 获取最大抽奖次数
        Integer maxenter = (Integer) redisUtil.hget(RedisKeys.MAXENTER + gameid, user.getLevel() + "");
        maxenter = maxenter == null ? 0 : maxenter;

        // 检查是否超过最大抽奖次数
        if (maxenter > 0 && enter >= maxenter) {
            return new ApiResult<>(-1, "抽奖次数已用完", null);
        }

        // 获取最大中奖次数
        Integer maxcount = (Integer) redisUtil.hget(RedisKeys.MAXGOAL + gameid, user.getLevel() + "");
        maxcount = maxcount == null ? 0 : maxcount;

        // 使用Lua脚本进行原子操作
        Long result = luaScript.tokenCheck(gameid, user.getId(), maxcount);

        if (result == -1) {
            return new ApiResult<>(-1, "已达到最大中奖次数", null);
        } else if (result == -2) {
            return new ApiResult<>(-1, "奖品已抽完", null);
        } else if (result == 0) {
            // 未中奖，增加用户抽奖次数
            redisUtil.incr(RedisKeys.USERENTER + gameid + "_" + user.getId(),1);
            return new ApiResult<>(0, "很遗憾，未中奖", null);
        } else {
            // 中奖，获取奖品信息
            String tokenKey = RedisKeys.TOKEN + gameid + "_" + result;
            CardProduct product = (CardProduct) redisUtil.get(tokenKey);

            if (product == null) {
                return new ApiResult<>(-1, "奖品信息不存在", null);
            }

            // 增加用户抽奖次数
            redisUtil.incr(RedisKeys.USERENTER + gameid + "_" + user.getId(),1);

            // 发送中奖消息到RabbitMQ (修改这里)
            Map<String, Object> msg = new HashMap<>();
            msg.put("userid", user.getId());
            msg.put("username", user.getUname());
            msg.put("gameid", gameid);
            msg.put("productid", product.getId());
            msg.put("createtime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            String jsonMessage = JSON.toJSONString(msg);
            rabbitTemplate.convertAndSend(RabbitKeys.EXCHANGE_DIRECT, RabbitKeys.QUEUE_HIT, jsonMessage);

            // 发送参与活动消息到RabbitMQ (新增这里)
            Map<String, Object> playMsg = new HashMap<>();
            playMsg.put("userid", user.getId());
            playMsg.put("gameid", gameid);
            playMsg.put("createtime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            String jsonPlayMessage = JSON.toJSONString(playMsg);
            rabbitTemplate.convertAndSend(RabbitKeys.EXCHANGE_DIRECT, RabbitKeys.QUEUE_PLAY, jsonPlayMessage);

            // 返回中奖信息
            return new ApiResult<>(1, "恭喜中奖", product);
        }

    }

    @GetMapping("/info/{gameid}")
    @ApiOperation(value = "缓存信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name="gameid",value = "活动id",example = "1",required = true)
    })
    public ApiResult info(@PathVariable int gameid){
        //TODO
        Map<String, Object> resultMap = new HashMap<>();


        // 获取缓存的活动策略信息
        String maxGoalKey = RedisKeys.MAXGOAL + gameid;
        String maxEnterKey = RedisKeys.MAXENTER + gameid;
        Map<String, Object> gameRules = new HashMap<>();
        gameRules.put("maxGoal", redisUtil.hmget(maxGoalKey));
        gameRules.put("maxEnter", redisUtil.hmget(maxEnterKey));
        log.info("获取到的活动策略信息: {}", gameRules);
        resultMap.put("game_maxgoal_" + gameid, redisUtil.hmget(maxGoalKey));
        resultMap.put("game_maxenter_" + gameid, redisUtil.hmget(maxEnterKey));

        // 获取缓存的活动奖品信息
        String tokenKey = RedisKeys.TOKENS + gameid;
        List<Object> tokens = redisUtil.lrange(tokenKey, 0, -1);
        log.info("获取到的令牌信息: {}", tokens);

        // 创建新的集合并获取对应的值
        Map<Object,Object> map = new HashMap<>();
        List<Object> results = new ArrayList<>();

        String s = RedisKeys.TOKEN + gameid;
        for (Object token : tokens) {
            String newKey = s + "_" + token.toString();
            System.out.println(newKey);
            Object value = redisUtil.get(newKey);
            map.put(token,value);
        }
        results.add(map);

        log.info("处理后的结果信息: {}", results);
        resultMap.put("game_tokens_" + gameid, results);

        // 获取缓存的活动基本信息
        String gameKey = RedisKeys.INFO + gameid;
        CardGame game = (CardGame) redisUtil.get(gameKey);
        log.info("获取到的活动信息: {}", game);
        resultMap.put("game_info_" + gameid, game);


        return new ApiResult<>(1, "缓存信息", resultMap);
    }
}
