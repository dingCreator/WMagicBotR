package com.whitemagic2014.util;

import net.mamoe.mirai.Mirai;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.MessageChain;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * @author ding
 * @date 2023/2/3
 */
public class RecognizeTextUtil {

    private final ITesseract iTesseract;
    private static RecognizeTextUtil util = null;
    private static final Object LOCK = new Object();

    private RecognizeTextUtil() {
        iTesseract = new Tesseract();
        iTesseract.setDatapath("tess");
        iTesseract.setLanguage("chi_sim");
    }

    public static RecognizeTextUtil getInstance() {
        synchronized(LOCK) {
            if (util == null) {
                synchronized (LOCK) {
                    util = new RecognizeTextUtil();
                }
            }
        }
        return util;
    }

    public String getImageText(MessageChain messageChain, Member sender) throws Exception {
        Image image = messageChain.get(Image.Key);
        if (image == null) {
            return null;
        }
        String urlStr = Mirai.getInstance().queryImageUrl(sender.getBot(), image);
        BufferedImage bImg = ImageIO.read(new URL(urlStr));
        String str = iTesseract.doOCR(bImg).trim().replace(" ", "");
        System.out.println("图片文字识别结果【" + str + "】");
        return str;
    }
}
