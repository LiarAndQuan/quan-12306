-- Long result = stringRedisTemplate.execute(actual, 
--Lists.newArrayList(actualHashKey, luaScriptKey), 
--JSON.toJSONString(seatTypeCountArray), 
--JSON.toJSONString(takeoutRouteDTOList));

--找到出发站点_结束站点
local inputString = KEYS[2]
local actualKey = inputString
local colonIndex = string.find(actualKey, ":")
if colonIndex ~= nil then
    actualKey = string.sub(actualKey, colonIndex + 1)
end

local jsonArrayStr = ARGV[1]
local jsonArray = cjson.decode(jsonArrayStr)
local alongJsonArrayStr = ARGV[2]
local alongJsonArray = cjson.decode(alongJsonArrayStr)

--遍历座位
for index, jsonObj in ipairs(jsonArray) do
    local seatType = tonumber(jsonObj.seatType)
    local count = tonumber(jsonObj.count)
    --遍历需要扣减的站点
    for indexTwo, alongJsonObj in ipairs(alongJsonArray) do
        local startStation = tostring(alongJsonObj.startStation)
        local endStation = tostring(alongJsonObj.endStation)
        local actualInnerHashKey = startStation .. "_" .. endStation .. "_" .. seatType
        --获取对应的token数量
        local ticketSeatAvailabilityTokenValue = tonumber(redis.call('hget', KEYS[1], tostring(actualInnerHashKey)))
        --增加token数量
        if ticketSeatAvailabilityTokenValue >= 0 then
            redis.call('hincrby', KEYS[1], tostring(actualInnerHashKey), count)
        end
    end
end

return 0
