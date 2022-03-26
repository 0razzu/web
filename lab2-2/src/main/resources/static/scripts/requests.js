const post = (path, body) => {
    let params = {method: 'POST'}

    if (body)
        params = {
            ...params,
            headers: {
                'Content-Type': 'application/json',
            },
            body: body,
        }

    return fetch(ROOT + '/api/games' + path, params)
        .then(response => response.json())
}


const get = path => {
    return fetch(ROOT + '/api/games' + path, {
        method: 'GET',
    })
        .then(response => response.json())
}


const del = path => {
    return fetch(ROOT + '/api/games' + path, {
        method: 'DELETE',
    })
        .then(response => response.json())
}


const mapToSituation = situationDto => {
    SITUATION.clear()

    situationDto.forEach(({from, moves}) =>
        SITUATION.set(
            BOARD[from.row][from.col],
            moves.map(move => ({
                dest: BOARD[move.to.row][move.to.col],
                state: move.state,
                foe: move.foe? BOARD[move.foe.row][move.foe.col] : null,
            }))
        )
    )
}


const mapToBoardCell = cell => {
    const boardCell = BOARD[cell.row][cell.col]

    boardCell.state = cell.state
    boardCell.checker = cell.checker? {type: cell.checker, cell: boardCell} : null  // TODO: is «cell» necessary?

    return boardCell
}


const mapToCellList = cells => cells.map(cell => mapToBoardCell(cell))


const mapToBoard = board => board.forEach(cell =>
    cell && mapToBoardCell(cell)
)


const mapBoardToCellList = () =>
    BOARD.reduce((acc, row) => acc.concat(row), []).filter(cell => cell).map(cell => ({
        row: cell.row,
        col: cell.col,
        state: cell.state,
        checker: cell.checker?.type
    }))


const createGame = async () => {
    return post(
        '',
        JSON.stringify({
            board: mapBoardToCellList()
        })
    )
        .then(({id, situation, status, whoseTurn: turn}) => {
            GAME_ID = id
            mapToSituation(situation)
            currentStatus = status
            whoseTurn = turn
        })
}


const parseTurns = async moveList => {
    return post(
        '',
        JSON.stringify({
            board: mapBoardToCellList(),
            moveList
        })
    )
        .then(({id, board, situation, status, whoseTurn: turn, moveList: moveListPerformed, errorCode, reason}) => {
            if (errorCode)
                throw {errorCode, reason}

            GAME_ID = id
            mapToBoard(board)
            mapToSituation(situation)
            currentStatus = status
            whoseTurn = turn

            return moveListPerformed
        })
}


const makeStep = async ({from, to}) => {
    return post(
        `/${GAME_ID}/currentMove/steps`,
        JSON.stringify({
            from: {row: from.row, col: from.col},
            to: {row: to.row, col: to.col},
        })
    )
        .then(({changedCells, situation}) => {
            mapToSituation(situation)

            return mapToCellList(changedCells)
        })
}


const cancelTurn = async () => {
    return del(`/${GAME_ID}/currentMove`)
        .then(({changedCells, situation}) => {
            mapToSituation(situation)

            return mapToCellList(changedCells)
        })
}


const applyTurn = async () => {
    return post(`/${GAME_ID}/moves`)
        .then(({changedCells, situation, status, whoseTurn: turn, lastMove}) => {
            mapToSituation(situation)
            currentStatus = status
            whoseTurn = turn

            return {
                changedCells: mapToCellList(changedCells),
                lastMove
            }
        })
}


const getGames = async () => {
    return get('?statusOnly=true')
}
