package online.aquan.index12306.biz.userservice.common.constant;

public class RedisKeyConstant {

    /**
     * 用户注册可复用用户名分片，Key Prefix + Idx
     */
    public static final String USER_REGISTER_REUSE_SHARDING = "index12306-user-service:user-reuse:";

    /**
     * 用户注册锁，Key Prefix + 用户名
     */
    public static final String LOCK_USER_REGISTER = "index12306-user-service:lock:user-register:";

}