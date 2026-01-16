import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import parser.Parser;
import petri.ConcurrentMatrixPetriNet;
import petri.PetriNetAnalysis;

public class Main {

	public static void main(String[] args) {

		// if(args.length < 1) {
		// System.out.println("Please provide the path to a file containing a Petri net
		// as an argument.");
		// System.out.println("Usage: java -jar PetriNetAnalyzer.jar
		// <path-to-petri-net-file>");
		// return;
		// }

		ConcurrentMatrixPetriNet petriNet;
		try {
			petriNet = Parser.parse("misc/PetriNetTPFinal2025.xml");
		} catch (Exception e) {
			System.out.println("Error parsing the Petri net file: " + e.getMessage());
			return;
		}

		// Print general info
		System.out.println("Incidence Matrix:");
		petriNet.printIncidenceMatrix();
		System.out.println("\nInitial Marking:");
		petriNet.printInitialState();
		System.out.println("\nTimed Transitions:");
		petriNet.printTimedTransitions();
		

	}
}
