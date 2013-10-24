package org.osmdroid.bonuspack.kml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.osmdroid.bonuspack.overlays.ExtendedOverlayItem;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.ItemizedOverlayWithBubble;
import org.osmdroid.bonuspack.overlays.PolygonOverlay;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.PathOverlay;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

/**
 * Helper class to read and parse KML content, and build related overlays. 
 * Supports KML Point, LineString and Polygon placemarks. 
 * Support KML Folder hierarchy. 
 * Supports styles for LineString and Polygon (not for Point). 
 * 
 * TODO: 
 * Handle folder name and description
 * Handle visibility
 * 
 * @author M.Kergall
 */
public class KmlProvider {

	protected static final int NO_SHAPE=0, POINT=1, LINE_STRING=2, POLYGON=3; //KML geometry
	HashMap<String, Style> mStyles;
	
	/**
	 * @param uri of the KML content. Can be for instance Google Map the url of a KML layer created with Google Maps. 
	 * @return DOM Document. 
	 */
	public Document getKml(String uri){
		DocumentBuilder docBuilder;
		Document kmlDoc = null;
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			kmlDoc = docBuilder.parse(uri);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return kmlDoc;
	}

	protected GeoPoint parseGeoPoint(String input){
		String[] coords = input.split(",");
		try {
			double lon = Double.parseDouble(coords[0]);
			double lat = Double.parseDouble(coords[1]);
			double alt = (coords.length == 3 ? Double.parseDouble(coords[2]) : 0.0);
			GeoPoint p = new GeoPoint(lat, lon, alt);
			return p;
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected ArrayList<GeoPoint> getCoordinates(Element node){
		String coords = getChildText(node);
		String[] splitted = coords.split("\\s");
		ArrayList<GeoPoint> list = new ArrayList<GeoPoint>(splitted.length);
		for (int i=0; i<splitted.length; i++){
			GeoPoint p = parseGeoPoint(splitted[i]);
			if (p != null)
				list.add(p);
		}
		return list;
	}

	public static String getChildText(Element element) {
		StringBuilder builder = new StringBuilder();
		NodeList list = element.getChildNodes();
		for(int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			short type = node.getNodeType();
			if(type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE) {
				builder.append(node.getNodeValue());
			}
		}
		return builder.toString();
	}
	
	public static List<Element> getChildrenByTagName(Element parent, String name) {
		List<Element> nodeList = new ArrayList<Element>();
		boolean all = name.equals("*");
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeType() == Node.ELEMENT_NODE && (all || name.equals(child.getNodeName()))) {
				nodeList.add((Element) child);
			}
		}
	    return nodeList;
	}

	protected void handleSubFolders(List<Element> subFolders, FolderOverlay folderOverlay, Context context, MapView map, Drawable marker){
		for (Element subFolder:subFolders){
			FolderOverlay subFolderOverlay = new FolderOverlay(context);
			handleElement(subFolder, subFolderOverlay, context, map, marker);
			folderOverlay.add(subFolderOverlay, subFolderOverlay.getBoundingBox());
		}
	}
	
	protected void handleElement(Element kmlElement, FolderOverlay folderOverlay, 
			Context context, MapView map, Drawable marker){
		
		final ArrayList<ExtendedOverlayItem> kmlPointsItems = new ArrayList<ExtendedOverlayItem>();
		ItemizedOverlayWithBubble<ExtendedOverlayItem> kmlPointsOverlay = new ItemizedOverlayWithBubble<ExtendedOverlayItem>(context, 
				kmlPointsItems, map);
		
		List<Element> subFolders = getChildrenByTagName(kmlElement, "Document");
		handleSubFolders(subFolders, folderOverlay, context, map, marker);
		
		subFolders = getChildrenByTagName(kmlElement, "Folder");
		handleSubFolders(subFolders, folderOverlay, context, map, marker);
		
		//Handle all placemarks:
		List<Element> placemarks = getChildrenByTagName(kmlElement, "Placemark");
		for (Element placemark:placemarks){
			List<Element> components = getChildrenByTagName(placemark, "*");
			String styleUrl = "", name = "", description = "";
			int type =  NO_SHAPE;
			ArrayList<GeoPoint> list = null;
			for (Element component:components){
				String nodeName = component.getTagName();
				if ("styleUrl".equals(nodeName)){
					styleUrl = getChildText(component);
					styleUrl = styleUrl.substring(1); //remove the #
				} else if ("name".equals(nodeName)){
						name = getChildText(component);
				} else if ("description".equals(nodeName)){
						description = getChildText(component);
				} else if ("Point".equals(nodeName)){
					NodeList coordinates = component.getElementsByTagName("coordinates");
					if (coordinates.getLength()>0){
						type = POINT;
						list = getCoordinates((Element)coordinates.item(0));
					}
				} else if ("LineString".equals(nodeName)){
					NodeList coordinates = component.getElementsByTagName("coordinates");
					if (coordinates.getLength()>0){
						type = LINE_STRING;
						list = getCoordinates((Element)coordinates.item(0));
					}
				} else if ("Polygon".equals(nodeName)){
					//TODO: distinguish outerBoundaryIs and innerBoundaryIs
					NodeList coordinates = component.getElementsByTagName("coordinates");
					if (coordinates.getLength()>0){
						type = POLYGON;
						list = getCoordinates((Element)coordinates.item(0));
					}
				}
			} //for placemark components
			
			//Create the overlay with the placemark geometry:
			switch (type){
			case POINT:
				if (list.size()>0){
					ExtendedOverlayItem item = new ExtendedOverlayItem(name, description, list.get(0), context);
					item.setMarkerHotspot(OverlayItem.HotspotPlace.BOTTOM_CENTER);
					item.setMarker(marker);
					kmlPointsOverlay.addItem(item);
				}
				break;
			case LINE_STRING:{
				Paint paint = null;
				Style style = mStyles.get(styleUrl);
				if (style != null){
					paint = style.outlinePaint;
				}
				if (paint == null){ 
					//set default:
					paint = new Paint();
					paint.setColor(0x90101010);
					paint.setStyle(Paint.Style.STROKE);
					paint.setStrokeWidth(5);
				}
				PathOverlay lineStringOverlay = new PathOverlay(0, context);
				lineStringOverlay.setPaint(paint);
				for (GeoPoint point:list)
					lineStringOverlay.addPoint(point);
				BoundingBoxE6 bb = BoundingBoxE6.fromGeoPoints(list);
				//Correcting osmdroid bug #359:
				//bb = new BoundingBoxE6(bb.getLatSouthE6(), bb.getLonWestE6(), bb.getLatNorthE6(), bb.getLonEastE6());
				folderOverlay.add(lineStringOverlay, bb);
				}
				break;
			case POLYGON:{
				Paint outlinePaint = null; 
				int fillColor = 0x20101010; //default
				Style style = mStyles.get(styleUrl);
				if (style != null){
					outlinePaint = style.outlinePaint;
					fillColor = style.fillColor;
				}
				if (outlinePaint == null){ 
					//set default:
					outlinePaint = new Paint();
					outlinePaint.setColor(0x90101010);
					outlinePaint.setStrokeWidth(5);
				}
				PolygonOverlay polygonOverlay = new PolygonOverlay(context);
				polygonOverlay.setFillColor(fillColor);
				polygonOverlay.setStrokeColor(outlinePaint.getColor());
				polygonOverlay.setStrokeWidth(outlinePaint.getStrokeWidth());
				polygonOverlay.setPoints(list);
				BoundingBoxE6 bb = BoundingBoxE6.fromGeoPoints(list);
				//Correcting osmdroid bug #359:
				//bb = new BoundingBoxE6(bb.getLatSouthE6(), bb.getLonWestE6(), bb.getLatNorthE6(), bb.getLonEastE6());
				folderOverlay.add(polygonOverlay, bb);
				}
				break;
			default:
				break;
			}
		} //for all placemarks

		if (kmlPointsOverlay.size()>0){
			BoundingBoxE6 bb = kmlPointsOverlay.getBoundingBoxE6();
			folderOverlay.add(kmlPointsOverlay, bb);
		}
	}
	
	protected void getStyles(Element kmlRoot){
		NodeList styles = kmlRoot.getElementsByTagName("Style");
		mStyles = new HashMap<String, Style>(styles.getLength());
		for (int i=0; i<styles.getLength(); i++){
			Element eStyle = (Element)styles.item(i);
			String id = eStyle.getAttribute("id");
			Style sStyle = new Style(eStyle);
			mStyles.put(id, sStyle);
		}
	}
	
	/**
	 * From a KML document, fill overlay with placemarks
	 * @param kmlDoc
	 * @param placemarksOverlay
	 * @param context
	 * @param marker
	 */
	public void buildOverlays(Element kmlRoot, FolderOverlay folderOverlay, Context context, MapView map, Drawable marker){
		getStyles(kmlRoot);
		
		//Recursively handle the element and all its sub-folders:
		//List<Element> subDocs = getChildrenByTagName(kmlRoot, "Document");
		handleElement(kmlRoot, folderOverlay, context, map, marker);
	}
}
