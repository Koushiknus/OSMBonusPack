package org.osmdroid.bonuspack.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.bonuspack.utils.HttpConnection;
import org.osmdroid.bonuspack.utils.PolylineEncoder;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import android.util.Log;

/** get a route between a start and a destination point.
 * It uses OSRM, a free open source routing service based on OpenSteetMap data. <br>
 * See https://github.com/DennisOSRM/Project-OSRM/wiki/Server-api<br>
 * 
 * It requests by default the OSRM demo site. 
 * Use setService() to request an other (for instance your own) OSRM service. <br> 
 * TODO: improve internationalization of instructions
 * @author M.Kergall
 */
public class OSRMRoadManager extends RoadManager {

	static final String OSRM_SERVICE = "http://router.project-osrm.org/viaroute?";
	//Note that the result of OSRM is quite close to Cloudmade NavEngine format:
	//http://developers.cloudmade.com/wiki/navengine/JSON_format

	protected String mServiceUrl;
	protected String mUserAgent;
	
	/** mapping from OSRM directions to MapQuest maneuver IDs: */
	static final HashMap<String, Integer> MANEUVERS;
	static {
		MANEUVERS = new HashMap<String, Integer>();
		MANEUVERS.put("0", 0); //No instruction
		MANEUVERS.put("1", 1); //Continue
		MANEUVERS.put("2", 6); //Slight right
		MANEUVERS.put("3", 7); //Right
		MANEUVERS.put("4", 8); //Sharp right
		MANEUVERS.put("5", 12); //U-turn
		MANEUVERS.put("6", 5); //Sharp left
		MANEUVERS.put("7", 4); //Left
		MANEUVERS.put("8", 3); //Slight left
		MANEUVERS.put("9", 24); //Arrived (at waypoint)
		//MANEUVERS.put("10", 0); //"Head" => used by OSRM as the start node
		MANEUVERS.put("11-1", 27); //Round-about, 1st exit
		MANEUVERS.put("11-2", 28); //2nd exit, etc ...
		MANEUVERS.put("11-3", 29);
		MANEUVERS.put("11-4", 30);
		MANEUVERS.put("11-5", 31);
		MANEUVERS.put("11-6", 32);
		MANEUVERS.put("11-7", 33);
		MANEUVERS.put("11-8", 34); //Round-about, 8th exit
		MANEUVERS.put("15", 24); //Arrived
	}
	
	//From: Project-OSRM-Web / WebContent / localization / OSRM.Locale.en.js
	// driving directions
	// %s: road name
	// %d: direction => removed
	// <*>: will only be printed when there actually is a road name
	static final HashMap<String, Object> DIRECTIONS;
	static {
		DIRECTIONS = new HashMap<String, Object>();
		HashMap<String, String> directions;
		
		directions = new HashMap<String, String>();
		DIRECTIONS.put("en", directions);
		directions.put("0", "Unknown instruction< on %s>");
		directions.put("1","Continue< on %s>");
		directions.put("2","Turn slight right< on %s>");
		directions.put("3","Turn right< on %s>");
		directions.put("4","Turn sharp right< on %s>");
		directions.put("5","U-Turn< on %s>");
		directions.put("6","Turn sharp left< on %s>");
		directions.put("7","Turn left< on %s>");
		directions.put("8","Turn slight left< on %s>");
		directions.put("9","You have reached a waypoint of your trip");
		directions.put("10","<Go on %s>");
		directions.put("11-1","Enter roundabout and leave at first exit< on %s>");
		directions.put("11-2","Enter roundabout and leave at second exit< on %s>");
		directions.put("11-3","Enter roundabout and leave at third exit< on %s>");
		directions.put("11-4","Enter roundabout and leave at fourth exit< on %s>");
		directions.put("11-5","Enter roundabout and leave at fifth exit< on %s>");
		directions.put("11-6","Enter roundabout and leave at sixth exit< on %s>");
		directions.put("11-7","Enter roundabout and leave at seventh exit< on %s>");
		directions.put("11-8","Enter roundabout and leave at eighth exit< on %s>");
		directions.put("11-9","Enter roundabout and leave at nineth exit< on %s>");
		directions.put("15","You have reached your destination");
		
		directions = new HashMap<String, String>();
		DIRECTIONS.put("fr", directions);
		directions.put("0", "Instruction inconnue< sur %s>");
		directions.put("1","Continuez< sur %s>");
		directions.put("2","Tournez l�g�rement � droite< sur %s>");
		directions.put("3","Tournez � droite< sur %s>");
		directions.put("4","Tournez fortement � droite< sur %s>");
		directions.put("5","Faites demi-tour< sur %s>");
		directions.put("6","Tournez fortement � gauche< sur %s>");
		directions.put("7","Tournez � gauche< sur %s>");
		directions.put("8","Tournez l�g�rement � gauche< sur %s>");
		directions.put("9","Vous �tes arriv� � une �tape de votre voyage");
		directions.put("10","<Prenez %s>");
		directions.put("11-1","Au rond-point, prenez la premi�re sortie< sur %s>");
		directions.put("11-2","Au rond-point, prenez la deuxi�me sortie< sur %s>");
		directions.put("11-3","Au rond-point, prenez la troisi�me sortie< sur %s>");
		directions.put("11-4","Au rond-point, prenez la quatri�me sortie< sur %s>");
		directions.put("11-5","Au rond-point, prenez la cinqui�me sortie< sur %s>");
		directions.put("11-6","Au rond-point, prenez la sixi�me sortie< sur %s>");
		directions.put("11-7","Au rond-point, prenez la septi�me sortie< sur %s>");
		directions.put("11-8","Au rond-point, prenez la huiti�me sortie< sur %s>");
		directions.put("11-9","Au rond-point, prenez la neuvi�me sortie< sur %s>");
		directions.put("15","Vous �tes arriv�");
	}
	
	public OSRMRoadManager(){
		super();
		mServiceUrl = OSRM_SERVICE;
		mUserAgent = BonusPackHelper.DEFAULT_USER_AGENT; //set user agent to the default one. 
	}
	
	/** allows to request on an other site than OSRM demo site */
	public void setService(String serviceUrl){
		mServiceUrl = serviceUrl;
	}

	/** allows to send to OSRM service a user agent specific to the app, 
	 * instead of the default user agent of OSMBonusPack lib. 
	 */
	public void setUserAgent(String userAgent){
		mUserAgent = userAgent;
	}
	
	protected String getUrl(ArrayList<GeoPoint> waypoints){
		StringBuffer urlString = new StringBuffer(mServiceUrl);
		for (int i=0; i<waypoints.size(); i++){
			GeoPoint p = waypoints.get(i);
			urlString.append("&loc="+geoPointAsString(p));
		}
		urlString.append(mOptions);
		return urlString.toString();
	}

	@Override public Road getRoad(ArrayList<GeoPoint> waypoints) {
		String url = getUrl(waypoints);
		Log.d(BonusPackHelper.LOG_TAG, "OSRMRoadManager.getRoad:"+url);

		//String jString = BonusPackHelper.requestStringFromUrl(url);
		HttpConnection connection = new HttpConnection();
		connection.setUserAgent(mUserAgent);
		connection.doGet(url);
		String jString = connection.getContentAsString();
		connection.close();

		if (jString == null) {
			Log.e(BonusPackHelper.LOG_TAG, "OSRMRoadManager::getRoad: request failed.");
			return new Road(waypoints);
		}
		Locale l = Locale.getDefault();
		HashMap<String, String> directions = (HashMap<String, String>)DIRECTIONS.get(l.getLanguage());
		if (directions == null)
			directions = (HashMap<String, String>)DIRECTIONS.get("en");
		Road road = new Road();
		try {
			JSONObject jObject = new JSONObject(jString);
			String route_geometry = jObject.getString("route_geometry");
			road.mRouteHigh = PolylineEncoder.decode(route_geometry, 10);
			JSONArray jInstructions = jObject.getJSONArray("route_instructions");
			int n = jInstructions.length();
			RoadNode lastNode = null;
			for (int i=0; i<n; i++){
				JSONArray jInstruction = jInstructions.getJSONArray(i);
				RoadNode node = new RoadNode();
				int positionIndex = jInstruction.getInt(3);
				node.mLocation = road.mRouteHigh.get(positionIndex);
				node.mLength = jInstruction.getInt(2)/1000.0;
				node.mDuration = jInstruction.getInt(4)/10.0;
					//duration unit is not documented. Seems to be 10th of seconds... 
				String direction = jInstruction.getString(0);
				String roadName = jInstruction.getString(1);
				if (lastNode!=null && "1".equals(direction) && "".equals(roadName)){
					//node "Continue" with no road name is useless, don't add it
					lastNode.mLength += node.mLength;
					lastNode.mDuration += node.mDuration;
				} else {
					node.mManeuverType = getManeuverCode(direction);
					node.mInstructions = buildInstructions(direction, roadName, directions);
					//Log.d(BonusPackHelper.LOG_TAG, direction+"=>"+node.mManeuverType+"; "+node.mInstructions);
					road.mNodes.add(node);
					lastNode = node;
				}
			}
			JSONObject jSummary = jObject.getJSONObject("route_summary");
			road.mLength = jSummary.getInt("total_distance")/1000.0;
			road.mDuration = jSummary.getInt("total_time");
		} catch (JSONException e) {
			e.printStackTrace();
			return new Road(waypoints);
		}
		if (road.mRouteHigh.size()==0){
			//Create default road:
			road = new Road(waypoints);
		} else {
			road.buildLegs(waypoints);
			BoundingBoxE6 bb = BoundingBoxE6.fromGeoPoints(road.mRouteHigh);
			//Correcting osmdroid bug #359:
			road.mBoundingBox = new BoundingBoxE6(
				bb.getLatSouthE6(), bb.getLonWestE6(), bb.getLatNorthE6(), bb.getLonEastE6());
			road.mStatus = Road.STATUS_OK;
		}
		Log.d(BonusPackHelper.LOG_TAG, "OSRMRoadManager.getRoad - finished");
		return road;
	}
	
	protected int getManeuverCode(String direction){
		Integer code = MANEUVERS.get(direction);
		if (code != null)
			return code;
		else 
			return 0;
	}
	
	protected String buildInstructions(String direction, String roadName,
			HashMap<String, String> directions){
		if (directions == null)
			return null;
		direction = directions.get(direction);
		if (direction == null)
			return null;
		String instructions = null;
		if (roadName.equals(""))
			//remove "<*>"
			instructions = direction.replaceFirst("<[^>]*>", "");
		else {
			direction = direction.replace('<', ' ');
			direction = direction.replace('>', ' ');
			instructions = String.format(direction, roadName);
		}
		return instructions;
	}
}
