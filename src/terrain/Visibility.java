package terrain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

public class Visibility {
	
	private static final String RES_LOC = "res/";
	
	// The size of the map
	private Integer mapSizex;
	private Integer mapSizey;
	
	private ArrayList<ArrayList<Integer>> terrainValues; // The map values: 0 for empty, 1 for walls
	private Map<Integer, Set<Integer>> visibleCells; // Map keys and values are cells coded as Integers to avoid duplicates
	
	Vector2f P1, P2; // Endpoints of the line (segment)
	
	public Visibility(String mapName, String visibilityFileName) {
		this.terrainValues = new ArrayList<ArrayList<Integer>>();
		this.visibleCells = new HashMap<Integer, Set<Integer>>();
		
		readMapFile(mapName);
		generateVisibilityFile(50000, visibilityFileName); // 50000 lines looked like enough for the maps used
	}
	
	// Reads the file corresponding to the map (now only focusing on the walls, not on the objects)
	private void readMapFile(String mapName) {
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
		
		try {
			// Map size
			line = reader.readLine();
			mapSizex = (Integer) Integer.valueOf(line);
			line = reader.readLine();
			mapSizey = (Integer) Integer.valueOf(line);
			
			// Current cell
			Integer xPos = 0, yPos = 0;
			terrainValues.add(new ArrayList<Integer>());
			
			line = reader.readLine();
			while (line != null) {
				cellValue = (Integer) Integer.valueOf(line); // The value of this cell
					
				if (xPos == mapSizey) {
					terrainValues.add(new ArrayList<Integer>());
					xPos = 0;
					yPos += 1;
				}
				
				// Cell value 1 if there is a wall
				// Cell value 0 otherwise
				terrainValues.get(yPos).add(cellValue == 0 ? 1 : 0);
				xPos += 1;
				
				line = reader.readLine();
			}
			reader.close();
		} catch (Exception e) {
			System.err.println("Error reading the file");
			System.exit(-1);
		}
		
	}
	
	// Generates the file with all the visibility information of this map
	public void generateVisibilityFile(Integer randomLineNumber, String fileName) {
		// First generate the data (draw the lines, cell intersections, subsets, ...)
		for (int i = 0; i < randomLineNumber; i++) {
			generateLinePoints();
			ArrayList<Vector3f> lineInfo = intersectLineWithCells();
			addVisibleSets(lineInfo);
		}
		
		FileWriter fw = null;
		try {
			fw = new FileWriter("res/" + fileName + ".txt");
			
			fw.write("# Visibility file generated by Rafael �vila\n");
			
			for (Map.Entry<Integer,Set<Integer>> entry : this.visibleCells.entrySet()) { // For each entry on the map
				fw.write(entry.getKey().toString() + "\n"); // Write the current cell ID
	            Iterator<Integer> it = entry.getValue().iterator();
	            while(it.hasNext()){ // For each value on the set
	            	fw.write(it.next().toString() + ","); // Write the visible cells IDs
	            }
	            fw.write("\n");
			}
			
			fw.close();
		} catch (IOException e) {
			System.err.println("Error writting the file");
			System.exit(-1);
		}
	}
	
	// Generates two random points on 2 different sides of the map
	public void generateLinePoints() {
		Random random = new Random();
		Integer side1, side2;
		side1 = random.nextInt(4);
		side2 = (side1 + random.nextInt(2)+1)%4;
		
		if (side1 == 0) P1 = new Vector2f(random.nextFloat()*mapSizex, 0.0f);
		if (side1 == 1) P1 = new Vector2f(0.0f, random.nextFloat()*mapSizey);
		if (side1 == 2) P1 = new Vector2f(random.nextFloat()*mapSizex, mapSizey);
		if (side1 == 3) P1 = new Vector2f(mapSizex, random.nextFloat()*mapSizey);
		
		if (side2 == 0) P2 = new Vector2f(random.nextFloat()*mapSizex, 0.0f);
		if (side2 == 1) P2 = new Vector2f(0.0f, random.nextFloat()*mapSizey);
		if (side2 == 2) P2 = new Vector2f(random.nextFloat()*mapSizex, mapSizey);
		if (side2 == 3) P2 = new Vector2f(mapSizex, random.nextFloat()*mapSizey);
	}
	
	// Returns all the cells intersected by the previously generated line connecting the two points
	public ArrayList<Vector3f> intersectLineWithCells() {
		// Start at P1 and iteratively move to P2
		Float currentX = P1.x, currentY = P1.y;
		Float slopeX = P2.x-P1.x, slopeY = P2.y-P1.y;
		if (slopeX == 0) slopeX = 0.000001f; // Avoid dividing by 0 (very unlikely, but possible)
		if (slopeY == 0) slopeY = 0.000001f;
		Integer signX = ((slopeX > 0 ? 1 : 0)); // 1 if positive, 0 if negative
		Integer signY = ((slopeY > 0 ? 1 : 0));
		Integer cellX = getCellX(currentX), cellY = getCellY(currentY); // current Cell
		
		// Now, visualize the grid formed by all the cells, these values are:
		Float dx = (cellX + signX - currentX)/slopeX; // part of the line remaining until intersecting a vertical line
		Float dy = (cellY + signY - currentY)/slopeY; // part of the line remaining until intersecting a horizontal line
		
		ArrayList<Vector3f> lineInfo = new ArrayList<Vector3f>();  // Format: (CellX, CellY, Value)
		
		while (cellX >= 0 && cellX < mapSizex && cellY >= 0 && cellY < mapSizey) { // While inside the map
			
			// Add cell to traversed list
			lineInfo.add(new Vector3f(cellX, cellY, terrainValues.get(cellX).get(cellY)));
		
			if (dx < dy) { // intersection with vertical line
				
				currentX += dx*slopeX;
				currentY += dx*slopeY;
				cellX = (signX == 0 ? cellX-1 : cellX+1);
				
			} else if (dy < dx) { // intersection with horizontal line
				
				currentX += dy*slopeX;
				currentY += dy*slopeY;
				cellY = (signY == 0 ? cellY-1 : cellY+1);
				
			} else { // intersection with both lines (unlikely)
				
				currentX += dx*slopeX;
				currentY += dy*slopeY;
				cellX = (signX == 0 ? cellX-1 : cellX+1);
				cellY = (signY == 0 ? cellY-1 : cellY+1);
				
			}
			
			// Recompute these values at the new position
			dx = (cellX + signX - currentX)/slopeX;
			dy = (cellY + signY - currentY)/slopeY;
			
		}
		
		return lineInfo;
		
	}
	
	// Divides the cells into subsets separated by walls
	// For each each subset, traverse all the cells and add every cell on the subset as a visible cell from each cell on the subset
	public void addVisibleSets(ArrayList<Vector3f> lineInfo) {
		Vector3f cell;
		ArrayList<Vector2f> actualSet = new ArrayList<Vector2f>();
		
		for (int i = 0; i < lineInfo.size(); i++) { // traverse the line
			cell = lineInfo.get(i);
			
			if (cell.z == 0) { // If no wall is found, keep adding cells to the visible subset
				actualSet.add(new Vector2f(cell.x, cell.y));
			} else { // If a wall is found...
				
				for (int ii = 0; ii < actualSet.size(); ii++) { // For each cell on the subset
					Integer cellId = convertCellToInt(actualSet.get(ii));
					
					for (int jj = 0; jj < actualSet.size(); jj++) { // Add all of them as visible cells
						Integer cellId2 = convertCellToInt(actualSet.get(jj));
						if (!visibleCells.containsKey(cellId)) { // If no cell was added to the current Id, create the set
							Set<Integer> newKey = new HashSet<Integer>();
							newKey.add(cellId2);
							visibleCells.put(cellId, newKey);
						} else { // Add the cell directly otherwise
							visibleCells.get(cellId).add(cellId2);
						}
					}
					
				}
				
				actualSet.clear();
			}
		}
	}
	
	// Returns the current Cell.x with a given x position
	public Integer getCellX(float c) {
		return ((int) c == mapSizex ? mapSizex-1 : (int) c);
	}
	
	// Returns the current Cell.y with a given y position
	public Integer getCellY(float c) {
		return ((int) c == mapSizey ? mapSizey-1 : (int) c);
	}
	
	// Cell(x,y) = xxxyyy. For example, Cell(12,5) = 012005
	public Integer convertCellToInt(Vector2f cell) {
		int x = (int) cell.y*1000; // Coordinates switched to match the map's coordinate system
		int y = (int) cell.x;
		return x+y;
	}

}
