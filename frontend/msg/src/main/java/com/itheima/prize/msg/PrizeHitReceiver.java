package com.itheima.prize.msg;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.itheima.prize.commons.config.RabbitKeys;
import com.itheima.prize.commons.db.entity.CardUserHit;
import com.itheima.prize.commons.db.service.CardUserHitService;
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
@RabbitListener(queues = RabbitKeys.QUEUE_HIT)
public class PrizeHitReceiver {
    private final static Logger logger = LoggerFactory.getLogger(PrizeHitReceiver.class);

    @Autowired
    private CardUserHitService hitService;

    @RabbitHandler
    public void processMessage(String message) {
        logger.info("user hit : message={}", message);
        //TODO
        try {
            JSONObject jsonObject = JSON.parseObject(message);

            CardUserHit cardUserHit = new CardUserHit();
            cardUserHit.setUserid(jsonObject.getInteger("userid"));
            cardUserHit.setGameid(jsonObject.getInteger("gameid"));
            cardUserHit.setProductid(jsonObject.getInteger("productid"));

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date hitTime = dateFormat.parse(jsonObject.getString("createtime"));
            cardUserHit.setHittime(hitTime);

            hitService.save(cardUserHit);

            logger.info("Successfully saved user hit: {}", cardUserHit);
        } catch (ParseException e) {
            logger.error("Error parsing date from message: {}", message, e);
        } catch (Exception e) {
            logger.error("Error processing message: {}", message, e);
        }
    }
}