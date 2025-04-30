package com.femboynuggy.helpfulnpcs.client;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.femboynuggy.helpfulnpcs.HelpfulNPCs;
import com.femboynuggy.helpfulnpcs.entity.WorkerEntity;
import com.femboynuggy.helpfulnpcs.registry.ModEntities;
import com.femboynuggy.helpfulnpcs.registry.ModMenus;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
  modid = HelpfulNPCs.MODID,
  bus   = Mod.EventBusSubscriber.Bus.MOD,
  value = Dist.CLIENT
)
public class ClientSetup {

  // 1) Define your texture arrays
  public static final ResourceLocation[] BODIES = {
    new ResourceLocation(HelpfulNPCs.MODID, "textures/entity/body1.png"),
    // … add more bodies here …
  };

  public static final ResourceLocation[] OUTFITS = {
    new ResourceLocation(HelpfulNPCs.MODID, "textures/entity/outfit1.png"),
    new ResourceLocation(HelpfulNPCs.MODID, "textures/entity/outfit2.png"),
    new ResourceLocation(HelpfulNPCs.MODID, "textures/entity/outfit3.png"),
    new ResourceLocation(HelpfulNPCs.MODID, "textures/entity/outfit4.png")
  };

  public static final ResourceLocation[] EYES = {
    new ResourceLocation(HelpfulNPCs.MODID, "textures/entity/eyes1.png"),
    new ResourceLocation(HelpfulNPCs.MODID, "textures/entity/eyes2.png"),
    new ResourceLocation(HelpfulNPCs.MODID, "textures/entity/eyes3.png")
  };

  public static final ResourceLocation[] HAIR = {
    new ResourceLocation(HelpfulNPCs.MODID, "textures/entity/hair1.png"),
    new ResourceLocation(HelpfulNPCs.MODID, "textures/entity/hair2.png"),
    new ResourceLocation(HelpfulNPCs.MODID, "textures/entity/hair3.png")
  };

  // 2) Cache composite textures per‐entityID
  private static final Map<Integer,ResourceLocation> CACHE = new ConcurrentHashMap<>();

  @SubscribeEvent
  public static void onClientSetup(FMLClientSetupEvent evt) {
    evt.enqueueWork(() -> {
      MenuScreens.register(ModMenus.WORKER_MENU.get(), WorkerScreen::new);
    });
  }

  @SubscribeEvent
  public static void registerRenderers(EntityRenderersEvent.RegisterRenderers evt) {
    evt.registerEntityRenderer(ModEntities.WORKER.get(),
      (EntityRendererProvider.Context ctx) -> {
        HumanoidMobRenderer<WorkerEntity,HumanoidModel<WorkerEntity>> renderer =
          new HumanoidMobRenderer<>(ctx,
            new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)),
            0.5f
          ) {
            @Override
            public ResourceLocation getTextureLocation(WorkerEntity worker) {
              return getOrCreateCombined(worker);
            }
          };

        ModelManager mm = Minecraft.getInstance().getModelManager();
        renderer.addLayer(new HumanoidArmorLayer<>(
          renderer,
          new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
          new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
          mm
        ));

        return renderer;
      }
    );
  }

  private static ResourceLocation getOrCreateCombined(WorkerEntity worker) {
    int id = worker.getId();
    return CACHE.computeIfAbsent(id, i -> {
      try {
        NativeImage base  = loadImage(BODIES[worker.getBodyIndex()]);
        NativeImage over1 = loadImage(OUTFITS[worker.getOutfitIndex()]);
        NativeImage over2 = loadImage(EYES[worker.getEyesIndex()]);
        NativeImage over3 = loadImage(HAIR[worker.getHairIndex()]);

        // composite in order: outfit -> eyes -> hair
        overlay(base, over1);
        overlay(base, over2);
        overlay(base, over3);

        DynamicTexture dyn = new DynamicTexture(base);
        ResourceLocation loc = new ResourceLocation(
          HelpfulNPCs.MODID, "textures/entity/worker_combined_" + id + ".png"
        );
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        tm.register(loc, dyn);
        return loc;

      } catch (IOException ex) {
        //SimpleWorkers.LOGGER.error("Failed to compose worker textures!", ex);
        // fallback to first body:
        return BODIES[0];
      }
    });
  }

  private static NativeImage loadImage(ResourceLocation loc) throws IOException {
    ResourceManager rm = Minecraft.getInstance().getResourceManager();
    Resource r = rm.getResource(loc)
                   .orElseThrow(() -> new IOException("Missing resource " + loc));
    try (var in = r.open()) {
      return NativeImage.read(in);
    }
  }

  private static void overlay(NativeImage base, NativeImage layer) {
    int w = Math.min(base.getWidth(),  layer.getWidth());
    int h = Math.min(base.getHeight(), layer.getHeight());
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int px = layer.getPixelRGBA(x, y);
        if ((px >>> 24) != 0) {
          base.setPixelRGBA(x, y, px);
        }
      }
    }
  }
}