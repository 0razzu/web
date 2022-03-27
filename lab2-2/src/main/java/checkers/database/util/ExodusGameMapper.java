package checkers.database.util;


import checkers.model.*;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jetbrains.exodus.entitystore.Entity;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;


public class ExodusGameMapper {
    private static final Gson GSON = new Gson();
    private static final Type strList = new TypeToken<List<String>>(){}.getType();
    private static final String NULL = "null";
    
    
    private static class Delimiters {
        public static final String CELL = "cell";
        public static final String POSSIBLE_MOVE = "pos_move";
        public static final String STEP = "step";
        public static final String MOVE = "move";
    }
    
    
    private static boolean strIsNull(String str) {
        return str == null || str.equals(NULL);
    }
    
    
    private static String toString(Cell cell) {
        return cell == null? NULL : cell.getRow() + Delimiters.CELL + cell.getCol();
    }
    
    
    private static Cell toCell(String str, Cell[][] board) {
        if (strIsNull(str))
            return null;
        
        String[] coordinates = str.split(Delimiters.CELL);
        
        return board[Integer.parseInt(coordinates[0])][Integer.parseInt(coordinates[1])];
    }
    
    
    private static String toString(PossibleMove possibleMove) {
        if (possibleMove == null)
            return NULL;
        
        Cell foe = possibleMove.getFoe();
        
        return toString(possibleMove.getDest()) + Delimiters.POSSIBLE_MOVE
                + (foe == null? "" : toString(foe)) + Delimiters.POSSIBLE_MOVE
                + possibleMove.getState();
    }
    
    
    private static PossibleMove toPossibleMove(String str, Cell[][] board) {
        if (strIsNull(str))
            return null;
        
        String[] fields = str.split(Delimiters.POSSIBLE_MOVE);
        
        return new PossibleMove(
                toCell(fields[0], board),
                (Strings.isNullOrEmpty(fields[1])? null : toCell(fields[1], board)),
                CellState.valueOf(fields[2])
        );
    }
    
    
    private static String toString(Multimap<Cell, PossibleMove> situation) {
        return situation == null?
                NULL :
                GSON.toJson(
                situation.asMap().entrySet().stream().map(entry ->
                        Map.entry(
                                toString(entry.getKey()),
                                entry.getValue().stream()
                                        .map(ExodusGameMapper::toString).collect(Collectors.toList())
                        )
                ).collect(Collectors.toList())
        );
    }
    
    
    private static Multimap<Cell, PossibleMove> toSituation(String str, Cell[][] board) {
        if (strIsNull(str))
            return null;
        
        List<Map.Entry<String, List<String>>> entries =
                GSON.fromJson(str, new TypeToken<List<AbstractMap.SimpleEntry<String, List<String>>>>(){}.getType());
        Multimap<Cell, PossibleMove> situation = ArrayListMultimap.create();
        
        for (Map.Entry<String, List<String>> entry: entries) {
            situation.putAll(
                    toCell(entry.getKey(), board),
                    entry.getValue().stream()
                            .map(possibleMoveStr -> toPossibleMove(possibleMoveStr, board)).collect(Collectors.toList())
            );
        }
        
        return situation;
    }
    
    
    private static String toString(Step step) {
        return step == null?
                NULL :
                toString(step.getFrom()) + Delimiters.STEP + toString(step.getTo());
    }
    
    
    private static Step toStep(String str, Cell[][] board) {
        if (strIsNull(str))
            return null;
        
        String[] fields = str.split(Delimiters.STEP);
        
        return new Step(
                toCell(fields[0], board),
                toCell(fields[1], board)
        );
    }
    
    
    private static String toString(Move move) {
        return move == null?
                NULL :
                GSON.toJson(move.getSteps().stream()
                        .map(ExodusGameMapper::toString).collect(Collectors.toList())) + Delimiters.MOVE
                        + move.isHaveKilled() + Delimiters.MOVE
                        + move.getWhoseTurn();
    }
    
    
    private static Move toMove(String str, Cell[][] board) {
        if (strIsNull(str))
            return null;
        
        String[] fields = str.split(Delimiters.MOVE);
        
        return new Move(
                ((List<String>) GSON.fromJson(fields[0], strList)).stream()
                        .map(stepStr -> toStep(stepStr, board)).collect(Collectors.toList()),
                Boolean.parseBoolean(fields[1]),
                Team.valueOf(fields[2])
        );
    }
    
    
    private static String moveListToString(List<Move> moveList) {
        return moveList == null?
                NULL :
                GSON.toJson(moveList.stream().map(ExodusGameMapper::toString).collect(Collectors.toList()));
    }
    
    
    private static List<Move> toMoveList(String str, Cell[][] board) {
        return strIsNull(str)?
                null :
                ((List<String>) GSON.fromJson(str, strList)).stream()
                        .map(moveStr -> toMove(moveStr, board)).collect(Collectors.toList());
    }
    
    
    private static String cellListToString(List<Cell> cellList) {
        return cellList == null?
                NULL :
                GSON.toJson(cellList.stream().map(ExodusGameMapper::toString).collect(Collectors.toList()));
    }
    
    
    private static List<Cell> toCellList(String str, Cell[][] board) {
        return strIsNull(str)?
                null :
                ((List<String>) GSON.fromJson(str, strList)).stream()
                        .map(cellStr -> toCell(cellStr, board)).collect(Collectors.toList());
    }
    
    
    public static void toEntity(Entity entity, Game game) {
        entity.setProperty("board", GSON.toJson(game.getBoard()));
        entity.setProperty("whoseTurn", game.getWhoseTurn().toString());
        entity.setProperty("status", game.getStatus().toString());
        entity.setProperty("situation", toString(game.getSituation()));
        entity.setProperty("moveList", moveListToString(game.getMoveList()));
        entity.setProperty("currentMove", toString(game.getCurrentMove()));
        entity.setProperty("killed", cellListToString(game.getKilled()));
        entity.setProperty("becomeKing", game.getBecomeKing());
    }
    
    
    public static Game fromEntity(Entity entity) {
        Cell[][] board = GSON.fromJson((String) entity.getProperty("board"), Cell[][].class);
        
        return new Game(
                entity.toIdString(),
                board,
                Team.valueOf((String) entity.getProperty("whoseTurn")),
                Status.valueOf((String) entity.getProperty("status")),
                toSituation((String) entity.getProperty("situation"), board),
                toMoveList((String) entity.getProperty("moveList"), board),
                toMove((String) entity.getProperty("currentMove"), board),
                toCellList((String) entity.getProperty("killed"), board),
                (boolean) entity.getProperty("becomeKing")
        );
    }
}
