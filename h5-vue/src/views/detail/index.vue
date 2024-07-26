<template>
  <main 
    class="detail-page"
    :class="`bg-type${info.type}`"
  >
    <GoBack />
    <!-- 红包雨活动 -->
    <Activity 
      v-if="info.status === 2 && info.type === 1" 
      :remainder="remainder" 
      @activityEndFun="activityEndFun"
    />
    <!-- 秒杀活动 (未开始时也显示活动内容) -->
    <FlashSale 
      v-if="info.status !== 1 && info.type === 2" 
      :status="info.status"
      :gameInfo="gameInfo"
      @prizeCallBack="prizeCallBack"
    />
    <!-- 大转盘活动 -->
    <Turntable 
      v-if="info.status === 2 && info.type === 3" 
      @prizeCallBack="prizeCallBack"
    />

    <!-- 活动未开始 -->
    <div class="status-mask" v-if="info.status === 0">
      <div class="card-box not-start">
        <img src="../../assets/detail/pic-not-start.png" alt="">
        <p>开始时间</p>
        <p>{{ info.startTimeStr }}</p>
      </div>
    </div>
    <!-- 活动已结束 -->
    <div class="status-mask" v-if="info.status === 1">
      <div class="card-box ended">
        <img src="../../assets/detail/pic-ended.png" alt="">
        <p>活动已结束</p>
      </div>
    </div>

    <!-- 红包雨活动 - 我的奖品、活动规则 -->
    <div class="right-float" v-if="info.type === 1">
      <p v-if="info.status === 2" @click="openMyPrizeFun">我的奖品</p>
      <p @click="toRule">活动规则</p>
    </div>

    <!-- 大转盘、秒杀 - 我的奖品、活动规则 -->
    <div class="bottom-float" v-else>
      <div @click="openMyPrizeFun">
        <span>我的奖品</span>
        <i></i>
      </div>
      <span class="line"></span>
      <div @click="toRule">
        <span>活动规则</span>
        <i></i>
      </div>
    </div>

    <!-- 红包雨活动-底部云朵、钱袋 -->
    <div class="page-footer" v-if="info.type === 1" :class="info.status === 1 || info.status === 2 ? 'ended' : 'not-start'"></div>

    <!-- 我的奖品 -->
    <div class="my-prize-mask" v-show="myPrizeFlag">
      <div class="my-prize-cont">
        <div class="popup-title">我的奖品</div>
        <i class="popup-close" @click="myPrizeFlag = false"></i>
        <div class="popup-cont-mask">
          <ul class="prize-list" v-if="myPrizeList && myPrizeList.length > 0">
            <li v-for="item in myPrizeList" :key="item.id">
              <span>{{ item.name || '-' }}</span>
              <span>{{ item.hittime ? item.hittime.slice(0, 11) : '-' }}</span>
            </li>
          </ul>
          <div class="default-page" v-else>
            <p>很遗憾，您未中奖</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 中奖弹框 -->
    <div class="prize-mask" v-show="prizeFlag">
      <div class="prize-cont-mask">
        <div class="prize-cont">
          <div class="cont-title">恭喜你抽中</div>
          <div class="cont-prize-info">
            <div class="prize-img-mask">
              <img :src="prizeInfo.pic" alt="">
            </div>
            <div class="prize-name">{{ prizeInfo.name }}</div>
          </div>
          <div class="cont-submit" @click="prizeFlag = false">开心收下</div>
          <p>可在我的奖品中查看</p>
        </div>
      </div>
    </div>

    <!-- 未中奖弹框 -->
    <div class="no-prize-mask" v-show="noPrizeFlag">
      <div class="no-prize-cont">
        <div class="cont-title">谢谢参与</div>
        <div class="cont-info">很遗憾，大奖与您擦肩而过<br />请再接再厉！</div>
        <div class="cont-submit" @click="noPrizeFlag = false">知道了</div>
      </div>
    </div>
  </main>
</template>

<script lang="ts">
import axios from 'axios';
import { Component, Vue } from 'vue-property-decorator';
import Activity from '@/components/detail/activity.vue'
import FlashSale from '@/components/detail/flash-sale.vue'
import Turntable from '@/components/detail/turntable.vue'
import GoBack from '@/components/layout/GoBack.vue'
import moment from 'moment'

@Component({
  components: {
    GoBack,
    Activity,
    FlashSale,
    Turntable,
  }
})
export default class Detail extends Vue {
  
  info: any = {} // info.status 0-未开始 1-已结束 2-进行中
  gameInfo: any = {} // 活动信息 
  currId = this.$route.query.id // 活动id
  myPrizeFlag = false
  myPrizeList: any = [] // 我的奖品
  prizeFlag = false // 中奖弹框
  prizeInfo: any = {} // 中奖信息
  noPrizeFlag = false // 未中奖弹框
  remainder = 0

  mounted() {
    if (this.currId) {
      this.getGameInfo()
    } else {
      this.$router.push('/')
    }
  }

  // 获取活动详情
  getGameInfo() {
    axios
      .get(`/game/info/${this.currId}`)
      .then((res: any) => {
        if (res.data.code === 1) {
          // 部分ios机型兼容问题 （时间格式 '-' 识别有问题 需要转换成 '/'）
          res.data.data.endtime = res.data.data.endtime && res.data.data.endtime.replace(/-/g, '/')
          res.data.data.starttime = res.data.data.starttime && res.data.data.starttime.replace(/-/g, '/')
          // res.data.now = '2024/05/27 15:55:00'
          const { data, now } = res.data
          this.gameInfo = res.data
          const currTime = new Date(now).getTime()
          const startTime = new Date(data.starttime).getTime()
          const endTime = new Date(data.endtime).getTime()
          this.remainder = endTime - currTime
          if( data.status === 1 ){
            if( currTime >= startTime && currTime < endTime )
            data.status = 2
          }
          data.startTimeStr = moment(data.starttime).format('YYYY年MM月DD日 HH:mm:ss')
          this.info = data
        }
      })
  }

  // 打开我的商品
  openMyPrizeFun() {
    this.myPrizeFlag = true
    axios
      .get(
        `/user/hit/-1/1/100`,
      )
      .then((res: any) => {
        if (res.data.code === 1) {
          const { data } = res.data
          if(data.totalNum && data.items.length > 0){
            // 接口数据处理，倒序显示最新奖品
            this.myPrizeList = data.items
          }
        }
      })
  }

  // 活动规则
  toRule() {
    this.$router.push({
      name: 'rule',
      query: {
        id: this.currId
      }
    })
  }

  // 活动结束回调
  activityEndFun() {
    this.info.status = 1
  }

  // 中奖回调
  prizeCallBack(flag: boolean, prizeInfo: any) {
    this.prizeInfo = {}
    if(flag){
      this.prizeFlag = true
      this.prizeInfo = prizeInfo
    }else{
      this.noPrizeFlag = true
    }
  }
}
</script>
<style lang="scss" scoped>
.detail-page{
  height: 100%;
  background-size: 100% auto;
  background-repeat: no-repeat;
  background-color: #ffeed5;
  position: relative;

  &.bg-type1{
    background-image: url(../../assets/detail/bg-detail.png);
  }
  &.bg-type2{
    height: 1624px;
    background-image: url(../../assets/detail/bg-flash-sale.png);
    background-size: 100% 100%;
    .icon-go-back{
      position: fixed;
    }

    // 底部按钮
    .bottom-float{
      background: #FFE4AB !important;
      div{
        span{
            color: #5E1A00 !important;
        }
        i{
            background-image: url(../../assets/detail/icon-right2.png) !important;
        }
      }
    }
  }
  &.bg-type3 {
    height: 1624px;
    background-image: url(../../assets/detail/bg-turntable.png);
    background-size: 100% 100%;
    .icon-go-back{
      position: fixed;
    }
  }
  
  .status-mask {
    width: 100%;
    height: 100%;
    position: fixed;
    z-index:2;
    top: 0;
    left: 0;
    background-color: rgba(0, 0, 0, .6);
    .card-box{
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 560px;
      height: 560px;
      background-color: #fff4eb;
      border-radius: 50px;
      box-sizing: border-box;
      text-align: center;
      p{
        font-family: SourceHanSansCN-Medium;
        font-weight: normal;
        font-stretch: normal;
        line-height: 46px;
        letter-spacing: 0px;
        color: #ff2b39;
      }
      &.not-start{
        padding-top: 103px;
        img{
          width: 271px;
          transform: translateX(8.5px);
          margin-bottom: 61px;
        }
        p{
          font-size: 36px;
        }
      }
      &.ended{
        padding-top: 86px;
        img{
          width: 291px;
          transform: translateX(18.5px);
          margin-bottom: 70px;
        }
        p{
          font-family: SourceHanSansCN-Heavy;
          font-size: 44px;
          font-weight: 600;
        }
      }
    }
  }

  .right-float {
    position: fixed;
    right: 0;
    bottom: 371px;
    width: 200px;
    z-index: 3;
    p{
      width: 200px;
      height: 72px;
      text-align: center;
      background-color: #f94823;
      border-radius: 36px 0px 0px 36px;
      margin-top: 32px;
      font-family: SourceHanSansCN-Heavy;
      font-size: 32px;
      font-weight: normal;
      font-stretch: normal;
      line-height: 72px;
      letter-spacing: 2px;
      color: #ffeddb;
    }
  }

  .bottom-float {
    width: 100%;
    position: fixed;
    left: 0;
    bottom: 0;
    z-index: 3;
    height: 108px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background-color: rgba(255, 255, 255, .1);
    div{
      flex: 1;
      height: 108px;
      display: flex;
      align-items: center;
      justify-content: center;
      span{
        font-family: PingFangSC, PingFang SC;
        font-weight: 400;
        font-size: 28px;
        color: #FFF4D2;
        line-height: 40px;
        text-align: left;
      }
      i{
        width: 28px;
        height: 28px;
        margin-left: 8px;
        background-image: url(../../assets/detail/icon-right.png);
        background-size: 100% 100%;
      }
    }
    
    .line{
      width: 2px;
      height: 52px;
      background: rgba(255,255,255,0.2);
    }
  }

  .page-footer {
    position: fixed;
    z-index:1;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 852px;
    background-size: 100% 100%;
    &.not-start{
      background-image: url(../../assets/detail/pic-purse-1.png);
    }

    &.ended{
      background-image: url(../../assets/detail/pic-purse-2.png);
    } 
  }

  // 我的奖品
  .my-prize-mask {
    position: fixed;
    top: 0;
    left: 0;
    background-color: rgba(0,0,0,.6);
    width: 100%;
    height: 100%;
    z-index: 3;
    .my-prize-cont{
      width: 650px;
      height: 629px;
      background-color: #f94823;
      border-radius: 40px;
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      padding-top: 97px;
      box-sizing: border-box;
      box-shadow: inset 0px 0px 25px rgba(255, 255, 255, .6);
      .popup-title{
        width: 396px;
        height: 108px;
        background-image: url(../../assets/user/pic-title-bg.png);
        background-size: 100% 100%;
        position: absolute;
        top: -22px;
        left: 50%;
        transform: translateX(-50%);
        text-align: center;
        padding-top: 28px;
        box-sizing: border-box;
        font-size: 40px;
        font-family: PingFangSC-Medium, PingFang SC;
        font-weight: 500;
        color: #ff2b39;
        line-height: 44px;
      }
      .popup-close{
        position: absolute;
        bottom: -89px;
        left: 50%;
        transform: translateX(-50%);
        width: 50px;
        height: 50px;
        background-image: url(../../assets/common/icon-close.png);
        background-size: 100% 100%;
      }

      .popup-cont-mask{
        width: 611px;
        height: 511px;
        background-color: #ffeddb;
        border-radius: 20px;
        margin: 0 auto;
        overflow: hidden;
        padding: 48px 30px;
        box-sizing: border-box;
        .prize-list{
          width: 631px;
          height: 100%;
          padding-right: 50px;
          overflow-y: auto;
          box-sizing: border-box;
          overflow-x: hidden;
          li{
            width: 551px;
            max-width: 551px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-top: 40px;
            box-sizing: border-box;
            &:first-child{
              margin: 0;
            }
            span{
              font-size: 30px;
              font-weight: normal;
              font-stretch: normal;
              line-height: 34px;
              letter-spacing: 0px;
              color: #0f0301;
              text-overflow:ellipsis;  
              overflow: hidden;  
              white-space: nowrap;
              &:nth-of-type(1){
                width: 190px;
                color: #f94823;
              }
              &:nth-of-type(2){
                width: 180px;
                text-align: right;
              }
            }
          }
        }

        .default-page{
          width: 100%;
          height: 100%;
          position: relative;
          p{
            width: 100%;
            position: absolute;
            top: 50%;
            transform: translateY(-50%);
            text-align: center;
            box-sizing: border-box;
            font-size: 40px;
            font-family: PingFangSC-Medium, PingFang SC;
            font-weight: 500;
            color: #f94823;
            line-height: 48px;
          }
        }
      }
    }
  }

  // 中奖弹框
  .prize-mask {
    position: fixed;
    top: 0;
    left: 0;
    background-color: rgba(0,0,0,.6);
    width: 100%;
    height: 100%;
    z-index: 3;
    .prize-cont-mask{
      width: 550px;
      height: 572px;
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      &::after{
        content:"";
        position: absolute;
        z-index: 1;
        top: -132px;
        left: 50%;
        transform: translateX(-50%);
        width: 292px;
        height: 204px;
        background-image: url(../../assets/detail/icon-popup-red-envelope.png);
        background-size: 100% 100%;
      }
      .prize-cont{
        width: 100%;
        height: 100%;
        background: linear-gradient( 180deg, #FFF6DA 0%, #FFFFFF 100%);
        border-radius: 48px;
        position: absolute;
        box-sizing: border-box;
        z-index: 2;
        padding-top: 48px;
        .cont-title{
          font-family: AlibabaPuHuiTiH;
          font-weight: bold;
          font-size: 44px;
          color: #DB4801;
          line-height: 52px;
          letter-spacing: 1px;
          text-align: center;
          font-style: normal;
          background: -webkit-linear-gradient(180deg, #F18107 0%, #DE4A03 100%); /* Chrome, Safari */
          background: linear-gradient(180deg, #F18107 0%, #DE4A03 100%); /* 标准语法 */
          -webkit-background-clip: text; /* Chrome, Safari */
          background-clip: text;
          -webkit-text-fill-color: transparent; /* Chrome, Safari */
          color: transparent; /* 其他浏览器 */
        }

        .cont-prize-info{
          .prize-img-mask{
            margin: 32px auto 0;
            text-align: center;
            padding: 5px 0;
            width: 176px;
            height: 176px;
            background: #FFFFFF;
            border-radius: 16px;
            border: 2px solid #F8DE8C;
            box-sizing: border-box;
            img{
              width: 170px;
              max-height: 166px;
            }
          }
          .prize-name{
            margin-top: 16px;
            font-family: PingFangSC, PingFang SC;
            font-weight: 600;
            font-size: 28px;
            color: #A35400;
            line-height: 44px;
            text-align: center;
          }
        }
        .cont-submit{
          width: 394px;
          height: 75px;
          margin: 48px auto 0;
          background-image: url(../../assets/detail/pic-popup-btn.png);
          background-size: 100% 100%;
          text-align: center;
          line-height: 75px;
          font-family: AlibabaPuHuiTiM;
          font-size: 32px;
          color: #C03100;
        }
        p{
          font-family: AlibabaPuHuiTiR;
          font-size: 24px;
          color: #C87F5C;
          line-height: 40px;
          text-align: center;
          margin-top: 16px;
        }
      }
    }
  }

  // 未中奖弹框
  .no-prize-mask {
    position: fixed;
    top: 0;
    left: 0;
    background-color: rgba(0,0,0,.6);
    width: 100%;
    height: 100%;
    z-index: 3;
    .no-prize-cont{
      width: 550px;
      height: 452px;
      background: linear-gradient( 180deg, #FFF6DA 0%, #FFFFFF 100%);
      border-radius: 48px;
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      padding-top: 100px;
      box-sizing: border-box;
      &:after{
        content:"";
        position: absolute;
        top: -74px;
        left: 50%;
        transform: translateX(-50%);
        width: 156px;
        height: 140px;
        background-image: url(../../assets/detail/icon-despair-b.png);
        background-size: 100% 100%;
      }
      .cont-title{
        font-family: AlibabaPuHuiTiH;
        font-weight: bold;
        font-size: 44px;
        color: #DB4801;
        line-height: 52px;
        letter-spacing: 1px;
        text-align: center;
        font-style: normal;
        background: -webkit-linear-gradient(180deg, #F18107 0%, #DE4A03 100%); /* Chrome, Safari */
        background: linear-gradient(180deg, #F18107 0%, #DE4A03 100%); /* 标准语法 */
        -webkit-background-clip: text; /* Chrome, Safari */
        background-clip: text;
        -webkit-text-fill-color: transparent; /* Chrome, Safari */
        color: transparent; /* 其他浏览器 */
      }
      .cont-info{
        font-family: AlibabaPuHuiTiR;
        font-size: 32px;
        color: #DB4801;
        line-height: 44px;
        text-align: center;
        font-style: normal;
        margin-top: 24px;
      }
      .cont-submit{
        width: 394px;
        height: 75px;
        margin: 48px auto 0;
        background-image: url(../../assets/detail/pic-popup-btn.png);
        background-size: 100% 100%;
        text-align: center;
        line-height: 75px;
        font-family: AlibabaPuHuiTiM;
        font-size: 32px;
        color: #C03100;
      }
    }
  }
}

</style>
