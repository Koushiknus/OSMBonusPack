package org.osmdroid.bonuspack;

import java.util.ArrayList;
import android.util.Log;

/** Road Leg is the portion of the road between 2 waypoints (intermediate points requested) 
 * 
 * @author M.Kergall
 * 
 */
public class RoadLeg {
	public double mLength; /** in km */
	public double mDuration; /** in sec */
	public int mStartLinkIndex; /** starting link of the leg, as index in links array */
	public int mEndLinkIndex; /** and ending link */
	
	public RoadLeg(){
		mLength = mDuration = 0.0;
		mStartLinkIndex = mEndLinkIndex = 0;
	}
	
	public RoadLeg(int startLinkIndex, int endLinkIndex, 
			ArrayList<RoadLink> roadLinks){
		mStartLinkIndex = startLinkIndex;
		mEndLinkIndex = endLinkIndex;
		mLength = mDuration = 0.0;
		for (int i=mStartLinkIndex; i<=mEndLinkIndex; i++){
			RoadLink link = roadLinks.get(i);
			mLength += link.mLength;
			mDuration += link.mDuration;
			//TODO: also integrate nodes traversal duration...
		}
		Log.d(BonusPackHelper.LOG_TAG, "Segment: " + mStartLinkIndex + "-" + mEndLinkIndex
				+ ", length=" + mLength + "km, duration="+mDuration+"s");
	}
}
