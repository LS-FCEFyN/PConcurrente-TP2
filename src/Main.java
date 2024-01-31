import petrinet.PetriNet;
import utils.TransitionUtils;
import parser.Parser;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    try {
      if (args.length > 0 && args[0].matches(".*\\.pflow")) {
        PetriNet petriNet = Parser.parseXmlFile(args[0]);

        for (int i = 1; i < args.length; i += 2) {
          String id = args[i];

          if (i + 1 >= args.length || !args[i + 1].matches("\\d+")) {
            System.out.println("Invalid argument format. " + 
            "Usage: PETRI_NET.pflow TRANSITION_ID FIRING_RATE_IN_MS");
            System.exit(0);
          }

          int firingRate = Integer.parseInt(args[i + 1]);

          if(firingRate == 0) {
            continue;
          }

          petriNet.getTransitionById(id).ifPresent(
              TransitionUtils.combine(t -> t.setFiringRate(firingRate),
                                      t -> t.setIsTimed(true)));
        }

        petriNet.printPetriNetAscii();
      } else {
        System.out.println(
            "Usage: PETRI_NET.pflow or " + 
            "PETRI_NET.pflow TRANSITION_ID FIRING_RATE_IN_MS");
        System.exit(0);
      }
    } catch (ParserConfigurationException | SAXException | IOException e) {
      System.out.println(e.getClass().getName());
    }
  }
}
