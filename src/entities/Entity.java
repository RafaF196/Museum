package entities;

import models.TexturedModel;

import java.util.ArrayList;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

public class Entity {

	private TexturedModel model;
	private Vector3f position;
	private float rotY;
	private float scale;
	
	private Integer numInstances; // Number of instances of this entity
	private ArrayList<Vector2f> offsets; // The offsets of each entity from the position above

	public Entity(TexturedModel model, Vector3f position, float rotY, float scale) {
		this.model = model;
		this.position = position;
		this.rotY = rotY;
		this.scale = scale;
		
		this.numInstances = 0;
		this.offsets = new ArrayList<Vector2f>();
	}
	
	public void addInstance(Vector2f Ioffset) {
		numInstances++;
		offsets.add(Ioffset);
	}

	public TexturedModel getModel() {
		return model;
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
	
	// Returns the total number of instances of this entity
	public Integer getNumInstances() {
		return numInstances;
	}
	
	// Returns all the offsets of this entity
	public ArrayList<Vector2f> getOffSets() {
		return offsets;
	}

}
