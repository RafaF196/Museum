package main;

import java.io.File;
import java.text.DecimalFormat;

import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import entities.Camera;
import fontMeshCreator.FontType;
import fontMeshCreator.GUIText;
import fontRender.TextMaster;
import render.DisplayManager;
import render.Loader;
import render.MasterRenderer;
import terrain.Terrain;
import terrain.Visibility;

public class MainTest {

	public static void main(String[] args) {

		DisplayManager.createDisplay();
		
		Loader loader = new Loader();
		MasterRenderer renderer = new MasterRenderer(loader);
		TextMaster.init(loader);
		
		Camera camera = new Camera(new Vector3f(16, 6, 30), 20.0f, 0.0f);
		
		//Visibility cellVisibility = new Visibility("map1", "map1visibility"); // Uncomment to generate the visibility file
		Terrain map = new Terrain(loader, "map1", "map1visibility", camera.getPosition());
		
		FontType font = new FontType(loader.loadTexture("candara"), new File("res/candara.fnt"));
		GUIText fps_text = new GUIText("FPS: ", 1.8f, font, new Vector2f(0.008f, 0.008f), 1f, false);
		fps_text.setColour(1, 0, 0);

		Integer counter = 1;
		float delta, deltacount = 0;
		
		while(!Display.isCloseRequested()) {

			delta = DisplayManager.getFrameTimeSeconds();
			
	        if (deltacount > 0.2f) { // update every 200 ms or so
	        	float fps = (float) (counter/deltacount);
		        DecimalFormat decimalFormat = new DecimalFormat("00");
		        String numberAsString = decimalFormat.format(fps);
		        fps_text.remove();
		        fps_text = new GUIText("FPS: " + numberAsString, 1.8f, font, new Vector2f(0.008f, 0.008f), 1f, false);
		        fps_text.setColour(1, 0, 0);
				counter = 1;
				deltacount = 0;
	        } else {
	        	counter++;
	        	deltacount += delta;
	        }
	        
	        map.update(camera.getPosition()); // update map information that may change every frame
			camera.move(delta); // move the camera
			
			renderer.renderScene(map.getWalls(), map.getObjects(), camera); // render entities and LOD entities
			TextMaster.render();
			DisplayManager.updateDisplay();
			
		}
		
		TextMaster.cleanUp();
		renderer.cleanUp();
		loader.cleanUp();
		DisplayManager.closeDisplay();

	}

}
