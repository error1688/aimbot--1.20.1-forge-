package com.example.yourmod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class AimHandler {
    private float mYaw;                // 备份真实视角（用于FOV检测）
    private float aYaw, aPitch;        // 目标角度（每tick更新）
    private boolean isLocking = false;

    // 平滑瞄准相关（静默模式）
    private float smoothYaw, smoothPitch;   // 当前用于发送的平滑角度（仅静默模式）
    private boolean smoothNeedsReset = true;

    // 平滑瞄准相关（非静默模式）
    private float visualYaw, visualPitch;   // 当前用于本地视角的平滑角度（仅非静默模式）
    private boolean visualNeedsReset = true;

    // 陀螺功能
    private float gyroAngle = 0f;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !Config.ENABLED.get()) {
            isLocking = false;
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            handleStartPhase(mc);
        }

        if (event.phase == TickEvent.Phase.END) {
            handleEndPhase(mc);
        }
    }

    private void handleStartPhase(Minecraft mc) {
        // 备份真实视角（用于FOV判断）
        mYaw = mc.player.getYRot();

        LivingEntity target = findTarget(mc);
        if (target != null) {
            float[] angles = getAngles(mc.player, target);
            aYaw = angles[0];
            aPitch = angles[1];

            // 处理静默模式的平滑初始化
            if (!isLocking) {
                smoothYaw = mc.player.getYRot();
                smoothPitch = mc.player.getXRot();
                smoothNeedsReset = false;

                // 非静默模式也需要初始化视觉角度
                visualYaw = mc.player.getYRot();
                visualPitch = mc.player.getXRot();
                visualNeedsReset = false;
            }
            isLocking = true;
        } else {
            // 无目标：重置所有平滑状态
            isLocking = false;
            smoothYaw = mc.player.getYRot();
            smoothPitch = mc.player.getXRot();
            smoothNeedsReset = true;
            visualYaw = mc.player.getYRot();
            visualPitch = mc.player.getXRot();
            visualNeedsReset = true;
        }
    }

    private void handleEndPhase(Minecraft mc) {
        if (isLocking) {
            if (Config.SILENT_MODE.get()) {
                // 静默模式：平滑后发送数据包，不修改本地视角
                if (!smoothNeedsReset) {
                    float speed = Config.SMOOTHNESS.get().floatValue();
                    float yawDiff = Mth.degreesDifference(smoothYaw, aYaw);
                    float pitchDiff = Mth.degreesDifference(smoothPitch, aPitch);
                    smoothYaw += yawDiff * speed;
                    smoothPitch += pitchDiff * speed;
                    mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(smoothYaw, smoothPitch, mc.player.onGround()));
                }
            } else {
                // 非静默模式：平滑修改本地视角
                if (!visualNeedsReset) {
                    float speed = Config.SMOOTHNESS.get().floatValue();
                    float yawDiff = Mth.degreesDifference(visualYaw, aYaw);
                    float pitchDiff = Mth.degreesDifference(visualPitch, aPitch);
                    visualYaw += yawDiff * speed;
                    visualPitch += pitchDiff * speed;

                    // 应用视角到玩家实体
                    mc.player.setYRot(visualYaw);
                    mc.player.setXRot(visualPitch);
                    mc.player.yRotO = visualYaw;
                    mc.player.xRotO = visualPitch;
                }
            }
        } else {
            // 无自瞄目标且陀螺开启时，执行陀螺旋转（仅静默模式）
            if (Config.SILENT_MODE.get() && Config.GYRO_ENABLED.get()) {
                float speed = Config.GYRO_SPEED.get().floatValue();
                gyroAngle += speed;
                gyroAngle = gyroAngle % 360.0f;
                float pitch = (float) Config.GYRO_PITCH.get().doubleValue();
                mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(gyroAngle, pitch, mc.player.onGround()));
            }
        }
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // 静默模式下强制玩家身体和头部朝向目标（使用目标角度）
        if (isLocking && Config.SILENT_MODE.get() && event.getEntity() == Minecraft.getInstance().player) {
            Player p = event.getEntity();
            p.yHeadRot = aYaw;
            p.yHeadRotO = aYaw;
            p.yBodyRot = aYaw;
            p.yBodyRotO = aYaw;
        }
        // 非静默模式不需要强制，因为视角已变，身体会自然跟随
    }

    private LivingEntity findTarget(Minecraft mc) {
        Player player = mc.player;
        double maxDist = Config.MAX_DISTANCE.get();
        List<LivingEntity> entities = mc.level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(maxDist),
                e -> e != player && e.isAlive() && isValid(mc, e) && canSee(player, e));

        LivingEntity best = null;
        double minDist = maxDist;
        for (LivingEntity e : entities) {
            double d = player.distanceTo(e);
            if (d < minDist && (Config.TARGET_360.get() || isInFov(mc, e))) {
                minDist = d;
                best = e;
            }
        }
        return best;
    }

    private boolean isValid(Minecraft mc, LivingEntity e) {
        // 玩家
        if (e instanceof Player player) {
            // 好友
            if (Config.TARGET_FRIENDS.get()) {
                List<String> friends = Config.FRIEND_NAMES.get();
                if (friends.contains(player.getName().getString())) return true;
            }
            // 同队伍
            if (Config.TARGET_TEAMMATES.get()) {
                Player self = mc.player;
                if (self.getTeam() != null && self.getTeam() == player.getTeam()) return true;
            }
            // 其他玩家
            return Config.TARGET_PLAYER.get();
        }
        // 宠物
        if (e instanceof TamableAnimal && ((TamableAnimal) e).isTame()) {
            return Config.TARGET_PET.get();
        }
        // 敌对怪物
        if (e instanceof Monster) return Config.TARGET_HOSTILE.get();
        // BOSS
        if (e instanceof EnderDragon || e instanceof WitherBoss) return Config.TARGET_BOSS.get();
        // 中立生物
        if (e instanceof net.minecraft.world.entity.NeutralMob) return Config.TARGET_NEUTRAL.get();
        // 其他友善动物
        if (e instanceof Animal && !(e instanceof TamableAnimal)) return Config.TARGET_PASSIVE.get();
        // 村民
        if (e instanceof Villager) return Config.TARGET_VILLAGER.get();
        // 铁傀儡
        if (e instanceof IronGolem) return Config.TARGET_GOLEM.get();
        // 盔甲架
        if (e instanceof ArmorStand) return Config.TARGET_STAND.get();
        return false;
    }

    private boolean isInFov(Minecraft mc, LivingEntity t) {
        float[] a = getAngles(mc.player, t);
        float fov = Config.FOV_DEGREES.get().floatValue();
        return Math.abs(Mth.wrapDegrees(a[0] - mYaw)) < fov;
    }

    private boolean canSee(LivingEntity p, LivingEntity t) {
        Vec3 start = p.getEyePosition();
        Vec3 end = t.getEyePosition();
        return p.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS;
    }

    private float[] getAngles(Entity p, Entity t) {
        double dx = t.getX() - p.getX();
        double dz = t.getZ() - p.getZ();
        double dy = (t instanceof LivingEntity li ? li.getEyeY() : t.getY()) - p.getEyeY();
        double dXZ = Math.sqrt(dx * dx + dz * dz);
        return new float[]{
                (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90,
                (float) -(Math.atan2(dy, dXZ) * 180 / Math.PI)
        };
    }
}