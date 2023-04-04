package vertexClustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

public class OctreeInfo {
	
	ArrayList<Vector3f> vertices = new ArrayList<Vector3f>(); // the new vertex positions
	ArrayList<Vector2f> textures = new ArrayList<Vector2f>(); // the new texture coordinates
	ArrayList<Vector3f> normals = new ArrayList<Vector3f>(); // the new vertex normals
	
	// a dictionary that includes all the conversions of vertex indices to create the new model
	Map<Integer,Integer> vertexIndexConversion = new HashMap<Integer,Integer>();
	
	ArrayList<Integer> newIndices = new ArrayList<Integer>(); // the new vertex indices
	
	public OctreeInfo() {}
	
	// Given the old array of indices of the original model, it computes a list of the new vertex indices
	// using the dictionary which has been previously defined
	public void computeNewIndices(int[] oldVertices) {
		Set<String> uniqueIndices = new HashSet<String>(); // set used to avoid the addition of repeated faces
		Integer index1, index2, index3, newIndex1, newIndex2, newIndex3;
		for (int i = 0; i < oldVertices.length; i+=3) {
			index1 = oldVertices[i];
			index2 = oldVertices[i+1];
			index3 = oldVertices[i+2];
			newIndex1 = vertexIndexConversion.get(index1);
			newIndex2 = vertexIndexConversion.get(index2);
			newIndex3 = vertexIndexConversion.get(index3);
			String testFace = newIndex1.toString() + newIndex2.toString() + newIndex3.toString();
			// Only add non repeated faces
			if (uniqueIndices.add(testFace)) {
				// Discard lines and points
				if (newIndex1 != newIndex2 && newIndex2 != newIndex3 && newIndex1 != newIndex3) {
					newIndices.add(newIndex1);
					newIndices.add(newIndex2);
					newIndices.add(newIndex3);
				}
			}
		}
	}
	
	public void addVertex(Vector3f vertex) {
		this.vertices.add(vertex);
	}
	
	public void addTexture(Vector2f texture) {
		this.textures.add(texture);
	}
	
	public void addNormal(Vector3f normal) {
		this.normals.add(normal);
	}
	
	public void addIndexConversion(Integer oldV, Integer newV) {
		this.vertexIndexConversion.put(oldV, newV);
	}
	
	public ArrayList<Vector3f> getNewVertices() {
		return this.vertices;
	}
	
	public ArrayList<Vector2f> getNewTextures() {
		return this.textures;
	}
	
	public ArrayList<Vector3f> getNewNormals() {
		return this.normals;
	}
	
	public Map<Integer,Integer> getIndexCoversion() {
		return this.vertexIndexConversion;
	}
	
	public ArrayList<Integer> getNewIndices() {
		return this.newIndices;
	}

}
