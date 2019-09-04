package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public interface Solver {

    Logger logger = LogManager.getLogger(Solver.class.getName());

    List<Node> solve();

    // TODO refactor

}
