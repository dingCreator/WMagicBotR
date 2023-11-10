package com.whitemagic2014.util;

import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.command.impl.group.immortal.util.TimeUtil;
import com.whitemagic2014.util.time.MagicPeriodTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.message.data.PlainText;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * @author ding
 * @date 2023/9/15
 */
//@Component
@Slf4j
public class TicketRemindUtilTmp {

    @Autowired
    private RestTemplate restTemplate;

    private static final String SEARCH_TICKET_URL = "https://kyfw.12306.cn/otn/leftTicket/queryZ?leftTicketDTO.train_date=%s"
            + "&leftTicketDTO.from_station=%s&leftTicketDTO.to_station=%s&purpose_codes=ADULT";

    private static final List<NeedTicketInfo> INFO_LIST = Arrays.asList(
            new NeedTicketInfo(348957891L, "2023-10-07", "MOQ", "GZQ", 1,
                    Arrays.asList("K9281"))
    );

    public TicketRemindUtilTmp() {
        log.info("开始火车票监控");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/116.0.0.0 Safari/537.36");
        httpHeaders.add("Cookie", "guidesStatus=off; highContrastMode=defaltMode; cursorStatus=off; "
                + "_jc_save_wfdc_flag=dc; _jc_save_fromStation=%u5E7F%u5DDE%2CGZQ; "
                + "fo=hi1jm53jp3tggxnkLXN6AwKrJ-pXHYsZSFmxpRZxtGqt7cqg74XoW7Rmm3kvAkxoGJ_RioRFmFH1mKo_DDUdLu6r_"
                + "GWZUwGUMyVIqHejkPGvmf--vdshTb2AO6zZv_381Rqaf5JQ6vjhWit_72sAczt_xsBonMNFwzm0BtY98P-DjALq15Xf5R5h390; "
                + "_jc_save_toStation=%u6885%u5DDE%2CMOQ; _jc_save_toDate=2023-09-15; _jc_save_fromDate=2023-09-28");
        HttpEntity<TicketReturn> entity = new HttpEntity<>(httpHeaders);

        MagicPeriodTask.build("ticket_monitor", () -> {
            for (NeedTicketInfo info : INFO_LIST) {
                List<String> result;
                try {
                    TimeUtil.waitRandomMillis(1000, 2 * 1000);
                    String realUrl = String.format(SEARCH_TICKET_URL, info.getDate(), info.getStartPoint(), info.getEndPoint());
                    ResponseEntity<TicketReturn> response = restTemplate.exchange(realUrl, HttpMethod.GET, entity, TicketReturn.class);
                    TicketReturn tr = response.getBody();
                    result = tr.getData().getResult();
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                    log.error("查询QQ号{}关注车票失败", info.getQq());
                    continue;
                } catch (Exception e) {
                    log.warn("查询车票信息失败");
                    continue;
                }

                // TODO 解析车票数据
                long count = result.stream().filter(r -> {
                    if (CollectionUtils.isEmpty(info.getTrainNo())) {
                        return true;
                    }
                    for (String singleTrainNo : info.getTrainNo()) {
                        if (r.contains(singleTrainNo)) {
                            return true;
                        }
                    }
                    return false;
                }).filter(r -> r.contains("暂停发售")).count();
                if (count < info.getStopCount()) {
                    info.setStopCount((int) count);
                    try {
                        Bot bot = MagicBotR.getBot();
                        bot.getFriendOrFail(info.getQq()).sendMessage(new PlainText("有新的车次" + info.getDate() + "开放购买"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).schedule(120 * 1000, 20 * 1000);
    }

    @Data
    @AllArgsConstructor
    private static class NeedTicketInfo {
        private Long qq;
        private String date;
        private String startPoint;
        private String endPoint;
//        private Integer total;
        private Integer stopCount;
        private List<String> trainNo;
    }

    @Data
    public static class TicketReturn implements Serializable {
        private Integer httpstatus;
        private TicketData data;

        @Data
        public static class TicketData implements Serializable {
            private List<String> result;
            private String flag;
        }
    }
}
