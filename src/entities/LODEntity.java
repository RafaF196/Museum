package entities;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import models.LODModel;
import models.TexturedModel;

public class LODEntity {

	private LODModel model; // You can check all about LODModels on the models package
	private Vector3f position;
	private float rotY;
	private float scale;
	
	private float diagonal; // The diagonal of the bounding box of the model (without scaling)
	
	private Integer numInstances; // Number of instances of this entity
	private ArrayList<Integer> LoD; // The level of detail of each instance
	private ArrayList<Vector2f> offsets; // The offsets of each entity from the position above
	private List<Boolean> visibleOffsets; // True if the instance is visible from the camera, false otherwise

	public LODEntity(LODModel model, Vector3f position, float rotY, float scale) {
		this.model = model;
		this.position = position;
		this.rotY = rotY;
		this.scale = scale;
		
		this.diagonal = computeDiagonal();
		
		this.numInstances = 0;
		this.LoD = new ArrayList<Integer>();
		this.offsets = new ArrayList<Vector2f>();
		this.visibleOffsets = new ArrayList<Boolean>();
	}
	
	private Float computeDiagonal() {
		
		// Use an average level of detail, to improve performance without having a noticeable effect on the result
		float[] vertices = model.getModel(2).getRawModel().getVertices();
		float minx = 9999, maxx = -9999, miny = 9999, maxy = -9999, minz = 9999, maxz = -9999;
		for (int i = 0; i < vertices.length; i+=3) {
			if (vertices[i] < minx) minx = vertices[i];
			if (vertices[i] > maxx) maxx = vertices[i];
			if (vertices[i+1] < miny) miny = vertices[i+1];
			if (vertices[i+1] > maxy) maxy = vertices[i+1];
			if (vertices[i+2] < minz) minz = vertices[i+2];
			if (vertices[i+2] > maxz) maxz = vertices[i+2];
		}
		
		Float d = (float) Math.sqrt( (maxx-minx)*(maxx-minx) + (maxy-miny)*(maxy-miny) + (maxz-minz)*(maxz-minz) );
		
		return d;
	}
	
	// Add an instance of this entity on the given offset.
	public void addInstance(Vector2f Ioffset) {
		numInstances++;
		LoD.add(0); // Initially at lowest level of detail
		offsets.add(Ioffset);
		visibleOffsets.add(false); // Not visible on first frame (avoid initial frame drop)
	}
	
	// Change the LoD of the instance at this index to this new level of detail
	public void changeLoDofInstance(Integer index, Integer newLoD) {
		LoD.set(index, newLoD);
	}
	
	// Updates the visibility of the instances
	public void updateVisibleOffsets(List<Boolean> visibleList) {
		this.visibleOffsets = visibleList;
	}

	public TexturedModel getModel(Integer index) {
		return model.getModel(index);
	}

	public Vector3f getPosition() {
		return position;
	}

	public float getRotY() {
		return rotY;
	}

	public float getScale() {
		return scale;
	}
	
	// Returns the diagonal of the bounding box with the correct scaling
	public float getDiagonal() {
		return diagonal*scale;
	}
	
	// Returns the total number of instances of this entity
	public Integer getNumInstances() {
		return numInstances;
	}
	
	// Returns the level of detail of an instance
	public Integer getLoDofInstance(Integer index) {
		return LoD.get(index);
	}
	
	// Returns the levels of details of all the instances of this entity
	public ArrayList<Integer> getLoDs() {
		return LoD;
	}
	
	// Gets all the instances (their offsets) that are at a certain level of detail
	public ArrayList<Vector2f> getOffSetsLoD(Integer level) {
		ArrayList<Vector2f> loffsets = new ArrayList<Vector2f>();
		for (int i = 0; i < numInstances; i++) {
			if (LoD.get(i) == level) loffsets.add(offsets.get(i));
		}
		return loffsets;
	}
	
	// Gets all the VISIBLE instances (their offsets) that are at a certain level of detail
	public ArrayList<Vector2f> getVisibleOffSetsLoD(Integer level) {
		ArrayList<Vector2f> loffsets = new ArrayList<Vector2f>();
		for (int i = 0; i < numInstances; i++) {
			if (LoD.get(i) == level && visibleOffsets.get(i)) loffsets.add(offsets.get(i));
		}
		return loffsets;
	}
	
	// Returns all the offsets of this entity
	public ArrayList<Vector2f> getOffSets() {
		return offsets;
	}

}
