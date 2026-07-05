import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import algebra.Matrix;
import logger.Logger;
import parser.Parser;
import petri.ConcurrentMatrixPetriNet;
import petri.PetriNetAnalysis;

public class Main {

	public static void main(String[] args) {

		ConcurrentMatrixPetriNet petriNet;
		try {
			petriNet = Parser.parse("misc/PetriNetTPFinal2025.xml");
		} catch (Exception e) {
			System.out.println("Error parsing the Petri net file: " + e.getMessage());
			return;
		}

		// If the petrinet was correctly parsed we may now initialize the logger and start logging events.
		Logger logger = Logger.getInstance();

		// Print general info to console and log to file for later analysis.
		// Login to console is done at least for the following items so that the user
		// is made aware of the fact that the program has loaded a Petri net and is now running 
		// the analysis and simulation.
		
		System.out.println("Incidence Matrix:");
		logger.info("Incidence Matrix:");
		petriNet.printIncidenceMatrix();
		System.out.println("\nInitial Marking:");
		petriNet.printInitialState();
		System.out.println("\nTimed Transitions:");
		petriNet.printTimedTransitions();
		System.out.println("Place Invariants:");
		Matrix.getNaturalBasis(Arrays.stream(petriNet.getIncidenceMatrix()).map(
				row -> Arrays.stream(row).asDoubleStream().toArray()).toArray(double[][]::new), true)
				.forEach(row -> System.out.println(Arrays.toString(row)));
		System.out.println("Transition Invariants:");
		Matrix.getNaturalBasis(Arrays.stream(petriNet.getIncidenceMatrix()).map(
				row -> Arrays.stream(row).asDoubleStream().toArray()).toArray(double[][]::new), false)
				.forEach(row -> System.out.println(Arrays.toString(row)));

		// Analysis
		Set<Integer> nonActionPlaces = new HashSet<>(Arrays.asList(0, 2, 3, 6, 11));

		PetriNetAnalysis analysis = new PetriNetAnalysis();
		System.out.println("\nAnalysis Results:");

		int maxActiveThreads = analysis.calculateMaxActiveThreads(petriNet, nonActionPlaces);
		int maxThreadsNeeded = analysis.calculateSegmentedMaxThreads(petriNet, nonActionPlaces);
		
		System.out.println("Maximum active threads: " + maxActiveThreads);
		System.out.println("Maximum amount of threads needed: " + maxThreadsNeeded);

		// Completely optional, can be easily obtained using PIPE but gaining a deeper understanding
		// of the Petri net as a mathematical object rather than just modelling things as objects
		// has proven to be benefital for the reduction of the percieved complexity of this project.
		// After all at its must fundamental level, all this program does is perform matrix operations;
		// be it  on a previous version Gaussian elimination when solving for the nullspace, or in the current
		// version the Farkas / Martinez-Silva algorithm (Fourier-Motzkin elimination), even regular matrix multiplication
		// when firing a transition by multiplying the incidence matrix by a firing vector and using the resulting vector to modify
		// the current marking vector.
		// The fact that this is a concurrent program is just a layer on top of the mathematical model, and it is not the core of the problem.
		PetriNetAnalysis.PropertiesReport report = analysis.analyzeProperties(petriNet);
		System.out.println("Properties:");
		report.print();


		// After that general preview of the loaded Petri net, and making use of the analysis results
		// we now know how many threads are needed in total to run the network, so we'll create a thread pool of that size and run the simulation.
		ExecutorService PetriNetThreadpool = Executors.newFixedThreadPool(maxThreadsNeeded);
		
		// Another thread would be needed for the monitor and an additional one for the logger.

	}
}
