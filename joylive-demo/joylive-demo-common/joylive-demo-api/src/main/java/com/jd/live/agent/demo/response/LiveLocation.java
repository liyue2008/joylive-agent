/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.demo.response;

import com.jd.live.agent.core.util.network.Ipv4;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.function.Function;

@Getter
@Setter
public class LiveLocation implements Serializable {
    private String liveSpaceId;
    private String unit;
    private String cell;
    private String ruleId;
    private String laneSpaceId;
    private String lane;
    private String ip;

    public LiveLocation() {

    }

    public LiveLocation(String liveSpaceId, String unit, String cell, String ruleId, String laneSpaceId, String lane) {
        this.liveSpaceId = liveSpaceId;
        this.unit = unit;
        this.cell = cell;
        this.ruleId = ruleId;
        this.laneSpaceId = laneSpaceId;
        this.lane = lane;
        this.ip = Ipv4.getLocalIp();
    }

    public static LiveLocation build() {
        return new LiveLocation(
                System.getProperty(LiveTransmission.X_LIVE_SPACE_ID),
                System.getProperty(LiveTransmission.X_LIVE_UNIT),
                System.getProperty(LiveTransmission.X_LIVE_CELL),
                System.getProperty(LiveTransmission.X_LIVE_RULE_ID),
                System.getProperty(LiveTransmission.X_LANE_SPACE_ID),
                System.getProperty(LiveTransmission.X_LANE_CODE)
        );
    }

    public static LiveLocation build(Function<String, String> tagFunc) {
        return new LiveLocation(
                tagFunc.apply(LiveTransmission.X_LIVE_SPACE_ID),
                tagFunc.apply(LiveTransmission.X_LIVE_UNIT),
                tagFunc.apply(LiveTransmission.X_LIVE_CELL),
                tagFunc.apply(LiveTransmission.X_LIVE_RULE_ID),
                tagFunc.apply(LiveTransmission.X_LANE_SPACE_ID),
                tagFunc.apply(LiveTransmission.X_LANE_CODE)
        );
    }

    @Override
    public String toString() {
        return "Location{" +
                "live-space-id=" + (liveSpaceId == null ? "null" : ('\'' + liveSpaceId + '\'')) +
                ", unit=" + (unit == null ? "null" : ('\'' + unit + '\'')) +
                ", cell=" + (cell == null ? "null" : ('\'' + cell + '\'')) +
                ", ruleId=" + (ruleId == null ? "null" : ('\'' + ruleId + '\'')) +
                ", lane-space-id=" + (laneSpaceId == null ? "null" : ('\'' + laneSpaceId + '\'')) +
                ", lane=" + (lane == null ? "null" : ('\'' + lane + '\'')) +
                ", ip=" + (ip == null ? "null" : ('\'' + ip + '\'')) +
                '}';
    }
}
