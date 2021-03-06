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

/**
 * 
 */
package org.matsim.contrib.parking.parkingsearch;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.parking.parkingsearch.sim.SetupParking;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 *
 */

public class RunParkingSearchExample {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("../../../shared-svn/projects/bmw_carsharing/example/config.xml");
		config.plans().setInputFile("population100.xml");
		config.facilities().setInputFile("parkingFacilities.xml");
		config.controler().setOutputDirectory("../../../shared-svn/projects/bmw_carsharing/example/output");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(50);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);
        controler.addOverridingModule(new OTFVisLiveModule());

		SetupParking.installParkingModules(controler);
		controler.run();

	}

}
