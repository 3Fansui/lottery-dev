package com.itheima.prize.msg;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.itheima.prize.commons.config.RabbitKeys;
import com.itheima.prize.commons.db.entity.CardUserGame;
import com.itheima.prize.commons.db.service.CardUserGameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@RabbitListener(queues = RabbitKeys.QUEUE_PLAY)
public class PrizeGameReceiver {

    private final static Logger logger = LoggerFactory.getLogger(PrizeGameReceiver.class);

    @Autowired
    private CardUserGameService cardUserGameService;

    @RabbitHandler
    public void processMessage(String message) {
        logger.info("user play : msg={}" , message);
        //TODO
        try {
            JSONObject jsonObject = JSON.parseObject(message);

            CardUserGame cardUserGame = new CardUserGame();
            cardUserGame.setUserid(jsonObject.getInteger("userid"));
            cardUserGame.setGameid(jsonObject.getInteger("gameid"));

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date createTime = dateFormat.parse(jsonObject.getString("createtime"));
            cardUserGame.setCreatetime(createTime);

            cardUserGameService.save(cardUserGame);

            logger.info("Successfully saved user game participation: {}", cardUserGame);
        } catch (ParseException e) {
            logger.error("Error parsing date from message: {}", message, e);
        } catch (Exception e) {
            logger.error("Error processing message: {}", message, e);
        }
    }

}
