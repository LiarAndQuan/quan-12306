/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package online.aquan.index12306.biz.ticketservice.service.handler.ticket.select;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 座位选择器
 *
 */
public class SeatSelection {

    public static int[][] adjacent(int numSeats, int[][] seatLayout) {
        //获取行和列
        int numRows = seatLayout.length;
        int numCols = seatLayout[0].length;
        List<int[]> selectedSeats = new ArrayList<>();
        //然后
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                //如果这个位置可用
                if (seatLayout[i][j] == 0) {
                    //连续座位标识
                    int consecutiveSeats = 0;
                    //往右遍历
                    for (int k = j; k < numCols; k++) {
                        //算上自己
                        if (seatLayout[i][k] == 0) {
                            //连续+1,其实默认就是1,因为可能传进来的numSeats就是1
                            consecutiveSeats++;
                            //如果找到了那么多个连续的
                            if (consecutiveSeats == numSeats) {
                                //然后倒序加入座位
                                for (int l = k - numSeats + 1; l <= k; l++) {
                                    selectedSeats.add(new int[]{i, l});
                                }
                                break;
                            }
                        } else {
                            //重置
                            consecutiveSeats = 0;
                        }
                    }
                    //如果不为空说明找到了连续的,break
                    if (!selectedSeats.isEmpty()) {
                        break;
                    }
                }
            }
            if (!selectedSeats.isEmpty()) {
                break;
            }
        }
        if (CollUtil.isEmpty(selectedSeats)) {
            return null;
        }
        //转化成真实的座位就好了
        int[][] actualSeat = new int[numSeats][2];
        int i = 0;
        for (int[] seat : selectedSeats) {
            int row = seat[0] + 1;
            int col = seat[1] + 1;
            actualSeat[i][0] = row;
            actualSeat[i][1] = col;
            i++;
        }
        return actualSeat;
    }

    public static int[][] nonAdjacent(int numSeats, int[][] seatLayout) {
        int numRows = seatLayout.length;
        int numCols = seatLayout[0].length;
        List<int[]> selectedSeats = new ArrayList<>();
        //不连续的只需要找到空闲座位就可以了
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                if (seatLayout[i][j] == 0) {
                    selectedSeats.add(new int[]{i, j});
                    if (selectedSeats.size() == numSeats) {
                        break;
                    }
                }
            }
            if (selectedSeats.size() == numSeats) {
                break;
            }
        }
        return convertToActualSeat(selectedSeats);
    }

    private static int[][] convertToActualSeat(List<int[]> selectedSeats) {
        int[][] actualSeat = new int[selectedSeats.size()][2];
        for (int i = 0; i < selectedSeats.size(); i++) {
            int[] seat = selectedSeats.get(i);
            int row = seat[0] + 1;
            int col = seat[1] + 1;
            actualSeat[i][0] = row;
            actualSeat[i][1] = col;
        }
        return actualSeat;
    }

    public static void main(String[] args) {
        int[][] seatLayout = {
                {1, 1, 1, 1},
                {1, 1, 1, 0},
                {1, 1, 1, 0},
                {0, 0, 0, 0}
        };
        int[][] select = adjacent(2, seatLayout);
        System.out.println("成功预订相邻座位，座位位置为：");
        assert select != null;
        for (int[] ints : select) {
            System.out.printf("第 %d 排，第 %d 列%n", ints[0], ints[1]);
        }

        int[][] seatLayoutTwo = {
                {1, 0, 1, 1},
                {1, 1, 0, 0},
                {1, 1, 1, 0},
                {0, 0, 0, 0}
        };
        int[][] selectTwo = nonAdjacent(3, seatLayoutTwo);
        System.out.println("成功预订不相邻座位，座位位置为：");
        for (int[] ints : selectTwo) {
            System.out.printf("第 %d 排，第 %d 列%n", ints[0], ints[1]);
        }
    }
}
