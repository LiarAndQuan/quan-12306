--Long result = stringRedisTemplate.execute(actual,
--Lists.newArrayList(actualHashKey, luaScriptKey), 
--JSON.toJSONString(seatTypeCountArray),
--JSON.toJSONString(takeoutRouteDTOList));

-- keys[2]就是用户购买的出发站点和结束站点
local inputString = KEYS[2]
local actualKey = inputString
-- 去除前缀
local colonIndex = string.find(actualKey, ":")
if colonIndex ~= nil then
    actualKey = string.sub(actualKey, colonIndex + 1)
end

--argv[1]是需要扣减的座位类型以及对应的数量
local jsonArrayStr = ARGV[1]
local jsonArray = cjson.decode(jsonArrayStr)

for index, jsonObj in ipairs(jsonArray) do
    local seatType = tonumber(jsonObj.seatType)
    local count = tonumber(jsonObj.count)
    local actualInnerHashKey = actualKey .. "_" .. seatType
    --获取指定座位类型的token余量
    local ticketSeatAvailabilityTokenValue = tonumber(redis.call('hget', KEYS[1], tostring(actualInnerHashKey)))
    --判断是否满足分配
    if ticketSeatAvailabilityTokenValue < count then
        return 1
    end
end

--需要扣减的站点
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
        --进行扣减操作
        redis.call('hincrby', KEYS[1], tostring(actualInnerHashKey), -count)
    end
end

return 0
