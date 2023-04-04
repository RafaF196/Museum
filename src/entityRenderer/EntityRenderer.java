package entityRenderer;

import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.util.vector.Matrix4f;

import entities.Camera;
import entities.Entity;
import entities.LODEntity;
import models.RawModel;
import models.TexturedModel;
import toolbox.MatrixOps;

public class EntityRenderer {

	private EntityShader entityShader;

	public EntityRenderer(Matrix4f projectionMatrix) {
		entityShader = new EntityShader();
		entityShader.start();
		entityShader.loadProjectionMatrix(projectionMatrix);
		entityShader.connectTextureUnits();
		entityShader.stop();
	}

	// Render method for all entities on the scene
	public void render(List<Entity> list, List<LODEntity> lodlist, Camera camera) {
		entityShader.start();
		entityShader.loadViewMatrix(camera);

		for (Entity entity : list) { // First we render all the entities (the floor and the walls)
			TexturedModel model = entity.getModel();
			bindModelVao(model);
			bindTexture(model);
			loadModelMatrix(entity);
			entityShader.loadOffsetPosition(entity.getOffSets()); // Load the offsets of each instance
			
			GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, model.getRawModel().getVertexCount(),
					GL11.GL_UNSIGNED_INT, 0, entity.getNumInstances()); // Draw the correct number of instances
			unbindVao();
		}
		
		for (LODEntity entity : lodlist) { // Then, all the LODentities
			for (int lod = 0; lod < 5; lod++) { // Render each level of detail at one time (different model at each step)
				// Number of instances of that entity on this level of detail that are visible from the camera position
				Integer numInst = entity.getVisibleOffSetsLoD(lod).size();
				if (numInst > 0) { // If no instances are found, don't do the render call
					TexturedModel model = entity.getModel(lod);
					bindModelVao(model);
					bindTexture(model);
					loadModelMatrix(entity);
					entityShader.loadOffsetPosition(entity.getVisibleOffSetsLoD(lod)); // Load the offsets of each instance
					// Notice how only the instances with a Level of detail "lod" and that are visible are loaded
					
					GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, model.getRawModel().getVertexCount(),
							GL11.GL_UNSIGNED_INT, 0, numInst);
					unbindVao();
				}
			}
		}
		
		entityShader.stop();
	}

	// binds the Vertex Array Object of a model and enables the vertex attributes stored in positions
	// 0 (vertex positions), 1 (texture coordinates) and 2 (normal vectors)
	private void bindModelVao(TexturedModel model) {
		RawModel rawModel = model.getRawModel();
		GL30.glBindVertexArray(rawModel.getVaoID());
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL20.glEnableVertexAttribArray(2);
	}

	// unbinds the Vertex Array Object
	private void unbindVao() {
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL20.glDisableVertexAttribArray(2);
		GL30.glBindVertexArray(0);
	}

	// binds the texture of the model
	private void bindTexture(TexturedModel model) {
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, model.getTexture().getID());
	}
	
	private void loadModelMatrix(Entity entity) {
		Matrix4f transformationMatrix = MatrixOps.createTransformationMatrix(entity.getPosition(), 0, entity.getRotY(), 0,
				entity.getScale());
		entityShader.loadTransformationMatrix(transformationMatrix);
	}

	private void loadModelMatrix(LODEntity entity) {
		Matrix4f transformationMatrix = MatrixOps.createTransformationMatrix(entity.getPosition(), 0, entity.getRotY(), 0,
				entity.getScale());
		entityShader.loadTransformationMatrix(transformationMatrix);
	}
	
	public void cleanUp() {
		entityShader.cleanUp();
	}

}
