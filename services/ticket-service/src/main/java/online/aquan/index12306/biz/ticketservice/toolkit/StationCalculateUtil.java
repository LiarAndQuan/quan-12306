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

package online.aquan.index12306.biz.ticketservice.toolkit;

import online.aquan.index12306.biz.ticketservice.dto.domain.RouteDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 站点计算工具
 */
public final class StationCalculateUtil {

    /**
     * 计算出发站和终点站中间的站点（包含出发站和终点站）
     *
     * @param stations     所有站点数据
     * @param startStation 出发站
     * @param endStation   终点站
     * @return 出发站和终点站中间的站点（包含出发站和终点站）
     */
    public static List<RouteDTO> throughStation(List<String> stations, String startStation, String endStation) {
        List<RouteDTO> routesToDeduct = new ArrayList<>();
        // 获取到用户的开始站点和结束站点在列车所有站点中的下标
        int startIndex = stations.indexOf(startStation);
        int endIndex = stations.indexOf(endStation);
        if (startIndex < 0 || endIndex < 0 || startIndex >= endIndex) {
            return routesToDeduct;
        }
        for (int i = startIndex; i < endIndex; i++) {
            for (int j = i + 1; j <= endIndex; j++) {
                String currentStation = stations.get(i);
                String nextStation = stations.get(j);
                RouteDTO routeDTO = new RouteDTO(currentStation, nextStation);
                routesToDeduct.add(routeDTO);
            }
        }
        // 获取到在a,b,c,d中,如果用户从a-d,那么返回(a-b,a-c,a-d,b-c,b-d,c-d)这个集合
        return routesToDeduct;
    }

    /**
     * 计算出发站和终点站需要扣减余票的站点（包含出发站和终点站）
     *
     * @param stations     所有站点数据
     * @param startStation 出发站
     * @param endStation   终点站
     * @return 出发站和终点站需要扣减余票的站点（包含出发站和终点站）
     */
    public static List<RouteDTO> takeoutStation(List<String> stations, String startStation, String endStation) {
        List<RouteDTO> takeoutStationList = new ArrayList<>();
        int startIndex = stations.indexOf(startStation);
        int endIndex = stations.indexOf(endStation);
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            return takeoutStationList;
        }
        //出发站之前的每一个站点都需要扣减出发站+1~列车终点站的票,因为中间票被买去了
        for (int i = 0; i < startIndex; i++) {
            for (int j = 1; j < stations.size() - startIndex; j++) {
                takeoutStationList.add(new RouteDTO(stations.get(i), stations.get(startIndex + j)));
            }
        }
        //出发点-结束点的站点都需要减去自身一直到结束的票
        for (int i = startIndex; i <= endIndex; i++) {
            for (int j = i + 1; j < stations.size() && i < endIndex; j++) {
                takeoutStationList.add(new RouteDTO(stations.get(i), stations.get(j)));
            }
        }
        return takeoutStationList;
    }

    public static void main(String[] args) {
        List<String> stations = Arrays.asList("北京南", "济南西", "南京南", "杭州东", "宁波");
        String startStation = "北京南";
        String endStation = "南京南";
        StationCalculateUtil.takeoutStation(stations, startStation, endStation).forEach(System.out::println);
    }
}
