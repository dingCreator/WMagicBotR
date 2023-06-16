package com.whitemagic2014.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class MazeCache {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MazeCell {
        int x;
        int y;
    }

    private static final String WALL_ICON = "img/maze/icon/wall.png";
    private static final String CELL_ICON = "img/maze/icon/cell.png";
    private static final String ROAD_ICON = "img/maze/icon/road.png";
    private static final String START_POINT_ICON = "img/maze/icon/startPoint.png";
    private static final String END_POINT_ICON = "img/maze/icon/endPoint.png";
    private static final String RESULT_JPG = "img/maze/result.jpg";
    private static final String SOLUTION_JPG = "img/maze/solution.jpg";

    private static int[][] mazeCache = null;

    public static void setMazeCache(int[][] maze) {
        mazeCache = maze;
    }

    public static int[][] getMazeCache() {
        return mazeCache;
    }

    public static File showMaze(int[][] maze, int startX, int startY, int endX, int endY, int height, int width, boolean solved) {
        int realStartX = startX * 2 - 1;
        int realStartY = startY * 2 - 1;
        int realEndX = endX * 2 - 1;
        int realEndY = endY * 2 - 1;
        int realHeight = solved ? height : height * 2 + 1;
        int realWidth = solved ? width : width * 2 + 1;

        BufferedImage image = new BufferedImage(30 * realWidth, 30 * realHeight, BufferedImage.TYPE_INT_RGB);
        try {
            BufferedImage blackImage = ImageIO.read(new File(WALL_ICON));
            BufferedImage whiteImage = ImageIO.read(new File(CELL_ICON));
            BufferedImage redImage = ImageIO.read(new File(ROAD_ICON));
            BufferedImage startImage = ImageIO.read(new File(START_POINT_ICON));
            BufferedImage endImage = ImageIO.read(new File(END_POINT_ICON));
            int[] blackRgb = blackImage.getRGB(0, 0, 30, 30, new int[30 * 30], 0, 30);
            int[] whiteRgb = whiteImage.getRGB(0, 0, 30, 30, new int[30 * 30], 0, 30);
            int[] redRgb = redImage.getRGB(0, 0, 30, 30, new int[30 * 30], 0, 30);
            int[] startRgb = startImage.getRGB(0, 0, 30, 30, new int[30 * 30], 0, 30);
            int[] endRgb = endImage.getRGB(0, 0, 30, 30, new int[30 * 30], 0, 30);

            for (int y = 0; y < maze.length; y++) {
                for (int x = 0; x < maze[y].length; x++) {
                    if (x == realStartX && y == realStartY) {
                        image.setRGB(30 * x, 30 * y, 30, 30, startRgb, 0, 30);
                        continue;
                    }
                    if (x == realEndX && y == realEndY) {
                        image.setRGB(30 * x, 30 * y, 30, 30, endRgb, 0, 30);
                        continue;
                    }

                    if (!solved) {
                        if (maze[y][x] == 2) {
                            maze[y][x] = 0;
                        }
                    } else {
                        if (maze[y][x] == 2) {
                            image.setRGB(30 * x, 30 * y, 30, 30, redRgb, 0, 30);
                            continue;
                        }
                    }

                    if (maze[y][x] == 0) {
                        image.setRGB(30 * x, 30 * y, 30, 30, whiteRgb, 0, 30);
                    } else if (maze[y][x] == 1) {
                        image.setRGB(30 * x, 30 * y, 30, 30, blackRgb, 0, 30);
                    }
                }
            }

            File file;
            if (solved) {
                file = new File(SOLUTION_JPG);
            } else {
                file = new File(RESULT_JPG);
            }
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            ImageIO.write(image, "jpg", file);
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
