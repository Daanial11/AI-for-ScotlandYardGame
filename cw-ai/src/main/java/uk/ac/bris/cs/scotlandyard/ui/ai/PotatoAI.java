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

import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.fromTransport;


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
            //Second level of check
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
        //Checks if detective can move to destination of a possible move, if so then -10 off score for move
        private int canDetectiveMovetoTarget(int destination, ScotlandYardView view){
            List<Colour> detectiveList = new ArrayList<>();
            detectiveList.addAll(view.getPlayers());
            detectiveList.remove(Colour.BLACK);

            for(Colour currentDetective : detectiveList){
                Set<Move> currentDetectiveValidMoves = validMove(currentDetective, view);
                for(Move currentMove: currentDetectiveValidMoves){
                    if(currentMove.getClass() == TicketMove.class){
                        if(((TicketMove)currentMove).destination() == destination){
                            return -10;
                        }
                    }

                }

            }
            return 0;

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
                scoreTracker+=2;
            }
            if(transports.contains(Transport.UNDERGROUND)){
                scoreTracker+=2;
            }
            if(transports.contains(Transport.FERRY)){
                scoreTracker+=1;
            }
            return scoreTracker;
        }
        //generates score for each move depending on several factors, including how many detectives are nearby, how many
        //options are opened up by the move and if detective can reach destination of some move
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
                    moveScoreTracker+=canDetectiveMovetoTarget(((TicketMove) currentMove).destination(), view);


                    nodeScores.put(moveScoreTracker, moveNumber);

                }
                //Double moves are more expensive so score less points, should only be used when no ticket move scores higher
                if (currentMove.getClass() == DoubleMove.class) {
                    int doubleMoveScoreTracker;
                    if (checkIfDetectiveNearMove(((DoubleMove) currentMove).finalDestination(), view)) {
                        doubleMoveScoreTracker = 1;
                    } else doubleMoveScoreTracker = 5;

                    doubleMoveScoreTracker+=optionsOpenedByMove(((DoubleMove) currentMove).finalDestination(), view);
                    doubleMoveScoreTracker+=canDetectiveMovetoTarget(((DoubleMove) currentMove).finalDestination(), view);
                    nodeScores.put(doubleMoveScoreTracker, moveNumber);

                }
                moveNumber++;
            }




            ArrayList<Integer> bestMoves = new ArrayList<>();
            int highestScore = 0;
            for (int currentMoveScore : nodeScores.keySet()) {
                if (currentMoveScore > highestScore) {
                    bestMoves.clear();
                    highestScore = currentMoveScore;
                    bestMoves.addAll(nodeScores.get(currentMoveScore));
                }

            }



            //As there may be multiple moves with the same score, one is chosen at random from the best scoring moves
            return moveList.get(bestMoves.get(random.nextInt(bestMoves.size())));


        }
        //Just Checks if location is occupied by another detective
        private Boolean isLocationOccupied(int destination, ScotlandYardView view){
            for (Colour currentPlayer : view.getPlayers()){
                if(view.getPlayerLocation(currentPlayer).get() == destination && !currentPlayer.isMrX()){
                    return true;
                }
            }
            return false;
        }



        // Modified version of validMoves, only for detectives.
        // Returns set of ticketMoves that the player can do, regarding tickets, locations and other player's location.
        private Set<Move> validMove(Colour player, ScotlandYardView view){
            // Array list of the edges around the player's first location (to make set of valid TicketMoves)
            ArrayList<Object> Edges = new ArrayList<>(view.getGraph().getEdgesFrom(view.getGraph().getNode(view.getPlayerLocation(player).get())));
            // Final validMove set
            Set<Move> validMoves = new HashSet<>();

            for(Object possibleMove: Edges){
                Transport transportType = (Transport)((Edge)possibleMove).data();
                int destination = (int)((Edge)possibleMove).destination().value();
                // Checks player has enough tickets of a transport type and location is not taken to make the move
                if(view.getPlayerTickets(player, fromTransport(transportType)).get() != 0 && !isLocationOccupied(destination, view)) {
                    validMoves.add(new TicketMove(player, fromTransport(transportType), destination));

                }

            }
            // Adds Passmove if player is Detective
            if(validMoves.isEmpty() && player.isDetective()){
                validMoves.add(new PassMove(player));
            }

            return validMoves;
        }


    }
}
