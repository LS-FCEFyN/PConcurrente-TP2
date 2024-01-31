import petrinet.ConcurrencyMonitor;
import petrinet.PetriNet;
import parser.Parser;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    try {
      PetriNet petriNet = Parser.parseXmlFile("Test.pflow");
      petriNet.printPetriNetAscii();
      ConcurrencyMonitor monitor = new ConcurrencyMonitor(petriNet);
      monitor.executeTransition(monitor.getEnabledTransitions().get(0));
      System.out.println("");
      petriNet.printPetriNetAscii();
    } catch (ParserConfigurationException | SAXException | IOException e) {
      e.printStackTrace();
    }
  }
}
