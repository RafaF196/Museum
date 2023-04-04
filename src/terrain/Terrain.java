package terrain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import entities.Entity;
import entities.LODEntity;
import models.LODModel;
import models.RawModel;
import models.TexturedModel;
import objFileLoader.ObjFileLoader;
import render.Loader;
import textures.ModelTexture;

public class Terrain {
	
	private static final String RES_LOC = "res/";
	
	private static final Integer MAX_TRIANGLES = 120000; // maximum number of triangles to be rendered at one frame
	private static final Integer MAX_HYSTERESIS = 100; // number of frames before an instance can change its level of detail
	
	private ArrayList<LODEntity> objects; // All the objects from the scene
	private ArrayList<Entity> walls; // The walls (Floor included)
	
	private ArrayList<Float> contributionUp = new ArrayList<Float>(); // difference in contribution with the +1 level of detail
	private ArrayList<Float> contributionDown = new ArrayList<Float>(); // difference in contribution with the -1 level of detail

	// The number of frames for each instance before they can change their level of detail
	private ArrayList<Integer> hysteresisValue = new ArrayList<Integer>();
	
	// The cells that are visible from the current point of view (the current cell, to be precise)
	private Map<Integer, Set<Integer>> visibleCells = new HashMap<Integer, Set<Integer>>();
	private ArrayList<Boolean> objectVisibility = new ArrayList<Boolean>(); // Whether each object is visible now
	
	Integer objectId, instanceId;

	
	public Terrain (Loader loader, String mapName, String visibilityName, Vector3f cameraPos) {
		this.objects = new ArrayList<LODEntity>();
		this.walls = new ArrayList<Entity>();
		
		readMapFile(loader, mapName);
		readVisibilityFile(visibilityName);
		updateContributionLists(cameraPos);
		updateObjectVisibility(cameraPos);
	}
	
	// Reads the file corresponding to the map and object distribution
	private void readMapFile(Loader loader, String mapName) {
		FileReader isr = null;
		File objFile = new File(RES_LOC + mapName + ".txt");
		try {
			isr = new FileReader(objFile);
		} catch (FileNotFoundException e) {
			System.err.println("File not found in res folder!");
			System.exit(-1);
		}
		
		BufferedReader reader = new BufferedReader(isr);
		String line;
		Integer cellValue;
		Integer xSize = 0, ySize = 0;
		
		// Model 1 (Red on the picture) = Tea model
		LODModel lodmodel1 = new LODModel(loadModel("teaCompressionLvl3", "lod0", loader), loadModel("teaCompressionLvl4", "lod1", loader),
				loadModel("teaCompressionLvl5", "lod2", loader), loadModel("teaCompressionLvl6", "lod3", loader),
				loadModel("tea", "lod4", loader));
		LODEntity lodentity1 = new LODEntity(lodmodel1, new Vector3f(0, 0, 0), 30, 0.15f);
		
		// Model 2 (Green on the picture) = Meta model
		LODModel lodmodel2 = new LODModel(loadModel("metaCompressionLvl3", "lod0", loader), loadModel("metaCompressionLvl4", "lod1", loader),
				loadModel("metaCompressionLvl5", "lod2", loader), loadModel("metaCompressionLvl6", "lod3", loader),
				loadModel("meta", "lod4", loader));
		LODEntity lodentity2 = new LODEntity(lodmodel2, new Vector3f(0, 0.3f, 0), 0, 0.25f);
		
		// Model 3 (Blue on the picture) = Dragon model
		LODModel lodmodel3 = new LODModel(loadModel("dragonCompressionLvl3", "lod0", loader), loadModel("dragonCompressionLvl4", "lod1", loader),
				loadModel("dragonCompressionLvl5", "lod2", loader), loadModel("dragonCompressionLvl6", "lod3", loader),
				loadModel("dragon", "lod4", loader));
		LODEntity lodentity3 = new LODEntity(lodmodel3, new Vector3f(0, 0, 0), 30, 0.15f);
		
		// The walls
		Entity ewalls = new Entity(loadModel("wall", "wall", loader), new Vector3f(0, 0, 0), 0, 1f);
		
		try {
			// Map dimensions
			line = reader.readLine();
			xSize = (Integer) Integer.valueOf(line);
			line = reader.readLine();
			ySize = (Integer) Integer.valueOf(line);
			
			Float xPos = 0.5f, yPos = 0.5f; // Center of the current cell
			
			line = reader.readLine();
			while (line != null) {
				cellValue = (Integer) Integer.valueOf(line);
				
				// Add instances to the entities in the correct position
				if (cellValue == 0) ewalls.addInstance(new Vector2f(xPos, yPos));
				if (cellValue == 1) lodentity1.addInstance(new Vector2f(xPos, yPos));
				if (cellValue == 2) lodentity2.addInstance(new Vector2f(xPos, yPos));
				if (cellValue == 3) lodentity3.addInstance(new Vector2f(xPos, yPos));
				
				if (cellValue > 0 && cellValue < 4) hysteresisValue.add(0); // If its an lodentity add a hysteresis value
					
				// Move along the map
				xPos += 1;
				if (xPos > ySize) {
					xPos = 0.5f;
					yPos += 1;
				}
				
				line = reader.readLine();
			}
			reader.close();
		} catch (Exception e) {
			System.err.println("Error reading the Map file");
			System.exit(-1);
		}
		
		// Create the floor (terrain)
		Entity terrain = new Entity(loadModel("floor", "floor", loader), new Vector3f(0, 0, 0), 0, xSize > ySize ? xSize : ySize);
		terrain.addInstance(new Vector2f(ySize/2, xSize/2));
		
		// Add the LoD entities
		objects.add(lodentity1);
		objects.add(lodentity2);
		objects.add(lodentity3);
		
		// Add the entities
		walls.add(terrain);
		walls.add(ewalls);
		
	}
	
	// Reads the visibility file and stores it for further use
	private void readVisibilityFile(String fileName) {
		FileReader isr = null;
		File objFile = new File(RES_LOC + fileName + ".txt");
		try {
			isr = new FileReader(objFile);
		} catch (FileNotFoundException e) {
			System.err.println("File not found in res folder!");
			System.exit(-1);
		}
		
		BufferedReader reader = new BufferedReader(isr);
		String line;
		Integer cellValue;
		Set<Integer> visibleCells;
		
		try {
			
			line = reader.readLine();
			line = reader.readLine();
			
			while (line != null) {
				cellValue = (Integer) Integer.valueOf(line); // The current cell
				
				line = reader.readLine();
				String[] currentLine = line.split(",");
				visibleCells = new HashSet<Integer>();
				for (int i = 0; i < currentLine.length; i++) { // Read all the visible cells from the current cell
					visibleCells.add(Integer.parseInt(currentLine[i]));
				}
				
				this.visibleCells.put(cellValue, visibleCells); // And store them

				line = reader.readLine();
				
			}
			reader.close();
		} catch (Exception e) {
			System.err.println("Error reading the Visibility file");
			System.exit(-1);
		}
		
	}
	
	// Updates both contributions lists (depending on the camera position and the current LoD of each instance)
	public void updateContributionLists(Vector3f cameraPos) {
		Float d, dist, contribution;
		Integer instanceLoD;
		Vector3f objPos;
		
		contributionDown.clear();
		contributionUp.clear();
		
		for (int i = 0; i < objects.size(); i++) {
			d = objects.get(i).getDiagonal(); // All instances have the same bounding box
			for (int j = 0; j < objects.get(i).getNumInstances(); j++) {
				// The position of this instance
				objPos = new Vector3f(objects.get(i).getPosition().x + objects.get(i).getOffSets().get(j).x,
									  objects.get(i).getPosition().y,
									  objects.get(i).getPosition().z + objects.get(i).getOffSets().get(j).y);
				// Its distance from the camera
				dist = (float) Math.sqrt((objPos.x-cameraPos.x)*(objPos.x-cameraPos.x) + (objPos.y-cameraPos.y)*(objPos.y-cameraPos.y) +
						(objPos.z-cameraPos.z)*(objPos.z-cameraPos.z));
				
				instanceLoD = objects.get(i).getLoDofInstance(j); // LoD of the instance
				contribution = (float) (d/(dist*Math.pow(2.0, instanceLoD))); // Its contribution at this level of detail
				
				// If possible, calculate the contribution if reducing 1 level of detail
				if (instanceLoD == 0) contributionDown.add(null);
				else contributionDown.add( (float) (contribution - d/(dist*(Math.pow(2.0, instanceLoD-1)))) );
				
				// If possible, calculate the contribution if increasing 1 level of detail
				if (instanceLoD == 4) contributionUp.add(null);
				else contributionUp.add( (float) (contribution - d/(dist*(Math.pow(2.0, instanceLoD+1)))) );
			}
		}
		
	}
	
	// Updates the object visibility list (depending on the camera position)
	public void updateObjectVisibility(Vector3f cameraPos) {
		Integer cameraCell = convertPosToCellId(cameraPos); // cell ID for the current camera position
		// Obtain the set of visible cells from the current position (if possible)
		Set<Integer> visibleCellsFromCam = new HashSet<Integer>();
		if (visibleCells.containsKey(cameraCell)) visibleCellsFromCam = this.visibleCells.get(cameraCell);
		this.objectVisibility.clear();
		
		Vector3f objPos;
		for (int i = 0; i < this.objects.size(); i++) {
			for (int j = 0; j < objects.get(i).getNumInstances(); j++) {
				objPos = new Vector3f(objects.get(i).getPosition().x + objects.get(i).getOffSets().get(j).x,
									  objects.get(i).getPosition().y,
									  objects.get(i).getPosition().z + objects.get(i).getOffSets().get(j).y);
				Integer objCell = convertPosToCellId(objPos);
				// If the object is in a cell contained in the visible set, the object will be visible.
				this.objectVisibility.add( visibleCellsFromCam.contains(objCell) );
			}
		}
		
		Integer numInst0 = objects.get(0).getNumInstances();
		Integer numInst1 = objects.get(1).getNumInstances();
		Integer numInst2 = objects.get(2).getNumInstances();
		
		// Update the information in each of the LODEntities
		objects.get(0).updateVisibleOffsets(objectVisibility.subList(0, numInst0));
		objects.get(1).updateVisibleOffsets(objectVisibility.subList(numInst0, numInst0+numInst1));
		objects.get(2).updateVisibleOffsets(objectVisibility.subList(numInst0+numInst1, numInst0+numInst1+numInst2));
	}
	
	public void timeCriticalRendering(Vector3f cameraPos) {
		
		updateContributionLists(cameraPos); // Update the list of contributions first

		if (getSceneTriangles() < MAX_TRIANGLES) { // If the triangle threshold has not been reached (increase LoD)
			
			Integer overallID = getMaximumContributionUp();
			// If there is a maximum and it is possible (hysteresis), increase the LoD of this instance
			if (overallID != null && hysteresisValue.get(overallID) == 0) {
				convertObjectId(overallID);
				Integer actualLoD = objects.get(objectId).getLoDofInstance(instanceId);
				objects.get(objectId).changeLoDofInstance(instanceId, actualLoD+1);
				hysteresisValue.set(overallID, MAX_HYSTERESIS);
			}
			
		} else { // If the triangle threshold has been reached (decrease LoD)
			
			Integer overallID = getMaximumContributionDown();
			// If there is a maximum and it is possible (hysteresis), decrease the LoD of this instance
			if (overallID != null && hysteresisValue.get(overallID) == 0) {
				convertObjectId(overallID);
				Integer actualLoD = objects.get(objectId).getLoDofInstance(instanceId);
				objects.get(objectId).changeLoDofInstance(instanceId, actualLoD-1);
				hysteresisValue.set(overallID, MAX_HYSTERESIS);
			}
			
		}
		
	}
	
	// Returns the higher amount of contribution from the list
	public Integer getMaximumContributionUp() {
		Float max = 0.0f;
		Integer maxPos = null;
		for (int i = 0; i < contributionUp.size(); i++) {
			if (contributionUp.get(i) != null && contributionUp.get(i) > max && objectVisibility.get(i)) {
				max = contributionUp.get(i);
				maxPos = i;
			}
		}
		return maxPos;
	}
	
	// Returns the higher ("least negative") amount of contribution from the list
	public Integer getMaximumContributionDown() {
		Float max = -99999.0f;
		Integer maxPos = null;
		for (int i = 0; i < contributionDown.size(); i++) {
			if (contributionDown.get(i) != null && contributionDown.get(i) > max && objectVisibility.get(i)) {
				max = contributionDown.get(i);
				maxPos = i;
			}
		}
		return maxPos;
	}
	
	// Pos(x,y,z) = xxxzzz. For example, Pos(12.7, 5.4, 1.6) = 012001
	public Integer convertPosToCellId(Vector3f pos) {
		int x = (int) pos.x*1000;
		int z = (int) pos.z;
		return x+z;
	}
	
	// Converts the global id of an instance into the object id and instance id
	private void convertObjectId(Integer OverallId) {
		objectId = 0;
		instanceId = 0;
		for (int i = 0; i < objects.size(); i++) {
			if (OverallId >= objects.get(i).getNumInstances()) {
				OverallId -= objects.get(i).getNumInstances();
				objectId++;
			} else {
				instanceId = OverallId;
				return;
			}
		}
	}
	
	// Computes the total number of VISIBLE triangles on the scene (not counting the walls or the floor)
	public Integer getSceneTriangles() {
		Integer sceneTriangles = 0, globalIndex = 0;
		for (int i = 0; i < objects.size(); i++) {
			for (int j = 0; j < objects.get(i).getNumInstances(); j++) {
				if (this.objectVisibility.get(globalIndex)) {
					Integer objectLoD = objects.get(i).getLoDofInstance(j);
					sceneTriangles += objects.get(i).getModel(objectLoD).getRawModel().getNumTriangles();
				}
				globalIndex++;
			}
		}
		return sceneTriangles;
	}
	
	// Updates the map (called every frame)
	public void update(Vector3f cameraPos) {
		Integer actualVal;
		for (int i = 0; i < hysteresisValue.size(); i++) {
			actualVal = hysteresisValue.get(i);
			hysteresisValue.set(i, actualVal == 0 ? 0 : actualVal-1);
		}
		timeCriticalRendering(cameraPos);
		updateObjectVisibility(cameraPos);
	}
	
	public ArrayList<LODEntity> getObjects() {
		return objects;
	}
	
	public ArrayList<Entity> getWalls() {
		return walls;
	}
	
	private static TexturedModel loadModel(String modelName, String textureName, Loader loader){
		RawModel model = ObjFileLoader.loadOBJ(modelName, loader);
		ModelTexture texture = new ModelTexture(loader.loadTexture(textureName));
		return new TexturedModel(model, texture);
	}

}
