/**
 * 
 */
package playground.yu.utils.qgis;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.population.BasicLeg.Mode;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;

import playground.yu.analysis.PlanModeJudger;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class ModeWithNewAttributeTextLayer4QGIS extends ModeTextLayer4QGIS {
	private int count, car6hCount, pt6hCount;
	private double travelTime, am6hTravelTime;

	public ModeWithNewAttributeTextLayer4QGIS(String textFilename,
			String attribute) {
		writer = new SimpleWriter(textFilename);
		writer.writeln("x\ty\tmode\t" + attribute + "\ttravelTime");
		count = 0;
		car6hCount = 0;
		pt6hCount = 0;
		travelTime = 0.0;
		am6hTravelTime = 0.0;
	}

	@Override
	public void run(Plan plan) {
		count++;
		Activity act = plan.getFirstActivity();
		Coord homeLoc = act.getCoord();
		double endTime = act.getEndTime();
		double travelTime = plan.getNextLeg(act).getTravelTime();
		this.travelTime += travelTime;
		String mode = "";

		if (PlanModeJudger.useCar(plan))
			mode = Mode.car.name();
		else if (PlanModeJudger.usePt(plan))
			mode = Mode.pt.name();
		else if (PlanModeJudger.useWalk(plan))
			mode = Mode.walk.name();

		if (endTime ==
		// 21600.0
		86340.0) {
			writer.writeln(homeLoc.getX() + "\t" + homeLoc.getY() + "\t" + mode
					+ "\t6\t" + travelTime);
			am6hTravelTime += travelTime;
			if (mode.equals(Mode.car.name()))
				car6hCount++;
			if (mode.equals(Mode.pt.name()))
				pt6hCount++;
		} else {
			writer.writeln(homeLoc.getX() + "\t" + homeLoc.getY() + "\t" + mode
					+ "\t0\t" + travelTime);
		}
	}

	@Override
	public void close() {
		writer.writeln("car6h :\t" + car6hCount + "\t" + (double) car6hCount
				/ (double) count + "%");
		writer.writeln("pt6h :\t" + pt6hCount + "\t" + (double) pt6hCount
				/ (double) count + "%");
		writer.writeln("am6h_travelTime :\t" + am6hTravelTime
				/ (double) (car6hCount + pt6hCount) + "\t[s]/[min]");
		writer.writeln("all_travelTime :\t" + travelTime / (double) count
				+ "\t[s]/[min]");
		super.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		// final String netFilename =
		// "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		// final String plansFilename =
		// "../runs_SVN/run674/it.1000/1000.plans.xml.gz";
		// final String textFilename =
		// "../runs_SVN/run674/it.1000/1000.analysis/mode_1.endTime.txt";
		final String netFilename = "../matsimTests/timeAllocationMutatorTest/network.xml";
		final String plansFilename = "../matsimTests/timeAllocationMutatorTest/it.100/100.plans.xml.gz";
		final String textFilename = "../matsimTests/timeAllocationMutatorTest/it.100/mode_1.endTime.txt";
		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		ModeWithNewAttributeTextLayer4QGIS mwnatl = new ModeWithNewAttributeTextLayer4QGIS(
				textFilename, "1.actEndTime");
		mwnatl.run(population);
		mwnatl.close();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
