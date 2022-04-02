package checkers.service;


import checkers.Properties;
import checkers.database.dao.GameDao;
import checkers.dto.request.CreateGameRequest;
import checkers.dto.response.CreateGameResponse;
import checkers.dto.response.PossibleMoveDto;
import checkers.dto.response.SituationEntryDto;
import checkers.dto.versatile.CellDto;
import checkers.dto.versatile.FullCellDto;
import checkers.error.CheckersException;
import checkers.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static checkers.error.CheckersErrorCode.NO_SUCH_CELL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TestGameService {
    private static final Properties properties = mock(Properties.class);
    private static final GameDao gameDao = mock(GameDao.class);
    private final GameService gameService = new GameService(gameDao, properties);
    
    
    @BeforeAll
    static void configureMocks() {
        when(properties.getMoveTime()).thenReturn(Integer.MAX_VALUE);
        when(gameDao.put(any(Game.class))).thenReturn("1");
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
                () -> assertEquals("1", response.getId(), "ids"),
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
}
