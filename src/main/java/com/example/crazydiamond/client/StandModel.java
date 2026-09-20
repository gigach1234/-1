package com.example.crazydiamond.client;

import com.example.crazydiamond.Ability;
import com.example.crazydiamond.CrazyDiamondMod;
import com.example.crazydiamond.StandEntity;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Player-shaped model for a 64x64 skin (wide arms) with elbows and knees.
 * Hierarchy: body -> head, arms(->forearms); legs(->shins) hang off the root.
 * The body pivots at the hips, so twisting/leaning it moves the head and arms too.
 */
public class StandModel extends EntityModel<StandEntity> {
	public static final EntityModelLayer LAYER =
			new EntityModelLayer(new Identifier(CrazyDiamondMod.MOD_ID, "crazy_diamond"), "main");

	// indices into the pose array (3 floats each: pitch, yaw, roll)
	private static final int BODY = 0;
	private static final int HEAD = 1;
	private static final int R_ARM = 2;
	private static final int R_FORE = 3;
	private static final int L_ARM = 4;
	private static final int L_FORE = 5;
	private static final int R_LEG = 6;
	private static final int R_SHIN = 7;
	private static final int L_LEG = 8;
	private static final int L_SHIN = 9;
	private static final int PARTS = 10;

	private final ModelPart root;
	private final ModelPart[] parts = new ModelPart[PARTS];

	public StandModel(ModelPart root) {
		this.root = root;
		ModelPart body = root.getChild("body");
		parts[BODY] = body;
		parts[HEAD] = body.getChild("head");
		parts[R_ARM] = body.getChild("right_arm");
		parts[R_FORE] = parts[R_ARM].getChild("right_forearm");
		parts[L_ARM] = body.getChild("left_arm");
		parts[L_FORE] = parts[L_ARM].getChild("left_forearm");
		parts[R_LEG] = root.getChild("right_leg");
		parts[R_SHIN] = parts[R_LEG].getChild("right_shin");
		parts[L_LEG] = root.getChild("left_leg");
		parts[L_SHIN] = parts[L_LEG].getChild("left_shin");
	}

	// ---------------------------------------------------------------- shape

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		Dilation sleeve = new Dilation(0.25f);

		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(16, 16).cuboid(-4.0f, -12.0f, -2.0f, 8.0f, 12.0f, 4.0f)
						.uv(16, 32).cuboid(-4.0f, -12.0f, -2.0f, 8.0f, 12.0f, 4.0f, sleeve),
				ModelTransform.pivot(0.0f, 12.0f, 0.0f));

		body.addChild("head",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f)
						.uv(32, 0).cuboid(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new Dilation(0.5f)),
				ModelTransform.pivot(0.0f, -12.0f, 0.0f));

		// right arm (upper half uses the top half of the skin's arm, forearm the bottom half)
		ModelPartData rightArm = body.addChild("right_arm",
				ModelPartBuilder.create()
						.uv(40, 16).cuboid(-3.0f, -2.0f, -2.0f, 4.0f, 6.0f, 4.0f)
						.uv(40, 32).cuboid(-3.0f, -2.0f, -2.0f, 4.0f, 6.0f, 4.0f, sleeve),
				ModelTransform.pivot(-5.0f, -10.0f, 0.0f));
		rightArm.addChild("right_forearm",
				ModelPartBuilder.create()
						.uv(40, 22).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f)
						.uv(40, 38).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f, sleeve),
				ModelTransform.pivot(-1.0f, 4.0f, 0.0f));

		ModelPartData leftArm = body.addChild("left_arm",
				ModelPartBuilder.create()
						.uv(32, 48).cuboid(-1.0f, -2.0f, -2.0f, 4.0f, 6.0f, 4.0f)
						.uv(48, 48).cuboid(-1.0f, -2.0f, -2.0f, 4.0f, 6.0f, 4.0f, sleeve),
				ModelTransform.pivot(5.0f, -10.0f, 0.0f));
		leftArm.addChild("left_forearm",
				ModelPartBuilder.create()
						.uv(32, 54).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f)
						.uv(48, 54).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f, sleeve),
				ModelTransform.pivot(1.0f, 4.0f, 0.0f));

		ModelPartData rightLeg = root.addChild("right_leg",
				ModelPartBuilder.create()
						.uv(0, 16).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f)
						.uv(0, 32).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f, sleeve),
				ModelTransform.pivot(-1.9f, 12.0f, 0.0f));
		rightLeg.addChild("right_shin",
				ModelPartBuilder.create()
						.uv(0, 22).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f)
						.uv(0, 38).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f, sleeve),
				ModelTransform.pivot(0.0f, 6.0f, 0.0f));

		ModelPartData leftLeg = root.addChild("left_leg",
				ModelPartBuilder.create()
						.uv(16, 48).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f)
						.uv(0, 48).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f, sleeve),
				ModelTransform.pivot(1.9f, 12.0f, 0.0f));
		leftLeg.addChild("left_shin",
				ModelPartBuilder.create()
						.uv(16, 54).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f)
						.uv(0, 54).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f, sleeve),
				ModelTransform.pivot(0.0f, 6.0f, 0.0f));

		return TexturedModelData.of(data, 64, 64);
	}

	// ------------------------------------------------------------ animation

	@Override
	public void setAngles(StandEntity entity, float limbAngle, float limbDistance, float t, float headYaw, float headPitch) {
		float[] pose = new float[PARTS * 3];
		idlePose(pose, t);

		Ability ability = entity.getClientAbility();
		if (ability != null) {
			// ticks since the ability started (synced from the server)
			float at = entity.getAnimTime() + (t - entity.age);

			float[] target = new float[PARTS * 3];
			abilityPose(target, ability, at, t);

			float w = smooth(MathHelper.clamp(at / 3.0f, 0.0f, 1.0f));
			for (int i = 0; i < pose.length; i++) {
				pose[i] = MathHelper.lerp(w, pose[i], target[i]);
			}
		}
		apply(pose);
	}

	private void apply(float[] p) {
		for (int i = 0; i < PARTS; i++) {
			parts[i].pitch = p[i * 3];
			parts[i].yaw = p[i * 3 + 1];
			parts[i].roll = p[i * 3 + 2];
		}
	}

	private static void set(float[] p, int part, float pitch, float yaw, float roll) {
		p[part * 3] = pitch;
		p[part * 3 + 1] = yaw;
		p[part * 3 + 2] = roll;
	}

	private static float smooth(float x) {
		return x * x * (3.0f - 2.0f * x);
	}

	private static void arms(float[] p, float rUp, float rUpRoll, float rFore, float lUp, float lUpRoll, float lFore) {
		set(p, R_ARM, rUp, 0.0f, rUpRoll);
		set(p, R_FORE, rFore, 0.0f, 0.0f);
		set(p, L_ARM, lUp, 0.0f, lUpRoll);
		set(p, L_FORE, lFore, 0.0f, 0.0f);
	}

	/** Neutral fighting stance. */
	private static void stance(float[] p, boolean wide) {
		set(p, BODY, 0.0f, 0.0f, 0.0f);
		set(p, HEAD, 0.0f, 0.0f, 0.0f);
		if (wide) {
			set(p, R_LEG, -0.75f, 0.0f, 0.15f);
			set(p, R_SHIN, 0.95f, 0.0f, 0.0f);
			set(p, L_LEG, 0.50f, 0.0f, -0.15f);
			set(p, L_SHIN, 0.55f, 0.0f, 0.0f);
		} else {
			set(p, R_LEG, -0.35f, 0.0f, 0.10f);
			set(p, R_SHIN, 0.45f, 0.0f, 0.0f);
			set(p, L_LEG, 0.25f, 0.0f, -0.10f);
			set(p, L_SHIN, 0.25f, 0.0f, 0.0f);
		}
	}

	/**
	 * The standing pose: torso twisted, weight on one leg, hands framing the face
	 * (right hand raised beside the face, left forearm folded under the chin).
	 */
	private static void idlePose(float[] p, float t) {
		float sway = MathHelper.sin(t * 0.08f);
		set(p, BODY, 0.0f, -0.25f + sway * 0.03f, 0.10f);
		set(p, HEAD, 0.06f, 0.40f, -0.18f);

		set(p, R_ARM, -0.50f, 0.0f, 2.70f + sway * 0.04f);
		set(p, R_FORE, -0.60f, 0.0f, 1.70f - sway * 0.05f);
		set(p, L_ARM, -0.60f, 0.0f, -2.20f - sway * 0.04f);
		set(p, L_FORE, -0.80f, 0.0f, -2.30f + sway * 0.05f);

		set(p, R_LEG, -0.30f, 0.0f, 0.20f);
		set(p, R_SHIN, 0.55f, 0.0f, 0.0f);
		set(p, L_LEG, 0.15f, 0.0f, -0.12f);
		set(p, L_SHIN, 0.10f, 0.0f, 0.0f);
	}

	private static void abilityPose(float[] p, Ability ability, float at, float t) {
		switch (ability) {
			case PUNCH -> {
				stance(p, false);
				float e = MathHelper.sin(MathHelper.PI * MathHelper.clamp(at / 6.0f, 0.0f, 1.0f));
				arms(p, MathHelper.lerp(e, -0.7f, -1.55f), 0.05f, MathHelper.lerp(e, -1.3f, -0.05f),
						-0.9f, -0.3f, -1.5f);
				set(p, BODY, 0.0f, MathHelper.lerp(e, 0.0f, -0.35f), 0.0f);
			}
			case BARRAGE -> barragePose(p, at, 0.0f, 0.0f);
			case RETURN_BLOCK -> {
				stance(p, false);
				float w = 0.15f * MathHelper.sin(at * 0.4f);
				arms(p, -1.9f + w, 0.25f, -0.2f, -1.9f + w, -0.25f, -0.2f);
				set(p, BODY, -0.15f, 0.0f, 0.0f);
				set(p, HEAD, 0.2f, 0.0f, 0.0f);
			}
			case HEAL_MODE -> {
				stance(p, false);
				float w = 0.10f * MathHelper.sin(at * 0.5f);
				arms(p, -1.35f + w, 0.35f, -0.15f, -1.35f - w, -0.35f, -0.15f);
				set(p, BODY, 0.10f, 0.0f, 0.0f);
			}
			case STONE_SHOT -> {
				stance(p, false);
				float up;
				float fore;
				float yaw;
				if (at < 5.0f) {
					float k = smooth(at / 5.0f);
					up = MathHelper.lerp(k, -0.3f, 0.9f);
					fore = MathHelper.lerp(k, -0.6f, -1.2f);
					yaw = MathHelper.lerp(k, 0.0f, 0.4f);
				} else {
					float k = smooth(MathHelper.clamp((at - 5.0f) / 3.0f, 0.0f, 1.0f));
					up = MathHelper.lerp(k, 0.9f, -1.6f);
					fore = MathHelper.lerp(k, -1.2f, -0.05f);
					yaw = MathHelper.lerp(k, 0.4f, -0.4f);
				}
				arms(p, up, 0.1f, fore, -1.4f, -0.1f, -0.2f);
				set(p, BODY, 0.0f, yaw, 0.0f);
			}
			case DISASSEMBLE -> {
				stance(p, false);
				float spread = smooth(MathHelper.clamp((at - 9.0f) / 5.0f, 0.0f, 1.0f));
				arms(p, -1.0f, 0.15f + spread * 0.9f, -1.3f, -1.0f, -0.15f - spread * 0.9f, -1.3f);
				set(p, BODY, 0.08f, 0.0f, 0.0f);
			}
			case REPAIR_ITEM -> {
				stance(p, false);
				float m = MathHelper.sin(at * 0.9f);
				arms(p, -0.9f, 0.10f, -1.2f - 0.4f * m, -0.9f, -0.10f, -1.2f + 0.4f * m);
				set(p, BODY, 0.10f, 0.0f, 0.0f);
			}
		}
	}

	/** Fast alternating jabs, leaning forward. phase/fan let the renderer draw extra "ghost" fists. */
	private static void barragePose(float[] p, float at, float phase, float fan) {
		stance(p, true);
		float ph = at * 2.4f + phase;
		float sr = (MathHelper.sin(ph) + 1.0f) * 0.5f;
		float sl = 1.0f - sr;
		arms(p,
				MathHelper.lerp(sr, -0.7f, -1.55f) + fan, 0.10f, MathHelper.lerp(sr, -1.3f, -0.05f),
				MathHelper.lerp(sl, -0.7f, -1.55f) + fan, -0.10f, MathHelper.lerp(sl, -1.3f, -0.05f));
		set(p, BODY, 0.35f, 0.3f * MathHelper.sin(at * 1.2f), 0.0f);
		set(p, HEAD, -0.30f, 0.0f, 0.0f);
	}

	/** Re-poses only the arms for a barrage afterimage. Call after the normal render. */
	public void poseGhostArms(StandEntity entity, float t, int index) {
		float at = entity.getAnimTime() + (t - entity.age);
		float[] p = new float[PARTS * 3];
		float phase = 0.8f * (index + 1);
		float fan = (index % 2 == 0 ? 1.0f : -1.0f) * (0.30f + 0.12f * index);
		barragePose(p, at, phase, fan);
		for (int part : new int[] {R_ARM, R_FORE, L_ARM, L_FORE}) {
			parts[part].pitch = p[part * 3];
			parts[part].yaw = p[part * 3 + 1];
			parts[part].roll = p[part * 3 + 2];
		}
	}

	public void renderArms(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float alpha) {
		matrices.push();
		parts[BODY].rotate(matrices);
		parts[R_ARM].render(matrices, vertices, light, overlay, 1.0f, 1.0f, 1.0f, alpha);
		parts[L_ARM].render(matrices, vertices, light, overlay, 1.0f, 1.0f, 1.0f, alpha);
		matrices.pop();
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
			float red, float green, float blue, float alpha) {
		root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
	}
}
