package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.utils.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public interface Solver {

    Logger logger = LogManager.getLogger(Solver.class.getName());

    public List<Node> solve();

    // TODO refactor

}
