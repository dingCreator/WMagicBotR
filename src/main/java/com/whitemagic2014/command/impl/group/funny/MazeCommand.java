package com.whitemagic2014.command.impl.group.funny;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.cache.MazeCache;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.whitemagic2014.cache.MazeCache.showMaze;

/**
 * 随机prim算法生成迷宫
 */
@Command
public class MazeCommand extends NoAuthCommand {

    private static final Random RANDOM = new Random();
    private static final int DEFAULT_HEIGHT = 10;
    private static final int DEFAULT_WIDTH = 10;
    private static final int DEFAULT_START_X = 1;
    private static final int DEFAULT_START_Y = 1;

    private static final String FORMAT_INFO = "格式：初音生成迷宫 [宽] [高] [起点x] [起点y] [终点x] [终点y]\n"
            + "参数需成对存在，均可不填，默认生成10*10，起点在左上角，终点在右下角的迷宫";

    @Autowired
    private GlobalParam globalParam;

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "生成迷宫", globalParam.botNick + "生成迷宫");
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        int height = DEFAULT_HEIGHT;
        int width = DEFAULT_WIDTH;
        int startX = DEFAULT_START_X;
        int startY = DEFAULT_START_Y;
        int endX = DEFAULT_WIDTH;
        int endY = DEFAULT_HEIGHT;

        if (!CollectionUtils.isEmpty(args)) {
            if (args.size() % 2 != 0) {
                return new PlainText("参数数目不正确，" + FORMAT_INFO);
            }
        }

        try {
            if (args.size() >= 2) {
                width = Integer.parseInt(args.get(0).trim());
                height = Integer.parseInt(args.get(1).trim());

                endY = width;
                endX = height;
            }

            if (args.size() >= 4) {
                startX = Integer.parseInt(args.get(2).trim());
                startY = Integer.parseInt(args.get(3).trim());
            }

            if (args.size() >= 6) {
                endX = Integer.parseInt(args.get(4).trim());
                endY = Integer.parseInt(args.get(5).trim());
            }

            if (height < 1 || width < 1 || height > 150 || width > 150) {
                return new PlainText("只能生成1*1至150*150之间的迷宫");
            }

            int[][] maze = primMaze(height, width, startX, startY);
            File file = showMaze(maze, startX, startY, endX, endY, height, width, false);
            if (file == null) {
                return new PlainText("生成失败惹~");
            }
            MazeCache.setMazeCache(maze);
            return new At(sender.getId()).plus(Contact.uploadImage(subject, file));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return new PlainText("参数格式不正确，必须为数字");
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return new PlainText("图片被拦截");
        }
    }

    private int[][] primMaze(int height, int width, int startX, int startY) {
        int realHeight = height * 2 + 1;
        int realWidth = width * 2 + 1;
        int realStartX = startX * 2 - 1;
        int realStartY = startY * 2 - 1;

        int[][] maze = initMazeWall(realHeight, realWidth);
        Map<String, MazeCache.MazeCell> visitedMap = new HashMap<>(height * width);
        MazeCache.MazeCell startCell = new MazeCache.MazeCell(realStartX, realStartY);
        visitedMap.put(realStartX + "," + realStartY, startCell);
        List<MazeCache.MazeCell> wallList = new ArrayList<>(getCellWallList(startCell, realHeight, realWidth));

        while (!CollectionUtils.isEmpty(wallList)) {
            int wallIndex = RANDOM.nextInt(wallList.size());
            checkWallAndBreak(wallIndex, maze, visitedMap, wallList, realHeight, realWidth);
        }
        return maze;
    }

    int[][] initMazeWall(int height, int width) {
        int[][] maze = new int[height][width];
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                if (h % 2 == 0 || w % 2 == 0) {
                    maze[h][w] = 1;
                }
            }
        }
        return maze;
    }

    List<MazeCache.MazeCell> getCellWallList(MazeCache.MazeCell cell, int height, int width) {
        int x = cell.getX();
        int y = cell.getY();
        List<MazeCache.MazeCell> wallList = new ArrayList<>();
        wallList.add(new MazeCache.MazeCell(x - 1, y));
        wallList.add(new MazeCache.MazeCell(x + 1, y));
        wallList.add(new MazeCache.MazeCell(x, y - 1));
        wallList.add(new MazeCache.MazeCell(x, y + 1));
        return wallList.stream()
                .filter(c -> (c.getX() > 0 && c.getX() < width - 1 && c.getY() > 0 && c.getY() < height - 1))
                .collect(Collectors.toList());
    }

    void checkWallAndBreak(int wallIndex, int[][] maze, Map<String, MazeCache.MazeCell> visitedMap,
                           List<MazeCache.MazeCell> wallList, int height, int width) {
        MazeCache.MazeCell wall = wallList.get(wallIndex);
        int x = wall.getX();
        int y = wall.getY();
        int visitedCellCount = 0;
        MazeCache.MazeCell unvisitedCell = null;
        if (x % 2 != 0) {
            // 检查上下
            if (visitedMap.get(x + "," + (y - 1)) != null) {
                visitedCellCount++;
                unvisitedCell = new MazeCache.MazeCell(x, y + 1);
            }
            if (visitedMap.get(x + "," + (y + 1)) != null) {
                visitedCellCount++;
                unvisitedCell = new MazeCache.MazeCell(x, y - 1);
            }
        } else if (y % 2 != 0) {
            // 检查左右
            if (visitedMap.get((x - 1) + "," + y) != null) {
                visitedCellCount++;
                unvisitedCell = new MazeCache.MazeCell(x + 1, y);
            }
            if (visitedMap.get((x + 1) + "," + y) != null) {
                visitedCellCount++;
                unvisitedCell = new MazeCache.MazeCell(x - 1, y);
            }
        } else {
            return;
        }

        if (visitedCellCount == 1) {
            wallList.remove(wallIndex);
            visitedMap.put(unvisitedCell.getX() + "," + unvisitedCell.getY(), unvisitedCell);
            maze[y][x] = 0;
            wallList.addAll(getCellWallList(unvisitedCell, height, width));
        } else if (visitedCellCount == 2) {
            wallList.remove(wallIndex);
        }
    }
}
