/**
 * 
 */
package com.someguyssoftware.treasure2.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.someguyssoftware.gottschcore.biome.BiomeHelper;
import com.someguyssoftware.gottschcore.biome.BiomeTypeHolder;
import com.someguyssoftware.gottschcore.config.AbstractConfig;
import com.someguyssoftware.gottschcore.mod.IMod;
import com.someguyssoftware.treasure2.Treasure;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import scala.actors.threadpool.Arrays;

/**
 * Based off of GottschCore IConfig/AbstractConfig class but does not inherit from it to avoid all the extraneous properties
 * and methods that are not needed for this class.
 * As well, there aren't any static probably as their will be multiple instances based on different attributes, such as Rarity.
 * @author Mark Gottschling on Jan 23, 2018
 *
 */
public class WellConfig implements IWellConfig {
	private IMod mod;
	private Configuration forgeConfiguration;
	
	// well
	private boolean wellAllowed;
	private int chunksPerWell;
	private double genProbability;
	
	// biome type white/black lists
	private  String[] rawBiomeWhiteList;
	private  String[] rawBiomeBlackList;
	
	private List<BiomeTypeHolder> biomeWhiteList;
	private List<BiomeTypeHolder> biomeBlackList;
	
	/**
	 * Empty constructor
	 */
	public WellConfig() {
		biomeWhiteList = new ArrayList<>(5);
		biomeBlackList = new ArrayList<>(5);
	}
	
	/**
	 * 
	 * @param configDir
	 * @param modDir
	 * @param filename
	 */
	public WellConfig(IMod mod, File configDir, String modDir, String filename, IWellConfig defaults) {
		this();
		this.mod = mod;
		// build the path to the minecraft config directory
		String configPath = (new StringBuilder()).append(configDir).append("/").append(modDir).append("/").toString();
		// create the config file
		File configFile = new File((new StringBuilder()).append(configPath).append(filename).toString());
		// load the config file
		Configuration configuration = load(configFile, defaults);
		this.forgeConfiguration = configuration;
	}

	/**
	 * 
	 * @param file
	 * @param defaults
	 * @return
	 */
	public Configuration load(File file, IWellConfig defaults) {
		// load the config file
		Configuration config = IWellConfig.super.load(file);
		// ge the modid
		String modid = mod.getClass().getAnnotation(Mod.class).modid();
		
		config.setCategoryComment("01-enable", "Enablements.");
        wellAllowed = config.getBoolean("wellAllowed", "01-enable", defaults.isWellAllowed(), "");
 
        // gen props
    	chunksPerWell = config.getInt("chunksPerWell", "02-gen", defaults.getChunksPerWell(), 50, 32000, "");
    	genProbability = config.getFloat("genProbability", "02-gen", (float)defaults.getGenProbability(), 0.0F, 100.0F, "");
    	
        // white/black lists
        rawBiomeWhiteList = config.getStringList("biomeWhiteList", "02-gen", (String[]) defaults.getRawBiomeWhiteList(), "Allowable Biome Types for general Well generation. Must match the Type identifer(s).");
        rawBiomeBlackList = config.getStringList("biomeBlackList", "02-gen", (String[]) defaults.getRawBiomeBlackList(), "Disallowable Biome Types for general Well generation. Must match the Type identifer(s).");
              
        // update the config if it has changed.
       if(config.hasChanged()) {
    	   config.save();
       }

		BiomeHelper.loadBiomeList(rawBiomeWhiteList, biomeWhiteList);
		BiomeHelper.loadBiomeList(rawBiomeBlackList, biomeBlackList);
       
		return config;
	}

	/**
	 * @return the wellAllowed
	 */
	@Override
	public boolean isWellAllowed() {
		return wellAllowed;
	}

	/**
	 * @param wellAllowed the wellAllowed to set
	 */
	@Override
	public IWellConfig setWellAllowed(boolean wellAllowed) {
		this.wellAllowed = wellAllowed;
		return this;
	}

	/**
	 * @return the chunksPerWell
	 */
	@Override
	public  int getChunksPerWell() {
		return chunksPerWell;
	}

	/**
	 * @param chunksPerWell the chunksPerWell to set
	 */
	@Override
	public IWellConfig setChunksPerWell(int chunksPerWell) {
		this.chunksPerWell = chunksPerWell;
		return this;
	}

	/**
	 * @return the genProbability
	 */
	@Override
	public double getGenProbability() {
		return genProbability;
	}

	/**
	 * @param genProbability the genProbability to set
	 */
	@Override
	public  IWellConfig setGenProbability(double genProbability) {
		this.genProbability = genProbability;
		return this;
	}

	/**
	 * @return the biomeWhiteList
	 */
	@Override
	public List<BiomeTypeHolder> getBiomeWhiteList() {
		return biomeWhiteList;
	}

	/**
	 * @param biomeWhiteList the biomeWhiteList to set
	 */
	@Override
	public void setBiomeWhiteList(List<BiomeTypeHolder> biomeWhiteList) {
		this.biomeWhiteList = biomeWhiteList;
	}

	/**
	 * @return the biomeBlackList
	 */
	@Override
	public List<BiomeTypeHolder> getBiomeBlackList() {
		return biomeBlackList;
	}

	/**
	 * @param biomeBlackList the biomeBlackList to set
	 */
	@Override
	public void setBiomeBlackList(List<BiomeTypeHolder> biomeBlackList) {
		this.biomeBlackList = biomeBlackList;
	}

	/**
	 * @return the rawBiomeWhiteList
	 */
	@Override
	public String[] getRawBiomeWhiteList() {
		return rawBiomeWhiteList;
	}

	/**
	 * @param rawBiomeWhiteList the rawBiomeWhiteList to set
	 */
	@Override
	public IWellConfig setRawBiomeWhiteList(String[] rawBiomeWhiteList) {
		this.rawBiomeWhiteList = rawBiomeWhiteList;
		return this;
	}

	/**
	 * @return the rawBiomeBlackList
	 */
	@Override
	public String[] getRawBiomeBlackList() {
		return rawBiomeBlackList;
	}

	/**
	 * @param rawBiomeBlackList the rawBiomeBlackList to set
	 */
	@Override
	public IWellConfig setRawBiomeBlackList(String[] rawBiomeBlackList) {
		this.rawBiomeBlackList = rawBiomeBlackList;
		return this;
	}

}
