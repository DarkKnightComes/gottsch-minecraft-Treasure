/**
 * 
 */
package com.someguyssoftware.treasure2.generator.special;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.someguyssoftware.gottschcore.Quantity;
import com.someguyssoftware.gottschcore.positional.Coords;
import com.someguyssoftware.gottschcore.positional.ICoords;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.block.TreasureBlocks;
import com.someguyssoftware.treasure2.config.Configs;
import com.someguyssoftware.treasure2.config.IWitchDenConfig;
import com.someguyssoftware.treasure2.enums.Rarity;
import com.someguyssoftware.treasure2.enums.StructureMarkers;
import com.someguyssoftware.treasure2.generator.GenUtil;
import com.someguyssoftware.treasure2.generator.chest.CauldronChestGenerator;
import com.someguyssoftware.treasure2.generator.structure.StructureGenerator;
import com.someguyssoftware.treasure2.tileentity.ProximitySpawnerTileEntity;
import com.someguyssoftware.treasure2.world.gen.structure.IStructureInfo;
import com.someguyssoftware.treasure2.world.gen.structure.TreasureTemplate;
import com.someguyssoftware.treasure2.world.gen.structure.TreasureTemplateManager.StructureType;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;

/**
 * @author Mark Gottschling on Jan 29, 2019
 *
 */
public class WitchDenGenerator {

//	private static final String WITCH_DEN_LOCATION = "treasure:special/witch-den";
	final List<String> witchDenLocations = Arrays.asList(new String[] {
			"treasure2:aboveground/witch/witch_den1",
			"treasure2:aboveground/witch/witch_den2",
			"treasure2:aboveground/witch/witch_den3",
			"treasure2:aboveground/witch/witch_den4",
			"treasure2:aboveground/witch/witch_den5"
	});
	
	// use linked list to keep in order
	final List<TreasureTemplate> templates = new LinkedList<>();
	
	/**
	 * 
	 */
	public WitchDenGenerator() {
		for (String s : witchDenLocations) {
			Treasure.logger.debug("loading witch's den -> {}", s);
			Template template = Treasure.TEMPLATE_MANAGER.load(new ResourceLocation(s), Treasure.TEMPLATE_MANAGER.getScanList());
			templates.add((TreasureTemplate)template);
		}
	}

	/**
	 * 
	 * @param world
	 * @param random
	 * @param coords
	 * @param config
	 * @return
	 */
	public boolean generate(World world, Random random, ICoords coords, IWitchDenConfig config) {
		ICoords surfaceCoords = null;

		// 1. determine y-coord of land for markers
		surfaceCoords = WorldInfo.getSurfaceCoords(world, coords);
		Treasure.logger.debug("Surface Coords @ {}", surfaceCoords.toShortString());
		if (surfaceCoords == null || surfaceCoords == WorldInfo.EMPTY_COORDS) {
			Treasure.logger.debug("Returning due to surface coords == null or EMPTY_COORDS");
			return false;
		}
		
		// 2. get base template which is on the ground
//		TreasureTemplate template = (TreasureTemplate) Treasure.TEMPLATE_MANAGER.getTemplate(new ResourceLocation(WITCH_DEN_LOCATION));
//		TreasureTemplate template = (TreasureTemplate) Treasure.TEMPLATE_MANAGER.getTemplate(new ResourceLocation(WITCH_DEN_LOCATIONS[0]));
//		List<Template> templates = (List<Template>) Treasure.TEMPLATE_MANAGER.getTemplatesByType(StructureType.WITCH);

//		if (template == null) {
		if (templates == null || templates.size() == 0) {
			Treasure.logger.debug("could not find a witch's den template");
			return false;
		}
		TreasureTemplate template = templates.get(0);
		Treasure.logger.debug("got handle to base witch's den -> {}", template.getSize());
		
		// 3. get the offset
		int offset = 0;
		ICoords offsetCoords = template.findCoords(random, GenUtil.getMarkerBlock(StructureMarkers.OFFSET));
		if (offsetCoords != null) {
			offset = -offsetCoords.getY();
		}

		// 4. update the spawn coords with the offset
		ICoords spawnCoords = surfaceCoords.add(0, offset, 0);
		
		// 4.5 find the entrance block (relative)
		ICoords entranceCoords = template.findCoords(random, GenUtil.getMarkerBlock(StructureMarkers.ENTRANCE));
		if (entranceCoords == null) {
			Treasure.logger.debug("Unable to locate entrance position.");
			return false;
		}
		
		// 5. select a rotation
		Rotation rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
		Treasure.logger.debug("witch den rotation used -> {}", rotation);
				
		// 6. setup placement
		PlacementSettings placement = new PlacementSettings();
		placement.setRotation(rotation).setRandom(random);
		
		// 6.5 update the entrance
		entranceCoords = new Coords(TreasureTemplate.transformedBlockPos(placement, entranceCoords.toPos()));
		
		// 7. get the transformed size
		BlockPos transformedSize = template.transformedSize(rotation);
		
		// 8. check if there is solid ground at spawn corods (not on surface since den should be set into the ground/swamp)
		if (!WorldInfo.isSolidBase(world, spawnCoords, transformedSize.getX(), transformedSize.getZ(), 75)) {
			Treasure.logger.debug("Coords -> [{}] does not meet {}% solid base requirements for size -> {} x {}", 75, spawnCoords.toShortString(), transformedSize.getX(), transformedSize.getY());
			return false;
		}
				
		// 9. generate the structure
		IStructureInfo info = new StructureGenerator().generate(world, random, template, placement, spawnCoords);
		if (info == null) {
			Treasure.logger.debug("Witch den did not return structure info -> {}", spawnCoords);
			return false;			
		}
		Treasure.logger.debug("returned info -> {}", info);
		
		/*
		 * 10. get special blocks
		 */
		// interrogate info for spawners and any other special block processing (except chests that are handler by caller
		List<ICoords> spawnerCoords = (List<ICoords>) info.getMap().get(GenUtil.getMarkerBlock(StructureMarkers.SPAWNER));
		List<ICoords> proximityCoords = (List<ICoords>) info.getMap().get(GenUtil.getMarkerBlock(StructureMarkers.PROXIMITY_SPAWNER));
		List<ICoords> chestCoordsList = (List<ICoords>) info.getMap().get(GenUtil.getMarkerBlock(StructureMarkers.CHEST));
		List<ICoords> bossChestCoordsList = (List<ICoords>) info.getMap().get(GenUtil.getMarkerBlock(StructureMarkers.BOSS_CHEST));

		int yOffset = template.getSize().getY();
		ICoords lastSpawnCoords = spawnCoords;
		
		// TODO load and place mid structure (repeating find specials and add to master)
		for (int i = 1; i < templates.size(); i++) {
			// get template
//			template = (TreasureTemplate) Treasure.TEMPLATE_MANAGER.getTemplate(new ResourceLocation(WITCH_DEN_LOCATIONS[i]));
			Treasure.logger.debug("getting next witch den @ index {} -> {}", i, template.getSize());
			template = templates.get(i);
			// get the entrance coords of the next piece of structure
			ICoords subStructEntranceCoords = template.findCoords(random, GenUtil.getMarkerBlock(StructureMarkers.ENTRANCE));
			if (entranceCoords == null) {
				Treasure.logger.debug("Unable to locate entrance position.");
				return false;
			}
			// update the rotate entrance coords
			subStructEntranceCoords = new Coords(TreasureTemplate.transformedBlockPos(placement, subStructEntranceCoords.toPos()));
			// determine spawn coords for structure
			ICoords subSpawnCoords = lastSpawnCoords
					.add(entranceCoords.getX(), 0, entranceCoords.getZ())
					.add(-subStructEntranceCoords.getX(), yOffset, -subStructEntranceCoords.getZ());
			Treasure.logger.debug("new spawn coords -> {}", subSpawnCoords);
			// build the struct
			IStructureInfo subInfo = new StructureGenerator().generate(world, random, template, placement, subSpawnCoords);
			if (subInfo == null) {
				Treasure.logger.debug("Witch den sub structure did not return structure info.");
			}
			else {
				Treasure.logger.debug("returned info -> {}", subInfo);
			}
			
			/*
			 *  all these coords are relative to the structure that they were a part of which isn't necessarily the same dimensions
			 *  of the base structure. so they are need to be coverted to be relative to the spawnCoords BEFORE being added to the master lists.
			 *  (function: (subSpawnCoords - spawnCoords) + markerCoords
			 */
			// find and add specials to master
			List<ICoords> subSpawnList = (List<ICoords>) subInfo.getMap().get(GenUtil.getMarkerBlock(StructureMarkers.SPAWNER))
					.stream().map(o -> o.add(subSpawnCoords.delta(spawnCoords))).collect(Collectors.toList());
			spawnerCoords.addAll(subSpawnList);
			
			List<ICoords> subProximityList = (List<ICoords>) info.getMap().get(GenUtil.getMarkerBlock(StructureMarkers.PROXIMITY_SPAWNER))
					.stream().map(o -> o.add(subSpawnCoords.delta(spawnCoords))).collect(Collectors.toList());
			proximityCoords.addAll(subProximityList);			
			
			List<ICoords> subChestList = (List<ICoords>) info.getMap().get(GenUtil.getMarkerBlock(StructureMarkers.CHEST))
					.stream().map(o -> o.add(subSpawnCoords.delta(spawnCoords))).collect(Collectors.toList());
			chestCoordsList.addAll(subChestList);
			
			List<ICoords> subBossChestList = (List<ICoords>) info.getMap().get(GenUtil.getMarkerBlock(StructureMarkers.BOSS_CHEST))
					.stream().map(o -> o.add(subSpawnCoords.delta(spawnCoords))).collect(Collectors.toList());
			bossChestCoordsList.addAll(subBossChestList);
			
			lastSpawnCoords = subSpawnCoords;
			entranceCoords = subStructEntranceCoords;
			// update the offset
			yOffset = template.getSize().getY();
		}

		/*
		 * to get the correct coords to an of the mapped specials, you must use info.getCoords() because the spawn coords may have shifted to align with the shaft
		 */
		ICoords bossChestCoords =  null;
		if (bossChestCoordsList != null && bossChestCoordsList.size() > 0) {
			spawnCoords.add(bossChestCoordsList.get(0));
		}
		
		/*
		 * 11. Add mobs
		 */
		// populate vanilla spawners with zombies or witches
		for (ICoords c : spawnerCoords) {
			ICoords c2 = spawnCoords.add(c);			
			world.setBlockState(c2.toPos(), Blocks.MOB_SPAWNER.getDefaultState());
			TileEntityMobSpawner te = (TileEntityMobSpawner) world.getTileEntity(c2.toPos());
			ResourceLocation r = null;
			if (c2.delta(spawnCoords).getY() < 3)  {
				// spawn with Drowned
				r = new ResourceLocation("Drowned");
			}
			else {
				r = new ResourceLocation("witch");
			}
			te.getSpawnerBaseLogic().setEntityId(r);			
		}
		
		// populate proximity spawners with zombies or witches
		for (ICoords c : proximityCoords) {
			ICoords c2 = spawnCoords.add(c);
	    	world.setBlockState(c2.toPos(), TreasureBlocks.PROXIMITY_SPAWNER.getDefaultState());
	    	ProximitySpawnerTileEntity te = (ProximitySpawnerTileEntity) world.getTileEntity(c2.toPos());
			ResourceLocation r = null;
			if (c2.delta(spawnCoords).getY() < 3)  {
				// spawn with Drowned - there are no Drowned in 1.12.2!!
				if (Minecraft.getMinecraft().getVersion().contains("1.12")) {
					r = new ResourceLocation("Zombie");
				}
				else if (Minecraft.getMinecraft().getVersion().contains("1.13")) {
					r = new ResourceLocation("drowned");
				}
				te.setMobNum(new Quantity(1, 3));
			}
			else {
				r = new ResourceLocation("witch");
		    	te.setMobNum(new Quantity(1, 2));
			}
	    	te.setMobName(r);
	    	te.setProximity(10D);
		}
		
		// TODO find the boss chest and regular chests
		
		// add chest
		CauldronChestGenerator chestGen = new CauldronChestGenerator();
		chestGen.generate(world, random, bossChestCoords, Rarity.EPIC, Configs.chestConfigs.get(Rarity.EPIC)); 
		
		return true;
	}
	
}
