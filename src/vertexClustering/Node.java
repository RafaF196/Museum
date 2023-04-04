package vertexClustering;

import java.util.ArrayList;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import toolbox.MatrixOps;

public class Node {
	
	private Integer depthLevel; // the depth level of this node
	private Vector3f midpoint; // the mid point of the space this node covers
	private Vector3f voxelSize; // the distance between the midpoint to the border of the voxel
	private ArrayList<Integer> vertexIndices; // the indices of the vertices that are inside this voxel
	
	private float[] vertices = null; // ROOT ONLY: the position of the vertices (original model)
	private float[] textures = null; // ROOT ONLY: the texture coordinates (original model)
	private float[] normals = null; // ROOT ONLY: the normals (original model)
	
	// ROOT ONLY: the Qv matrices for each vertex (original model)
	private ArrayList<Matrix4f> quadrics = new ArrayList<Matrix4f>();
	
	private Node parentNode = null; // the parent node of the vertex (not the whole tree)
	private ArrayList<Node> childNodes = new ArrayList<Node>(); // the children nodes of this node (0 or 8)
	
	 // ROOT ONLY: data structure containing all the information for a compressed model
	private OctreeInfo compressedData = null;
	private Integer globalIndex = 1; // ROOT ONLY: index used to assign new vertices for compressed models
	
	// Constructor (only used for the root node)
	public Node(Integer depthLevel, Vector3f midpoint, Vector3f voxelSize, ArrayList<Integer> vertexIndices, 
			float[] vertices, float[] textures, float[] normals, ArrayList<Matrix4f> quadrics) {
		this.depthLevel = depthLevel;
		this.midpoint = midpoint;
		this.voxelSize = voxelSize;
		this.vertexIndices = vertexIndices;
		
		this.vertices = vertices;
		this.textures = textures;
		this.normals = normals;
		this.quadrics = quadrics;
	}
	
	// Constructor used for the rest of the nodes
	public Node(Node parent, Integer depthLevel, Vector3f midpoint, Vector3f voxelSize, ArrayList<Integer> vertexIndices) {
		this.parentNode = parent;
		this.depthLevel = depthLevel;
		this.midpoint = midpoint;
		this.voxelSize = voxelSize;
		this.vertexIndices = vertexIndices;
	}
	
	// Computes all the data to generate a compressed model using the data at one level
	// of the octree (all data goes into the OctreeInfo data structure)
	public void compressData(Integer level) {
		compressedData = new OctreeInfo();
		globalIndex = 1;
		extractDataFromVoxel(level);
	}
	
	// Recursive function that travels through all the nodes until it reaches a certain depth level.
	// When it does, it extracts all the necessary information from this particular voxel
	public void extractDataFromVoxel(Integer level) {
		// Recursive part of the function
		if (depthLevel != level) {
			for (int i = 0; i < 8; i++) {
				if (!isNodeEmpty(childNodes.get(i))) {
					childNodes.get(i).extractDataFromVoxel(level);
				}
			}
		} else { // This node is on the intended level
			// add new Vertex
			Matrix4f qMat = new Matrix4f();
			// Computing the matrix Q for this voxel
			for (int i = 0; i < vertexIndices.size(); i++) {
				Integer vIndex = vertexIndices.get(i);
				Matrix4f.add(qMat, getQuadricByIndex(vIndex), qMat);
			}
			// Computing the representative of this voxel
			Vector3f newVertex = MatrixOps.obtainOptimalContractionVertex(qMat);
			if (newVertex == null) { // Alternative based on paper
				// If we can't use the Quadrics method, obtain the average
				newVertex = computeAveragePoint();
			}
			// Since the Quadrics method only computes new vertex positions, I compute
			// the texture coords and the normal vectors by doing the average.
			Vector2f newTexture = computeAverageTexture();
			Vector3f newNormal = computeAverageNormal();
			addVertexInfoToCompressedData(newVertex, newTexture, newNormal);
			
			// new index for all the previous vertices in this voxel
			// (in order to replace old indexes with the new one)
			Integer currentGlobalIndex = getGlobalIndex();
			for (int i = 0; i < vertexIndices.size(); i++) {
				Integer iIndex = vertexIndices.get(i);
				addConversionToCompressedData(iIndex, currentGlobalIndex);
			}
			incrementGlobalIndex();
		}
	}
	
	// Computes the new indices for the compressed model (see the function in "OctreeInfo")
	public void computeNewIndices(int[] indices) {
		compressedData.computeNewIndices(indices);
	}
	
	// Function that recursively generates the octree until reaching a certain depth level
	// It will keep dividing the voxels and classifying the vertices accordingly
	// while computing some other information
	public void generateSubvoxels(Integer deepestLevel) {
		subDivideVoxel();
		for (int i = 0; i < 8; i++) {
			if (!isNodeEmpty(childNodes.get(i)) && deepestLevel > (depthLevel+1)) {
				childNodes.get(i).generateSubvoxels(deepestLevel);
			}
		}
	}
	
	// Function used to divide a voxel into 8 subdivisions amongst its children
	public void subDivideVoxel() {
		// Temporal data structure to divide the vertex indices
		ArrayList<ArrayList<Integer>> vertexClasification = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < 8; i++) {
			vertexClasification.add(new ArrayList<Integer>());
		}
		
		// Classify the vertices depending on their coordinates and the midpoint of the actual voxel
		// These are the possible outcomes depending on their position:
		// 0. x+y+z+  1. x+y+z-  2. x+y-z+  3. x+y-z-  4. x-y+z+  5. x-y+z- 6. x-y-z+  7. x-y-z-
		for (int i = 0; i < vertexIndices.size(); i++) {
			Integer vIndex = vertexIndices.get(i); // Travel the vertices on this voxel
			Vector3f currentV = getVertexByIndex(vIndex);
			if (currentV.x >= midpoint.x && currentV.y >= midpoint.y && currentV.z >= midpoint.z)
				vertexClasification.get(0).add(vIndex);
			else if (currentV.x >= midpoint.x && currentV.y >= midpoint.y && currentV.z <= midpoint.z)
				vertexClasification.get(1).add(vIndex);
			else if (currentV.x >= midpoint.x && currentV.y <= midpoint.y && currentV.z >= midpoint.z)
				vertexClasification.get(2).add(vIndex);
			else if (currentV.x >= midpoint.x && currentV.y <= midpoint.y && currentV.z <= midpoint.z)
				vertexClasification.get(3).add(vIndex);
			else if (currentV.x <= midpoint.x && currentV.y >= midpoint.y && currentV.z >= midpoint.z)
				vertexClasification.get(4).add(vIndex);
			else if (currentV.x <= midpoint.x && currentV.y >= midpoint.y && currentV.z <= midpoint.z)
				vertexClasification.get(5).add(vIndex);
			else if (currentV.x <= midpoint.x && currentV.y <= midpoint.y && currentV.z >= midpoint.z)
				vertexClasification.get(6).add(vIndex);
			else if (currentV.x <= midpoint.x && currentV.y <= midpoint.y && currentV.z <= midpoint.z)
				vertexClasification.get(7).add(vIndex);
		}
		
		// Generate new size and new midpoints
		Vector3f newVoxelSize = new Vector3f(voxelSize.x/2, voxelSize.y/2, voxelSize.z/2);
		Vector3f child0newMidpoint = new Vector3f(midpoint.x + newVoxelSize.x, midpoint.y + newVoxelSize.y, midpoint.z + newVoxelSize.z);
		Vector3f child1newMidpoint = new Vector3f(midpoint.x + newVoxelSize.x, midpoint.y + newVoxelSize.y, midpoint.z - newVoxelSize.z);
		Vector3f child2newMidpoint = new Vector3f(midpoint.x + newVoxelSize.x, midpoint.y - newVoxelSize.y, midpoint.z + newVoxelSize.z);
		Vector3f child3newMidpoint = new Vector3f(midpoint.x + newVoxelSize.x, midpoint.y - newVoxelSize.y, midpoint.z - newVoxelSize.z);
		Vector3f child4newMidpoint = new Vector3f(midpoint.x - newVoxelSize.x, midpoint.y + newVoxelSize.y, midpoint.z + newVoxelSize.z);
		Vector3f child5newMidpoint = new Vector3f(midpoint.x - newVoxelSize.x, midpoint.y + newVoxelSize.y, midpoint.z - newVoxelSize.z);
		Vector3f child6newMidpoint = new Vector3f(midpoint.x - newVoxelSize.x, midpoint.y - newVoxelSize.y, midpoint.z + newVoxelSize.z);
		Vector3f child7newMidpoint = new Vector3f(midpoint.x - newVoxelSize.x, midpoint.y - newVoxelSize.y, midpoint.z - newVoxelSize.z);
		
		// Add the children nodes to the octree
		childNodes.add(0, new Node(this, depthLevel+1, child0newMidpoint, newVoxelSize, vertexClasification.get(0)));
		childNodes.add(1, new Node(this, depthLevel+1, child1newMidpoint, newVoxelSize, vertexClasification.get(1)));
		childNodes.add(2, new Node(this, depthLevel+1, child2newMidpoint, newVoxelSize, vertexClasification.get(2)));
		childNodes.add(3, new Node(this, depthLevel+1, child3newMidpoint, newVoxelSize, vertexClasification.get(3)));
		childNodes.add(4, new Node(this, depthLevel+1, child4newMidpoint, newVoxelSize, vertexClasification.get(4)));
		childNodes.add(5, new Node(this, depthLevel+1, child5newMidpoint, newVoxelSize, vertexClasification.get(5)));
		childNodes.add(6, new Node(this, depthLevel+1, child6newMidpoint, newVoxelSize, vertexClasification.get(6)));
		childNodes.add(7, new Node(this, depthLevel+1, child7newMidpoint, newVoxelSize, vertexClasification.get(7)));
	}
	
	// Computes the average vertex coordinates of all the vertices in this voxel
	public Vector3f computeAveragePoint() {
		Vector3f avVertex = new Vector3f(0,0,0);
		Integer numVert = vertexIndices.size();
		for (int i = 0; i < numVert; i++) {
			Integer vIndex = vertexIndices.get(i);
			Vector3f currentV = getVertexByIndex(vIndex);
			avVertex.x = avVertex.x + currentV.x;
			avVertex.y = avVertex.y + currentV.y;
			avVertex.z = avVertex.z + currentV.z;
		}
		return new Vector3f(avVertex.x/numVert, avVertex.y/numVert, avVertex.z/numVert);
	}
	
	// Computes the average texture coordinates of all the vertices in this voxel
	public Vector2f computeAverageTexture() {
		Vector2f avTexture = new Vector2f(0,0);
		Integer numVert = vertexIndices.size();
		for (int i = 0; i < numVert; i++) {
			Integer vIndex = vertexIndices.get(i);
			Vector2f currentV = getTextureByIndex(vIndex);
			avTexture.x = avTexture.x + currentV.x;
			avTexture.y = avTexture.y + currentV.y;
		}
		return new Vector2f(avTexture.x/numVert, avTexture.y/numVert);
	}
	
	// Computes the average vertex normal of all the vertices in this voxel
	public Vector3f computeAverageNormal() {
		Vector3f avNormal = new Vector3f(0,0,0);
		Integer numVert = vertexIndices.size();
		for (int i = 0; i < numVert; i++) {
			Integer vIndex = vertexIndices.get(i);
			Vector3f currentV = getNormalByIndex(vIndex);
			avNormal.x = avNormal.x + currentV.x;
			avNormal.y = avNormal.y + currentV.y;
			avNormal.z = avNormal.z + currentV.z;
		}
		return new Vector3f(avNormal.x/numVert, avNormal.y/numVert, avNormal.z/numVert);
	}
	
	// Check if a node is empty
	public Boolean isNodeEmpty(Node node) {
		return (node.vertexIndices.isEmpty());
	}
	
	public void addVertex(Integer vertex) {
		vertexIndices.add(vertex);
	}
	
	public Integer getDepthLevel() {
		return depthLevel;
	}
	
	public Vector3f getMidPoint() {
		return midpoint;
	}
	
	public Vector3f getVoxelSize() {
		return voxelSize;
	}
	
	public ArrayList<Integer> getNodeVertices() {
		return vertexIndices;
	}
	
	public Node getChild(Integer numChild) {
		return childNodes.get(numChild);
	}
	
	public OctreeInfo getCompressedData() {
		return compressedData;
	}
	
	// Get the vertex position of a vertex by taking the information from the root
	public Vector3f getVertexByIndex(Integer index) {
		if (depthLevel > 0) {
			return parentNode.getVertexByIndex(index);
		} else {
			return new Vector3f(vertices[3*index], vertices[3*index+1], vertices[3*index+2]);
		}
	}
	
	// The same but with the texture coordinates
	public Vector2f getTextureByIndex(Integer index) {
		if (depthLevel > 0) {
			return parentNode.getTextureByIndex(index);
		} else {
			return new Vector2f(textures[2*index], textures[2*index+1]);
		}
	}
	
	// The same but with the normal vectors
	public Vector3f getNormalByIndex(Integer index) {
		if (depthLevel > 0) {
			return parentNode.getNormalByIndex(index);
		} else {
			return new Vector3f(normals[3*index], normals[3*index+1], normals[3*index+2]);
		}
	}
	
	// The same but with the quadrics
	public Matrix4f getQuadricByIndex(Integer index) {
		if (depthLevel > 0) {
			return parentNode.getQuadricByIndex(index);
		} else {
			return quadrics.get(index);
		}
	}
	
	// Increment the global index by one
	public void incrementGlobalIndex() {
		if (depthLevel > 0) {
			parentNode.incrementGlobalIndex();
		} else {
			this.globalIndex++;
		}
	}
	
	// Get the global index from the root
	public Integer getGlobalIndex() {
		if (depthLevel > 0) {
			return parentNode.getGlobalIndex();
		} else {
			return this.globalIndex;
		}
	}
	
	// Adds all the information of a vertex (position, tex coord and normal) to the data structure for
	// the compressed model. Remember that it's only stored on the root node.
	public void addVertexInfoToCompressedData(Vector3f vertex, Vector2f texture, Vector3f normal) {
		if (depthLevel > 0) {
			parentNode.addVertexInfoToCompressedData(vertex, texture, normal);
		} else {
			this.compressedData.addVertex(vertex);
			this.compressedData.addTexture(texture);
			this.compressedData.addNormal(normal);
		}
	}
	
	// Adds a new vertex index conversion tuple to the compressed data structure
	public void addConversionToCompressedData(Integer oldV, Integer newV) {
		if (depthLevel > 0) {
			parentNode.addConversionToCompressedData(oldV, newV);
		} else {
			this.compressedData.addIndexConversion(oldV, newV);
		}
	}

}
