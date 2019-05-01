package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.function.Consumer;

import com.google.common.collect.HashMultimap;
import com.sun.javafx.image.IntPixelGetter;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.*;



// TODO name the AI
@ManagedAI("Potato AI")
public class PotatoAI implements PlayerFactory {

    // TODO create a new player here
    @Override
    public Player createPlayer(Colour colour) {
        return new MyPlayer();

    }


    // TODO A sample player that selects a random move
    private static class MyPlayer implements Player {

        private final Random random = new Random();

        @Override
        public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
                             Consumer<Move> callback) {
            // TODO do something interesting here; find the best move
            // picks a random move
            Move bestMove = score(view, moves);
            callback.accept(bestMove);
            System.out.println(view.getPlayerLocation(Colour.BLACK));

        }

        //Checks if detective is right next to the destination of the move being made
        private Boolean checkIfDetectiveNearMove(int destination, ScotlandYardView view) {
            ArrayList<Object> edgesTo = new ArrayList<>(view.getGraph().getEdgesTo(view.getGraph().getNode(destination)));
            ArrayList<Colour> currentPlayers = new ArrayList<>(view.getPlayers());
            Boolean thisCheck = false;
            for (Object currentEdgeTo : edgesTo) {
                for (Colour currentPlayer : currentPlayers) {
                    if (view.getPlayerLocation(currentPlayer) == ((Edge) currentEdgeTo).data() && !currentPlayer.isMrX()) {
                        thisCheck = true;
                    }

                }
            }
            //Second layer of check
            for (Object currentEdgeTo : edgesTo){
                ArrayList<Object> edgesTotheEdges = new ArrayList<>(view.getGraph().getEdgesTo(view.getGraph().getNode(((int)((Edge) currentEdgeTo).source().value()))));
                for (Object currentEdgeToTheEdge : edgesTotheEdges) {
                    for (Colour currentPlayer : currentPlayers) {
                        if (view.getPlayerLocation(currentPlayer) == ((Edge) currentEdgeToTheEdge).data() && !currentPlayer.isMrX()) {
                            thisCheck = true;
                        }

                    }
                }

                
            }
            return thisCheck;


        }
        //checks how many options are opened up when making a move and returns a score
        private int optionsOpenedByMove (int destination, ScotlandYardView view){
            ArrayList<Object> edgesFrom =new ArrayList<>(view.getGraph().getEdgesFrom(view.getGraph().getNode(destination)));
            ArrayList<Transport> transports = new ArrayList<>();
            int scoreTracker =0;

            for(Object currentEdge : edgesFrom){
                if(!transports.contains(((Edge)currentEdge).data())){
                    transports.add((Transport)((Edge)currentEdge).data());

                }
            }
            if(transports.contains(Transport.BUS)){
                scoreTracker+=1;
            }
            if(transports.contains(Transport.UNDERGROUND)){
                scoreTracker+=2;
            }
            if(transports.contains(Transport.FERRY)){
                scoreTracker+=2;
            }
            return scoreTracker;
        }

        private Move score(ScotlandYardView view, Set<Move> moves) {
            HashMultimap<Integer, Integer> nodeScores = HashMultimap.create();
            List<Move> moveList = new ArrayList<>(moves);
            int moveNumber = 0;
            for (Move currentMove : moveList) {
                if (currentMove.getClass() == PassMove.class) {
                    nodeScores.put(0, moveNumber);

                }
                if (currentMove.getClass() == TicketMove.class) {
                    int moveScoreTracker;
                    if (checkIfDetectiveNearMove(((TicketMove) currentMove).destination(), view)) {
                        moveScoreTracker = 2;
                    } else moveScoreTracker = 7;
                    moveScoreTracker+=optionsOpenedByMove(((TicketMove) currentMove).destination(), view);


                    nodeScores.put(moveScoreTracker, moveNumber);

                }
                if (currentMove.getClass() == DoubleMove.class) {
                    int doubleMoveScoreTracker;
                    if (checkIfDetectiveNearMove(((DoubleMove) currentMove).finalDestination(), view)) {
                        doubleMoveScoreTracker = 1;
                    } else doubleMoveScoreTracker = 5;

                    doubleMoveScoreTracker+=optionsOpenedByMove(((DoubleMove) currentMove).finalDestination(), view);
                    nodeScores.put(doubleMoveScoreTracker, moveNumber);

                }
                moveNumber++;
            }
            System.out.println(nodeScores.keySet());
            System.out.println(nodeScores.get(4));




            ArrayList<Integer> bestMoves = new ArrayList<>();
            int highestScore = 0;
            for (int currentMoveScore : nodeScores.keySet()) {
                if (currentMoveScore > highestScore) {
                    bestMoves.clear();
                    highestScore = currentMoveScore;
                    bestMoves.addAll(nodeScores.get(currentMoveScore));
                }

            }
            System.out.println(bestMoves);




            return moveList.get(bestMoves.get(random.nextInt(bestMoves.size())));


        }


    }
}
