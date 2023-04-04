package vertexClustering;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.lwjgl.util.vector.Vector3f;

import models.RawModel;

public class Octree {
	
	private Node rootNode; // root of the octree (depth level 0)
	private RawModel model; // the model that will be compressed
	
	public Octree(RawModel model) {
		this.model = model;
		
		// Compute the bounding box of the model
		float[] vertices = model.getVertices();
		float minx = 9999, maxx = -9999, miny = 9999, maxy = -9999, minz = 9999, maxz = -9999;
		for (int i = 0; i < vertices.length; i+=3) {
			if (vertices[i] < minx) minx = vertices[i];
			if (vertices[i] > maxx) maxx = vertices[i];
			if (vertices[i+1] < miny) miny = vertices[i+1];
			if (vertices[i+1] > maxy) maxy = vertices[i+1];
			if (vertices[i+2] < minz) minz = vertices[i+2];
			if (vertices[i+2] > maxz) maxz = vertices[i+2];
		}
		
		Vector3f mid = new Vector3f((maxx+minx)/2, (maxy+miny)/2, (maxz+minz)/2); // Its middle point
		Vector3f size = new Vector3f((maxx-minx)/2, (maxy-miny)/2, (maxz-minz)/2); // Its size (from mid to border)
		
		// Add all the vertices (the indices) to the root (all inside the main bounding box)
		ArrayList<Integer> indices = (ArrayList<Integer>) IntStream.rangeClosed(0, 
				model.getVertices().length/3 -1).boxed().collect(Collectors.toList());
		
		// Create the root of the octree (which will store all the information)
		Node parent = new Node(0, mid, size, indices, model.getVertices(), model.getTextures(),
				model.getNormals(), model.getQuadrics());
		this.rootNode = parent;
	}
	
	// function that generates the octree until an indicated depth level (included)
	public void generateOctreeLevels(Integer deepestLevel) {
		rootNode.generateSubvoxels(deepestLevel);
	}
	
	// function that obtains all the information from an octree level in order to
	// generate a compressed model
	public OctreeInfo getDataFromOctreeLevel(Integer level) {
		rootNode.compressData(level);
		rootNode.computeNewIndices(model.getIndices());
		return rootNode.getCompressedData();
	}
	
	public Node getRootNode() {
		return rootNode;
	}
	
	public RawModel getModel() {
		return model;
	}
	
	public float[] getModelVertices() {
		return model.getVertices();
	}
	
	public float[] getModelTextures() {
		return model.getTextures();
	}
	
	public float[] getModelNormals() {
		return model.getNormals();
	}
	
	public int[] getModelIndices() {
		return model.getIndices();
	}
	
}
