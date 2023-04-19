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
import java.util.ArrayList;

@Command
public class SolvedMazeCommand extends NoAuthCommand {

    private static final String FORMAT_INFO = "格式：初音解迷宫 起点x 起点y 终点x 终点y\n注意空格";

    @Autowired
    private GlobalParam globalParam;

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "解迷宫", globalParam.botNick + "解迷宫");
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        int[][] maze;
        if ((maze = MazeCache.getMazeCache()) == null) {
            return new PlainText("还没有生成迷宫哦~");
        }

        if (CollectionUtils.isEmpty(args)) {
            return new PlainText("格式不正确，" + FORMAT_INFO);
        }

        if (args.size() != 4) {
            return new PlainText("格式不正确，" + FORMAT_INFO);
        }

        int startX;
        int startY;
        int endX;
        int endY;
        try {
            startX = Integer.parseInt(args.get(0).trim());
            startY = Integer.parseInt(args.get(1).trim());
            endX = Integer.parseInt(args.get(2).trim());
            endY = Integer.parseInt(args.get(3).trim());
        } catch (Exception e) {
            e.printStackTrace();
            return new PlainText("参数必须为纯数字");
        }
        File file = MazeCache.showMaze(solveMaze(maze, startX, startY, endX, endY),
                startX, startY, endX, endY, maze.length, maze[0].length, true);
        if (file == null) {
            return new PlainText("生成失败惹~");
        }
        return new At(sender.getId()).plus(Contact.uploadImage(subject, file));
    }

    private int[][] solveMaze(int[][] maze, int startX, int startY, int endX, int endY) {
        int realStartX = startX * 2 - 1;
        int realStartY = startY * 2 - 1;
        int realEndX = endX * 2 - 1;
        int realEndY = endY * 2 - 1;

        int[][] solveMaze = deepCopyArray(maze);
        if (findSolution(new MazeCache.MazeCell(realStartX, realStartY), -1, solveMaze, new MazeCache.MazeCell(realEndX, realEndY))) {
            return solveMaze;
        }
        return null;
    }

    private boolean findSolution(MazeCache.MazeCell thisCell, int previousDirection, int[][] maze, final MazeCache.MazeCell endPoint) {
        int nextDirection;

        if (thisCell.getX() == endPoint.getX() && thisCell.getY() == endPoint.getY()) {
            return true;
        }
        // 0-左 1-右 2-上 3-下
        for (int direction = 0; direction < 4; direction++) {
            MazeCache.MazeCell nextCell = new MazeCache.MazeCell(thisCell.getX(), thisCell.getY());
            switch (direction) {
                case 0:
                    if (direction == previousDirection || maze[thisCell.getY()][thisCell.getX() - 1] == 1) {
                        continue;
                    }
                    nextCell.setX(thisCell.getX() - 1);
                    nextDirection = 1;
                    break;
                case 1:
                    if (direction == previousDirection || maze[thisCell.getY()][thisCell.getX() + 1] == 1) {
                        continue;
                    }
                    nextCell.setX(thisCell.getX() + 1);
                    nextDirection = 0;
                    break;
                case 2:
                    if (direction == previousDirection || maze[thisCell.getY() - 1][thisCell.getX()] == 1) {
                        continue;
                    }
                    nextCell.setY(thisCell.getY() - 1);
                    nextDirection = 3;
                    break;
                case 3:
                    if (direction == previousDirection || maze[thisCell.getY() + 1][thisCell.getX()] == 1) {
                        continue;
                    }
                    nextCell.setY(thisCell.getY() + 1);
                    nextDirection = 2;
                    break;
                default:
                    return false;
            }
            maze[thisCell.getY()][thisCell.getX()] = 2;
            if (findSolution(nextCell, nextDirection, maze, endPoint)) {
                return true;
            }
        }
        maze[thisCell.getY()][thisCell.getX()] = 0;
        return false;
    }

    static int[][] deepCopyArray(int[][] src) {
        int[][] tar = new int[src.length][src[0].length];
        for (int y = 0; y < src.length; y++) {
            System.arraycopy(src[y], 0, tar[y], 0, src[y].length);
        }
        return tar;
    }
}
