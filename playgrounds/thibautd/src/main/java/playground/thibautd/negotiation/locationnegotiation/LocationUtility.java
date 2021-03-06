/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.negotiation.locationnegotiation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import playground.thibautd.negotiation.framework.NegotiationAgent;
import playground.thibautd.negotiation.framework.PropositionUtility;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author thibautd
 */
@Singleton
public class LocationUtility implements PropositionUtility<LocationProposition> {
	private final RandomSeedHelper seeds;
	private final LocationHelper locations;
	private final LocationUtilityConfigGroup configGroup;
	private final Population population;

	@Inject
	public LocationUtility( final RandomSeedHelper seeds,
			final LocationHelper locations,
			final LocationUtilityConfigGroup configGroup,
			final Population population ) {
		this.seeds = seeds;
		this.locations = locations;
		this.configGroup = configGroup;
		this.population = population;
	}

	@Override
	public double utility( final NegotiationAgent<LocationProposition> agent, final LocationProposition proposition ) {
		final Person ego = population.getPersons().get( agent.getId() );
		final Collection<Person> alters =
				proposition.getGroupIds().stream()
						.map( population.getPersons()::get )
						.collect( Collectors.toList() );

		final ActivityFacility location = proposition.getFacility();

		final double sumOfAlterUtils =
				alters.stream()
					.mapToDouble( a -> seeds.getUniformErrorTerm( a , ego ) * configGroup.getMuContact() )
					.sum();

		final double utilLocation = seeds.getGaussianErrorTerm( ego , asAttr( location ) ) * configGroup.getSigmaFacility();

		final double utilTravelTime = getTravelTime( ego , location ) * configGroup.getBetaTime();

		return sumOfAlterUtils + utilLocation + utilTravelTime;
	}

	// TODO: make facilities actually implement attributable!
	private Attributable asAttr( final Customizable facility ) {
		if ( !facility.getCustomAttributes().containsKey( "attributes" ) ) {
			facility.getCustomAttributes().put( "attributes" , new Attributes() );
		}
		return () -> (Attributes) facility.getCustomAttributes().get( "attributes" );
	}

	private double getTravelTime( final Person ego, final ActivityFacility location ) {
		switch ( configGroup.getTravelTimeType() ) {
			case crowFly:
				final Coord homeCoord = locations.getHomeLocation( ego ).getCoord();
				return CoordUtils.calcEuclideanDistance( homeCoord , location.getCoord() );
		}
		throw new RuntimeException( );
	}
}
