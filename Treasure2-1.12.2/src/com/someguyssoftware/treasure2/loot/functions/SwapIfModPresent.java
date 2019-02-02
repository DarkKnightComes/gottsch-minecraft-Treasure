/**
 * 
 */
package com.someguyssoftware.treasure2.loot.functions;

import java.util.Random;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.someguyssoftware.gottschcore.item.util.ItemUtil;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.loot.TreasureLootContext;
import com.someguyssoftware.treasure2.loot.conditions.ModPresent;
import com.someguyssoftware.treasure2.loot.conditions.TreasureLootCondition;

import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;

/**
 * 
 * @author Mark Gottschling on Dec 6, 2018
 *
 */
public class SwapIfModPresent extends TreasureLootFunction {
	private final String modID;
	private final String name;

	/**
	 * 
	 * @param conditionsIn
	 * @param modID
	 * @param swapName
	 */
	public SwapIfModPresent(TreasureLootCondition[] conditionsIn, String name, String modID) {
		super(conditionsIn);
		this.modID = modID;
		this.name = name;
	}

	/**
	 * 
	 */
	public ItemStack apply(ItemStack stack, Random rand, TreasureLootContext context) {
		ModPresent mp = new ModPresent(this.modID);
		
		// if ModPresent then return the alternate item
		ItemStack newStack = stack;
		Treasure.logger.debug("Is mod present -> {}, {}", this.modID, mp.testCondition(rand, context));
		if (mp.testCondition(rand, context)) {
			newStack = new ItemStack(ItemUtil.getItemFromName(name));
			Treasure.logger.debug("swap item -> {}", newStack.getItem().getRegistryName());
		}
		return newStack;
	}

	/**
	 * 
	 * @author Mark Gottschling on Dec 6, 2018
	 *
	 */
	public static class Serializer extends TreasureLootFunction.Serializer<SwapIfModPresent> {
		protected Serializer() {
			super(new ResourceLocation("treasure2:swap_if_mod_present"), SwapIfModPresent.class);
		}

		/**
		 * 
		 */
		public void serialize(JsonObject object, SwapIfModPresent functionClazz, JsonSerializationContext serializationContext) {
			object.add("name", serializationContext.serialize(String.valueOf(functionClazz.name)));
			object.add("modid", serializationContext.serialize(String.valueOf(functionClazz.modID)));
		}

		/**
		 * 
		 */
		public SwapIfModPresent deserialize(JsonObject object, JsonDeserializationContext deserializationContext, TreasureLootCondition[] conditionsIn) {
			return new SwapIfModPresent(conditionsIn, 
					(String) JsonUtils.deserializeClass(object, "name", deserializationContext, String.class),
					(String) JsonUtils.deserializeClass(object, "modid", deserializationContext, String.class));
		}
	}
}