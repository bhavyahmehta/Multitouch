/**************************************************************************
 * This is a class that creates a View on which a hashtable containing 
 * TouchPoint data can be visually depicted. Basically, the touch points
 * can be drawn on the screen using colored markers
 **************************************************************************/

package com.asim.learning;

import java.util.Enumeration;
import java.util.Hashtable;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.View;

public class Slate extends View{
	
	Hashtable<Integer,TouchPoint> ptrMap;		//the hashtable containing the touch points mapped to their pointer indices
	final int RADIUS_POINT = 15;				//radius of the colored marker
	
	/**************************************************************************
	 * Constructor - doesn't do much except nullify the pointer map. In order
	 * to draw the pointer map, call the update() method
	 **************************************************************************/
	public Slate(Context context) {
		super(context);
		this.setTag("drawingSurface");
		ptrMap = null;
	}
	
	/**************************************************************************
	 * draws the updated pointer map. The markers used to indicate the touch
	 * point are colored dots
	 * (non-Javadoc)
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 **************************************************************************/
	protected void onDraw(Canvas c){
		
		//ensure that this map contains some valid point information
		if(ptrMap != null && !ptrMap.isEmpty()){
			
			Paint painter = new Paint();									//initialize the painter object
			painter.setColor(Color.RED);									//set the color of choice
			
			Enumeration<TouchPoint> touchPoints = ptrMap.elements();		//grab the elements stored in the map
			while(touchPoints.hasMoreElements()){							//do the following till there are no more elements to be read 
				
				TouchPoint current = touchPoints.nextElement();				//grab the next element
				if(current.isDown){											//if this pointer is pressed down onto the screen
					
					//draw a bright outline
					painter.setStyle(Style.STROKE);
					c.drawCircle(current.getX(), current.getY(), RADIUS_POINT, painter); //draw the marker at (x,y)
					
					//draw a translucent inner filling
					painter.setStyle(Style.FILL);
					painter.setAlpha(80);
					c.drawCircle(current.getX(), current.getY(), RADIUS_POINT, painter); //draw the marker at (x,y)
				}	
			}
		}
	}

	/**********************************************************************
	 * this will redraw the touch points based on the map that is passed
	 * as the argument
	 **********************************************************************/
	public void update(Hashtable<Integer,TouchPoint> map){
		ptrMap = map;														//save the new pointer map
		invalidate();														//draw this new pointer map
	}
}






