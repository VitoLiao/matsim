/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingsearch;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

/**
 * @author  jbischoff
 *
 */
public class ParkingUtils {
	
	static public final String PARKACTIVITYTYPE = "car interaction";
	static public final double UNPARKDURATION = 60;
	static public final double PARKDURATION = 60;
	static public final int NO_OF_LINKS_TO_GET_ON_ROUTE = 5;
	
	
	public ParkingUtils() {
		// TODO Auto-generated constructor stub
	}
	
	public static Coord getRandomPointAlongLink(Random rnd, Link link){
		Coord fromNodeCoord = link.getFromNode().getCoord();
		Coord toNodeCoord = link.getToNode().getCoord();
		double r = rnd.nextDouble();
		
		double x = (fromNodeCoord.getX()*r)+(toNodeCoord.getX()*(1-r));
		double y = (fromNodeCoord.getY()*r)+(toNodeCoord.getY()*(1-r));
		
		return new Coord(x,y);
	}

}
