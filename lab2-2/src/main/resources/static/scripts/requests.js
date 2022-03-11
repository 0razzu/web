const post = (path, body) => {
    return fetch(ROOT + '/api' + path, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: body,
    })
}


const createGame = callback => {
    post(
        '/game',
        JSON.stringify({board: BOARD.map(row => row.map(cell => cell.checker?.type))})
    )
        .then(response => response.json())
        .then(({id, situation}) => {
            GAME_ID = id
            situation.forEach(({from, moves}) =>
                SITUATION.set(
                    BOARD[from.row][from.col],
                    moves.map(move => ({
                        dest: BOARD[move.to.row][move.to.col],
                        state: move.state,
                        foe: move.foe? BOARD[move.foe.row][move.foe.col] : null,
                    }))
                )
            )

            callback?.()
        })
}
