const post = (path, body) => {
    return fetch(ROOT + '/api/games' + path, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: body,
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
        .then(({id, situation}) => {
            GAME_ID = id
            mapToSituation(situation)
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
