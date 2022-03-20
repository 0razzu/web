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


const mapToCellList = cells => {
    const cellList = []

    for (const cell of cells) {
        const boardCell = BOARD[cell.row][cell.col]

        boardCell.state = cell.state
        boardCell.checker = cell.checker? {type: cell.checker, cell: boardCell} : null  // TODO: is «cell» necessary?

        cellList.push(boardCell)
    }

    return cellList
}


const createGame = async () => {
    return post(
        '',
        JSON.stringify({board: BOARD.map(row => row.map(cell => cell.checker?.type))})
    )
        .then(({id, situation, status, whoseTurn: turn}) => {
            GAME_ID = id
            mapToSituation(situation)
            currentStatus = status
            whoseTurn = turn
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
