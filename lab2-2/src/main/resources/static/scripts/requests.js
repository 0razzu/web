const postOrPut = (method, path, body) => {
    let params = {method}

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


const post = (path, body) => postOrPut('POST', path, body)


const put = (path, body) => postOrPut('PUT', path, body)


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

    situationDto?.forEach(({from, moves}) =>
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


const throwIfError = (errorCode, reason) => {
    if (errorCode)
        throw {errorCode, reason}
}


const createGame = async () => {
    return post(
        '',
        JSON.stringify({
            board: mapBoardToCellList()
        })
    )
        .then(({id, situation, status, whoseTurn: turn, errorCode, reason}) => {
            throwIfError(errorCode, reason)

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
        .then(({id, board, situation, status, whoseTurn: turn, moveList, errorCode, reason}) => {
            throwIfError(errorCode, reason)

            GAME_ID = id
            mapToBoard(board)
            mapToSituation(situation)
            currentStatus = status
            whoseTurn = turn

            return moveList
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
        .then(({changedCells, situation, status, whoseTurn: turn, errorCode, reason}) => {
            throwIfError(errorCode, reason)

            mapToSituation(situation)
            currentStatus = status
            whoseTurn = turn

            return mapToCellList(changedCells)
        })
}


const cancelTurn = async () => {
    return del(`/${GAME_ID}/currentMove`)
        .then(({changedCells, situation, status, whoseTurn: turn, errorCode, reason}) => {
            throwIfError(errorCode, reason)

            mapToSituation(situation)
            currentStatus = status
            whoseTurn = turn

            return mapToCellList(changedCells)
        })
}


const applyTurn = async () => {
    return post(`/${GAME_ID}/moves`)
        .then(({changedCells, situation, status, whoseTurn: turn, lastMove, errorCode, reason}) => {
            throwIfError(errorCode, reason)

            mapToSituation(situation)
            currentStatus = status
            whoseTurn = turn

            return {
                changedCells: mapToCellList(changedCells),
                lastMove
            }
        })
}


const surrender = async () => {
    return put(
        `/${GAME_ID}/status`,
        JSON.stringify({status: STATUS.OVER})
    )
        .then(({changedCells, situation, status, whoseTurn: turn, errorCode, reason}) => {
            throwIfError(errorCode, reason)

            mapToSituation(situation)
            currentStatus = status
            whoseTurn = turn

            return mapToCellList(changedCells)
        })
}


const getGame = async id => {
    return get(`/${id}`)
        .then(({id, board, situation, status, whoseTurn: turn, moveList, currentMove, errorCode, reason}) => {
            throwIfError(errorCode, reason)

            GAME_ID = id
            mapToBoard(board)
            mapToSituation(situation)
            currentStatus = status
            whoseTurn = turn
            inMove = currentMove !== undefined

            return moveList
        })
}


const getGames = async () => {
    return get('?statusOnly=true')
        .then(dto => {
            throwIfError(dto.errorCode, dto.reason)

            return dto
        })
}
