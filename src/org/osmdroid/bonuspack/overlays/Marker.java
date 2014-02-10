package org.osmdroid.bonuspack.overlays;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.ResourceProxy.bitmap;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.SafeDrawOverlay;
import org.osmdroid.views.safecanvas.ISafeCanvas;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;

/**
 * An icon placed at a particular point on the map's surface. 
 * Mimics the Marker class from Google Maps Android API v2 as much as possible. Main differences:<br/>
 * - Doesn't support Z-Index: as other osmdroid overlays, Marker is drawn in the order of appearance. <br/>
 * - The icon can be any standard Android Drawable, instead of the BitmapDescriptor introduced in Google Maps API v2. <br/>
 * - The icon can be changed at any time. <br/>
 * - The InfoWindow hosts a standard Android View. It can handle Android widgets like buttons and so on. <br/>
 * - Supports a "sub-description", to be displayed in the InfoWindow, under the snippet, in a smaller text font. <br/>
 * - Supports an image, to be displayed in the InfoWindow. <br/>
 * - Supports "panning to view" = when touching a marker, center the map on marker position. <br/>
 * - Opening a Marker InfoWindow doesn't automatically close others - except if the InfoWindow is shared between Markers. 
 * 
 * TODO: 
 * Marker events - custom Listeners (onMarkerClick, onMarkerDrag)
 * Impact of marker rotation on hitTest and on InfoWindow anchor
 * When map is rotated, when panning the map, bug on the InfoWindow positioning (bug already there in ItemizedOverlayWithBubble)
 * 
 * @see MarkerInfoWindow
 * @see http://developer.android.com/reference/com/google/android/gms/maps/model/Marker.html
 * 
 * @author M.Kergall
 *
 */
public class Marker extends SafeDrawOverlay {

	/*attributes for standard features:*/
	protected Drawable mIcon;
	protected GeoPoint mPosition;
	protected float mBearing;
	protected float mAnchorU, mAnchorV;
	protected float mIWAnchorU, mIWAnchorV;
	protected float mAlpha;
	protected String mTitle, mSnippet;
	protected boolean mDraggable, mIsDragged;
	protected InfoWindow mInfoWindow;
	protected boolean mFlat;
	
	/*attributes for non-standard features:*/
	protected Drawable mImage;
	protected String mSubDescription;
	protected boolean mPanToView;
	protected Object mRelatedObject;
	
	protected Point mPositionPixels;
	
	/** center in the (U,V) coordinates system of the icon image */
	public static final float ANCHOR_CENTER = 0.5f;
	protected static int defaultLayoutResId = 0;
	
	public Marker(MapView mapView) {
		this(mapView, new DefaultResourceProxyImpl(mapView.getContext()));
	}

	public Marker(MapView mapView, final ResourceProxy resourceProxy) {
		super(resourceProxy);
		mBearing = 0.0f;
		mAlpha = 1.0f; //opaque
		mPosition = new GeoPoint(0.0, 0.0);
		mAnchorU = ANCHOR_CENTER;
		mAnchorV = ANCHOR_CENTER;
		mIWAnchorU = ANCHOR_CENTER;
		mIWAnchorV = 0.0f;
		mDraggable = false;
		mIsDragged = false;
		mPositionPixels = new Point();
		mPanToView = true;
		mFlat = false; //billboard
		mIcon = resourceProxy.getDrawable(bitmap.marker_default);
		//build default bubble:
		if (defaultLayoutResId == 0){
			Context context = mapView.getContext();
			String packageName = context.getPackageName();
			defaultLayoutResId = context.getResources().getIdentifier("layout/bonuspack_bubble", null, packageName);
			if (defaultLayoutResId == 0)
				Log.e(BonusPackHelper.LOG_TAG, "Marker: layout/bonuspack_bubble not found in "+packageName);
		}
		mInfoWindow = new MarkerInfoWindow(defaultLayoutResId, mapView);
	}

	public void setIcon(Drawable icon){
		mIcon = icon;
	}
	
	public GeoPoint getPosition(){
		return mPosition.clone();
	}
	
	public void setPosition(GeoPoint position){
		mPosition = position.clone();
	}

	public float getRotation(){
		return mBearing;
	}
	
	public void setRotation(float rotation){
		mBearing = rotation;
	}
	
	public void setAnchor(float anchorU, float anchorV){
		mAnchorU = anchorU;
		mAnchorV= anchorV;
	}
	
	public void setInfoWindowAnchor(float anchorU, float anchorV){
		mIWAnchorU = anchorU;
		mIWAnchorV= anchorV;
	}
	
	public void setAlpha(float alpha){
		mAlpha = alpha;
	}
	
	public float getAlpha(){
		return mAlpha;
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

	public void setDraggable(boolean draggable){
		mDraggable = draggable;
	}
	
	public boolean isDraggable(){
		return mDraggable;
	}

	public void setFlat(boolean flat){
		mFlat = flat;
	}
	
	public boolean isFlat(){
		return mFlat;
	}
	
	public void remove(MapView mapView){
		mapView.getOverlays().remove(this);
	}

	/** set the "sub-description", an optional text to be shown in the InfoWindow, below the snippet, in a smaller text size */
	public void setSubDescription(String subDescription){
		mSubDescription = subDescription;
	}
	
	public String getSubDescription(){
		return mSubDescription;
	}

	/** set the image to be shown in the InfoWindow  - this is not the marker icon */
	public void setImage(Drawable image){
		mImage = image;
	}

	/** get the image to be shown in the InfoWindow - this is not the marker icon */
	public Drawable getImage(){
		return mImage;
	}

	/** Set the InfoWindow to be used. 
	 * Note that this InfoWindow will receive the Marker object, so it must be able to handle its data. 
	 * You can use this method either to use your own layout, or to use your own sub-class of InfoWindow. 
	 * If you don't want InfoWindow to open, set it to null. */
	public void setInfoWindow(InfoWindow infoWindow){
		mInfoWindow = infoWindow;
	}

	/** If set to true, when clicking the marker, the map will be centered on the marker position. 
	 * Default is true. */
	public void setPanToView(boolean panToView){
		mPanToView = panToView;
	}
	
	/** Allows to link an Object (any Object) to this marker. 
	 * This is particularly useful to handle custom InfoWindow. */
	public void setRelatedObject(Object relatedObject){
		mRelatedObject = relatedObject;
	}

	/** @return the related object. */
	public Object getRelatedObject(){
		return mRelatedObject;
	}
	
	public void showInfoWindow(){
		if (mInfoWindow == null)
			return;
		int markerWidth = 0, markerHeight = 0;
		if (mIcon != null){
			markerWidth = mIcon.getIntrinsicWidth(); 
			markerHeight = mIcon.getIntrinsicHeight();
		} else {
			//TODO: use the default marker size. 
		}
		
		int offsetX = (int)(mIWAnchorU*markerWidth) - (int)(mAnchorU*markerWidth);
		int offsetY = (int)(mIWAnchorV*markerHeight) - (int)(mAnchorV*markerHeight);
		
		mInfoWindow.open(this, mPosition, offsetX, offsetY);
	}
	
	public void hideInfoWindow(){
		if (mInfoWindow != null)
			mInfoWindow.close();
	}

	public boolean isInfoWindowShown(){
		return (mInfoWindow != null) && mInfoWindow.isOpen();
	}
	
	@Override protected void drawSafe(ISafeCanvas canvas, MapView mapView, boolean shadow) {
		if (shadow)
			return;
		if (mIcon == null)
			return;
		
		final Projection pj = mapView.getProjection();
		
		pj.toMapPixels(mPosition, mPositionPixels);
		int width = mIcon.getIntrinsicWidth();
		int height = mIcon.getIntrinsicHeight();
		Rect rect = new Rect(0, 0, width, height);
		rect.offset(-(int)(mAnchorU*width), -(int)(mAnchorV*height));
		mIcon.setBounds(rect);
		
		mIcon.setAlpha((int)(mAlpha*255));

		float rotationOnScreen = (mFlat ? -mBearing : mapView.getMapOrientation()-mBearing);
		drawAt(canvas.getSafeCanvas(), mIcon, mPositionPixels.x, mPositionPixels.y, false, rotationOnScreen);
	}

	public boolean hitTest(final MotionEvent event, final MapView mapView){
		final Projection pj = mapView.getProjection();
		pj.toMapPixels(mPosition, mPositionPixels);
		final Rect screenRect = pj.getIntrinsicScreenRect();
		int x = -mPositionPixels.x + screenRect.left + (int) event.getX();
		int y = -mPositionPixels.y + screenRect.top + (int) event.getY();
		boolean hit = mIcon.getBounds().contains(x, y);
		return hit;
	}
	
	@Override public boolean onSingleTapConfirmed(final MotionEvent event, final MapView mapView){
		boolean touched = hitTest(event, mapView);
		if (touched){
			showInfoWindow();
			if (mPanToView)
				mapView.getController().animateTo(getPosition());
		}
		return touched;
	}
	
	@Override public boolean onLongPress(final MotionEvent event, final MapView mapView) {
		boolean touched = hitTest(event, mapView);
		if (touched){
			if (mDraggable){
				//starts dragging mode:
				mIsDragged = true;
				hideInfoWindow();
			}
		}
		return touched;
	}
	
	@Override public boolean onTouchEvent(final MotionEvent event, final MapView mapView) {
		if (mDraggable && mIsDragged){
			if (event.getAction() == MotionEvent.ACTION_UP) {
				mIsDragged = false;
				return true;
			} else if (event.getAction() == MotionEvent.ACTION_MOVE){
				final Projection pj = mapView.getProjection();
				mPosition = (GeoPoint) pj.fromPixels(event.getX(), event.getY());
				mapView.invalidate();
				return true;
			} else 
				return false;
		} else 
			return false;
	}
	
}
