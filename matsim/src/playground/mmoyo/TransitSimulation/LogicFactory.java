package playground.mmoyo.TransitSimulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.TransitScheduleWriterV1;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;
import playground.mmoyo.PTRouter.PTLink;
import playground.mmoyo.PTRouter.PTNode;
import playground.mmoyo.PTRouter.PTValues;

/**
 * Reads a TransitSchedule and creates from it:
 * -Plain network: A node represent a station. A link represents a simple connection between stations 
 * -logic layer network: with sequences of independent nodes for each TansitLine, includes transfer links
 * -logicTransitSchedule: with a cloned stop facility for each transit stop of each Transit Line with new Id's mapped to the real transitFacilities
 * -A PTRouter object containing departure information according to the logicTransitSchedule
 * -logicToPlainConverter: translates logic nodes and links into plain nodes and links. 
 */

public class LogicFactory{
	private NetworkLayer logicNet= new NetworkLayer();
	private NetworkLayer plainNet= new NetworkLayer();
	private TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
	private TransitSchedule logicTransitSchedule = builder.createTransitSchedule();
	private LogicIntoPlainTranslator logicToPlainTranslator; 
	private PTValues ptValues= new PTValues(); 
	
	private Map<Id,List<Node>> facilityNodeMap = new TreeMap<Id,List<Node>>(); /* <key =PlainStop, value = List of logicStops to be joined by transfer links>*/
	//private Map<Id,Node> logicToPlanStopMap = new TreeMap<Id,Node>();    // stores the equivalent plainNode of a logicNode   <logic, plain>
	//private Map<Id,LinkImpl> logicToPlanLinkMap = new TreeMap<Id,LinkImpl>();    // stores the equivalent plainNode of a logicNode   <logic, plain>
	//private Map<Id,LinkImpl> lastLinkMap = new TreeMap<Id,LinkImpl>();    //stores the head node as key and the link as value //useful to find lastStandardlink of transfer links
	//private Map<Id,Id> nodeLineMap = new TreeMap<Id,Id>();                  
	
	long newLinkId=0;
	long newPlainLinkId=0;
	long newStopId=0;
	

	//Creates a logic network file and a logic TransitSchedule file with individualized id's for nodes and stops*/
	public LogicFactory(final TransitSchedule transitSchedule){
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()){
			TransitLine logicTransitLine = this.builder.createTransitLine(transitLine.getId()); 
			
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				List<TransitRouteStop> logicTransitStops = new ArrayList<TransitRouteStop>();
				NodeImpl lastLogicNode = null;
				NodeImpl lastPlainNode=null;
				boolean first=true;
				
				
				/**Creates an array of Transit Route departures*/
				int numDepartures= transitRoute.getDepartures().size();
				double[] departuresArray =new double[numDepartures];
				int i=0;
				for (Departure departure : transitRoute.getDepartures().values()){
					departuresArray[i]=departure.getDepartureTime();
					i++;
				}
				
				
				//iterates in each transit stop to create nodes and links */
				for (TransitRouteStop transitRouteStop: transitRoute.getStops()) { 
					TransitStopFacility transitStopFacility = transitRouteStop.getStopFacility(); 
					
					/**Creates nodes*/
					//plain node
					NodeImpl plainNode= createPlainNode(transitStopFacility);
					
					//logicNode
					Coord coord = transitStopFacility.getCoord();
					Id idStopFacility = transitStopFacility.getId();  
					Id logicalId= createLogicalId(idStopFacility, transitLine.getId()); 
					//PTNode logicNode= logicNet.createAndAddNode(logicalId, coord); 09 oct 
					//new implementation 09oct
					PTNode logicNode= new PTNode (logicalId, coord);
					logicNode.setTransitLine(transitLine);
					logicNet.addNode(logicNode);
					logicNode.setPlainNode(plainNode);
					
					//logicToPlanStopMap.put(logicNode.getId(), plainNode); 9oct
					
					//fills the facilityNodeMap to create transfer links later on
					if (!facilityNodeMap.containsKey(idStopFacility)){
						List<Node> nodeStationArray = new ArrayList<Node>();
						facilityNodeMap.put(idStopFacility, nodeStationArray);
					}
					facilityNodeMap.get(idStopFacility).add(logicNode);

					/**Creates links*/
					if (!first){
						LinkImpl plainLink= createPlainLink(lastPlainNode, plainNode);
						
						Id id = new IdImpl(newLinkId++);
						PTLink logicLink = new PTLink(id, lastLogicNode, logicNode, logicNet, "Standard"); 
						logicLink.setPlainLink(plainLink);
						logicNode.setInStandardLink(logicLink);
						
						// 09oct logicToPlanLinkMap.put(id, plainLink);  //stores here the correspondent plainLink!! it will help to the translation!
						// 09oct lastLinkMap.put(logicNode.getId(), logicLink);   // stores the inStandardLink of a Node. Will be used in translation for transfers link
						
					}else{
						first=false;
					}

					/**creates logic stops and stopFacilities in logicTransitSchedule*/
					TransitStopFacility logicTransitStopFacility = this.builder.createTransitStopFacility(logicalId, coord, false); 
					logicTransitSchedule.addStopFacility(logicTransitStopFacility);
					TransitRouteStop logicTransitRouteStop = this.builder.createTransitRouteStop(logicTransitStopFacility, transitRouteStop.getArrivalOffset(), transitRouteStop.getDepartureOffset()); 
					logicTransitStops.add(logicTransitRouteStop);
					
					
					/**saves departures in an array for each node*/
					double[] nodeDeparturesArray =new double[numDepartures];
					for (int j=0; j<numDepartures; j++){
						double departureTime =departuresArray[j] + transitRouteStop.getDepartureOffset();
						if (departureTime > 86400) departureTime=departureTime-86400;
						nodeDeparturesArray[j] = departureTime; 
					} 
					Arrays.sort(nodeDeparturesArray);
					logicNode.setArrDep(nodeDeparturesArray);
					
					
					lastLogicNode= logicNode;
					lastPlainNode= plainNode;
				}
				TransitRoute logicTransitRoute = this.builder.createTransitRoute(transitRoute.getId(), null, logicTransitStops, transitRoute.getTransportMode());
				for (Departure departure: transitRoute.getDepartures().values()) {
					logicTransitRoute.addDeparture(departure);
				}
				logicTransitRoute.setDescription(transitRoute.getDescription());
				logicTransitLine.addRoute(logicTransitRoute);
			}
			logicTransitSchedule.addTransitLine(logicTransitLine);
		}
		
		createTransferLinks();
		createDetachedTransferLinks();
	}
	
	//Created a new id for a new node. the nodeLinMap stores the transitLine of each node. */ 
	private Id createLogicalId(final Id idStopFacility, final Id lineId){
		Id newId = new IdImpl(newStopId++);
		//nodeLineMap.put(newId, lineId);
		return newId;
	}
	
	private void createTransferLinks(){
		for (List<Node> chList : facilityNodeMap.values()) 
			for (Node fromNode : chList) 
				for (Node toNode : chList){ 
					//boolean belongToSameLine = nodeLineMap.get(fromNode.getId()) == nodeLineMap.get(toNode.getId());
					boolean belongToSameLine = ((PTNode)fromNode).getTransitLine().getId().equals(((PTNode)toNode).getTransitLine().getId());
					if (!belongToSameLine && !fromNode.equals(toNode)  && willJoin2StandardLinks((NodeImpl) fromNode, (NodeImpl) toNode)){
						Id idNewLink = new IdImpl("T" + ++newLinkId);
						createLogicLink(idNewLink, (NodeImpl)fromNode, (NodeImpl) toNode, "Transfer");
					}
				}
		facilityNodeMap = null;
	}
	
	
	public void createDetachedTransferLinks (){
		for (NodeImpl centerNode: logicNet.getNodes().values()){
			Collection<NodeImpl> nearNodes= logicNet.getNearestNodes(centerNode.getCoord(), ptValues.DETTRANSFER_RANGE );
			nearNodes.remove(centerNode);
			for (NodeImpl nearNode : nearNodes){
				boolean areConected = centerNode.getOutNodes().containsValue(nearNode); 
				//boolean belongToSameLine = nodeLineMap.get(centerNode.getId()) == nodeLineMap.get(nearNode.getId()); 
				boolean belongToSameLine = ((PTNode)centerNode).getTransitLine().getId().equals(((PTNode)nearNode).getTransitLine().getId());
				if (!belongToSameLine && !areConected && willJoin2StandardLinks(centerNode, nearNode)){  /**avoid joining nodes that are already joined by standard links AND joining nodes of the same Line*/
					Id idNewLink = new IdImpl("DT" + ++newLinkId);
					createLogicLink(idNewLink, centerNode, nearNode, "DetTransfer");
				}
			}
		}
		//NodeLineMap= null;??
	}

	/**return TRUE the new transfer link will join two Standard links otherwise it is senseless to create a transfer link between the nodes */
	private boolean willJoin2StandardLinks(NodeImpl fromNode, NodeImpl toNode){
		int numInGoingStandards =0;
		int numOutgoingStandards =0;
		
		for (LinkImpl inLink : fromNode.getInLinks().values())
			if (inLink.getType().equals("Standard")) 	numInGoingStandards++;

		for (LinkImpl outLink : toNode.getOutLinks().values())
			if (outLink.getType().equals("Standard")) numOutgoingStandards++;
	
		return (numInGoingStandards>0) && (numOutgoingStandards>0); 
	}	

	/**Creates the links for the logical network, one for transitRoute*/
	private PTLink createLogicLink(Id id, NodeImpl fromNode, NodeImpl toNode, String type){
		PTLink ptLink = new PTLink(id, fromNode, toNode, logicNet, type); 
		//logicNet.addLink(ptLink);   //check	
		return ptLink;
	}
	
	/**returns -or creates if does not exist- a plain link between two nodes*/
	private LinkImpl createPlainLink(NodeImpl fromNode, NodeImpl toNode){
		LinkImpl linkImpl = null;
		if (!fromNode.getOutNodes().containsValue(toNode)){
			double length= CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
			linkImpl= plainNet.createAndAddLink(new IdImpl(newPlainLinkId++), fromNode, toNode, length, 1.0, 1.0 , 1);
		}
		
		else{
			//-> make better method : store and read in map
			for (LinkImpl outLink : fromNode.getOutLinks().values()){
				if (outLink.getToNode().equals(toNode)){
					return outLink;
				}
			}
		}
		return linkImpl;
	}
	
	private NodeImpl createPlainNode(TransitStopFacility transitStopFacility){
		NodeImpl plainNode = null;
		Id id = transitStopFacility.getId();
		if (this.plainNet.getNodes().containsKey(id)){
			plainNode = plainNet.getNode(id);
		}else{
			plainNode = plainNet.createAndAddNode(id, transitStopFacility.getCoord());
		}
		return plainNode;
	}
	
	public void writeLogicElements(final String outPlainNetFile, final String outTransitScheduleFile, final String outLogicNetFile ){
		/**Writes logicTransitSchedule*/
		TransitScheduleWriterV1 transitScheduleWriterV1 = new TransitScheduleWriterV1 (this.logicTransitSchedule);
		try{
			transitScheduleWriterV1.write(outTransitScheduleFile);
		} catch (IOException ex) {
			System.out.println(this + ex.getMessage());
		}
		new NetworkWriter(logicNet, outLogicNetFile).write();
		new NetworkWriter(plainNet, outPlainNetFile).write();
		System.out.println("done.");
	}

	/****************get methods************/
	public NetworkLayer getLogicNet(){
		return this.logicNet;
	}

	public TransitSchedule getLogicTransitSchedule(){
		return this.logicTransitSchedule;
	}

	public NetworkLayer getPlainNet(){
		return this.plainNet;
	}
	
	public LogicIntoPlainTranslator getLogicToPlainTranslator(){
		if (this.logicToPlainTranslator==null){
			//this.logicToPlainTranslator = new LogicIntoPlainTranslator(plainNet,  logicToPlanStopMap, logicToPlanLinkMap, lastLinkMap);
			this.logicToPlainTranslator = new LogicIntoPlainTranslator(plainNet);
		}
		return this.logicToPlainTranslator;
	}

	/*
	public PTTimeTable getLogicPTTimeTable (){
		if(this.logicPTTimeTable==null){
			logicPTTimeTable = new PTTimeTable();
			TransitTravelTimeCalculator transitTravelTimeCalculator = new TransitTravelTimeCalculator(logicTransitSchedule,logicNet);
			transitTravelTimeCalculator.fillTimeTable(logicPTTimeTable);
		}
		return this.logicPTTimeTable;
	}
	*/

}
