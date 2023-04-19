package com.whitemagic2014.command.impl.group.funny;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.cache.HomeworkCache;
import com.whitemagic2014.command.impl.group.OwnerCommand;
import com.whitemagic2014.command.impl.group.pcr.tool.SearchWork;
import com.whitemagic2014.dao.HomeworkIconDao;
import com.whitemagic2014.pojo.CommandProperties;
import com.whitemagic2014.pojo.HomeworkIcon;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.OnlineMessageSource;
import net.mamoe.mirai.message.data.PlainText;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * 从花舞作业网获取icon数据
 * 目前暂时只用于幸运角色玩法
 *
 * @author huangkd
 * @see com.whitemagic2014.command.impl.group.funny.LuckyRoleCommand
 */
@Command
public class InitIconCommand extends OwnerCommand {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HomeworkIconDao homeworkIconDao;

    private static final Date START_TIME;
    private static final String HOMEWORK_ICON_URL = "https://www.caimogu.cc/gzlj/data/icon?lang=%s&date=";
    private static final String ICON_PATH = "img/icon/normal/";
    private static final String CN_SERVER_NAME = "zh-cn";
    private static final String CN_TW_SERVER_NANE = "zh-tw";

    static {
        Date date = null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            date = format.parse("2022-03-01");
        } catch (Exception e) {
            e.printStackTrace();
        }
        START_TIME = date;
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("同步icon数据");
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (!CollectionUtils.isEmpty(args) && args.get(0).contains("全量")) {
            File iconDirectory = new File(ICON_PATH);
            if (iconDirectory.isDirectory()) {
                FileUtils.cleanDirectory(iconDirectory);
            }
            homeworkIconDao.deleteAll();
        }

        if (START_TIME == null) {
            return new PlainText("同步icon数据失败");
        }

        Date startDate = START_TIME;
        Date nowDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        initIconDataMap("");
        while (nowDate.after(startDate)) {
            initIconDataMap(sdf.format(startDate));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            calendar.add(Calendar.MONTH, 1);
            startDate = calendar.getTime();
        }
        HomeworkCache.getIconDataList().clear();
        return new PlainText("同步icon数据成功");
    }

    private void initIconDataMap(String date) {
        String homeworkIconUrl = StringUtils.isEmpty(date) ? HOMEWORK_ICON_URL : HOMEWORK_ICON_URL + date;
        String cnTwUrl = String.format(homeworkIconUrl, CN_TW_SERVER_NANE);
        getIconData(cnTwUrl, CN_TW_SERVER_NANE);
        String cnUrl = String.format(homeworkIconUrl, CN_SERVER_NAME);
        getIconData(cnUrl, CN_SERVER_NAME);
    }

    private void getIconData(String homeworkIconUrl, String serverName) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("x-requested-with", "XMLHttpRequest");
        ResponseEntity<SearchWork.SearchIconResponse> entity = restTemplate.exchange(homeworkIconUrl,
                HttpMethod.GET, new HttpEntity<>(null, httpHeaders), SearchWork.SearchIconResponse.class);
        SearchWork.SearchIconResponse response = entity.getBody();
        response.getData().forEach(iconDatas -> iconDatas.forEach(iconData -> {
            HomeworkIcon homeworkIcon;
            if ((homeworkIcon = homeworkIconDao.getByName(iconData.getIconValue())) == null) {
                String filePath = downloadIcon(iconData, serverName);
                homeworkIcon = new HomeworkIcon();
                homeworkIcon.setServerId(iconData.getId());
                homeworkIcon.setServerType(serverName.equals(CN_SERVER_NAME) ? 0 : 1);
                homeworkIcon.setIconUrl(filePath);
                homeworkIcon.setIconName(iconData.getIconValue());
                homeworkIconDao.insert(homeworkIcon);
            } else {
                File tmp = new File(homeworkIcon.getIconUrl());
                if (!tmp.exists()) {
                    downloadIcon(iconData, serverName);
                }
            }
        }));
    }

    private String downloadIcon(SearchWork.SearchIconResponse.IconData iconData, String serverName) {
        String url = iconData.getIconFilePath();
        String fileName = url.substring(url.lastIndexOf("."));
        File file;
        URL urlFile;
        InputStream inStream = null;
        OutputStream os = null;
        String filePath = ICON_PATH + serverName + "_" + iconData.getIconValue() + fileName;
        try {
            file = new File(filePath);
            //下载
            urlFile = new URL(url);
            URLConnection conn = urlFile.openConnection();
            conn.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
            conn.connect();

            inStream = conn.getInputStream();
            os = Files.newOutputStream(file.toPath());
            int bytesRead;
            byte[] buffer = new byte[8192];
            while ((bytesRead = inStream.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != os) {
                    os.close();
                }
                if (null != inStream) {
                    inStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return filePath;
    }
}
