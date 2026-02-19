package com.example.yourmod;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.ArrayList;
import java.util.List;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // 核心开关
    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER.define("enabled", true);
    public static final ForgeConfigSpec.BooleanValue SILENT_MODE = BUILDER.define("silentMode", true);
    public static final ForgeConfigSpec.BooleanValue TARGET_360 = BUILDER.define("target360", true);
    public static final ForgeConfigSpec.DoubleValue FOV_DEGREES = BUILDER.defineInRange("fovDegrees", 30.0, 1.0, 50.0);
    public static final ForgeConfigSpec.DoubleValue SMOOTHNESS = BUILDER.defineInRange("smoothness", 0.5, 0.01, 1.0);
    public static final ForgeConfigSpec.DoubleValue MAX_DISTANCE = BUILDER.defineInRange("maxDistance", 30.0, 5.0, 100.0);

    // 目标过滤
    public static final ForgeConfigSpec.BooleanValue TARGET_PLAYER = BUILDER.define("targetPlayer", false);
    public static final ForgeConfigSpec.BooleanValue TARGET_HOSTILE = BUILDER.define("targetHostile", true);
    public static final ForgeConfigSpec.BooleanValue TARGET_BOSS = BUILDER.define("targetBoss", true);
    public static final ForgeConfigSpec.BooleanValue TARGET_NEUTRAL = BUILDER.define("targetNeutral", false);
    public static final ForgeConfigSpec.BooleanValue TARGET_PASSIVE = BUILDER.define("targetPassive", false);
    public static final ForgeConfigSpec.BooleanValue TARGET_VILLAGER = BUILDER.define("targetVillager", false);
    public static final ForgeConfigSpec.BooleanValue TARGET_GOLEM = BUILDER.define("targetGolem", false);
    public static final ForgeConfigSpec.BooleanValue TARGET_STAND = BUILDER.define("targetStand", false);
    // 新增过滤
    public static final ForgeConfigSpec.BooleanValue TARGET_PET = BUILDER.define("targetPet", false);          // 宠物（已驯服动物）
    public static final ForgeConfigSpec.BooleanValue TARGET_FRIENDS = BUILDER.define("targetFriends", false); // 好友（指定名称）
    public static final ForgeConfigSpec.BooleanValue TARGET_TEAMMATES = BUILDER.define("targetTeammates", false); // 同队伍玩家
    public static final ForgeConfigSpec.ConfigValue<List<String>> FRIEND_NAMES = BUILDER.define("friendNames", new ArrayList<>(), (o) -> o instanceof List);

    // 陀螺娱乐功能
    public static final ForgeConfigSpec.BooleanValue GYRO_ENABLED = BUILDER.define("gyroEnabled", false);
    public static final ForgeConfigSpec.DoubleValue GYRO_SPEED = BUILDER.defineInRange("gyroSpeed", 5.0, 0.1, 50.0);
    public static final ForgeConfigSpec.DoubleValue GYRO_PITCH = BUILDER.defineInRange("gyroPitch", 85.0, -90.0, 90.0);

    // UI 设置
    public static final ForgeConfigSpec.BooleanValue BLUR_BACKGROUND = BUILDER.define("blurBackground", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // 保存配置到文件（委托给 ExampleMod）
    public static void save() {
        ExampleMod.saveClientConfig();
    }
}