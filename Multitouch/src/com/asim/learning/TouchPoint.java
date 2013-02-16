package com.asim.learning;

import android.graphics.Point;
import android.view.View;

/******************************************************************************
 * This class is used to tie together pointer coordinates, the view associated 
 * with it and the pointer index of that touch point
 * @author Asim
 ******************************************************************************/
public class TouchPoint {
	
	Point coordinates;						//stores the X and Y coordinates
	int pointerId;
	int pointerIndex;						//pointer index for this touch point goes in here
	Object extra;							//this is used to store the marker object
	boolean valid;
	boolean isDown;
	
	/******************************************************************************
	 * Class constructor
	 ******************************************************************************/
	public TouchPoint(){
		coordinates = new Point();
		pointerId = -1;
		pointerIndex = -1;
		extra = null;
		valid = false;
		isDown = false;
	}
	
	/******************************************************************************
	 * Overloaded constructor
	 * @param x	- x coordinate to be stored
	 * @param y - y coordinate to be stored
	 * @param pIndex - pointer index for this point
	 * @param ext - the marker object to be stored
	 ******************************************************************************/
	public TouchPoint(int x, int y,int pId, int pIndex, Object ext){
		coordinates = new Point();
		setValues(x,y,pId,pIndex,ext);		
	}
	
	/******************************************************************************
	 * Simple setter routine
	 * @param x - x coordinate value
	 * @param y - y coordinate value
	 * @param pIndex - pointer index for this point
	 * @param ext - the marker object to be stored
	 ******************************************************************************/
	public void setValues(int x, int y,int pId, int pIndex, Object ext){
		coordinates.x = x; 
		coordinates.y = y;
		pointerIndex = pIndex;
		pointerId = pId;
		extra = ext;
		
	}
	
	public int getX(){
		return coordinates.x;
	}
	
	public int getY(){
		return coordinates.y;
	}
	
	public void putXY(int x, int y){
		coordinates.x = x;
		coordinates.y = y;
	}
	
	public boolean isValid(){
		return valid;
	}
	
	public void setValidity(boolean value){
		valid = value;
	}
	
	public String toString(){
		String toReturn = "Valid:"+valid+" X:"+coordinates.x+" Y:"+coordinates.y+" index:"+pointerIndex;
		return toReturn;
	}
	
	
}
