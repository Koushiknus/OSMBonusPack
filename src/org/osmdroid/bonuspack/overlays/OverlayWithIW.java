package org.osmdroid.bonuspack.overlays;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.views.overlay.Overlay;
import android.content.Context;

/**
 * Overlay able to open an InfoWindow (a bubble). 
 * Handling tap event and showing the InfoWindow at a relevant position is let to sub-classes. 
 * 
 * @see BasicInfoWindow
 * 
 * @author M.Kergall
 */
public abstract class OverlayWithIW extends Overlay {
 
	//InfoWindow handling
	protected String mTitle, mSnippet;
	protected InfoWindow mInfoWindow;
	
	public OverlayWithIW(final Context ctx) {
		this(new DefaultResourceProxyImpl(ctx));
	}

	public OverlayWithIW(final ResourceProxy resourceProxy) {
		super(resourceProxy);
		/* already done by default:
		mTitle = null; 
		mSnippet = null;
		mInfoWindow = null;
		*/
	}

	public void setTitle(String title){
		mTitle = title;
	}
	
	public String getTitle(){
		return mTitle;
	}
	
	public void setSnippet(String snippet){
		mSnippet= snippet;
	}
	
	public String getSnippet(){
		return mSnippet;
	}

	/** By default, TapOverlay has no InfoWindow. 
	 * Usage: setInfoWindow(new BasicInfoWindow(layoutResId, mapView));
	 * @param infoWindow the InfoWindow to be opened when tapping the overlay. 
	 * This InfoWindow MUST be able to handle a TapOverlay (as BasicInfoWindow does). 
	 * Set it to null to remove an existing InfoWindow. 
	 */
	public void setInfoWindow(InfoWindow infoWindow){
		mInfoWindow = infoWindow;
	}

	public InfoWindow getInfoWindow(){
		return mInfoWindow;
	}
	
	public void hideInfoWindow(){
		if (mInfoWindow != null)
			mInfoWindow.close();
	}

	public boolean isInfoWindowShown(){
		return (mInfoWindow != null) && mInfoWindow.isOpen();
	}
	
}
