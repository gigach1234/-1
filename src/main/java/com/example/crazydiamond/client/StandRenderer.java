package com.example.crazydiamond.client;

import com.example.crazydiamond.Ability;
import com.example.crazydiamond.CrazyDiamondMod;
import com.example.crazydiamond.StandEntity;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class StandRenderer extends EntityRenderer<StandEntity> {
	private static final Identifier TEXTURE =
			new Identifier(CrazyDiamondMod.MOD_ID, "textures/entity/crazy_diamond.png");

	private static final float[] GHOST_ALPHA = {0.55f, 0.40f, 0.28f};

	private final StandModel model;

	public StandRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.model = new StandModel(context.getPart(StandModel.LAYER));
		this.shadowRadius = 0.3f;
	}

	@Override
	public Identifier getTexture(StandEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(StandEntity entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - yaw));
		matrices.scale(-1.0f, -1.0f, 1.0f);
		matrices.translate(0.0, -1.501, 0.0);

		float t = entity.age + tickDelta;
		model.setAngles(entity, 0.0f, 0.0f, t, 0.0f, 0.0f);
		VertexConsumer consumer = vertexConsumers.getBuffer(model.getLayer(TEXTURE));
		model.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);

		// Barrage: extra translucent fists at other phases/angles = the "ORA ORA" blur
		if (entity.getClientAbility() == Ability.BARRAGE) {
			VertexConsumer ghost = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
			for (int i = 0; i < GHOST_ALPHA.length; i++) {
				model.poseGhostArms(entity, t, i);
				model.renderArms(matrices, ghost, light, OverlayTexture.DEFAULT_UV, GHOST_ALPHA[i]);
			}
		}

		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}
}
