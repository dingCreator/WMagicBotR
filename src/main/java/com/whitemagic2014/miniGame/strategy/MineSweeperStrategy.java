package com.whitemagic2014.miniGame.strategy;

import com.whitemagic2014.miniGame.MiniGameUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * 扫雷游戏
 *
 * @author ding
 */
public class MineSweeperStrategy implements BaseMiniGameStrategy {

    private boolean initGame = false;
    private int clickCount = 0;
    private boolean warn = false;
    /**
     * 显示的图形
     * 1-8数字 0已点击周围无雷 -1未点击 -2标记 -9踩雷
     */
    private int[][] showMineMap;

    /**
     * 实际雷的分布
     * 0-无雷 1-有雷
     */
    private int[][] realMineMap;

    /**
     * 是否胜利
     */
    private boolean win;

    /**
     * 输出图片
     */
    private File outFile;

    /**
     * 上一次的雷区分布图，减少计算量
     */
    static int[][] previousMineMap;

    static Random random = new Random();

    private int height;
    private int width;
    private int mineCount;

    @Override
    public boolean support(Long id) {
        return MiniGameUtil.playerIsPlayingGame(id);
    }

    @Override
    public MiniGameUtil.MiniGameResultEnum play(String msg, Member sender, Group subject) {
        if (!initGame) {
            String[] params = msg.split(" ");
            if (params.length < 3) {
                return dealIllegalCommand("请输入地雷的长，宽，雷的数量，用空格隔开", sender);
            }

            try {
                height = Integer.parseInt(params[0].trim());
                width = Integer.parseInt(params[1].trim());
                mineCount = Integer.parseInt(params[2].trim());
            } catch (Exception e) {
                return dealIllegalCommand("格式错误！请输入地雷的长，宽，雷的数量，用空格隔开", sender);
            }

            if (height < 5 || width < 5 || height > 100 || width > 100 || mineCount < 1 || mineCount > height * width / 2) {
                return dealIllegalCommand("格式错误！长宽范围：5-100，雷数不能少于1且不能超过格子数的一半", sender);
            }

            showMineMap = new int[height][width];
            for (int y = 0; y < showMineMap.length; y++) {
                for (int x = 0; x < showMineMap[0].length; x++) {
                    showMineMap[y][x] = -1;
                }
            }
            initGame = true;
            win = false;
            outFile = new File("img/mine/mine_map_" + sender.getId() + ".png");
            try {
                outFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return MiniGameUtil.MiniGameResultEnum.STOP;
            }


            showMap(showMineMap, width, height);
            sender.getGroup().sendMessage(Contact.uploadImage(subject, outFile));
            sender.getGroup().sendMessage(new PlainText("请输入地雷坐标，用空格隔开，坐标后再加空格t表示标记/取消标记"));
            return MiniGameUtil.MiniGameResultEnum.CONTINUE;
        }


        try {
            String[] firstPosition = msg.split(" ");
            int x = Integer.parseInt(firstPosition[0]) - 1;
            int y = Integer.parseInt(firstPosition[1]) - 1;
            boolean label = false;
            if (firstPosition.length > 2) {
                String str = firstPosition[2].toUpperCase();
                if (str.contains("T")) {
                    label = true;
                }
            }
            MineCell mineCell = new MineCell(x, y);
            if (clickCount == 0) {
                initMineMap(mineCell, width, height, mineCount);
            }
            click(mineCell, width, height, label);
            showMap(showMineMap, width, height);
            sender.getGroup().sendMessage(Contact.uploadImage(subject, outFile));
            clickCount++;
        } catch (Exception e) {
            return dealIllegalCommand("格式错误！请输入地雷坐标，用空格隔开，坐标后再加空格t表示标记/取消标记", sender);
        }

        if (gameContinue()) {
            return MiniGameUtil.MiniGameResultEnum.CONTINUE;
        }

        MiniGameUtil.stopGame(sender.getId());
        if (outFile.exists()) {
            outFile.delete();
        }
        if (win) {
            showMap(showMineMap, width, height);
            sender.getGroup().sendMessage(Contact.uploadImage(subject, outFile));
            return MiniGameUtil.MiniGameResultEnum.WIN;
        } else {
            return MiniGameUtil.MiniGameResultEnum.LOSE;
        }
    }

    private MiniGameUtil.MiniGameResultEnum dealIllegalCommand(String warnMsg, Member sender) {
        // 为避免重复提示（例如游戏者中途回复消息等），只提醒一次
        if (!warn) {
            warn = true;
            sender.getGroup().sendMessage(new PlainText(warnMsg));
        }
        return MiniGameUtil.MiniGameResultEnum.NOT_GAME_COMMAND;
    }

    private boolean gameContinue() {
        boolean flag = false;
        for (int y = 0; y < showMineMap.length; y++) {
            for (int x = 0; x < showMineMap[y].length; x++) {
                if (showMineMap[y][x] == -9) {
                    win = false;
                    return false;
                }
                if (showMineMap[y][x] < 0 && realMineMap[y][x] == 0) {
                    flag = true;
                }
            }
        }
        win = true;
        return flag;
    }

    private void showMap(int[][] map, int width, int height) {
        int realWidth = (width + 1) * 30;
        int realHeight = (height + 2) * 30;

        try {
            BufferedImage backGround;
            if (outFile.exists()) {
                backGround = ImageIO.read(outFile);
            } else {
                outFile.createNewFile();
                backGround = new BufferedImage(realWidth, realHeight, BufferedImage.TYPE_INT_RGB);
                backGround.setRGB(0, 0, realWidth, 30,
                        createImage(Color.WHITE, realWidth, 30)
                                .getRGB(0, 0, realWidth, 30, new int[30 * realWidth], 0, 30), 0, 30);

                for (int w = 0; w < width; w++) {
                    backGround.setRGB(30 * (w + 1), 30, 30, 30,
                            createImage(String.valueOf(w + 1), new Font("微软雅黑", Font.PLAIN, 14), Color.GREEN, Color.BLACK, 30, 30)
                                    .getRGB(0, 0, 30, 30, new int[30 * 30], 0, 30), 0, 30);
                }

                for (int h = 0; h < height; h++) {
                    backGround.setRGB(0, 30 * (h + 2), 30, 30,
                            createImage(String.valueOf(h + 1), new Font("微软雅黑", Font.PLAIN, 14), Color.GREEN, Color.BLACK, 30, 30)
                                    .getRGB(0, 0, 30, 30, new int[30 * 30], 0, 30), 0, 30);
                }
            }

            for (int y = 0; y < map.length; y++) {
                for (int x = 0; x < map[y].length; x++) {
                    if (map[y][x] == 0) {
                        backGround.setRGB(getPosition(x, true), getPosition(y, false), 30, 30,
                                createImage(Color.GRAY, 30, 30)
                                        .getRGB(0, 0, 30, 30, new int[30 * 30], 0, 30), 0, 30);
                    } else if (map[y][x] > 0) {
                        backGround.setRGB(getPosition(x, true), getPosition(y, false), 30, 30,
                                createImage(String.valueOf(map[y][x]), new Font("微软雅黑", Font.PLAIN, 14), Color.GRAY, Color.BLACK, 30, 30)
                                        .getRGB(0, 0, 30, 30, new int[30 * 30], 0, 30), 0, 30);
                    } else if (map[y][x] == -1) {
                        backGround.setRGB(getPosition(x, true), getPosition(y, false), 30, 30,
                                createImage(Color.WHITE, 30, 30)
                                        .getRGB(0, 0, 30, 30, new int[30 * 30], 0, 30), 0, 30);
                    } else if (map[y][x] == -2) {
                        backGround.setRGB(getPosition(x, true), getPosition(y, false), 30, 30,
                                createImage("标", new Font("微软雅黑", Font.PLAIN, 14), Color.GRAY, Color.BLACK, 30, 30)
                                        .getRGB(0, 0, 30, 30, new int[30 * 30], 0, 30), 0, 30);
                    } else if (map[y][x] == -9) {
                        backGround.setRGB(getPosition(x, true), getPosition(y, false), 30, 30,
                                createImage("雷", new Font("微软雅黑", Font.PLAIN, 14), Color.GRAY, Color.BLACK, 30, 30)
                                        .getRGB(0, 0, 30, 30, new int[30 * 30], 0, 30), 0, 30);
                    }
                }
            }

            // 输出png图片
            ImageIO.write(backGround, "png", outFile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            previousMineMap = map;
        }
    }

    private int getPosition(int p, boolean isX) {
        return isX ? (p + 1) * 30 : (p + 2) * 30;
    }

    private BufferedImage createImage(Color backgroundColor, int width, int height) {
        return createImage(null, null, backgroundColor, null, width, height);
    }

    private BufferedImage createImage(String text, Font font, Color backgroundColor, Color textColor, int width, int height) {
//        if (!StringUtils.isEmpty(text)) {
//            // 获取font的样式应用在str上的整个矩形
//            int[] arr = getWidthAndHeight(text, font);
//            width = arr[0];
//            height = arr[1];
//        }
        // 创建图片画布
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        Graphics g = image.getGraphics();
        // 先用白色填充整张图片,也就是背景 WHITE
        g.setColor(backgroundColor);
        //画出矩形区域，以便于在矩形区域内写入文字
        g.fillRect(0, 0, width, height);
        if (!StringUtils.isEmpty(text)) {
            // 再换成黑色，以便于写入文字 black
            g.setColor(textColor);
            // 设置画笔字体
            g.setFont(font);
            // 画出一行字符串
            g.drawString(text, 13, font.getSize());
        }
        g.dispose();
        return image;
    }

    /**
     * 初始化扫雷图，第一个必不为雷
     *
     * @param mineCell  点击的第一个格子
     * @param width     扫雷图宽度
     * @param height    扫雷图高度
     * @param mineCount 雷数量
     */
    private void initMineMap(final MineCell mineCell, final int width, final int height, final int mineCount) {
        realMineMap = new int[height][width];
        int excludeX = mineCell.getX();
        int excludeY = mineCell.getY();

        Set<String> positionSet = new HashSet<>();
        positionSet.add(excludeX + "," + excludeY);

        int nowMineCount = 0;
        while (nowMineCount < mineCount) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            if (!positionSet.add(x + "," + y)) {
                continue;
            }
            realMineMap[y][x] = 1;
            nowMineCount++;
        }
    }

    /**
     * 点击事件
     *
     * @param mineCell 点击的格子
     * @param width    扫雷图宽度
     * @param height   扫雷图长度
     * @param label    做标记/点击
     */
    private void click(final MineCell mineCell, final int width, final int height, boolean label) {
        if (label) {
            if (showMineMap[mineCell.getY()][mineCell.getX()] != -2) {
                showMineMap[mineCell.getY()][mineCell.getX()] = -2;
            } else {
                showMineMap[mineCell.getY()][mineCell.getX()] = -1;
            }
        }
        if (showMineMap[mineCell.getY()][mineCell.getX()] != -1) {
            return;
        }
        if (realMineMap[mineCell.getY()][mineCell.getX()] == 1) {
            System.out.println("踩雷了");
            showMineMap[mineCell.getY()][mineCell.getX()] = -9;
            win = false;
            return;
        }
        spreadCell(mineCell, width, height, -1);
    }

    /**
     * 从点击的格子开始向周围八个方向蔓延
     *
     * @param mineCell          点击的格子
     * @param width             扫雷图宽度
     * @param height            扫雷图长度
     * @param previousDirection 蔓延前的格子位于的方向，用于防止往回蔓延造成死循环
     */
    private void spreadCell(final MineCell mineCell, final int width, final int height, final int previousDirection) {
        int x = mineCell.getX();
        int y = mineCell.getY();

        int count, nextDirection;
        if ((count = getMineCount(mineCell, width, height)) > 0) {
            showMineMap[y][x] = count;
            return;
        }
        showMineMap[y][x] = count;

        for (int direction = 0; direction < 7; direction++) {
            if (direction == previousDirection) {
                continue;
            }
            MineCell nextMineCell;
            // 0← 1↖ 2↑ 3↗ 4→ 5↘ 6↓ 7↙
            switch (direction) {
                case 0:
                    nextMineCell = new MineCell(x - 1, y);
                    nextDirection = 4;
                    break;
                case 1:
                    nextMineCell = new MineCell(x - 1, y - 1);
                    nextDirection = 5;
                    break;
                case 2:
                    nextMineCell = new MineCell(x, y - 1);
                    nextDirection = 6;
                    break;
                case 3:
                    nextMineCell = new MineCell(x + 1, y - 1);
                    nextDirection = 7;
                    break;
                case 4:
                    nextMineCell = new MineCell(x + 1, y);
                    nextDirection = 0;
                    break;
                case 5:
                    nextMineCell = new MineCell(x + 1, y + 1);
                    nextDirection = 1;
                    break;
                case 6:
                    nextMineCell = new MineCell(x, y + 1);
                    nextDirection = 2;
                    break;
                case 7:
                    nextMineCell = new MineCell(x - 1, y + 1);
                    nextDirection = 3;
                    break;
                default:
                    throw new IllegalArgumentException("direction must belong to [0, 8) and must be integer");
            }
            if (nextMineCell.getX() < 0 || nextMineCell.getX() >= width || nextMineCell.getY() < 0 || nextMineCell.getY() >= height) {
                continue;
            }
            if (showMineMap[nextMineCell.getY()][nextMineCell.getX()] != -1) {
                continue;
            }
            if (realMineMap[nextMineCell.getY()][nextMineCell.getX()] != 0) {
                continue;
            }
            spreadCell(nextMineCell, width, height, nextDirection);
        }
    }

    /**
     * 获取周围8格雷的数量
     *
     * @param mineCell 格子
     * @param width    扫雷图宽度
     * @param height   扫雷图长度
     * @return 数量
     */
    private int getMineCount(final MineCell mineCell, final int width, final int height) {
        int count = 0;
        for (int x = mineCell.getX() - 1; x <= mineCell.getX() + 1; x++) {
            for (int y = mineCell.getY() - 1; y <= mineCell.getY() + 1; y++) {
                if (x == mineCell.getX() && y == mineCell.getY()) {
                    continue;
                }
                if (x < 0 || x >= width) {
                    continue;
                }
                if (y < 0 || y >= height) {
                    continue;
                }
                if (realMineMap[y][x] == 1) {
                    count++;
                }
            }
        }
        return count;
    }
}

//    static int[] getWidthAndHeight(String text, Font font) {
//        Rectangle2D r = font.getStringBounds(text, new FontRenderContext(AffineTransform.getScaleInstance(1, 1), false, false));
//        int unitHeight = (int) Math.floor(r.getHeight());
//        // 获取整个str用了font样式的宽度这里用四舍五入后+1保证宽度绝对能容纳这个字符串作为图片的宽度
//        int width = (int) Math.round(r.getWidth()) + 1;
//        // 把单个字符的高度+3保证高度绝对能容纳字符串作为图片的高度
//        int height = unitHeight + 3;
//        System.out.println("width:" + width + ", height:" + height);
//        return new int[]{width, height};
//    }

@Data
@NoArgsConstructor
@AllArgsConstructor
class MineCell {
    private int x;
    private int y;
}
