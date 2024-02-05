import petrinet.ConcurrencyMonitor;
import petrinet.PetriNet;
import utils.TransitionUtils;
import parser.Parser;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Arrays;

public class Main {
  public static void main(String[] args) {

    PetriNet petriNet = null;

    try {
      if (args.length > 0 && args[0].matches(".*\\.pflow")) {
        petriNet = Parser.parseXmlFile(args[0]);

        for (int i = 1; i < args.length; i += 2) {
          String id = args[i];

          if (i + 1 >= args.length || !args[i + 1].matches("\\d+")) {
            System.err.println("Invalid argument format. "
                + "Usage: PETRI_NET.pflow TRANSITION_ID FIRING_RATE_IN_MS");
            System.exit(0);
          }

          int firingRate = Integer.parseInt(args[i + 1]);

          if (firingRate == 0) {
            continue;
          }

          petriNet.getTransitionById(id).ifPresent(
              TransitionUtils.combine(t -> t.setFiringRate(firingRate), t -> t.setIsTimed(true)));

          petriNet.printPetriNetAscii();
        }

      } else {
        System.err.println(
            "Usage: PETRI_NET.pflow or " + "PETRI_NET.pflow TRANSITION_ID FIRING_RATE_IN_MS");
        System.exit(0);
      }
    } catch (ParserConfigurationException | SAXException | IOException e) {
      System.out.println(e.getClass().getName());
    }

    double mat[][] = petriNet.generateIncidenceMatrix();
    mat = utils.Math.rref(mat);

    System.out.println(Arrays.deepToString(mat).replace("], ", "]\n"));

    System.out.println("");

//    ConcurrencyMonitor monitor = new ConcurrencyMonitor(petriNet);

//    Thread thread1 = new Thread(() -> monitor.run());
//    Thread thread2 = new Thread(() -> monitor.run());
//    Thread thread3 = new Thread(() -> monitor.run());
//    Thread thread4 = new Thread(() -> monitor.run());

//    thread1.start();
//    thread2.start();
//    thread3.start();
//    thread4.start();
    
  //  try {
//      thread1.join();
//      thread2.join();
//      thread3.join();
//      thread4.join();
//    } catch (InterruptedException e) {
 //     e.printStackTrace();
  //  }

    petriNet.printPetriNetAscii();
  }
}
