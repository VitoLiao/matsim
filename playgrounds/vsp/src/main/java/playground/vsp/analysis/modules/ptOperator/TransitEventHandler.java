/* *********************************************************************** *
 * project: org.matsim.*
 * TransitEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.vsp.analysis.modules.ptOperator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;

import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;

/**
 * @author ikaddoura
 *
 */
public class TransitEventHandler implements TransitDriverStartsEventHandler, LinkLeaveEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler {
	private final static Logger log = Logger.getLogger(PtOperatorAnalyzer.class);
	private List<Id> vehicleIDs = new ArrayList<Id>();
	private Network network;
	private double vehicleKm;
	private PtDriverIdAnalyzer ptDriverIdAnalyzer;
	
	private Map<Id, Double> personID2firstDepartureTime = new HashMap<Id, Double>();
	private Map<Id, Double> personID2lastArrivalTime = new HashMap<Id, Double>();
	
	public TransitEventHandler(Network network, PtDriverIdAnalyzer ptDriverIdAnalyzer) {
		this.network = network;
		this.ptDriverIdAnalyzer = ptDriverIdAnalyzer;
	}

	@Override
	public void reset(int iteration) {
		this.vehicleIDs.clear();
		this.personID2firstDepartureTime.clear();
		this.personID2lastArrivalTime.clear();
		this.vehicleKm = 0.0;
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		Id vehicleId = event.getVehicleId();
		if (vehicleIDs.contains(vehicleId)){
			// vehicleID bereits in Liste
		}
		else{
			this.vehicleIDs.add(vehicleId);
		}
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id personId = event.getPersonId();
		if (this.ptDriverIdAnalyzer.isPtDriver(personId)){
			this.vehicleKm = this.vehicleKm + network.getLinks().get(event.getLinkId()).getLength() / 1000;
		} else {
			// no public vehicle
		}
	}
	
	public List<Id> getVehicleIDs() {
		return vehicleIDs;
	}
	
	public double getVehicleKm() {
		return this.vehicleKm;
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (this.ptDriverIdAnalyzer.isPtDriver(event.getPersonId())){
			if (this.personID2firstDepartureTime.containsKey(event.getPersonId())){
				if (event.getTime() < this.personID2firstDepartureTime.get(event.getPersonId())){
					this.personID2firstDepartureTime.put(event.getPersonId(), event.getTime());
				}
				else {
					// not the first departure time of this public vehicle
				}
			}
			else {
				this.personID2firstDepartureTime.put(event.getPersonId(), event.getTime());
			}
		} else {
			// no public vehicle
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (this.ptDriverIdAnalyzer.isPtDriver(event.getPersonId())) {
			if (this.personID2lastArrivalTime.containsKey(event.getPersonId())){
				if (event.getTime() > this.personID2lastArrivalTime.get(event.getPersonId())){
					this.personID2lastArrivalTime.put(event.getPersonId(), event.getTime());
				}
				else {
					// not the last arrival time of this public vehicle
				}
			}
			else {
				this.personID2lastArrivalTime.put(event.getPersonId(), event.getTime());
			}
		} else {
			// no public vehicle
		}
	}
	
	public double getVehicleHours() {
		double vehicleSeconds = 0;
		for (Id id : this.personID2firstDepartureTime.keySet()){
			vehicleSeconds = vehicleSeconds + ((this.personID2lastArrivalTime.get(id) - this.personID2firstDepartureTime.get(id)));
		}
		return vehicleSeconds / 3600.0;
	}
	
}
