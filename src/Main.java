import petrinet.PetriNet;
import utils.TransitionUtils;
import parser.Parser;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    try {
      PetriNet petriNet = Parser.parseXmlFile("Test.pflow");

      for(int i = 0; i < args.length; i += 2) {
        String id = args[i];
        int firingRate = Integer.parseInt(args[i+1]);
        petriNet.getTransitionById(id).ifPresent(TransitionUtils.combine(
          t -> t.setFiringRate(firingRate), 
          t -> t.setIsTimed(true)));
    }

      petriNet.printPetriNetAscii();
    } catch (ParserConfigurationException | SAXException | IOException e) {
      e.printStackTrace();
    }
  }
}
