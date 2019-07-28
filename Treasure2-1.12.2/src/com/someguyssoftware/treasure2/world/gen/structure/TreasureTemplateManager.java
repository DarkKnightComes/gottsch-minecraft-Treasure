package com.someguyssoftware.treasure2.world.gen.structure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.someguyssoftware.gottschcore.GottschCore;
import com.someguyssoftware.gottschcore.loot.LootTable;
import com.someguyssoftware.gottschcore.loot.LootTableMaster;
import com.someguyssoftware.gottschcore.mod.IMod;
import com.someguyssoftware.gottschcore.resource.AbstractResourceManager;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.enums.Rarity;
import com.someguyssoftware.treasure2.enums.StructureMarkers;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.world.gen.structure.template.Template;

import static com.someguyssoftware.treasure2.enums.StructureMarkers.*;


/**
 * Custom template manager that create TreasureTemplates instead of Template.
 * @author Mark Gottschling on Jan 19, 2019
 *
 */
// TODO redo this. doesn't work well for specials like witch's den, plus doesn't work well with generic template manager unless they key wasn't StructureType but a String or Integer etc
public class TreasureTemplateManager extends AbstractResourceManager {
//	private static final String CUSTOM_TEMPLATE_RESOURCE_PATH = "/structures/";
	
	/*
	 * NOTE this enum is key for external templates. all templates are
	 * categorized and placed into folders according to the type.
	 */
	public enum StructureType {
		ABOVEGROUND,
		UNDERGROUND
	};
	
//	private IMod mod;
	
	/*
	 * templates is the master map where the key is the String representation resource location.
	 */
	private final Map<String, Template> templates = Maps.<String, Template>newHashMap();
	/** the folder in the assets folder where the structure templates are found. */
//	private final String baseFolder;
	private final DataFixer fixer;
		
	private final Map<StructureType, List<Template>> templatesByType = Maps.<StructureType, List<Template>>newHashMap();
	/** NOTE not in use yet. can organize tempaltes by type and rarity */
	private final Table<StructureType, Rarity, List<Template>> templateTable = HashBasedTable.create();
	
	/*
	 * builtin underground locations for structures
	 */
	List<String> undergroundLocations = Arrays.asList(new String [] {
			"treasure2:underground/basic1",
			"treasure2:underground/basic2",
			"treasure2:underground/basic3",
			"treasure2:underground/basic4",
			"treasure2:underground/basic5",
			"treasure2:underground/cave1",
			"treasure2:underground/cave2",
			"treasure2:underground/cobb1",
			"treasure2:underground/crypt1"
	});

	/*
	 * builtin aboveground locations for structures
	 */
	List<String> abovegroundLocations = Arrays.asList(new String[] {
			"treasure2:aboveground/crypt2",
			"treasure2:aboveground/crypt3"
	});
	
	/*
	 * standard list of blocks to scan for 
	 */
	private List<Block> scanList;
	
	/*
	 * 
	 */
	private Map<StructureMarkers, Block> markerMap;

	List<String> FOLDER_LOCATIONS = ImmutableList.of(
			"surface",
			"subterranean",
			"submerged",
			"float"
			);
	
	/**
	 * 
	 * @param baseFolder
	 * @param fixer
	 */
	public TreasureTemplateManager(IMod mod, String baseFolder, DataFixer fixer) {
		super(mod, "/structures");
		Treasure.logger.debug("creating a TreasureTemplateManager");
//		this.mod = mod;
//        this.baseFolder = baseFolder;
        this.fixer = fixer;        
        
        // init maps
        for (StructureType structureType : StructureType.values()) {
        	getTemplatesByType().put(structureType, new ArrayList<Template>(5));
        }
        
        // setup standard list of markers
        markerMap = Maps.newHashMapWithExpectedSize(10);
        markerMap.put(CHEST, Blocks.CHEST);
        markerMap.put(StructureMarkers.BOSS_CHEST, Blocks.ENDER_CHEST);
        markerMap.put(StructureMarkers.SPAWNER, Blocks.MOB_SPAWNER);
        markerMap.put(StructureMarkers.ENTRANCE, Blocks.GOLD_BLOCK);
        markerMap.put(StructureMarkers.OFFSET, Blocks.REDSTONE_BLOCK);
        markerMap.put(StructureMarkers.PROXIMITY_SPAWNER, Blocks.IRON_BLOCK);
        markerMap.put(StructureMarkers.NULL, Blocks.BEDROCK);
        // TODO need a marker/replacer for water, lava
        		
        // default scan list
        scanList = Arrays.asList(new Block[] {
    			markerMap.get(CHEST),
    			markerMap.get(BOSS_CHEST),
    			markerMap.get(SPAWNER),
    			markerMap.get(ENTRANCE),
    			markerMap.get(OFFSET),
    			markerMap.get(PROXIMITY_SPAWNER)
    			});
        
        // load all the builtin (jar) structure templates
        loadAll(undergroundLocations, StructureType.UNDERGROUND);
        loadAll(abovegroundLocations, StructureType.ABOVEGROUND);
        
        // load external structures
//        for (StructureType customLocation : StructureType.values()) {
//        	createTemplateFolder(customLocation.name().toLowerCase());
//        	loadAll(Arrays.asList(customLocation.name().toLowerCase()), customLocation);
//        }
        
        /*
         *  TODO build and expose
         */
        String CUSTOM_TEMPLATES_RESOURCE_PATH = "/structures";
        buildAndExpose(getBaseResourceFolder(), Treasure.MODID, FOLDER_LOCATIONS);
    }

//	public void buildAndExpose(String resourceRootPath, String modID, List<String> locations) {
//		GottschCore.logger.debug("templates folder locations -> {}", locations);
//		// create paths to custom templates if they don't exist
//		for (String location : locations) {
//			GottschCore.logger.debug("buildAndExpose location -> {}", location);
//			createTemplateFolder(modID, location);
//			exposeResource(resourceRootPath, modID, location);
//		}
//	}
	
	// TODO use this as the base method when extracting to GottschCore in a ResourceManager class
//	protected void exposeResource(String resourceRootPath, String modID, String location) {
//		// ensure that the requried properties are not null
//		if (modID == null || modID.isEmpty())
//			modID = getMod().getId();
//		location = (location != null && !location.equals("")) ? (location + "/") : "";
//		
//		Path folder = null;
//		Stream<Path> walk = null;
//		Treasure.logger.debug("resource as file system path -> {},{},{}", resourceRootPath.toString(), modID, location);
//
//		FileSystem fs = getResourceAsFileSystem(resourceRootPath, modID, location);
//		if (fs == null) {
//			return;
//		}
//
//		try {
//			// get the base path of the resource
//			Path resourceBasePath = fs.getPath(resourceRootPath, modID, location);
//			Treasure.logger.debug("resource base path -> {}", resourceBasePath.toString());
//			
//			folder = Paths.get(getMod().getConfig().getModsFolder(), getMod().getId(), /*getLootTablesFolderName()*/"structures", modID, location).toAbsolutePath();
//
//			boolean isFirst = true;
//			// proces all the files in the folder
//			walk = Files.walk(resourceBasePath, 1);
//			for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
//				Path resourceFilePath = it.next();
//				// check the first file, which is actually the given directory itself
//				if (isFirst) {
//					// create the file system folder if it doesn't exist
//					if (Files.notExists(folder)) {
//						createTemplateFolder(modID, location);
//					}
//				} else {
//					// test if file exists on the file system
//					Path fileSystemFilePath = Paths.get(folder.toString(), resourceFilePath.getFileName().toString()).toAbsolutePath();
//					Treasure.logger.debug("folderTemplatePath -> {}", fileSystemFilePath.toString());
//
//					if (Files.notExists(fileSystemFilePath)) {
//						// copy from resource/classpath to file path
//						InputStream is = LootTableMaster.class.getResourceAsStream(resourceFilePath.toString());
//						try (FileOutputStream fos = new FileOutputStream(fileSystemFilePath.toFile())) {
//							byte[] buf = new byte[2048];
//							int r;
//							while ((r = is.read(buf)) != -1) {
//								fos.write(buf, 0, r);
//							}
//						} catch (IOException e) {
//							Treasure.logger.error("Error exposing resource to file system.", e);
//						}
//					}
//				}
//				isFirst = false;
//			}
//		} catch (Exception e) {
//			Treasure.logger.error("error:", e);
//		} finally {
//			// close the stream
//			if (walk != null) {
//				walk.close();
//			}
//		}
//
//		// close the file system
//		if (fs != null && fs.isOpen()) {
//			try {
//				fs.close();
//			} catch (IOException e) {
//				GottschCore.logger.debug("An error occurred attempting to close the FileSystem:", e);
//			}
//		}		
//	}
//	
//	// TODO move to GottschCore ResourceManager
//	/**
//	 * 
//	 * @param modID
//	 * @param location
//	 * @return
//	 */
//	protected FileSystem getResourceAsFileSystem(String resourceRootPath, String modID, String location) {
//		FileSystem fs = null;
//		Map<String, String> env = new HashMap<>();
//		URI uri = null;
//
//		// get the asset resource folder that is unique to this mod
//		resourceRootPath = "/" + resourceRootPath.replaceAll("^/|/$", "") + "/";
//		URL url = GottschCore.class.getResource(resourceRootPath + modID + "/" + location);
//		if (url == null) {
//			Treasure.logger.error("Unable to locate resource {}", resourceRootPath + modID + "/" + location);
//			return null;
//		}
//
//		// convert to a uri
//		try {
//			uri = url.toURI();
//		} catch (URISyntaxException e) {
//			Treasure.logger.error("An error occurred during loot table processing:", e);
//			return null;
//		}
//
//		// split the uri into 2 parts - jar path and folder path within jar
//		String[] array = uri.toString().split("!");
//		try {
//			fs = FileSystems.newFileSystem(URI.create(array[0]), env);
//		} catch (IOException e) {
//			GottschCore.logger.error("An error occurred during loot table processing:", e);
//			return null;
//		}
//
//		return fs;
//	}
//	
//	/**
//	 * 
//	 * @param location
//	 */
//	protected void createTemplateFolder(String location) {
//		// build a path to the specified location
//		Path folder = Paths.get(getBaseFolder(), ((location != null && !location.equals("")) ? (location + "/") : "")).toAbsolutePath();
//
//		if (Files.notExists(folder)) {
//			Treasure.logger.debug("templates folder \"{}\" will be created.", folder.toString());
//			try {
//				Files.createDirectories(folder);
//
//			} catch (IOException e) {
//				Treasure.logger.warn("Unable to create templates folder \"{}\"", folder.toString());
//			}
//		}
//	}
//	
//	protected void createTemplateFolder(String modID, String location) {
//		Path path = Paths.get(modID, location);
//		createTemplateFolder(path.toString());
//	}
	
	/**
	 * 
	 * @param modID
	 */
	public void register(String modID) {
		for (String location : FOLDER_LOCATIONS) {
			Treasure.logger.debug("registering template -> {}", location);
			// get loot table files as ResourceLocations from the file system location
			List<ResourceLocation> locs = getResourceLocations(modID, location);
			
			// load each ResourceLocation as LootTable and map it.
			for (ResourceLocation loc : locs) {
				Path path = Paths.get(loc.getResourcePath());
				Treasure.logger.debug("path to template resource loc -> {}", path.toString());
				
				// load template
				Treasure.logger.debug("attempted to load custom template  with key -> {} : {}", location, location);
				Template template = load(loc, getScanList());
				// add the id to the map
				if (template != null) {
					Treasure.logger.debug("loaded custom template  with key -> {} : {}", location, location);
				}				
			}	
		}
	}
	
//	/**
//	 * TODO move to GottschCore
//	 * Get all resource as ResourceLocation objects
//	 */
//	public List<ResourceLocation> getResourceLocations(String modIDIn, String locationIn) {
//		// ensure that the requried properties (modID) is not null
//		final String modID = (modIDIn == null || modIDIn.isEmpty()) ? getMod().getId() : modIDIn;
//		final String location= (locationIn != null && !locationIn.equals("")) ? (locationIn + "/") : "";
//
//		List<ResourceLocation> locs = new ArrayList<>();
//		Path path = Paths.get(getMod().getConfig().getModsFolder(), getMod().getId(), /*getLootTablesFolderName()*/"structures", modID, location).toAbsolutePath();
//
//		 GottschCore.logger.debug("Path to custom template -> {}", path.toString());
//		// check if path/folder exists
//		if (Files.notExists(path)) {
//			GottschCore.logger.debug("Unable to locate -> {}", path.toString());
//			return locs;
//		}
//
//		try {
//			Files.walk(path).filter(Files::isRegularFile).forEach(f -> {
//				 GottschCore.logger.debug("Custom loot table -> {}", f.toAbsolutePath().toString());
//				ResourceLocation loc = 
//						new ResourceLocation(
//								/*getMod().getId() + ":" + //getLootTablesFolderName()//"structures" + */ // took this out becuase of conflict with baseFolder property used elsewhere - fix
//								"/" + modID + "/" + location
//										+ f.getFileName().toString().replace(".json", ""));
//				GottschCore.logger.debug("Resource location -> {}", loc);
//				locs.add(loc);
//			});
//		} catch (IOException e) {
//			GottschCore.logger.error("Error processing custom loot table:", e);
//		}
//
//		return locs;
//	}
	
	///////////////////////////////////////
	/**
	 * Convenience method.
	 * @param type
	 * @return
	 */
	public List<Template> getTemplatesByType(StructureType type) {
		List<Template> templates = getTemplatesByType().get(type);
		return templates;
	}
	
	/**
	 * @deprecated use getTemplatesByType(type)
	 * @param server
	 * @param id
	 * @return
	 */
	@Deprecated
	public Template getTemplate(/*@Nullable MinecraftServer server, */ResourceLocation r) {
//		String s = r.getResourcePath();
		String s = r.toString();

		Template template = null;
		if (this.templates.containsKey(s)) {
			return this.templates.get(s);
		}
		
//		Template template = this.get(/*server,*/id);
//
//		if (template == null) {
//			template = new Template();
//			this.templates.put(id.getResourcePath(), template);
//		}

		return template;
	}

	/**
	 * 
	 * @param locations
	 * @param type
	 */
	public void loadAll(List<String> locations, StructureType type) {
		Treasure.logger.debug("loading all typed structures -> {}", type.name());
		for (String location : locations) {
			Treasure.logger.debug("loading from -> {}", location);
			Template template = load(new ResourceLocation(location), scanList);
			Treasure.logger.debug("loaded template  with key -> {} : {}", location, location);
			// add the id to the map
			if (template != null) {
				Treasure.logger.debug("adding tempate to typed map -> {} : {}", type.name(), location);
				getTemplatesByType().get(type).add(template);
			}
		}	
	}
	
	/**
	 * 
	 * @param server
	 * @param templatePath
	 * @return
	 */
	public Template load(/*@Nullable MinecraftServer server, */ResourceLocation templatePath, List<Block> scanForBlocks) {
		String key = templatePath.toString();	//templatePath.getResourcePath();
		
		if (this.getTemplates().containsKey(key)) {
			return this.templates.get(key);
		}

		this.readTemplate(templatePath, scanForBlocks);
		if (this.templates.get(key) != null) {
			Treasure.logger.debug("Loaded structure from -> {}", templatePath.toString());
		}
		else {
			Treasure.logger.debug("Unable to read structure from -> {}", templatePath.toString());
		}
		return this.templates.containsKey(key) ? (Template) this.templates.get(key) : null;
//		return this.templates.get(s);
	}

	/**
	 * This reads a structure template from the given location and stores it. This
	 * first attempts get the template from an external folder. If it isn't there
	 * then it attempts to take it from the minecraft jar.
	 */
	public boolean readTemplate(ResourceLocation location, List<Block> scanForBlocks) {
		String s = location.getResourcePath();
		Treasure.logger.debug("template resource path -> {}", s);
		String suffix = "";
		if (!s.endsWith(".nbt")) {
			suffix = ".nbt";
		}
		File file1 = new File(this.getBaseResourceFolder(), s + suffix);
		Treasure.logger.debug("template file path -> {}", file1.getAbsoluteFile());
		if (!file1.exists()) {
			Treasure.logger.debug("file does not exist, read from jar -> {}", file1.getAbsolutePath());
			return this.readTemplateFromJar(location, scanForBlocks);
		} else {
			InputStream inputstream = null;
			boolean flag;

			try {
				inputstream = new FileInputStream(file1);
				this.readTemplateFromStream(location.toString(), inputstream, scanForBlocks);
				return true;
			} catch (Throwable var10) {
				flag = false;
			} finally {
				IOUtils.closeQuietly(inputstream);
			}

			return flag;
		}
	}

	/**
	 * reads a template from the minecraft jar
	 */
	private boolean readTemplateFromJar(ResourceLocation id, List<Block> scanForBlocks) {
		String s = id.getResourceDomain();
		String s1 = id.getResourcePath();
		InputStream inputstream = null;
		boolean flag;

		try {
			Treasure.logger.debug("attempting to open resource stream -> {}", "/assets/" + s + "/strucutres/" + s1 + ".nbt");
			inputstream = MinecraftServer.class.getResourceAsStream("/assets/" + s + "/structures/" + s1 + ".nbt");
			this.readTemplateFromStream(id.toString(), inputstream, scanForBlocks);
			return true;
		} catch (Throwable var10) {
			Treasure.logger.error("error reading resource: ", var10);
			flag = false;
		} finally {
			IOUtils.closeQuietly(inputstream);
		}

		return flag;
	}

	/**
	 * reads a template from an inputstream
	 */
	private void readTemplateFromStream(String id, InputStream stream, List<Block> scanForBlocks) throws IOException {
		NBTTagCompound nbttagcompound = CompressedStreamTools.readCompressed(stream);

		if (!nbttagcompound.hasKey("DataVersion", 99)) {
			nbttagcompound.setInteger("DataVersion", 500);
		}

		TreasureTemplate template = new TreasureTemplate();
		template.read(this.fixer.process(FixTypes.STRUCTURE, nbttagcompound), scanForBlocks);
		Treasure.logger.debug("adding template to map with key -> {}", id);
		this.templates.put(id, template);
	}

	/**
	 * writes the template to an external folder
	 */
	public boolean writeTemplate(@Nullable MinecraftServer server, ResourceLocation id) {
		String s = id.getResourcePath();

		if (server != null && this.templates.containsKey(s)) {
			File file1 = new File(this.getBaseResourceFolder());

			if (!file1.exists()) {
				if (!file1.mkdirs()) {
					return false;
				}
			} else if (!file1.isDirectory()) {
				return false;
			}

			File file2 = new File(file1, s + ".nbt");
			Template template = this.templates.get(s);
			OutputStream outputstream = null;
			boolean flag;

			try {
				NBTTagCompound nbttagcompound = template.writeToNBT(new NBTTagCompound());
				outputstream = new FileOutputStream(file2);
				CompressedStreamTools.writeCompressed(nbttagcompound, outputstream);
				return true;
			} catch (Throwable var13) {
				flag = false;
			} finally {
				IOUtils.closeQuietly(outputstream);
			}

			return flag;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @param templatePath
	 */
	public void remove(ResourceLocation templatePath) {
		this.templates.remove(templatePath.getResourcePath());
	}

	public Map<StructureMarkers, Block> getMarkerMap() {
		return markerMap;
	}

	public void setMarkerMap(Map<StructureMarkers, Block> markerMap) {
		this.markerMap = markerMap;
	}

	public List<Block> getScanList() {
		return scanList;
	}

	public void setScanList(List<Block> scanList) {
		this.scanList = scanList;
	}

	public Map<String, Template> getTemplates() {
		return templates;
	}

	/**
	 * @return the templateTable
	 */
	public Table<StructureType, Rarity, List<Template>> getTemplateTable() {
		return templateTable;
	}

	/**
	 * @return the templatesByType
	 */
	public Map<StructureType, List<Template>> getTemplatesByType() {
		return templatesByType;
	}
	
//	/**
//	 * @return the mod
//	 */
//	protected IMod getMod() {
//		return mod;
//	}
//
//	/**
//	 * @param mod
//	 *            the mod to set
//	 */
//	protected void setMod(IMod mod) {
//		this.mod = mod;
//	}
//
//	public String getBaseFolder() {
//		return baseFolder;
//	}
}