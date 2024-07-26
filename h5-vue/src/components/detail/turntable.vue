<!-- 大转变 -->
<template>
    <div class="turntale-cont">
        <div class="title-box"></div>
        <div class="turntable-box">
            <div class="chassis" ref="Chassis">
                <div class="prize-item"
                    v-for="(item, index) in gameList" 
                    :key="index"
                    :style="{transform: `rotate(${(index + 1) * 60 - 30}deg)`}"
                >
                    <p>{{ item.name }}</p>
                    <div class="img-mask">
                        <img :src="item.pic" alt="">
                    </div>
                </div>
            </div>
            <div class="pointer" @click="receiveFun">
                <span>幸运</span>
            </div>
        </div>
        <div class="turntable-btn" @click="receiveFun">
            <span>立即抽奖</span>
        </div>
        <div class="enter-box" v-show="enter != -1">
            <span>剩余机会：{{ enter }}次</span>
        </div>
    </div>
</template>

<script lang="ts">
import axios from 'axios';
import { Vue, Prop, Component } from 'vue-property-decorator';
import { Toast } from 'vant';

@Component
export default class Turntable extends Vue {
    userInfo: any = {} // 用户信息
    gameList: any = [] // 奖品数组
    currPrizeId = 0 // 中奖的id
    currPrizeInfo: any = {} // 中奖信息
    lotteryFlag = false // 抽奖按钮状态
    submitLoading = false // 抽奖loading
    time: any = null // 转盘旋转定时器
    currDom: any = null // 转盘dom
    enter = -1 // -1 无限次数 
    prizeInterval: any = [ // 中奖区间
        { start: 1, end: 59},
        { start: 61, end: 119},
        { start: 121, end: 179},
        { start: 181, end: 239},
        { start: 241, end: 299},
        { start: 301, end: 359},
    ]
    @Prop() readonly remainder: any;


    mounted() {
        this.getUserInfo()
        this.currDom = this.$refs.Chassis
    }

    beforeDestroy(){
        // 组件销毁清空定时器
        clearInterval(this.time)
        this.time = null
    }

    // 立即抽奖（旋转转盘并指向到指定位置）
    lotteryFun() {
        let num = 0 // 当前旋转速度
        let speed1 = 1 // 初始速度
        let speed2 = 10 // 最大速度
        let timeNum = 0 // 记录旋转的时间
        this.time = setInterval(()=>{
            timeNum += 10
            if (timeNum <= 1000) { 
                // 1000ms 速度增加
                if( num >= 360 ) num = 0
                speed1 += 0.1
                num += speed1
            } else if(timeNum < 5000){ 
                // 全速 持续4000ms
                if( num >= 360 ) num = 0
                num += 10
            } else if( timeNum >= 5000){ 
                // 第5000ms时 开始减速，并停到指定位置
                if(timeNum === 5000) {
                    // 获取中奖的初始角度
                    const { start } = this.prizeInterval.filter((item: any)=> item.id == this.currPrizeId )[0]
                    // 此处 num -= 36 时， 指针会指向初始位置（奖品数变化后需要重新计算）
                    num -= 36
                    // 58 为 奖品栏（60°）范围，防止箭头指向边界
                    num -= Math.floor(Math.random() * 58 + start)
                }
                // 旋转速度递减
                speed2 -= 0.1
                num += speed2
            }

            // 转盘旋转角度
            this.currDom.style.transform = `rotate(${num}deg)`

            // 超过6000ms 关闭定时器，可以再次抽奖
            if ( timeNum >= 6000){
                clearInterval(this.time)
                this.submitLoading = false

                // 返回抽奖结果
                this.$emit('prizeCallBack', this.currPrizeId === 10001 ? false : true, this.currPrizeInfo)
            }
        }, 10)
    }

    // 获取用户信息
    getUserInfo() {
        axios
            .get(
                `/user/info`,
            )
            .then((res: any) => {
                if (res.data.code === 1) {
                    // 用户已登录
                    const { data } = res.data
                    this.userInfo = data

                    // 获取奖品列表
                    this.getProductsList()
                } else {
                    // 未登录，跳转登录
                    this.$router.push({
                        name: 'login'
                    })
                }
            })
    }

    // 获取奖品列表
    getProductsList () {
        axios
            .get(`/game/products/${this.$route.query.id}`)
            .then((res: any) => {
                if(res.data.code === 1){
                    const { data } = res.data
                    // 大转盘只取5个奖品
                    this.gameList = data
                    this.gameList.push({id: 10001, name: '谢谢参与', pic: require('../../assets/detail/icon-despair-s.png')})

                    // 设置转盘奖品位置
                    this.prizeInterval.forEach((val: any, index: number) => {
                        val.id = this.gameList[index].id 
                    });

                    // 是否可以开始抽奖
                    this.lotteryFlag = true
                    // 获取剩余抽奖次数
                    this.enterFun()
                } else {
                    Toast(res.data.msg || '暂无配置奖品');
                }
            })
            .catch((err: any) => {
                Toast('网络异常');
            })
    }

    // 抽奖奖品
    receiveFun() {
        if(!this.lotteryFlag) {
            Toast('暂无配置奖品');
            return
        }

        // 防抖
        if(this.submitLoading) return
        this.submitLoading = true

        axios
            .get(
                `act/go/${this.$route.query.id}`,
            )
            .then((res: any) => {
                const { data, code, msg } = res.data
                if (code === 1) {
                    // 用户已登录， 且中奖
                    this.currPrizeId = data.id
                    this.currPrizeInfo = data
                    this.lotteryFun()
                    this.enterFun()
                } else if (code === 0) {
                    // 未中奖
                    this.currPrizeId = 10001
                    this.lotteryFun()
                    this.enterFun()
                } else {
                    // 其他情况，toast提示
                    Toast(msg);
                    this.submitLoading = false
                }
            })
            .catch(() => {
                Toast('网络异常');
                this.submitLoading = false
            })
    }

    // 剩余抽奖次数
    enterFun() {
        axios
            .get(
                `/act/limits/${this.$route.query.id}`,
            )
            .then((res: any) => {
                const { data, code } = res.data
                if (code === 1) {
                    /*
                        maxenter 最大可用抽奖次数，0=无限
                        enter 已抽奖次数
                    */
                    if(data.maxenter > 0){
                        this.enter = data.maxenter - data.enter
                    } else {
                        this.enter = -1
                    }
                } 
            })
            .catch((err) => {
                // Toast('网络异常');
            })
    }

}
</script>

<style lang="scss" scoped>
.turntale-cont {
    overflow: hidden;
    .title-box{
        width: 540px;
        height: 258px;
        background-image: url(../../assets/detail/pic-turntable-title.png);
        background-size: 100% 100%;
        margin: 108px auto 0;
    }
    .turntable-box{
        width: 640px;
        height: 830px;
        position: relative;
        margin: 72px auto 0;
        background-image: url(../../assets/detail/pic-turntable.png);
        background-size: 100% 100%;
        .chassis{
            position: absolute;
            top: 76px;
            left: 50%;
            margin-left: -239px;
            width: 478px;
            height: 478px;
            background-image: url(../../assets/detail/pic-chassis.png);
            background-size: 100% 100%;
            .prize-item{
                width: 224px;
                height: 239px;
                position: absolute;
                top: 0px;
                left: 50%;
                margin-left: -112px;
                text-align: center;
                box-sizing: border-box;
                transform-origin: 50% 100%; // 以当前元素 x轴中部，Y轴底部 为旋转中心
                p{
                    padding-top: 20px;
                    font-family: PingFangSC, PingFang SC;
                    font-weight: bold;
                    font-size: 24px;
                    color: #A35400;
                    line-height: 40px;
                    text-align: center;
                    margin: 0;
                }
                .img-mask{
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    height: 72px;
                    img{
                        
                        max-height: 72px;
                        max-width: 72px;
                    }
                }
            }
        }
        .pointer{
            position: absolute;
            top: 170px;
            left: 50%;
            transform: translateX(-50%);
            width: 184px;
            height: 244px;
            background-image: url(../../assets/detail/pic-pointer.png);
            background-size: 100% 100%;
            padding-top: 110px;
            box-sizing: border-box;
            display: flex;
            justify-content: center;
            span{
                font-family: AlibabaPuHuiTiH;
                font-size: 40px;
                color: #FFFFFF;
                line-height: 64px;
                text-shadow: 0px 4px 8px #E15500;
                text-align: center;
                font-style: normal;
            }
        }
    }
    .turntable-btn{
        width: 562px;
        height: 108px;
        position: relative;
        margin: -40px auto 0;
        background-image: url(../../assets/detail/turntable-btn.png);
        background-size: 100% 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        span{
            font-family: AlibabaPuHuiTiH;
            font-size: 48px;
            color: #C03100;
            line-height: 66px;
            font-weight: bold;
        }
    }

    .enter-box{
        margin-top: 28px;
        text-align: center;
        span{
            font-family: PingFangSC, PingFang SC;
            font-weight: 400;
            font-size: 28px;
            color: #FFF4D2;
            line-height: 40px;
        }
    }
}
</style>