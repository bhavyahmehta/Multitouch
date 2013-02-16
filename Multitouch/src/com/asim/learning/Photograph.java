package com.asim.learning;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.view.View;

public class Photograph extends View{
	
	//CLASS CONSTANTS
	final static int MAX_BORDER = 5;										//The maximum thickness of the border to be put around the Photo
	final static float MIN_SCALE_VAL = 0.5f;								//The minimum value of scaling factor - scaling down below this value is not allowed
	final static int HIGHLIGHT_WIDTH = 10;
	final static int MARKER_SIZE = 15;
	
	//CLASS DATA MEMBERS
	private int border;												//This will hold the border thickness  
	private int left, top, right, bottom;							//these coordinates denote the boundary of the basic, unscaled, un-rotated image
	private int wBmp, hBmp;											//these denote the width and height of the image
	
	private float angleBmp;											//this denotes the angle of rotation for the canvas
	private float sx, sy;											//these denote the scaling factors of the canvas along x and y
	private float tx, ty;											//these denote the translation along x and y directions
	
	private float aspectRatio;										//the aspect ratio of the image is preserved in this member
	private Bitmap source;											//the original bitmap content of the image is saved here
	private Bitmap scrubbedSource;									//a scrubbed (low quality) version of the source image is stored in here
	private String label;
	public boolean isHighQuality;									//this flag indicates whether the image will be drawn in high quality or low quality
	public boolean isHighlighted;									//this flag is used to determine whether a highlighted aura should be put around the drawn area
	public boolean isMarked;
	public boolean isPlayable;
	public boolean isLocked;
	
	private Paint painter;											//the painter object - handles color selection etc
	private PointF topLeft, topRight, botLeft, botRight;			//these points define the "REGION OF INTEREST"
	private TouchPoint[] ptsOfContact = new TouchPoint[3];			//these indicate the touch points that are currently on this view
	private Rect rectInvalidate;									//this indicates the rectangular area of the view that will be redrawn		
	public Object extra;											//to store something extra - just in case
	
	/***************************************************************************
	 * Class constructor
	 * @param context - the context of the application
	 * @param content - a bitmap containing the image to be loaded
	 * @param angle - the angle of inclination for this image
	 * @param initWidth - the initial width for the image
	 * @param borderThickness - thickness of the "white border" to be drawn around
	 * the picture. This value will be truncated to a max of five pixels
	 ***************************************************************************/
	public Photograph(Context context,Bitmap content,Bitmap lowQuality,float angle, int initWidth, int borderThickness, boolean isMovie) {
		
		super(context);															//initialize the base class constructor	
		source = content;														//content from the bitmap is saved
		scrubbedSource = lowQuality;
		
		angleBmp = angle;														//the angle of inclination
		
		if(source == null || scrubbedSource == null){ 
			aspectRatio = 1.0f;
			scrubbedSource = null;
			source = null;
		}
		else aspectRatio = ((float)source.getWidth()/source.getHeight());			//calc and save the aspect ratio of the original image
		
		wBmp = initWidth;														//the initial width is set as the current width
		hBmp = (int)(wBmp/aspectRatio);											//accordingly the initial height is calculcated
		border = (borderThickness > MAX_BORDER)?MAX_BORDER:borderThickness;		//truncate border thickness to a max of five pixels
		
		sx = 1; sy = 1;															//no scaling as of now (scaling is a multiplication operation)		
		tx = 0; tx = 0;															//no translation as of now either
		
		initCoordinates();														//initialize the coordinates
		ptsOfContact[0] = null; ptsOfContact[1] = null;							//nullify the points of contact - no points touching this at the moment
		rectInvalidate = new Rect();											//create a new area to invalidate - this will be computed during the draw	
		
		label = "";
		isHighQuality = false;													//will initially not be rendered in good quality
		isHighlighted = false;
		isMarked = false;
		isPlayable = isMovie;
		isLocked = false;
		
		MultiTouchActivity.logHeap();
	}
	
	/*******************************************************************************
	 * initialize the values of the left, top, right and bottom of the image
	 *******************************************************************************/
	private void initCoordinates(){
		
		left = (int) ((this.getWidth()/2) + tx);																			
		top =  (int) (this.getHeight()/2 + ty);														
		right = left + wBmp;						
		bottom= top + hBmp;								
	}
	
	/*******************************************************************************
	 * Returns the coordinates of the center of the picture
	 * @return coords of center of image
	 *******************************************************************************/
	public PointF getCenterImage(){
		
		int xc = left + wBmp/2;
		int yc = top + hBmp/2;
		return new PointF(xc,yc);		
	}
	
	/*******************************************************************************
	 * debug drawing routine... this may be used to draw the region of interest (colored
	 * dots) and the area to invalidate.
	 * @param c - the canvas object which is used to create the drawing
	 *******************************************************************************/
	private void debugDraw(Canvas c){ 
		
		/*
		 * Area to invalidate - this is the rectangular area that will be redrawn every time the
		 * view gets dirty (basically when something changes) 
		 */
		
		painter.setColor(Color.LTGRAY);
		painter.setAlpha(150);
		c.drawRect(rectInvalidate, painter);
				
		/*
		 * Region of interest - this is the set of corner points that will help us track the 
		 * drawing after rotation and scaling transforms are applied to it
		 */
		painter.setColor(Color.RED);																			
		c.drawCircle(topLeft.x, topLeft.y,15, painter);
		painter.setColor(Color.BLUE);
		c.drawCircle(topRight.x, topRight.y,15, painter);
		painter.setColor(Color.GREEN);
		c.drawCircle(botRight.x, botRight.y,15, painter);
		painter.setColor(Color.YELLOW);
		c.drawCircle(botLeft.x, botLeft.y,15, painter);
		//end debug
		
	}
	
	/***************************************************************************
	 * Draw routine for this view. For every view, the drawn area is the part
	 * which actually shows the image. The view may be much larger (in order to
	 * accomodate the rotation and scaling of this image), so a majority of the area
	 * in the view is empty. 
	 * 
	 * In this scenario a touch on any part of the view (empty/drawn) will generate
	 * an event. But we need to know if the touched point is within the draw area
	 * only. In order to segregate the drawn area from the empty area, we will define
	 * a "region of interest". This region is nothing but a set of four points (basically
	 * the corners of the drawn image). 
	 * 
	 * As the image is scaled and rotated, these points must also be transformed accordingly
	 * the logic for the same is implemented in this routine
	 ***************************************************************************/
	protected void onDraw(Canvas c){
		
		Stopwatch timerDraw = new Stopwatch();
		timerDraw.start();
				
		painter = new Paint();										//initialize the painter	
		initCoordinates();											//re-initialize your coordinates (this is important as it handles translation)
		
		int pivotX = (left + wBmp/2);								//calculate the center of this rectangle (which will be drawn upon)								
		int pivotY = (top + hBmp/2);								//this center will act as the pivot for all rotation and scaling								
		
		transformROI(pivotX,pivotY);								//transform the Region of Interest about this pivot
		setAreaToInvalidate();										//calculate our area to invalidate using this ROI
		
		//debugDraw(c);												//draw the ROI and invalid area
				
		c.save();													//save the current canvas 
		
		/*
		 * There are many parts to drawing this view. They are:
		 * - the drawing must have a faded aura around it. this is done to highlight the drawing in case a pointer touches it
		 * - then there is the white border around the image (for the classic photograph look)
		 * - after that we have to draw the bitmap of the image
		 * - if the image needs to be checked/marked, we need to draw the green check box on it
		 */
		
		//Step 1 - TRANSFORMATIONS APPLIED ONTO THE CANVAS
		c.scale(sx, sy,pivotX,pivotY);								//scale by the specified amount about the center 
		c.rotate(angleBmp,pivotX,pivotY);							//rotate by specified value about center
						
		//Step 2 -DRAW THE AURA / HIGHLIGHT FOR THIS IMAGE IF NECESSARY (if the image is currently being touched by at least one pointer, draw this aura)
		int ptrCount = getCountPointsOfContact();
		if(ptrCount > 0){
			//the aura is achieved by drawing a translucent rounded rectangle of a size larger than the image rectangle 
			painter.setColor(Color.YELLOW);
			painter.setAlpha(50);
			RectF highlightRect = new RectF(left-border-HIGHLIGHT_WIDTH, top-border-HIGHLIGHT_WIDTH, right+border+HIGHLIGHT_WIDTH, bottom+border+HIGHLIGHT_WIDTH);
			c.drawRoundRect(highlightRect, 15, 15, painter);
		}
				
		//Step 3 - DRAW THE WHITE PICTURE BORDER
		painter.setColor(Color.WHITE);
		Rect whiteBorder = new Rect(left - border, top - border, right + border, bottom + border);
		c.drawRect(whiteBorder, painter);
		painter.setColor(Color.LTGRAY);
		painter.setStrokeWidth(10);
		int offset = wBmp / 5;
		c.drawLine(whiteBorder.left+offset, whiteBorder.top+offset, whiteBorder.right-offset, whiteBorder.bottom-offset, painter);
		c.drawLine(whiteBorder.right-offset, whiteBorder.top+offset, whiteBorder.left+offset, whiteBorder.bottom-offset, painter);
		
		//Step 4 - DRAW THE ACTUAL BITMAP FOR THIS PICTURE
		
		/* Note on drawing the bitmap. When we created this view, we stored two Bitmap objects - one of high quality and the other was 
		 * as scrubbed version. Drawing the high quality bitmap all the time is not feasible. It takes too much time (simply because all
		 * the other views around this view may also need drawing). Using the "isHighQuality" flag, you can control the rendering of this
		 * bitmap. If you need a faster drawing, you can set the flag to false. If speed is not of the essence, and you're more worried about
		 * quality, then set it to true*/
		
		Rect paintedArea = new Rect(left,top,right,bottom);						//define the area (rectangle) to be drawn upon using coords calculated earlier
		painter.setAlpha(255);													//this must be drawn at maximum opacity
		
		if(source!=null && isHighQuality)
			c.drawBitmap(source, null, paintedArea, painter);					//if higher quality is needed, draw this
		else if(scrubbedSource!=null && !isHighQuality) 
			c.drawBitmap(scrubbedSource, null, paintedArea, painter);			//else draw this
		
		
		//step 7 - Draw the MEDIA PLAYER icon if neccessary
		if(isPlayable){
			
			painter.setColor(Color.WHITE);
			painter.setAlpha(200);
			
			painter.setStyle(Style.STROKE);
			painter.setStrokeWidth(5);
			c.drawCircle(pivotX, pivotY, wBmp/6, painter);
			
			painter.setStyle(Style.FILL);
			Path triangle = new Path();
			triangle.moveTo(pivotX - wBmp/24,pivotY - wBmp/12);
			triangle.lineTo(pivotX - wBmp/24,pivotY + wBmp/12);
			triangle.lineTo(pivotX + wBmp/12,pivotY);
			c.drawPath(triangle, painter);
		}
		
		//Step 6 - DRAW THE CHECKBOX IF NECESSARY (check the isMarked flag)
		if(isMarked){
			
			Rect selectableArea = new Rect(left - border, top - border, right + border, bottom + border);
			painter.setColor(Color.argb(50, 0, 50, 0));
			c.drawRect(selectableArea, painter);
			
			//The checkbox contains a green rounded rectangle with a white tick mark on it. The green box is outlined by a lighter green color
			//Here are the rectangles for the outline and the filled are for the checkbox
			RectF markerRectOutline = new RectF(left-border*2,top-border*2,left+MARKER_SIZE,top+MARKER_SIZE);
			RectF markerRectFill = new RectF(left-border*2 + 1,top-border*2+1,left+MARKER_SIZE-1,top+MARKER_SIZE-1);
			
			painter.setColor(Color.argb(255,0,178,0));							//choose a lighter and brighter green for the outline 
			c.drawRoundRect(markerRectOutline,3,3,painter);						//draw the lighter green outline using rounded rectangle
			
			painter.setColor(Color.argb(250,0,128,0));							//choose a darker and less brighter green for the fill for the checkbox
			c.drawRoundRect(markerRectFill,3,3,painter);						//draw this filled area
			
			painter.setColor(Color.WHITE);										//set the color to white for the tick mark
			painter.setStrokeWidth(3);											//slightly thicker than normal
			
			//the tick mark has two line segments - a short part and a long part. To make it look nicer, i've added a little shadow to the longer segment
			c.drawLine(markerRectOutline.left+5, markerRectOutline.bottom-10, markerRectOutline.left+10, markerRectOutline.bottom-5, painter);	//draw short segment
			c.drawLine(markerRectOutline.left+8, markerRectOutline.bottom-5, markerRectOutline.right-5, markerRectOutline.top+5, painter);		//draw longer segment
			
		}
		
		//Step 7 - Draw the LOCK if necessary
		if(isLocked){
			
			RectF rectLockBase = new RectF(right-border,top,right+MARKER_SIZE,top+MARKER_SIZE);
			
			//draw the U shaped arm of the lock
			painter.setColor(Color.GRAY);
			painter.setStyle(Style.STROKE);
			c.drawCircle(rectLockBase.left + rectLockBase.width()/2, rectLockBase.top, rectLockBase.width()/3, painter);
			
			//draw the base of the lock
			painter.setColor(Color.argb(250,249,200,30));
			painter.setStyle(Style.FILL);
			c.drawRoundRect(rectLockBase,3, 3, painter);
			
			int lockCenterX = (int) (rectLockBase.left + rectLockBase.width()/2);
			int lockCenterY = (int) (rectLockBase.top + rectLockBase.height()/2);
			
			//draw the keyhole
			painter.setColor(Color.BLACK);
			painter.setStrokeWidth(1);
			c.drawCircle(lockCenterX,lockCenterY, 2, painter);
			c.drawLine(lockCenterX,lockCenterY,lockCenterX,lockCenterY+border*2,painter);
		}
						
		c.restore();															//restore the canvas to original state
						
		long ticks = timerDraw.stop();
		//Log.i("-----------------DRAWING PIC" + this.getTag(), " took " + ticks + "millisec");
		
	}
	
	/*******************************************************************************
	 * This routine is used to transform (scale and rotate) the REGION OF INTEREST
	 * points by the specified rotation and scaling factors. This is needed as the 
	 * region of interest is merely a set of points, which will not move along with 
	 * the canvas. Therefore its binding upon us to make sure it always tracks the 
	 * drawn the area even after the canvas is rotated or scaled 
	 * 
	 * @param pivotX - the x coordinate of the point about which the scaling/rotation is done
	 * @param pivotY - the y coordinate of the point about which the scaling/rotation is done
	 *******************************************************************************/
	private void transformROI(int pivotX, int pivotY){
		
		//This is where the coords of the "region of interest" are recalculated after rotation and scaling has been applied
		
		topLeft  = getRotatedPoint(left,top,pivotX,pivotY,angleBmp);			//get top left corner after rotation
		topRight = getRotatedPoint(right,top,pivotX,pivotY,angleBmp);			//get top right corner after rotation
		botLeft  = getRotatedPoint(left,bottom,pivotX,pivotY,angleBmp);			//get bot left corner after rotation
		botRight = getRotatedPoint(right,bottom,pivotX,pivotY,angleBmp);		//get bot right corner after rotation	
		
		topLeft = getScaledPoint(topLeft.x,topLeft.y,pivotX,pivotY,sx,sy);		//get top left after scaling
		topRight = getScaledPoint(topRight.x,topRight.y,pivotX,pivotY,sx,sy);	//get top right after scaling
		botLeft = getScaledPoint(botLeft.x,botLeft.y,pivotX,pivotY,sx,sy);		//get the bot left after scaling
		botRight = getScaledPoint(botRight.x,botRight.y,pivotX,pivotY,sx,sy);	//get the bot right after scaling
		
		/*//debug
		Log.i("REGION OF INTEREST"," Top Left :" + ""+topLeft.x+","+topLeft.y);
		Log.i("REGION OF INTEREST"," Bot Left :" + ""+botLeft.x+","+botLeft.y);
		Log.i("REGION OF INTEREST"," Top Right :" + ""+topRight.x+","+topRight.y);
		Log.i("REGION OF INTEREST"," bot Right :" + ""+botRight.x+","+botRight.y);
		*/
	}
	
	/*******************************************************************************
	 * This method is used to dynamically compute the area to redraw. The reason we need
	 * to constantly do this is because redrawing the view is quite expensive, specially
	 * if the number of view in the layout are very high. So in order to save a little
	 * time in the drawing routine, we only draw the part of this view convered by this
	 * area.
	 *******************************************************************************/
	private void setAreaToInvalidate(){
		
		/* The logic used here is simple. Our drawn area can be at any angle. Hence we must find 
		 * a square of size large enough to accomodate it - basically a square big enough to contain
		 * the drawing even if it is rotated a full 360 degrees.
		 * 
		 * So we calculate the diagonal of the drawn area (which is a rectangle). A circle of diameter
		 * equal to this diagonal will be large enough, but in order to use the invalidate method, we need
		 * a rect, not a circle. So we take a square of size = diameter (like a square drawn around this 
		 * hypothetical circle.
		 */
		
		int A = (int) Math.pow((botLeft.x - topLeft.x),2);
		int B = (int) Math.pow((botLeft.y - topLeft.y),2);
		int diag = (int)Math.sqrt(A + B);
		
		int centerX = left + wBmp/2;
		int centerY = top + hBmp/2;
		
		rectInvalidate.left = centerX - diag;
		rectInvalidate.top = centerY - diag;
		rectInvalidate.bottom = centerY + diag;
		rectInvalidate.right = centerX + diag;
		
		/*//debug
		Log.i("INVALIDATE AREA LEFT",""+rectInvalidate.left);
		Log.i("INVALIDATE AREA TOP",""+rectInvalidate.top);
		Log.i("INVALIDATE AREA RIGHT",""+rectInvalidate.right);
		Log.i("INVALIDATE AREA BOTTOM",""+rectInvalidate.bottom);
		 */
	}
	
	/*******************************************************************************
	 * routine to get the coords of a point after the specified translation is applied
	 * to that point 
	 * @param x - x coord of the point
	 * @param y - y coord of the point
	 * @param tx2 - translation to be applied along x axis
	 * @param ty2 - translation to be applied along y axis
	 * @return the PointF object containing the revised coordinates of the point
	 *******************************************************************************/
	private PointF getTranslatedPoint(float x, float y,float tx2, float ty2){
		PointF toReturn = new PointF(x+tx2,y+ty2);
		return toReturn;
	}
	
	/***************************************************************************
	 * This returns the coordinates of a point after a scaling of sx and sy would
	 * be performed on it
	 * 
	 * @param x	- current X coordinate of the point
	 * @param y	- current Y coordinate of the point
	 * @param xc- X coord of the center of scaling (the point about which scaling will happen)
	 * @param yc- Y coord of the center of scaling (the point about which scaling will happen)
	 * @param sx- scaling factor along x direction 
	 * @param sy- scaling factor along y direction
	 * @return A PointF object that contains the coords of the point (x,y) if a scaling
	 * of sx and sy are applied.
	 ***************************************************************************/
	private PointF getScaledPoint(float x, float y, int xc, int yc, float sx, float sy){
		
		/* Logic for this formula is simple. If we transfer the coordinate system from (0,0) to (xc, yc)
		* any point (x,y) will now be represented as (x-xc,y-yc). Therefore scaling by sx and sy would result
		* in the scaled point (sx*(x-xc), sy*(y-yc)) -> lets call this (H,K)
		* 
		* now we transfer the point back to the origin (by adding xc and xy respectively) so we get
		* (H + xc),(K + yc) --> solving these you will get (x*sx - xc*(sx-1)),(y*sy - yc*(sy-1))
		*/
		PointF scaledPoint = new PointF();
		scaledPoint.x = x*sx - xc*(sx-1);
		scaledPoint.y = y*sy - yc*(sy-1);		
		return scaledPoint;
	}
	
	/**************************************************************************
	 * This returns the coordinates of a point after a specified rotation may be 
	 * applied to it.
	 * 
	 * @param x - current x coordinate of the point
	 * @param y - current y coordinate of the point
	 * @param xc- x coordinate of the pivot 
	 * @param yc- y coordinate of the pivot
	 * @param degrees - angle of rotation in degrees
	 * @return a PointF object that contains the point coordinates after rotation
	 **************************************************************************/
	private PointF getRotatedPoint(int x, int y, int xc, int yc, float degrees){
		
		PointF rotatedPointCoords = new PointF();
		
		if(degrees != 0){
			
			float radians = (float) (degrees * Math.PI / 180);
			float kc = (float) Math.cos(radians);
			float ks = (float) Math.sin(radians);
		
			int xNew = (int) (xc + (kc*(x-xc) - ks*(y-yc)));
			int yNew = (int) (yc + (ks*(x-xc) + kc*(y-yc)));
			
			rotatedPointCoords.x = xNew;
			rotatedPointCoords.y = yNew;
		}
		else {
			
			rotatedPointCoords.x = x;
			rotatedPointCoords.y = y;
		}
		
		return rotatedPointCoords;
	}
	
	/*******************************************************************************
	 * This routine returns a value that represents the orientation of a point along
	 * a line. The orientation can be among the following values:
	 *  - the point lies on the line (return value = 0)
	 *  - the point lies on the right of line (return value > 0)
	 *  - the point lies on the left of line(return value < 0)
	 *  
	 * @param lOrigin - the starting vertex of the line
	 * @param lEnd	- ending vertex of the line
	 * @param p - coordinate of the point whose orientation is to be found
	 * @return - the orientation (0,positive,negative) -> (on line,left,right)
	 *******************************************************************************/
	private int getPointOrientationAboutLine(PointF lOrigin, PointF lEnd, PointF p){
		
		int result = -1;
		
		int x0 = (int) lOrigin.x;
		int y0 = (int) lOrigin.y;
		int x1 = (int) lEnd.x;
		int y1 = (int) lEnd.y;
		int x = (int) p.x; 
		int y = (int) p.y;
		
		/*this is the formula used to determine orientation of a point (x,y) about a line
		 * formed by points (x1,y1) and (x0,y0) 
		 */
		
		result = ((y-y0)*(x1-x0)) - ((x-x0)*(y1-y0));	
		return result;
	}
	
	/*******************************************************************************
	 * returns true if the point (x,y) lies in the "Region of Interest" or the drawn
	 * area of the view
	 *******************************************************************************/
	public boolean isPointInROI(int x, int y){
		
		boolean result = false;
		PointF pt = new PointF(x,y);
				
		/*
		 * The algorithm to determine if a point (x,y) lies inside the region of interest
		 * or a quad formed by the vertices A,B,C and D uses a simple technique.
		 * we'll create a line from A to B, B to C, C to D and D to A (direction is important).
		 * If the point x,y lies on the left side of these lines (w.r.t origin and the direction
		 * of the line), then the point is inside this quadrilateral.
		 */
		
		int orient1 = getPointOrientationAboutLine(topLeft,topRight,pt);
		int orient2 = getPointOrientationAboutLine(topRight,botRight,pt);
		int orient3 = getPointOrientationAboutLine(botRight,botLeft,pt);
		int orient4 = getPointOrientationAboutLine(botLeft,topLeft,pt);
		
		if(orient1 >= 0 && orient2 >= 0 && orient3 >= 0 && orient4 >= 0 )
			result = true;
		
		//Log.i("//////////ANSWER",""+result);
		return result;
	}
	
	public void setLabel(String message){
		label = message;
		invalidate(rectInvalidate);
	}
	
	/**************************************************************************
	 * Set the angle of inclination of the drawing
	 * @param degrees - the angle in degrees by which the drawing needs to be rotated
	 **************************************************************************/
	public void setAngle(float degrees){
		angleBmp = degrees;
		isHighQuality = true;
		invalidate(rectInvalidate);
	}
	/**************************************************************************
	 * Get the current angle value
	 * @return - the current Angle value
	 **************************************************************************/
	public float getAngle(){
		return angleBmp;
	}
	
	/**************************************************************************
	 * Set the scale of the image. The final value for scale is determined by
	 * multiplying by scalefactor and granularity. This is perfect when you have 
	 * a change in an integral quantity that needs to be used to affect the scale
	 * of the image. In that case the finer control can be obtained over the scaling
	 * operation using granularity 
	 * 
	 * @param scaleFactor - an integral value (for coarse adjustment)
	 * @param granularity - a float value (for fine adjustment)
	 **************************************************************************/
	
	public void setScale(int scaleFactor, float granularity){
		if(isLocked) return;
		float sxNew = sx + scaleFactor * granularity;
		float syNew = sy + scaleFactor * granularity;
		
		if(sxNew > MIN_SCALE_VAL) sx = sxNew;
		if(syNew > MIN_SCALE_VAL) sy = syNew;
				
		isHighQuality = true;
		invalidate();
	}
	
	
	public void setMarker(boolean isChecked){
		isMarked = isChecked;
		invalidate(rectInvalidate);
	}
	
	/*******************************************************************************
	 * translates the drawing by specified pixels along x and y directions
	 * @param x - amount to translate along x
	 * @param y - amount to translate along y
	 *******************************************************************************/
	public void translate(int x,int y){
		if(isLocked) return;
		tx += x;
		ty += y;	
		isHighQuality = true;
		invalidate();
	}
	
	/******************************************************************************
	 * redraws or invalidates the part of the view that needs to be redrawn
	 *******************************************************************************/
	public void refresh(){
		invalidate(rectInvalidate);
	}
	
	/*******************************************************************************
	 * Add a touch point to the points of contact. This routine is to let the view know
	 * who is in contact with it
	 * 
	 * @param point - the TouchPoint object bearing the information about this touch 
	 * point
	 *******************************************************************************/
	public void addTouchPoint(TouchPoint point){
				
		for(int i=0; i < ptsOfContact.length; i++){
			if(ptsOfContact[i] == null){
				ptsOfContact[i] = point;
				break;
			}
		}
	}
	
	/*******************************************************************************
	 * removes the touch point with the specified id, if it exists in the points of
	 * contact
	 * @param pId - ID of the point to be removed
	 *******************************************************************************/
	public void removeTouchPoint(int pId){
		
		for(int i=0;i<ptsOfContact.length;i++){
			if(ptsOfContact[i] != null){
				if(ptsOfContact[i].pointerId == pId){
					ptsOfContact[i] = null;
					break;
				}
			}
		}
	}
	/******************************************************************************* 		
	 * @return  - returns the number of points currently in contact with this view 
	 *******************************************************************************/
	public int getCountPointsOfContact(){
		int count = 0;
		for(int i=0; i< ptsOfContact.length; i++){
			if(ptsOfContact[i] != null) count++;
		}
		
		return count;
	}
	
	/*******************************************************************************
	 * @return - the array of TouchPoint objects containing the points of contact
	 *******************************************************************************/
	public TouchPoint[] getPointsOfContact(){
		return ptsOfContact;
	}
}
