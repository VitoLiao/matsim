/* *********************************************************************** *
 * project: org.matsim.*
 * ScheduleCleaner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.mfeil;

import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;

public class ScheduleCleaner {
	
	private final LegTravelTimeEstimator	estimator;
	private final double					minimumTime;
	
	public ScheduleCleaner(LegTravelTimeEstimator estimator, double minimumTime){
		this.estimator = estimator;
		this.minimumTime = minimumTime;
	}
	
	public double run (double now, Plan plan){
		((Activity)(plan.getPlanElements().get(0))).setEndTime(now);
		((Activity)(plan.getPlanElements().get(0))).setDuration(now);
			
		double travelTime;
		for (int i=1;i<=plan.getPlanElements().size()-2;i=i+2){
			((Leg)(plan.getPlanElements().get(i))).setDepartureTime(now);
			travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Activity)(plan.getPlanElements().get(i-1)), (Activity)(plan.getPlanElements().get(i+1)), (Leg)(plan.getPlanElements().get(i)));
			((Leg)(plan.getPlanElements().get(i))).setArrivalTime(now+travelTime);
			((Leg)(plan.getPlanElements().get(i))).setTravelTime(travelTime);
			now+=travelTime;
			
			if (i!=plan.getPlanElements().size()-2){
				((Activity)(plan.getPlanElements().get(i+1))).setStartTime(now);
				travelTime = java.lang.Math.max(((Activity)(plan.getPlanElements().get(i+1))).getDuration()-travelTime, this.minimumTime);
				((Activity)(plan.getPlanElements().get(i+1))).setDuration(travelTime);	
				((Activity)(plan.getPlanElements().get(i+1))).setEndTime(now+travelTime);	
				now+=travelTime;
			}
			else {
				((Activity)(plan.getPlanElements().get(i+1))).setStartTime(now);
				/* NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW*/
				if (86400>now+this.minimumTime){
					((Activity)(plan.getPlanElements().get(i+1))).setDuration(86400-now);
					((Activity)(plan.getPlanElements().get(i+1))).setEndTime(86400);
				}
				else if (86400+((Activity)(plan.getPlanElements().get(0))).getDuration()>now+this.minimumTime){
					if (now<86400){
						((Activity)(plan.getPlanElements().get(i+1))).setDuration(86400-now);
						((Activity)(plan.getPlanElements().get(i+1))).setEndTime(86400);
					}
					else {
					((Activity)(plan.getPlanElements().get(i+1))).setDuration(this.minimumTime);
					((Activity)(plan.getPlanElements().get(i+1))).setEndTime(now+this.minimumTime);
					}
				}
				else {
					return (now+this.minimumTime-(86400+((Activity)(plan.getPlanElements().get(0))).getDuration()));
				}
			}
		}
		return 0;
	}

}
