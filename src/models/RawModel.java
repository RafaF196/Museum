package models;

import java.util.ArrayList;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import toolbox.MatrixOps;

public class RawModel {
	
	private String modelName;
	
	private int vaoID; // ID of the VAO
	private int vertexCount; // number of vertex of the model
	
	private float[] verticesArray; // array of all the vertex positions
	private float[] texturesArray; // array of all the texture coordinates
	private float[] normalsArray; // array of all the normal vectors of the vertices
	private int[] indicesArray; // indices of vertices to obtain the faces
	
	private ArrayList<Matrix4f> quadricsArray = new ArrayList<Matrix4f>(); // the matrix Qv for each vertex
	
	public RawModel(String modelName, int vaoID, int vertexCount, float[] verticesArray, float[] texturesArray,
			float[] normalsArray, int[] indicesArray) {
		this.modelName = modelName;
		this.vaoID = vaoID;
		this.vertexCount = vertexCount;
		
		this.verticesArray = verticesArray;
		this.texturesArray = texturesArray;
		this.normalsArray = normalsArray;
		this.indicesArray = indicesArray;
		
		calculateQuadrics(); // compute only once (when loaded)
	}
	
	// Calculates the quadrics for all the vertices in the model
	public void calculateQuadrics() {
		Vector3f pos = null, norm = null;
		for (int i = 0; i < verticesArray.length; i+=3) {
			pos = new Vector3f(verticesArray[i], verticesArray[i+1], verticesArray[i+2]);
			norm = new Vector3f(normalsArray[i], normalsArray[i+1], normalsArray[i+2]);
			this.quadricsArray.add(MatrixOps.createQuadricMatrix(pos, norm));
		}
	}

	public String getModelName() {
		return modelName;
	}
	
	public int getVaoID() {
		return vaoID;
	}

	public int getVertexCount() {
		return vertexCount;
	}
	
	public float[] getVertices() {
		return verticesArray;
	}
	
	public float[] getTextures() {
		return texturesArray;
	}
	
	public float[] getNormals() {
		return normalsArray;
	}
	
	public int[] getIndices() {
		return indicesArray;
	}
	
	public Integer getNumTriangles() {
		return indicesArray.length/3;
	}
	
	public ArrayList<Matrix4f> getQuadrics() {
		return quadricsArray;
	}
	
}
