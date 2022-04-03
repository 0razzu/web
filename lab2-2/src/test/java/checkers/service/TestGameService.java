package checkers.service;


import checkers.Properties;
import checkers.database.dao.GameDao;
import checkers.dto.request.ChangeStatusRequest;
import checkers.dto.request.CreateGameRequest;
import checkers.dto.response.*;
import checkers.dto.versatile.CellDto;
import checkers.dto.versatile.FullCellDto;
import checkers.dto.versatile.StepDto;
import checkers.error.CheckersException;
import checkers.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static checkers.error.CheckersErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


public class TestGameService {
    private static final Properties properties = mock(Properties.class);
    private static final GameDao gameDao = mock(GameDao.class);
    private final GameService gameService = new GameService(gameDao, properties);
    
    
    @BeforeAll
    static void configureMocks() {
        when(properties.getMoveTime()).thenReturn(Integer.MAX_VALUE);
        
        Map<String, Game> games = new HashMap<>();
        
        doAnswer(invocation -> {
            Game game = (Game) invocation.getArguments()[0];
            String id = UUID.randomUUID().toString();
            
            games.put(id, game);
            game.setId(id);
            
            return id;
        }).when(gameDao).put(any(Game.class));
        
        doAnswer(invocation -> {
            String id = (String) invocation.getArguments()[0];
            
            return games.get(id);
        }).when(gameDao).get(anyString());
    }
    
    
    @Test
    void testCreateGame() {
        List<FullCellDto> cells = List.of(
                new FullCellDto(0, 0, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(0, 2, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(0, 4, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(7, 1, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(7, 3, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(7, 5, CellState.DEFAULT, Checker.BLACK)
        );
        CreateGameRequest request = new CreateGameRequest(cells, null);
        
        List<SituationEntryDto> expectedSituation = List.of(
                new SituationEntryDto(new CellDto(0, 0), List.of(
                        new PossibleMoveDto(new CellDto(1, 1), null, CellState.CAN_BE_FILLED)
                )),
                new SituationEntryDto(new CellDto(0, 2), List.of(
                        new PossibleMoveDto(new CellDto(1, 1), null, CellState.CAN_BE_FILLED),
                        new PossibleMoveDto(new CellDto(1, 3), null, CellState.CAN_BE_FILLED)
                )),
                new SituationEntryDto(new CellDto(0, 4), List.of(
                        new PossibleMoveDto(new CellDto(1, 3), null, CellState.CAN_BE_FILLED),
                        new PossibleMoveDto(new CellDto(1, 5), null, CellState.CAN_BE_FILLED)
                ))
        );
        
        CreateGameResponse response = assertDoesNotThrow(() -> gameService.createGame(request));
        
        assertAll(
                () -> assertNotNull(response.getId(), "id"),
                () -> assertEquals(expectedSituation, response.getSituation(), "situation"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whoseTurn"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status")
        );
    }
    
    
    @Test
    void testCreateGameNoSuchCell() {
        List<FullCellDto> cells = List.of(
                new FullCellDto(0, 0, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(0, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(7, 5, CellState.DEFAULT, Checker.BLACK)
        );
        CreateGameRequest request = new CreateGameRequest(cells, null);
        
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.createGame(request));
        
        assertAll(
                () -> assertEquals(NO_SUCH_CELL, e.getErrorCode()),
                () -> assertEquals(cells.get(1).toString(), e.getReason())
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListEvenNoKills() {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 3, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 4, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(5, 3, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                "1. e3-f4 d6-c5",
                "2. d2-e3 c5-b4"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        List<FullCellDto> expectedBoard = List.of(
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 4, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(3, 1, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(3, 5, CellState.DEFAULT, Checker.WHITE)
        );
        List<SituationEntryDto> expectedSituation = List.of(
                new SituationEntryDto(new CellDto(2, 2), List.of(
                        new PossibleMoveDto(new CellDto(4, 0), new CellDto(3, 1), CellState.MUST_BE_FILLED)
                ))
        );
        
        CreateGameResponse response = assertDoesNotThrow(() -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertNotNull(response.getId(), "id"),
                () -> assertEquals(expectedBoard, response.getBoard(), "board"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whoseTurn"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(expectedSituation, response.getSituation(), "situation"),
                () -> assertEquals(moveList, response.getMoveList(), "moveList")
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListOddNoKills() {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 3, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 4, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(5, 3, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                "1. e3-f4 d6-c5",
                "2. d2-e3"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        List<FullCellDto> expectedBoard = List.of(
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 4, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(3, 5, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(4, 2, CellState.DEFAULT, Checker.BLACK)
        );
        List<SituationEntryDto> expectedSituation = List.of(
                new SituationEntryDto(new CellDto(4, 2), List.of(
                        new PossibleMoveDto(new CellDto(3, 1), null, CellState.CAN_BE_FILLED),
                        new PossibleMoveDto(new CellDto(3, 3), null, CellState.CAN_BE_FILLED)
                ))
        );
        
        CreateGameResponse response = assertDoesNotThrow(() -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertNotNull(response.getId(), "id"),
                () -> assertEquals(expectedBoard, response.getBoard(), "board"),
                () -> assertEquals(Team.BLACK, response.getWhoseTurn(), "whoseTurn"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(expectedSituation, response.getSituation(), "situation"),
                () -> assertEquals(moveList, response.getMoveList(), "moveList")
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListEven() {
        List<FullCellDto> board = List.of(
                new FullCellDto(0, 0, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 4, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(5, 5, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(5, 7, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                "1. e3-f4 f6-g5",
                "2. c3-d4 g5:e3:c5"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        List<FullCellDto> expectedBoard = List.of(
                new FullCellDto(0, 0, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(4, 2, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(5, 7, CellState.DEFAULT, Checker.BLACK)
        );
        List<SituationEntryDto> expectedSituation = List.of(
                new SituationEntryDto(new CellDto(0, 0), List.of(
                        new PossibleMoveDto(new CellDto(1, 1), null, CellState.CAN_BE_FILLED)
                ))
        );
        
        CreateGameResponse response = assertDoesNotThrow(() -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertNotNull(response.getId(), "id"),
                () -> assertEquals(expectedBoard, response.getBoard(), "board"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whoseTurn"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(expectedSituation, response.getSituation(), "situation"),
                () -> assertEquals(moveList, response.getMoveList(), "moveList")
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListOdd() {
        List<FullCellDto> board = List.of(
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 6, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(5, 5, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(5, 7, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                "1. g3-h4 h6-g5",
                "2. c3-d4 f6-e5",
                "3. h4:f6"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        List<FullCellDto> expectedBoard = List.of(
                new FullCellDto(3, 3, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(4, 4, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(5, 5, CellState.DEFAULT, Checker.WHITE)
        );
        Set<PossibleMoveDto> expectedSituationMovesFrom44 = Set.of(
                new PossibleMoveDto(new CellDto(2, 2), new CellDto(3, 3), CellState.MUST_BE_FILLED),
                new PossibleMoveDto(new CellDto(6, 6), new CellDto(5, 5), CellState.MUST_BE_FILLED)
        );
        
        CreateGameResponse response = assertDoesNotThrow(() -> gameService.createGameFromMoveList(request));
        List<SituationEntryDto> responseSituation = response.getSituation();
        
        assertAll(
                () -> assertNotNull(response.getId(), "id"),
                () -> assertEquals(expectedBoard, response.getBoard(), "board"),
                () -> assertEquals(Team.BLACK, response.getWhoseTurn(), "whoseTurn"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(1, responseSituation.size(), "situation size"),
                () -> assertEquals(new CellDto(4, 4), responseSituation.get(0).getFrom(), "situation from"),
                () -> assertEquals(expectedSituationMovesFrom44, Set.copyOf(responseSituation.get(0).getMoves()),
                        "situation moves from (4; 4)"),
                () -> assertEquals(moveList, response.getMoveList(), "moveList")
        );
    }
    
    
    @Test
    void testCreateGameFromMoveList1StepFinish() {
        List<FullCellDto> board = List.of(
                new FullCellDto(0, 0, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                "1. a1:c3"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        List<FullCellDto> expectedBoard = List.of(
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.WHITE)
        );
        List<SituationEntryDto> expectedSituation = List.of();
        
        CreateGameResponse response = assertDoesNotThrow(() -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertNotNull(response.getId(), "id"),
                () -> assertEquals(expectedBoard, response.getBoard(), "board"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whoseTurn"),
                () -> assertEquals(Status.OVER, response.getStatus(), "status"),
                () -> assertEquals(expectedSituation, response.getSituation(), "situation"),
                () -> assertEquals(moveList, response.getMoveList(), "moveList")
        );
    }
    
    
    @Test
    void testCreateGameFromMoveList2StepFinish() {
        List<FullCellDto> board = List.of(
                new FullCellDto(0, 0, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                "1. a1-b2 c3:a1"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        List<FullCellDto> expectedBoard = List.of(
                new FullCellDto(0, 0, CellState.DEFAULT, Checker.BLACK_KING)
        );
        List<SituationEntryDto> expectedSituation = List.of();
        
        CreateGameResponse response = assertDoesNotThrow(() -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertNotNull(response.getId(), "id"),
                () -> assertEquals(expectedBoard, response.getBoard(), "board"),
                () -> assertEquals(Team.BLACK, response.getWhoseTurn(), "whoseTurn"),
                () -> assertEquals(Status.OVER, response.getStatus(), "status"),
                () -> assertEquals(expectedSituation, response.getSituation(), "situation"),
                () -> assertEquals(moveList, response.getMoveList(), "moveList")
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListBecomeKingAndContinue() {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(1, 3, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 0, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(3, 7, CellState.DEFAULT, Checker.WHITE)
        );
        List<String> moveList = List.of(
                "1. h4-g5 a3:c1:e3:h6"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        List<FullCellDto> expectedBoard = List.of(
                new FullCellDto(5, 7, CellState.DEFAULT, Checker.BLACK_KING)
        );
        List<SituationEntryDto> expectedSituation = List.of();
        
        CreateGameResponse response = assertDoesNotThrow(() -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertNotNull(response.getId(), "id"),
                () -> assertEquals(expectedBoard, response.getBoard(), "board"),
                () -> assertEquals(Team.BLACK, response.getWhoseTurn(), "whoseTurn"),
                () -> assertEquals(Status.OVER, response.getStatus(), "status"),
                () -> assertEquals(expectedSituation, response.getSituation(), "situation"),
                () -> assertEquals(moveList, response.getMoveList(), "moveList")
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListEmptyLinesAndSpaces() {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(7, 7, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                " \t",
                "     1.     \t    b2-a3        ",
                ""
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        List<FullCellDto> expectedBoard = List.of(
                new FullCellDto(2, 0, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(7, 7, CellState.DEFAULT, Checker.BLACK)
        );
        List<SituationEntryDto> expectedSituation = List.of(
                new SituationEntryDto(new CellDto(7, 7), List.of(
                        new PossibleMoveDto(new CellDto(6, 6), null, CellState.CAN_BE_FILLED)
                ))
        );
        
        CreateGameResponse response = assertDoesNotThrow(() -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertNotNull(response.getId(), "id"),
                () -> assertEquals(expectedBoard, response.getBoard(), "board"),
                () -> assertEquals(Team.BLACK, response.getWhoseTurn(), "whoseTurn"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(expectedSituation, response.getSituation(), "situation"),
                () -> assertEquals(List.of("1. b2-a3"), response.getMoveList(), "moveList")
        );
    }
    
    
    @Test
    void CreateGameFromMoveListExtraneousSymbols() {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 6, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                "1. b2-a3 g3-f2",
                "2. a3-b4 fsda"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertEquals(PARSING_ERROR, e.getErrorCode()),
                () -> assertEquals(moveList.get(1), e.getReason())
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListMoveAfter1StepMove1() {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 6, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                "1. b2-a3 g3-f2",
                "2. a3-b4",
                "3. b4-c5"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertEquals(PARSING_ERROR, e.getErrorCode()),
                () -> assertEquals(moveList.get(1), e.getReason())
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListMoveAfter1StepMove2() {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 6, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                "1. b2-a3 g3-f2",
                "2. a3-b4",
                "3. f2-g1"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertEquals(PARSING_ERROR, e.getErrorCode()),
                () -> assertEquals(moveList.get(1), e.getReason())
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListMoveWhenFinished() {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(4, 2, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                "1. b2-c3 c5-b4",
                "2. c3:a5 b4-a3"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertEquals(PARSING_ERROR, e.getErrorCode()),
                () -> assertEquals(moveList.get(1), e.getReason())
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListImpossibleStep1() {
        List<FullCellDto> board = List.of(
                new FullCellDto(2, 0, CellState.DEFAULT, Checker.WHITE)
        );
        List<String> moveList = List.of(
                "1. a3-a4"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertEquals(PARSING_ERROR, e.getErrorCode()),
                () -> assertEquals(moveList.get(0), e.getReason())
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListImpossibleStep2() {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(4, 2, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                "1. b2-c3 c5-b4",
                "2. c3-d4"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertEquals(PARSING_ERROR, e.getErrorCode()),
                () -> assertEquals(moveList.get(1), e.getReason())
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListHaveKilledMismatch1() {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(3, 3, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                "1. b2:c3"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertEquals(PARSING_ERROR, e.getErrorCode()),
                () -> assertEquals(moveList.get(0), e.getReason())
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListHaveKilledMismatch2() {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(3, 3, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                "1. b2-c3 d4-b2"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertEquals(PARSING_ERROR, e.getErrorCode()),
                () -> assertEquals(moveList.get(0), e.getReason())
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListUnfinishedMove() {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 3, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(3, 5, CellState.DEFAULT, Checker.BLACK)
        );
        List<String> moveList = List.of(
                "1. b2-e3 f4:d2"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertEquals(PARSING_ERROR, e.getErrorCode()),
                () -> assertEquals(moveList.get(0), e.getReason())
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListStepToNowhere1() {
        List<FullCellDto> board = List.of(
                new FullCellDto(2, 0, CellState.DEFAULT, Checker.WHITE)
        );
        List<String> moveList = List.of(
                "1. a3-z4"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertEquals(PARSING_ERROR, e.getErrorCode()),
                () -> assertEquals(moveList.get(0), e.getReason())
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListStepToNowhere2() {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 7, CellState.DEFAULT, Checker.WHITE)
        );
        List<String> moveList = List.of(
                "1. h2-i3"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertEquals(PARSING_ERROR, e.getErrorCode()),
                () -> assertEquals(moveList.get(0), e.getReason())
        );
    }
    
    
    @Test
    void testCreateGameFromMoveListStepToNowhere3() {
        List<FullCellDto> board = List.of(
                new FullCellDto(7, 1, CellState.DEFAULT, Checker.WHITE)
        );
        List<String> moveList = List.of(
                "1. b8-a9"
        );
        CreateGameRequest request = new CreateGameRequest(board, moveList);
        
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.createGameFromMoveList(request));
        
        assertAll(
                () -> assertEquals(PARSING_ERROR, e.getErrorCode()),
                () -> assertEquals(moveList.get(0), e.getReason())
        );
    }
    
    
    @Test
    void testSurrenderAfterCreation() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(3, 5, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        
        EditStateResponse response = assertDoesNotThrow(() ->
                gameService.surrender(gameId, new ChangeStatusRequest(Status.OVER)));
        
        assertAll(
                () -> assertEquals(0, response.getChangedCells().size(), "changed cells size"),
                () -> assertEquals(0, response.getSituation().size(), "situation size"),
                () -> assertEquals(Status.OVER, response.getStatus(), "status"),
                () -> assertEquals(Team.BLACK, response.getWhoseTurn(), "whose turn")
        );
    }
    
    
    @Test
    void testSurrenderInMove() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(3, 5, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        gameService.makeStep(gameId, new StepDto(new CellDto(1, 1), new CellDto(2, 0)));
        
        Set<FullCellDto> expectedChangedCells = Set.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 0, CellState.DEFAULT, null)
        );
        
        EditStateResponse response = assertDoesNotThrow(() ->
                gameService.surrender(gameId, new ChangeStatusRequest(Status.OVER)));
        
        assertAll(
                () -> assertEquals(expectedChangedCells, Set.copyOf(response.getChangedCells()), "changed cells"),
                () -> assertEquals(0, response.getSituation().size(), "situation size"),
                () -> assertEquals(Status.OVER, response.getStatus(), "status"),
                () -> assertEquals(Team.BLACK, response.getWhoseTurn(), "whose turn")
        );
    }
    
    
    @Test
    void testSurrenderAfterMove() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(3, 5, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        gameService.makeStep(gameId, new StepDto(new CellDto(1, 1), new CellDto(2, 0)));
        gameService.applyCurrentMove(gameId);
        
        EditStateResponse response = assertDoesNotThrow(() ->
                gameService.surrender(gameId, new ChangeStatusRequest(Status.OVER)));
        
        assertAll(
                () -> assertEquals(0, response.getChangedCells().size(), "changed cells size"),
                () -> assertEquals(0, response.getSituation().size(), "situation size"),
                () -> assertEquals(Status.OVER, response.getStatus(), "status"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whose turn")
        );
    }
    
    
    @Test
    void testSurrenderIncorrectStatus() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(3, 5, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        
        CheckersException e = assertThrows(CheckersException.class, () ->
                gameService.surrender(gameId, new ChangeStatusRequest(Status.RUNNING)));
        
        assertAll(
                () -> assertEquals(INCORRECT_STATUS, e.getErrorCode()),
                () -> assertEquals(Status.RUNNING.toString(), e.getReason())
        );
    }
    
    
    @Test
    void testSurrenderGameOver() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        gameService.makeStep(gameId, new StepDto(new CellDto(1, 1), new CellDto(3, 3)));
        gameService.applyCurrentMove(gameId);
        
        CheckersException e = assertThrows(CheckersException.class, () ->
                gameService.surrender(gameId, new ChangeStatusRequest(Status.OVER)));
        
        assertAll(
                () -> assertEquals(GAME_OVER, e.getErrorCode()),
                () -> assertNull(e.getReason())
        );
    }
    
    
    @Test
    void testMakeStepSilent() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(3, 3, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        
        Set<FullCellDto> expectedChangedCells = Set.of(
                new FullCellDto(1, 1, CellState.DEFAULT, null),
                new FullCellDto(2, 0, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, null)
        );
        
        EditStateResponse response = assertDoesNotThrow(() ->
                gameService.makeStep(gameId, new StepDto(new CellDto(1, 1), new CellDto(2, 0))));
        
        assertAll(
                () -> assertEquals(expectedChangedCells, Set.copyOf(response.getChangedCells()), "changed cells"),
                () -> assertEquals(0, response.getSituation().size(), "situation size"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whose turn")
        );
    }
    
    
    @Test
    void testMakeStepKill() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        
        Set<FullCellDto> expectedChangedCells = Set.of(
                new FullCellDto(1, 1, CellState.DEFAULT, null),
                new FullCellDto(2, 2, CellState.KILLED, Checker.BLACK),
                new FullCellDto(3, 3, CellState.DEFAULT, Checker.WHITE)
        );
        
        EditStateResponse response = assertDoesNotThrow(() ->
                gameService.makeStep(gameId, new StepDto(new CellDto(1, 1), new CellDto(3, 3))));
        
        assertAll(
                () -> assertEquals(expectedChangedCells, Set.copyOf(response.getChangedCells()), "changed cells"),
                () -> assertEquals(0, response.getSituation().size(), "situation size"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whose turn")
        );
    }
    
    
    @Test
    void testMakeStepKillBackwards() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.BLACK_KING),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.WHITE)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        
        Set<FullCellDto> expectedChangedCells = Set.of(
                new FullCellDto(0, 0, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(1, 1, CellState.KILLED, Checker.BLACK_KING),
                new FullCellDto(2, 2, CellState.DEFAULT, null)
        );
        
        EditStateResponse response = assertDoesNotThrow(() ->
                gameService.makeStep(gameId, new StepDto(new CellDto(2, 2), new CellDto(0, 0))));
        
        assertAll(
                () -> assertEquals(expectedChangedCells, Set.copyOf(response.getChangedCells()), "changed cells"),
                () -> assertEquals(0, response.getSituation().size(), "situation size"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whose turn")
        );
    }
    
    
    @Test
    void testMakeStepKillCanContinue() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(0, 0, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(3, 1, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        
        Set<FullCellDto> expectedChangedCells = Set.of(
                new FullCellDto(0, 0, CellState.DEFAULT, null),
                new FullCellDto(1, 1, CellState.KILLED, Checker.BLACK),
                new FullCellDto(2, 2, CellState.PROMPT, Checker.WHITE),
                new FullCellDto(4, 0, CellState.MUST_BE_FILLED, null)
        );
        List<SituationEntryDto> expectedSituation = List.of(
                new SituationEntryDto(new CellDto(2, 2), List.of(
                        new PossibleMoveDto(new CellDto(4, 0), new CellDto(3, 1), CellState.MUST_BE_FILLED)
                ))
        );
        
        EditStateResponse response = assertDoesNotThrow(() ->
                gameService.makeStep(gameId, new StepDto(new CellDto(0, 0), new CellDto(2, 2))));
        
        assertAll(
                () -> assertEquals(expectedChangedCells, Set.copyOf(response.getChangedCells()), "changed cells"),
                () -> assertEquals(expectedSituation, response.getSituation(), "situation"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whose turn")
        );
    }
    
    
    @Test
    void testMakeStepKillContinued() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(0, 0, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(3, 1, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        
        Set<FullCellDto> expectedChangedCells = Set.of(
                new FullCellDto(2, 2, CellState.DEFAULT, null),
                new FullCellDto(3, 1, CellState.KILLED, Checker.BLACK),
                new FullCellDto(4, 0, CellState.DEFAULT, Checker.WHITE)
        );
        
        assertDoesNotThrow(() ->
                gameService.makeStep(gameId, new StepDto(new CellDto(0, 0), new CellDto(2, 2))));
        EditStateResponse response = assertDoesNotThrow(() ->
                gameService.makeStep(gameId, new StepDto(new CellDto(2, 2), new CellDto(4, 0))));
        
        assertAll(
                () -> assertEquals(expectedChangedCells, Set.copyOf(response.getChangedCells()), "changed cells"),
                () -> assertEquals(0, response.getSituation().size(), "situation size"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whose turn")
        );
    }
    
    
    @Test
    void testMakeStepSilentBecomeKing() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(6, 0, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(3, 3, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        
        Set<FullCellDto> expectedChangedCells = Set.of(
                new FullCellDto(6, 0, CellState.DEFAULT, null),
                new FullCellDto(7, 1, CellState.DEFAULT, Checker.WHITE_KING)
        );
        
        EditStateResponse response = assertDoesNotThrow(() ->
                gameService.makeStep(gameId, new StepDto(new CellDto(6, 0), new CellDto(7, 1))));
        
        assertAll(
                () -> assertEquals(expectedChangedCells, Set.copyOf(response.getChangedCells()), "changed cells"),
                () -> assertEquals(0, response.getSituation().size(), "situation size"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whose turn")
        );
    }
    
    
    @Test
    void testMakeStepKillBecomeKing() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(5, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(6, 2, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        
        Set<FullCellDto> expectedChangedCells = Set.of(
                new FullCellDto(5, 1, CellState.DEFAULT, null),
                new FullCellDto(6, 2, CellState.KILLED, Checker.BLACK),
                new FullCellDto(7, 3, CellState.DEFAULT, Checker.WHITE_KING)
        );
        
        EditStateResponse response = assertDoesNotThrow(() ->
                gameService.makeStep(gameId, new StepDto(new CellDto(5, 1), new CellDto(7, 3))));
        
        assertAll(
                () -> assertEquals(expectedChangedCells, Set.copyOf(response.getChangedCells()), "changed cells"),
                () -> assertEquals(0, response.getSituation().size(), "situation size"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whose turn")
        );
    }
    
    
    @Test
    void testMakeStepKillBecomeKingCanContinue() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(5, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(6, 2, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(5, 5, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        
        Set<FullCellDto> expectedChangedCells = Set.of(
                new FullCellDto(3, 7, CellState.MUST_BE_FILLED, null),
                new FullCellDto(4, 6, CellState.MUST_BE_FILLED, null),
                new FullCellDto(5, 1, CellState.DEFAULT, null),
                new FullCellDto(6, 2, CellState.KILLED, Checker.BLACK),
                new FullCellDto(7, 3, CellState.PROMPT, Checker.WHITE_KING)
        );
        Set<PossibleMoveDto> expectedSituationMovesFrom73 = Set.of(
                new PossibleMoveDto(new CellDto(3, 7), new CellDto(5, 5), CellState.MUST_BE_FILLED),
                new PossibleMoveDto(new CellDto(4, 6), new CellDto(5, 5), CellState.MUST_BE_FILLED)
        );
        
        EditStateResponse response = assertDoesNotThrow(() ->
                gameService.makeStep(gameId, new StepDto(new CellDto(5, 1), new CellDto(7, 3))));
        List<SituationEntryDto> responseSituation = response.getSituation();
        
        assertAll(
                () -> assertEquals(expectedChangedCells, Set.copyOf(response.getChangedCells()), "changed cells"),
                () -> assertEquals(1, responseSituation.size(), "situation size"),
                () -> assertEquals(new CellDto(7, 3), responseSituation.get(0).getFrom(), "situation from"),
                () -> assertEquals(expectedSituationMovesFrom73, Set.copyOf(responseSituation.get(0).getMoves()),
                        "situation moves from (7; 3)"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whose turn")
        );
    }
    
    
    @Test
    void testMakeStepKillBecomeKingContinued() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(5, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(6, 2, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(5, 5, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        
        Set<FullCellDto> expectedChangedCells = Set.of(
                new FullCellDto(3, 7, CellState.DEFAULT, Checker.WHITE_KING),
                new FullCellDto(4, 6, CellState.DEFAULT, null),
                new FullCellDto(5, 5, CellState.KILLED, Checker.BLACK),
                new FullCellDto(7, 3, CellState.DEFAULT, null)
        );
        
        assertDoesNotThrow(() ->
                gameService.makeStep(gameId, new StepDto(new CellDto(5, 1), new CellDto(7, 3))));
        EditStateResponse response = assertDoesNotThrow(() ->
                gameService.makeStep(gameId, new StepDto(new CellDto(7, 3), new CellDto(3, 7))));
        
        assertAll(
                () -> assertEquals(expectedChangedCells, Set.copyOf(response.getChangedCells()), "changed cells"),
                () -> assertEquals(0, response.getSituation().size(), "situation size"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whose turn")
        );
    }
    
    
    @Test
    void testMakeStepGameOver() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        gameService.surrender(gameId, new ChangeStatusRequest(Status.OVER));
        
        CheckersException e = assertThrows(CheckersException.class, () ->
                gameService.makeStep(gameId, new StepDto(new CellDto(2, 2), new CellDto(0, 0))));
        
        assertAll(
                () -> assertEquals(GAME_OVER, e.getErrorCode()),
                () -> assertNull(e.getReason())
        );
    }
    
    
    @Test
    void testMakeStepImpossibleStep() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        
        CheckersException e = assertThrows(CheckersException.class, () ->
                gameService.makeStep(gameId, new StepDto(new CellDto(1, 1), new CellDto(2, 0))));
        
        assertAll(
                () -> assertEquals(IMPOSSIBLE_STEP, e.getErrorCode()),
                () -> assertEquals(
                        new Step(
                                new Cell(1, 1, CellState.PROMPT, Checker.WHITE),
                                new Cell(2, 0, CellState.DEFAULT, null)
                        ).toString(), e.getReason())
        );
    }
    
    
    @Test
    void testMakeStepImpossibleStepDoubleKillAttempt() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(2, 4, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(2, 6, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(4, 4, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(4, 6, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        
        gameService.makeStep(gameId, new StepDto(new CellDto(1, 1), new CellDto(3, 3)));
        gameService.makeStep(gameId, new StepDto(new CellDto(3, 3), new CellDto(1, 5)));
        gameService.makeStep(gameId, new StepDto(new CellDto(1, 5), new CellDto(3, 7)));
        gameService.makeStep(gameId, new StepDto(new CellDto(3, 7), new CellDto(5, 5)));
        gameService.makeStep(gameId, new StepDto(new CellDto(5, 5), new CellDto(3, 3)));
        CheckersException e = assertThrows(CheckersException.class, () ->
                gameService.makeStep(gameId, new StepDto(new CellDto(3, 3), new CellDto(1, 1))));
        
        assertAll(
                () -> assertEquals(IMPOSSIBLE_STEP, e.getErrorCode()),
                () -> assertEquals(
                        new Step(
                                new Cell(3, 3, CellState.DEFAULT, Checker.WHITE),
                                new Cell(1, 1, CellState.DEFAULT, null)
                        ).toString(), e.getReason())
        );
    }
    
    
    @Test
    void testApplyCurrentMove() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(3, 5, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(4, 2, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(4, 4, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        gameService.makeStep(gameId, new StepDto(new CellDto(1, 1), new CellDto(3, 3)));
        gameService.makeStep(gameId, new StepDto(new CellDto(3, 3), new CellDto(5, 1)));
        
        Set<FullCellDto> expectedChangedCells = Set.of(
                new FullCellDto(2, 2, CellState.DEFAULT, null),
                new FullCellDto(4, 2, CellState.DEFAULT, null)
        );
        List<SituationEntryDto> expectedSituation = List.of(
                new SituationEntryDto(new CellDto(4, 4), List.of(
                        new PossibleMoveDto(new CellDto(2, 6), new CellDto(3, 5), CellState.MUST_BE_FILLED)
                ))
        );
        
        ApplyCurrentMoveResponse response = gameService.applyCurrentMove(gameId);
        
        assertAll(
                () -> assertEquals(expectedChangedCells, Set.copyOf(response.getChangedCells()), "changed cells"),
                () -> assertEquals(expectedSituation, response.getSituation(), "situation"),
                () -> assertEquals(Status.RUNNING, response.getStatus(), "status"),
                () -> assertEquals(Team.BLACK, response.getWhoseTurn(), "whose turn"),
                () -> assertEquals("b2:d4:b6", response.getLastMove(), "last move")
        );
    }
    
    
    @Test
    void testApplyCurrentMoveAndWin() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(4, 2, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        gameService.makeStep(gameId, new StepDto(new CellDto(1, 1), new CellDto(3, 3)));
        gameService.makeStep(gameId, new StepDto(new CellDto(3, 3), new CellDto(5, 1)));
        
        Set<FullCellDto> expectedChangedCells = Set.of(
                new FullCellDto(2, 2, CellState.DEFAULT, null),
                new FullCellDto(4, 2, CellState.DEFAULT, null)
        );
        
        ApplyCurrentMoveResponse response = gameService.applyCurrentMove(gameId);
        
        assertAll(
                () -> assertEquals(expectedChangedCells, Set.copyOf(response.getChangedCells()), "changed cells"),
                () -> assertEquals(0, response.getSituation().size(), "situation size"),
                () -> assertEquals(Status.OVER, response.getStatus(), "status"),
                () -> assertEquals(Team.WHITE, response.getWhoseTurn(), "whose turn"),
                () -> assertEquals("b2:d4:b6", response.getLastMove(), "last move")
        );
    }
    
    
    @Test
    void testApplyCurrentMoveNotFinished() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(4, 2, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        gameService.makeStep(gameId, new StepDto(new CellDto(1, 1), new CellDto(3, 3)));
        
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.applyCurrentMove(gameId));
        
        assertAll(
                () -> assertEquals(MOVE_NOT_FINISHED, e.getErrorCode()),
                () -> assertNull(e.getReason())
        );
    }
    
    
    @Test
    void testApplyCurrentMoveGameOver() throws CheckersException {
        List<FullCellDto> board = List.of(
                new FullCellDto(1, 1, CellState.DEFAULT, Checker.WHITE),
                new FullCellDto(2, 2, CellState.DEFAULT, Checker.BLACK),
                new FullCellDto(4, 2, CellState.DEFAULT, Checker.BLACK)
        );
        String gameId = gameService.createGame(new CreateGameRequest(board, null)).getId();
        gameService.makeStep(gameId, new StepDto(new CellDto(1, 1), new CellDto(3, 3)));
        gameService.makeStep(gameId, new StepDto(new CellDto(3, 3), new CellDto(5, 1)));
        gameService.surrender(gameId, new ChangeStatusRequest(Status.OVER));
    
        CheckersException e = assertThrows(CheckersException.class, () -> gameService.applyCurrentMove(gameId));
    
        assertAll(
                () -> assertEquals(GAME_OVER, e.getErrorCode()),
                () -> assertNull(e.getReason())
        );
    }
}
