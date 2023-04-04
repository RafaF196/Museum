package models;

import java.util.ArrayList;

public class LODModel {
	
	private ArrayList<TexturedModel> models = new ArrayList<TexturedModel>(); // The model used for each level of detail
	private Integer[] numTriangles = new Integer[5]; // The number of triangles the model has at each level
	
	public LODModel (TexturedModel l0, TexturedModel l1, TexturedModel l2, TexturedModel l3, TexturedModel l4) {
		models.clear();
		models.add(l0);
		models.add(l1);
		models.add(l2);
		models.add(l3);
		models.add(l4);
		
		for (int i = 0; i < 5; i++) {
			numTriangles[i] = models.get(i).getRawModel().getNumTriangles();
		}
	}
	
	// Returns the model at a certain level of detail
	public TexturedModel getModel(Integer index) {
		return models.get(index);
	}
	
	// Returns the number of triangles at a certain level of detail
	public Integer getNumTriangles(Integer index) {
		return numTriangles[index];
	}

}
