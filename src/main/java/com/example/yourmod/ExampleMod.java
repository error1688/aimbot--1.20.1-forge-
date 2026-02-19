package com.example.yourmod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@Mod("yourmod")
public class ExampleMod {
    public static final KeyMapping KEY = new KeyMapping("自瞄菜单", GLFW.GLFW_KEY_Y, "自瞄");
    // 保存客户端配置对象，用于保存
    private static ModConfig clientConfig;

    public ExampleMod() {
        // 注册客户端配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        // 注册事件总线
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new AimHandler());

        // 注册 Mod 总线事件（配置加载和按键注册）
        net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onModConfigEvent);
        net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus().addListener(this::regKey);
    }

    public void regKey(RegisterKeyMappingsEvent e) {
        e.register(KEY);
    }

    // 监听配置加载事件
    @SubscribeEvent
    public void onModConfigEvent(ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if (config.getType() == ModConfig.Type.CLIENT && config.getModId().equals("yourmod")) {
            clientConfig = config;
        }
    }

    // 供 Config.save() 调用的静态方法
    public static void saveClientConfig() {
        if (clientConfig != null) {
            clientConfig.save();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            while (KEY.consumeClick()) {
                Minecraft.getInstance().setScreen(new AimScreen());
            }
        }
    }
}