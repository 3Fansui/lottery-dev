<!-- 秒杀 -->
<template>
    <div class="flash-sale-cont">
        <div class="title-box"></div>
        <div class="stit-box">更多好物 等你来抢</div>
        <div class="flash-sale-info">
            <p>秒杀倒计时</p>
            <div class="countdown-box-mask">
                <div class="countdown-box">
                    <span>距离{{ status === 0 ? '开始' : '结束' }}还剩</span>
                    <div class="">
                        <span>{{ hours }}</span>
                    </div>
                    <span>时</span>
                    <div class="">
                        <span>{{ minutes }}</span>
                    </div>
                    <span>分</span>
                    <div class="">
                        <span>{{ seconds }}</span>
                    </div>
                    <span>秒</span>
                </div>
            </div>
            <div class="lottery-btn-mask">
                <div 
                    class="lottery-btn" 
                    :class="{'disabled' : status !== 2}"
                    @click="receiveFun"
                >
                    {{ status === 2 ? '开始抽奖' : status === 0 ? '等待抽奖' : '抽奖结束'}}
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import axios from 'axios';
import { Vue, Prop, Component } from 'vue-property-decorator';
import { Toast } from 'vant';
import moment from 'moment'

@Component
export default class Turntable extends Vue {
    userInfo: any = {} // 用户信息
    gameList: any = [] // 奖品数组
    lotteryFlag = false // 抽奖按钮状态
    submitLoading = false // 抽奖loading
    hours: string|number = '00' 
    minutes: string|number = '00'
    seconds: string|number = '00'
    time: any = null // 倒计时
    @Prop() status: any;
    @Prop() readonly gameInfo: any;


    mounted() {
        this.getUserInfo()
    }

    beforeDestroy(){
        // 组件销毁清空定时器
        clearInterval(this.time)
        this.time = null
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
                    this.countdownFun()
                } else {
                    // 未登录，跳转登录
                    this.$router.push({
                        name: 'login'
                    })
                }
            })
    }

    // 活动倒计时
    countdownFun(){
        const { data, now } = this.gameInfo
        let currTime = new Date(now).getTime()
        const startTime = new Date(data.starttime).getTime()
        const endTime = new Date(data.endtime).getTime()
    
        this.time = setInterval(() => {
            currTime += 1000
            let milliseconds = 0
            if( currTime > endTime ) {
                // 活动结束
                this.status = 1
            } else if (currTime > startTime ){
                // 活动进行中
                milliseconds = endTime - currTime
                this.status = 2
            } else {
                // 活动未开始
                milliseconds = startTime - currTime
                this.startCountdown()
                this.status = 0
            }
            if(milliseconds <= 0){
                milliseconds = 0
            }
            // 使用moment.duration来转换
            const duration = moment.duration(milliseconds);
            
            // 获取时分秒
            const days = duration.days()
            this.hours = duration.hours() + days * 24
            if(this.hours < 10) {
                this.hours = `0${duration.hours()}`
            } else if (this.hours > 99) {
                this.hours = '99+'
            }
            this.minutes = duration.minutes() < 10 ? `0${duration.minutes()}` : duration.minutes()
            this.seconds = duration.seconds() < 10 ? `0${duration.seconds()}` : duration.seconds()
        }, 1000)
    }

    // 秒杀开始倒计时
    startCountdown() {
        // 
        
    }

    // 结束倒计时
    endCountdown(){
        //
        setInterval(() => {
            //

        }, 1000)
    }


    // 抽奖
    receiveFun() {
        if(this.status !== 2) return
        // 防抖
        if (this.submitLoading) return
        this.submitLoading = true
        axios
            .get(
                `act/go/${this.$route.query.id}`,
            )
            .then((res: any) => {
                const { data, code, msg } = res.data
                if (code === 1) {
                    // 用户已登录， 且中奖
                    // 抽奖结果回调
                    this.$emit('prizeCallBack', true, data)
                } else if (code === 0) {
                    // 抽奖结果回调
                    this.$emit('prizeCallBack', false, {})

                } else {
                    // 其他情况，toast提示
                    Toast(msg);
                }
            })
            .catch(() => {
                Toast('网络异常');
            })
            .finally(() => {
                this.submitLoading = false
            })
    }

}
</script>

<style lang="scss" scoped>
.flash-sale-cont {
    overflow: hidden;

    .title-box {
        width: 448px;
        height: 146px;
        background-image: url(../../assets/detail/pic-flash-sale-title.png);
        background-size: 100% 100%;
        margin: 122px auto 0;
    }

    .stit-box {
        width: 432px;
        height: 64px;
        margin: 14px auto 0;
        background: linear-gradient(180deg, rgba(253, 180, 54, 0.5) 0%, #FC4049 100%);
        border-radius: 32px;
        font-family: AlibabaPuHuiTiM;
        font-size: 36px;
        color: #FFF2C7;
        line-height: 64px;
        text-align: center;
    }

    .flash-sale-info {
        margin: 588px auto 0;
        width: 562px;

        p {
            font-family: AlibabaPuHuiTiM;
            font-size: 36px;
            color: #DA430A;
            line-height: 52px;
            text-align: center;
        }
        .countdown-box-mask{
            width: 562px;
            height: 108px;
            background: linear-gradient(360deg, rgba(239, 120, 24, 1), rgba(218, 67, 10, 1));
            border-radius: 54px;
            box-shadow: 0px 4px 12px 0px #E6C37C;
            margin-top: 16px;
            display: flex;
            align-items: center;
            justify-content: center;
            .countdown-box {
                width: 554px;
                height: 100px;
                background: #FCD074;
                border-radius: 50px;
                display: flex;
                align-items: center;
                justify-content: center;
                >span{
                    font-family: AlibabaPuHuiTiM;
                    font-size: 30px;
                    color: #DA430A;
                    line-height: 44px;
                }
                div{
                    width: 60px;
                    height: 60px;
                    background: #DA430A;
                    border-radius: 8px;
                    margin: 0 12px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    span{
                        font-family: AlibabaPuHuiTiM;
                        font-size: 32px;
                        color: #FFEBC2;
                    }
                }
            }
        }
        .lottery-btn-mask{
            width: 562px;
            height: 108px;
            border-radius: 54px;
            background: linear-gradient( 180deg, rgba(255, 207, 132, 1), rgba(250, 157, 14, 1));
            margin-top: 48px;
            display: flex;
            align-items: center;
            justify-content: center;
            .lottery-btn{
                width: 554px;
                line-height: 100px;
                border-radius: 50px;
                font-family: AlibabaPuHuiTiH;
                font-size: 48px;
                text-align: center;
                background: linear-gradient( 180deg, #FFE5AD 0%, #FEC775 100%);
                color: #C03100;
                &.disabled{
                    color: rgba(192,49,0,0.4);
                }
            }
        }
    }
}
</style>