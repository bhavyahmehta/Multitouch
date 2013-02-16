/*******************************************************************************
 * This activity demonstrates how free transformable workspace for manipulating
 * images using simultaneous action from multiple points of contact can be achieved.
 * This is usually not possible as the multi-touch structure in android is not
 * really bound to the view and is quite painful to work with.
 * 
 * In order to successfully track all the points touching the screen, we maintain
 * a hashtable or a map that stores the current coordinates of each pointer against
 * its respective pointer ID. This map also keeps track of which view was touched last
 * by this pointer.
 * 
 * In case of an event action (DOWN or UP), the ptrID of the touch point can be easily
 * and accurately resolved. However this cannot happen in the case of a "MOVE" action.
 * In that case we check how many points are touching the screen and check the positions
 * of each one. These positions are then compared with the prior position information
 * stored in the map. A comparison of the current position and prior position can give
 * us data about the pointer's movement. In this manner we can calculate the displacement
 * of each touch point along x and y axes.
 * 
 * Since we store the last touched view in the hashtable against each pointer id, we can know
 * that all successive move events (until the pointer goes up) were for this view only. In this
 * manner we can accurately determine the touching of a view and the dragging of the pointer
 * from one point on the screen to the other.
 * 
 * In the case of our application, where a view reacts differently if a single point is touching
 * it and when there is more than one point touching it, we need to know (easily and quickly), the 
 * number of points of contact for every view.
 * 
 * In most other multitouch frameworks touch events can be view specific and can accurately 
 * give information about which view they were generated upon. This is not true for android. Here, all
 * secondary points will always point to the same view as pointed by the primary point. This creates a lot
 * of problems as we cannot easily track how many points are touching a particular view at a particular time
 * 
 * in order to overcome this, we do this - when a pointer goes down on a particular view, we store information
 * about this pointer into the view. This info is then pulled out during a move event to determine how many
 * points are actually touching the view.
 *******************************************************************************/

package com.asim.learning;

import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Random;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MultiTouchActivity extends Activity implements OnTouchListener, OnClickListener, AnimationListener{

	final int PICTURE_ADD_DURATION = 500;							//the duration of the alphaAnimation - when adding a view
	final private int SCRUB_FACTOR = 2;							//scrub factor - bitmaps will be scrubbed down by a factor of this value

	LinearLayout outer;												//this is the outer container, will hold the add button and parent container
	Button btnAdd;													//button to add images

	FrameLayout parent; 											//parent container will hold all the images
	Slate drawingSurface;											//this is a surface on which the touch point markers will be drawn
	FrameLayout grandParent;										//this container will hold the parent container and the drawing surface

	/* this hashtable will store the touch point data against pointer IDs. This map will help us track multiple pointers */
	Hashtable<Integer,TouchPoint> ptrMap = new Hashtable<Integer,TouchPoint>();

	int tagCount = 0;												//a variable used to assign sequential tags to the views
	boolean isInteractive = true;									//this will indicate if the UI is interactive or not

	private static boolean disableFlag = false;


	/******************************************************************************
	 * Called when the activity starts - initialise the views
	 ******************************************************************************/
	public void onCreate(Bundle instance){
		super.onCreate(instance);

		outer = new LinearLayout(this);											//initialize the outer container
		outer.setOrientation(LinearLayout.VERTICAL);							//set the orientation of this linear layout

		btnAdd = new Button(this);												//this is the button to add images
		btnAdd.setText("ADD IMAGE");											//set the text label
		btnAdd.setOnClickListener(this);										//assign it a click event handler

		parent = new FrameLayout(this);											//this container will hold all the photographs
		parent.setTag(tagCount-1);												//assign it a tag of -1

		drawingSurface = new Slate(this);										//the drawing surface on which touch point markers will be drawn
		grandParent = new FrameLayout(this);									//this container will hold the parent and the surface
		grandParent.setOnTouchListener(this);									//all touch events pass through the grand parent
		grandParent.addView(parent);											//add the image holder first
		grandParent.addView(drawingSurface);									//cover that with the drawing surface 

		//outer.addView(btnAdd);													//add the button to the linear layout
		outer.addView(grandParent);												//add the big container (photos + surface) into this linear layout
		
		this.addDrawableToParent(R.drawable.img1);
		this.addDrawableToParent(R.drawable.img2);
		this.addDrawableToParent(R.drawable.img3);
		this.addDrawableToParent(R.drawable.img4);
		this.addDrawableToParent(R.drawable.img5);
		
		
		setContentView(outer);													//the linear layout will be the final view for this activity
	}

	/******************************************************************************
	 * This routine adds a randomly chosen image to the parent layout. The addition is
	 * done using a smooth alpha animation. 
	 * 
	 * @param id - id of the resource whose image will be added
	 ******************************************************************************/
	public void addDrawableToParent(int id){


		Random rnd = new Random();
		isInteractive = false;													//the UI will not be interactive - touch events will not be serviced

		Drawable drw = (getResources().getDrawable(id));						//get the drawable for this resource
		Bitmap bmp = (((BitmapDrawable)drw).getBitmap());							//convert that into a bitmap
		bmp = Bitmap.createScaledBitmap(bmp,(int)( bmp.getWidth() * 0.75f), (int)(bmp.getHeight() * 0.75f), true);

		BitmapFactory.Options opt = new BitmapFactory.Options();				//get a scrubbed version of this bitmap
		opt.inSampleSize = SCRUB_FACTOR;
		Bitmap lowBmp = BitmapFactory.decodeResource(getResources(), id, opt);	

		Photograph pic = new Photograph(this,bmp,lowBmp,rnd.nextInt(360),150 + rnd.nextInt(300),3,false); //create a new Photograph object
		pic.setTag(tagCount++);													//assign a tag to this photograph
		btnAdd.setText("ADD IMAGE ("+tagCount+")");								//display the number of images along with button text	

		AlphaAnimation anim = new AlphaAnimation(0,1);							//create a new alpha animation
		anim.setDuration(PICTURE_ADD_DURATION);									//set the desired duration for this animation
		anim.setAnimationListener(this);										//set the listener (onAnimEnd will make UI interactive again)

		parent.addView(pic);													//add this photo to the parent
		pic.startAnimation(anim);												//start this animation

	}

	/******************************************************************************
	 * OnTouch event handler
	 ******************************************************************************/
	public boolean onTouch(View v, MotionEvent event) {

		if(isInteractive){	//if UI is interactive (no animations going on)

			/* The following section of code allows us to resolve the ACTION codes for a touch event. Multi touch event actions
			 * come to us through a combination of hex values. The basic values are still 0,1,2 (down, up and move). Any other value, 
			 * that comes to us via secondary pointers can be resolved or broken down to 0,1,2 using the following code
			 * 
			 * Also note that the values of pointerIDs are accurately resolved only for action Down and UP. For move, there is no clear
			 * indication of pointer ID, and hence we have to scan each and every pointer that is in contact with the screen
			 */

			//variable declarations
			int action = event.getAction();
			int count = event.getPointerCount();
			int ptrIndex = 0, ptrId;
			int actionResolved;

			//resolve the action as a basic type (up, down or move) 
			actionResolved = action & MotionEvent.ACTION_MASK;
			if(actionResolved < 7 && actionResolved > 4)actionResolved = actionResolved - 5;

			//if multiple pointers exist, try to get the correct pointer ID for this action - this is accurate only for UP/DOWN actions
			//first get the ptrIndex and then use that to get the pointer ID
			if(count > 1) ptrIndex = (action & MotionEvent.ACTION_POINTER_ID_MASK) >>> MotionEvent.ACTION_POINTER_ID_SHIFT;
			ptrId = event.getPointerId(ptrIndex);

			/****************** action resolution and pointer id retrieval ends here *******************/

			switch(actionResolved){
			case MotionEvent.ACTION_DOWN:
				handleActionDown(ptrIndex,ptrId,event);
				break;

			case MotionEvent.ACTION_MOVE:
				handleActionMove(count,event);
				break;

			case MotionEvent.ACTION_UP:
				handleActionUp(ptrIndex,ptrId,event);
				break;
			}

			//the event handlers for each event action would have updated the pointer map
			//so lets mark these updated points on the drawing surface using colored dots
			drawingSurface.update(ptrMap);

		}
		//Don't ever fucking recycle this event ever!!!
		return true;
	}

	/******************************************************************************
	 * Action Down handler - this is called when any pointer goes down. Here we aim
	 * to resolve if the pointer was touching a Photograph or not. If it was touching
	 * a photograph, it is marked as "valid" and stored in the hashtable
	 * 
	 * The validity of a pointer in the hashtable tells us if it was touching a photo
	 * or was a random touch on the parent layout somewhere
	 * 
	 * @param ptrIndex - index of this pointer
	 * @param ptrId  - id of this pointer
	 * @param event - MotionEvent object
	 ******************************************************************************/
	private void handleActionDown(int ptrIndex, int ptrId, MotionEvent event){

		int x,y;
		x = (int)event.getX(ptrIndex);													//read the x coord of this pointer's current location
		y = (int)event.getY(ptrIndex);													//read the y coord of this pointer's current location

		View touchedView = getTouchedView(x,y);											//find out which view is being touched by this pointer
		TouchPoint tp = new TouchPoint(x,y,ptrId,ptrIndex,touchedView.getTag());		//create a touch point for this pointer
		tp.setValidity(false);															//mark this as invalid pointer	
		tp.isDown = true;

		if(touchedView instanceof Photograph){														//if the view touched was not the parent layout (then it must be photograph)	

			Photograph pic = ((Photograph)touchedView);									//grab this photo
			pic.addTouchPoint(new TouchPoint(x,y,ptrId,ptrIndex,null));					//add the touch point into this view
			pic.isHighQuality = true;													//make this render in high quality bmp

			int numPtsContact = pic.getCountPointsOfContact();
			if(numPtsContact == 3){
				pic.isMarked = !pic.isMarked;
				pic.isLocked = !pic.isLocked;
			}

			touchedView.bringToFront();													//bring this photo to the top of the parent layout
			parent.invalidate();														//redraw the parent to show this change
			tp.extra = touchedView.getTag();											//store the tag of this photo into the touch point obj 
			tp.setValidity(true);														//make this a valid pointer

		}

		ptrMap.put(ptrId, tp);															//put this pointer into our hashtable 
	}

	/******************************************************************************
	 * Action Up handler - this is called when any pointer goes up. Here we check 
	 * if this pointer has a valid entry in the hashtable. If it does, that means it 
	 * was last associated with a photograph and we must take some action on that photo
	 * 
	 * @param ptrIndex - index of the pointer
	 * @param ptrId - id of the pointer which generated this event
	 * @param event - MotionEvent object for this pointer
	 ******************************************************************************/
	private void handleActionUp(int ptrIndex, int ptrId, MotionEvent event){

		int x,y;																		//get the x and y coordinates of this ptr's current location
		x = (int)event.getX(ptrIndex);
		y = (int)event.getY(ptrIndex);

		TouchPoint tp = ptrMap.get(ptrId);												//get this pointer's prior entry from the hashtable

		if(tp.isValid()){																//if it was touching a photograph
			Photograph pic = (Photograph)(parent.findViewWithTag(tp.extra));			//grab that photo
			pic.removeTouchPoint(ptrId);												//remove this pointer as an active point of contact for this photo
			pic.isHighQuality = false;													//the pointer went up, so on next redraw, make this pic low quality
			//NO REDRAW MUST HAPPEN WHEN THE POINTER GOES UP
		}

		tp.setValidity(false);															//make this pointer invalid - since it has gone up
		tp.isDown = false;
		ptrMap.put(ptrId, tp);															//push it back into the map

	}

	/******************************************************************************
	 * Action Move handler - here the pointer Id info is not accurate. We know the number
	 * of pointers currently touching the screen, and so we scan each and every one
	 * and determine which views they're interacting with
	 * 
	 * @param ptrCount - total number of pointers touching the screen
	 * @param event	- MotionEvent object for this 
	 ******************************************************************************/
	private void handleActionMove(int ptrCount, MotionEvent event){

		int x,y;

		for(int index = 0; index < ptrCount; index++){	//do this for every pointer touching the screen

			int ptrId = event.getPointerId(index);		//grab this pointer's id
			x = (int)event.getX(index);					//get its x and y coordinates
			y = (int)event.getY(index);

			TouchPoint tp = ptrMap.get(ptrId);			//find the prior entry in the hashtable for this pointer

			if(tp!=null && tp.isValid()){							//if this pointer was touching a photograph earlier on

				int dx = x - tp.getX();					//compute its displacement from its prior location
				int dy = y - tp.getY();					//this difference in position is what we will use 

				if(dx == 0 && dy == 0){					//if this difference was zero, then this pointer clearly isn't the one moving (and causing this event to be raised)
					continue;							//forget everything else, continue with the loop
				}
				else if((Math.abs(dx) >= 3 || Math.abs(dy) >= 3)){									//this displacement is non zero, so this pointer has moved

					//find the photograph associated with this touch point and find out how many points are touching it at the moment
					Photograph pic = (Photograph)(parent.findViewWithTag(tp.extra));
					int numPtsContact = pic.getCountPointsOfContact();
					Log.i("----------NUM PTS:",""+numPtsContact+" dx:"+dx+" dy:"+dy);
					pic.refresh();

					try{
						//if the number of points touching it is 1 (then we simply translate this pic) else if its 2, we rotate/scale
						if(numPtsContact == 1){
							pic.translate(dx,dy);			
						}
						else if (numPtsContact == 2){

							//in order to rotate, we need to know the angle by which the two points on this pic have shifted
							//and in order to scale, we need to know how far apart they are now w.r.t their prior positions.
							//since this move event is raised by only one point, the other point is fixed w.r.t this moving point.
							//so lets find that fixed point. we know the ptrId of the moving point. And we know there are only two
							//points touching this image. So the one whose pointer id is not the same as that of the moving point, must be the
							//fixed pointer

							TouchPoint[] ptsOfContact = pic.getPointsOfContact();
							TouchPoint fixed = (ptsOfContact[0].pointerId != ptrId)?ptsOfContact[0]:ptsOfContact[1];

							//calculate the distance values - between fixed and old location of pointer & fixed and current location of pointer
							float oldDistBetweenPoints = getDistance(fixed.getX(),fixed.getY(),tp.getX(),tp.getY());
							float newDistBetweenPoints = getDistance(fixed.getX(),fixed.getY(),x,y);

							//calculate the difference in these distances
							float diff = newDistBetweenPoints - oldDistBetweenPoints;

							//similarly calculate the elevation of the old line and slope of the new line
							float angleOld =  getInclination(fixed.getX(),fixed.getY(),tp.getX(),tp.getY());
							float angleNew =  getInclination(fixed.getX(),fixed.getY(),x,y);

							//get the difference in these elevations. If the difference in elevation is greater than 100, ignore this change
							float theta = angleNew - angleOld;
							if(Math.abs(theta) > 100) theta = 0;

							//use these values to change the angle and scaling of this photograph
							pic.setAngle(pic.getAngle() + theta);
							pic.setScale((int)diff,0.005f);

						}
					}catch(Exception e){

					}


				}
			}


			//all is done, store the current position of this pointer into the hashtable (for next comparison)
			tp.putXY(x, y);
			ptrMap.put(ptrId, tp);
		}

	}

	/******************************************************************************
	 * Returns the view that contains the point X,Y
	 * @param x - x coord of point
	 * @param y - y coord of point
	 * @return - View object that contains this point
	 ******************************************************************************/
	private View getTouchedView(int x, int y){

		/* In order to assess if the point x,y lies inside a view, we utilize the Region of Interest concept */

		Photograph pic = null;								//temp variable to hold the child
		int numViews = parent.getChildCount();				//find out how many views are inside the parent

		for(int i= numViews-1; i>= 0; i--){					//scan the parent for every child

			if(parent.getChildAt(i) instanceof Photograph){
				pic = (Photograph)(parent.getChildAt(i));
				if(pic.isPointInROI(x,y) == true){				
					return pic;									//if yes, return this child
				}
			}
		}

		return parent;										//no child found which contains this point, so surely the parent was touched
	}

	/******************************************************************************
	 * Gets the distance between two points (x1,y1) and (x2,y2)
	 ******************************************************************************/
	private float getDistance(int x1,int y1, int x2, int y2){
		float dist = (float) Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
		return dist;
	}

	/******************************************************************************
	 * Gets the elevation of a line formed by two points (x1,y1) and (x2,y2) 
	 ******************************************************************************/
	private float getInclination(int x1, int y1, int x2, int y2){

		if((x2-x1) == 0) return 90.0f;

		float m = ((float)(y2-y1))/(x2-x1);
		float angle = (float) ((float) ((float)Math.atan(m)) * 180 / Math.PI);
		return angle;
	}

	/******************************************************************************
	 * Click event handler
	 ******************************************************************************/
	public void onClick(View v) {

		if(v == btnAdd){						//if the add button was pressed, randomly select an image from the listed resources

			if(disableFlag!=true)
			{
				Random rnd = new Random();
				
				/*R.drawable.bellrock, R.drawable.collosseum, R.drawable.galapagos,
				R.drawable.northern, R.drawable.petra, R.drawable.sydney*/

				int[] ids = {
						R.drawable.img1, R.drawable.img3, R.drawable.img5,
						R.drawable.img2, R.drawable.img4, R.drawable.img6
				};
				int choice = rnd.nextInt(ids.length); 
				addDrawableToParent(ids[choice]);	//add this resources image to the parent layout as a Photograph
			}else
			{
				Toast.makeText(MultiTouchActivity.this, "Cants add more image", Toast.LENGTH_SHORT).show();
			}
		}
	}

	/******************************************************************************
	 * Animation listener - when animation ends, the UI is declared interactive again
	 ******************************************************************************/
	public void onAnimationEnd(Animation arg0) {
		isInteractive = true;
	}

	public void onAnimationRepeat(Animation animation) {

	}

	public void onAnimationStart(Animation animation) {
		isInteractive = false;
	}


	/*
	 * Routine to check memory map for VM and Native Heap
	 *  
	 */

	public static void logHeap() {

		Double allocated = new Double(Debug.getNativeHeapAllocatedSize());// new
		// Double((1048576));
		Double available = new Double(Debug.getNativeHeapSize());// 1048576.0;
		Double free = new Double(Debug.getNativeHeapFreeSize());// 1048576.0;

		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);

		Log.i("gettings", "debug. =================================");
		Log.i("gettings", "debug.heap native: allocated "
				+ df.format(allocated) + "MB of " + df.format(available)
				+ "MB (" + df.format(free) + "MB free)");
		//Log.i("gettings", "debug.memory: allocated: " + df.format(new Double(Runtime.getRuntime().totalMemory()/1048576)) + "MB of " + df.format(new Double(Runtime.getRuntime().maxMemory()/1048576))+"MB (" + df.format(new Double(Runtime.getRuntime().freeMemory()/1048576)) +"MB free)");


		
		if((Runtime.getRuntime().maxMemory()/1024)-allocated/1024<=(6*1024)) {

			Log.i("gettings", "debug. for shut down=================================");

			disableFlag = true;



		}


	}


}
