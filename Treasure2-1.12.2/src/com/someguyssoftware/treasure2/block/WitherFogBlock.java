/**
 * 
 */
package com.someguyssoftware.treasure2.block;

import java.util.Map;

import com.someguyssoftware.treasure2.enums.FogHeight;

import net.minecraft.block.material.Material;

/**
 * 
 * @author Mark Gottschling on Apr 18, 2018
 *
 */
public class WitherFogBlock extends FogBlock {
    
	/**
	 * 
	 * @param modID
	 * @param name
	 * @param material
	 * @param map
	 */
	public WitherFogBlock(String modID, String name, Material material, Map<FogHeight, FogBlock> map) {
		super(modID, name, material, map);
	}
}
