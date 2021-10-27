const BOARD_SIZE = 8
const BOARD = Array.from(Array(BOARD_SIZE), () => Array(BOARD_SIZE))
const BOARD_BACKUP = Array.from(Array(BOARD_SIZE), () => Array(BOARD_SIZE))
let BOARD_VIEW
let SITUATION = new Map()
let inPromptMode = null
let movedFrom = null
let movedTo = null
let becomeKing = false
let killed = []
let whoseTurn = 'w'

const CELL_STATE = {
    DEFAULT: 0,
    PROMPT: 1,
    CAN_BE_FILLED: 2,
    MUST_BE_FILLED: 3,
    KILLED: 4
}
const CELL_STATE_CLASS = {
    [CELL_STATE.PROMPT]: 'prompt',
    [CELL_STATE.CAN_BE_FILLED]: 'can-be-filled',
    [CELL_STATE.MUST_BE_FILLED]: 'must-be-filled',
    [CELL_STATE.KILLED]: 'killed'
}
const CHECKER_TYPE = {
    BLACK: -1,
    BLACK_KING: -2,
    WHITE: 1,
    WHITE_KING: 2
}
const CHECKER_PIC = {
    [CHECKER_TYPE.BLACK]: '../img/black-checker.svg',
    [CHECKER_TYPE.BLACK_KING]: '../img/black-checker-king.svg',
    [CHECKER_TYPE.WHITE]: '../img/white-checker.svg',
    [CHECKER_TYPE.WHITE_KING]: '../img/white-checker-king.svg'
}
const CHECKER_COLOR = {
    [CHECKER_TYPE.BLACK]: 'b',
    [CHECKER_TYPE.BLACK_KING]: 'b',
    [CHECKER_TYPE.WHITE]: 'w',
    [CHECKER_TYPE.WHITE_KING]: 'w'
}

const statusStr = document.getElementById('status')
const startButton = document.getElementById('start')
const example1button = document.getElementById('example1')
const cancelTurnButton = document.getElementById('cancel-turn')
const finishTurnButton = document.getElementById('finish-turn')
const moveList = document.getElementById('move-list')


const isPlayCell = (row, col) => (row + col) % 2 === 0


const hasChecker = (row, col) => BOARD[row][col]?.checker != null


const renderChecker = (row, col) => {
    const checker = BOARD[row][col].checker

    BOARD_VIEW[row][col].innerHTML = checker == null? '' : '<img src="' + CHECKER_PIC[checker.type] + '">'
}


const place = (type, row, col) => {
    const cell = BOARD[row][col]

    cell.checker = {type: type, cell: cell}
}


const clear = (row, col) => {
    BOARD[row][col].checker = null
}


const move = (rowFrom, colFrom, rowTo, colTo) => {
    const type = BOARD[rowFrom][colFrom].checker.type

    clear(rowFrom, colFrom)
    place(type, rowTo, colTo)
}


const renderCell = (row, col) => {
    if (!isPlayCell(row, col))
        return

    const state = BOARD[row][col].state

    if (state === CELL_STATE.DEFAULT)
        BOARD_VIEW[row][col].removeAttribute('class')
    else
        BOARD_VIEW[row][col].className = CELL_STATE_CLASS[state]

    renderChecker(row, col)
}


const renderBoard = () => {
    for (let row = 0; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            renderCell(row, col)
}


const toggleTurn = () => {
    whoseTurn = whoseTurn === 'w'? 'b' : 'w'

    calculateSituation()
}


const renderTurn = () => {
    statusStr.innerText = whoseTurn === 'w'?
        'Ходят белые' :
        'Ходят чёрные'
}


const isWhite = (row, col) => {
    const type = BOARD[row][col]?.checker?.type

    return type == CHECKER_TYPE.WHITE || type == CHECKER_TYPE.WHITE_KING
}


const isBlack = (row, col) => {
    const type = BOARD[row][col]?.checker?.type

    return type === CHECKER_TYPE.BLACK || type === CHECKER_TYPE.BLACK_KING
}


const isTurnOf = (row, col) => {
    if (!hasChecker(row, col))
        return false

    const type = BOARD[row][col].checker.type

    return (whoseTurn === 'w' && (type === CHECKER_TYPE.WHITE || type === CHECKER_TYPE.WHITE_KING)) ||
        (whoseTurn === 'b' && (type === CHECKER_TYPE.BLACK || type === CHECKER_TYPE.BLACK_KING))
}


const addToSituation = (cell, dest, state, foe) => {
    let dests = SITUATION.get(cell)
    const newDest = {dest: dest, state: state, foe: foe}

    if (dests === undefined)
        SITUATION.set(cell, [newDest])

    else
        dests.push(newDest)
}


const areFoes = (row1, col1, row2, col2) => {
    const color1 = CHECKER_COLOR[BOARD[row1][col1]?.checker?.type]
    const color2 = CHECKER_COLOR[BOARD[row2][col2]?.checker?.type]

    return color1 != null && color2 != null && color1 !== color2
}


const iterator = (row, col, rowDir, colDir) => {
    return {
        next: () => {
            row += rowDir
            col += colDir

            return (row > -1 && row < BOARD_SIZE && col > -1 && col < BOARD_SIZE)?
                {value: {row: row, col: col}, done: false} :
                {done: true}
        }
    }
}


const calculateSituation = () => {
    SITUATION.clear()

    let foundMustBeFilled = false

    for (let row = 0; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++) {
            if (!isTurnOf(row, col))
                continue
            
            const type = BOARD[row][col].checker.type

            if (type === CHECKER_TYPE.WHITE_KING || type === CHECKER_TYPE.BLACK_KING)
                for (it of [iterator(row, col, 1, -1), iterator(row, col, 1, 1), iterator(row, col, -1, 1), iterator(row, col, -1, -1)]) {
                    let res = it.next()
                    let foe = null

                    while (!res.done) {
                        let {row: rowTo, col: colTo} = res.value

                        if (!hasChecker(rowTo, colTo)) {
                            if (foe !== null) {
                                addToSituation(BOARD[row][col], BOARD[rowTo][colTo], CELL_STATE.MUST_BE_FILLED, foe)
                                foundMustBeFilled = true
                                break
                            }

                            else if (!foundMustBeFilled)
                                addToSituation(BOARD[row][col], BOARD[rowTo][colTo], CELL_STATE.CAN_BE_FILLED)
                        }

                        else if (areFoes(row, col, rowTo, colTo))
                            foe = BOARD[rowTo][colTo]

                        else
                            break

                        res = it.next()
                    } 
                }

            else {
                if (row < BOARD_SIZE - 2) {
                    if (col > 1 && areFoes(row, col, row + 1, col - 1) && !hasChecker(row + 2, col - 2)) {
                        addToSituation(BOARD[row][col], BOARD[row + 2][col - 2], CELL_STATE.MUST_BE_FILLED, BOARD[row + 1][col - 1])
                        foundMustBeFilled = true
                    }

                    if (col < BOARD_SIZE - 2 && areFoes(row, col, row + 1, col + 1) && !hasChecker(row + 2, col + 2)) {
                        addToSituation(BOARD[row][col], BOARD[row + 2][col + 2], CELL_STATE.MUST_BE_FILLED, BOARD[row + 1][col + 1])
                        foundMustBeFilled = true
                    }
                }

                if (row > 1) {
                    if (col > 1 && areFoes(row, col, row - 1, col - 1) && !hasChecker(row - 2, col - 2)) {
                        addToSituation(BOARD[row][col], BOARD[row - 2][col - 2], CELL_STATE.MUST_BE_FILLED, BOARD[row - 1][col - 1])
                        foundMustBeFilled = true
                    }

                    if (col < BOARD_SIZE - 2 && areFoes(row, col, row - 1, col + 1) && !hasChecker(row - 2, col + 2)) {
                        addToSituation(BOARD[row][col], BOARD[row - 2][col + 2], CELL_STATE.MUST_BE_FILLED, BOARD[row - 1][col + 1])
                        foundMustBeFilled = true
                    }
                }

                if (!foundMustBeFilled) {
                    if (isWhite(row, col) && row < BOARD_SIZE - 1) {
                        if (col > 0 && !hasChecker(row + 1, col - 1))
                            addToSituation(BOARD[row][col], BOARD[row + 1][col - 1], CELL_STATE.CAN_BE_FILLED)

                        if (col < BOARD_SIZE - 1 && !hasChecker(row + 1, col + 1))
                            addToSituation(BOARD[row][col], BOARD[row + 1][col + 1], CELL_STATE.CAN_BE_FILLED)
                    }

                    else if (isBlack(row, col) && row > 0) {
                        if (col > 0 && !hasChecker(row - 1, col - 1))
                            addToSituation(BOARD[row][col], BOARD[row - 1][col - 1], CELL_STATE.CAN_BE_FILLED)

                        if (col < BOARD_SIZE - 1 && !hasChecker(row - 1, col + 1))
                            addToSituation(BOARD[row][col], BOARD[row - 1][col + 1], CELL_STATE.CAN_BE_FILLED)
                    }
                }
            }
        }

    if (foundMustBeFilled)
        for (let entry of SITUATION) {
            const [cellFrom, cellsTo] = entry
            const filteredCellsTo = cellsTo.filter(cellTo => cellTo.state === CELL_STATE.MUST_BE_FILLED && cellTo.foe.state !== CELL_STATE.KILLED)

            if (filteredCellsTo.length === 0)
                SITUATION.delete(cellFrom)
            else
                SITUATION.set(cellFrom, filteredCellsTo)
        }
}


const togglePromptMode = cell => {
    if (!isTurnOf(cell.row, cell.col))
        return []

    const dests = SITUATION.get(cell) || []

    if (inPromptMode === cell) {
        inPromptMode = null
        cell.state = CELL_STATE.DEFAULT

        for (dest of dests)
            dest.dest.state = CELL_STATE.DEFAULT
    }

    else if (inPromptMode === null && (movedFrom === null || (killed.length !== 0 && dests.length !== 0))) {
        inPromptMode = cell
        cell.state = CELL_STATE.PROMPT

        for (dest of dests)
            dest.dest.state = dest.state
    }

    let changedCells = dests.map(dest => dest.dest)
    changedCells.push(cell)

    return changedCells
}


const cellOnClick = (row, col) => {
    let changedCells
    let targetCell = BOARD[row][col]

    if (inPromptMode === null || (!movedFrom && inPromptMode === targetCell))
        changedCells = togglePromptMode(targetCell)

    else if (targetCell.state === CELL_STATE.CAN_BE_FILLED) {
        movedFrom = inPromptMode
        changedCells = togglePromptMode(inPromptMode)
        move(movedFrom.row, movedFrom.col, row, col)
        movedTo = BOARD[row][col]
    }

    else if (targetCell.state === CELL_STATE.MUST_BE_FILLED) {
        const wasInPromptMode = inPromptMode
        changedCells = togglePromptMode(inPromptMode)
        move(wasInPromptMode.row, wasInPromptMode.col, row, col)
        movedTo = BOARD[row][col]

        if (whoseTurn === 'w' && row === BOARD_SIZE - 1) {
            targetCell.checker.type = CHECKER_TYPE.WHITE_KING
            becomeKing = true
        }

        else if (whoseTurn === 'b' && row === 0) {
            targetCell.checker.type = CHECKER_TYPE.BLACK_KING
            becomeKing = true
        }

        const killedCell = SITUATION.get(BOARD[wasInPromptMode.row][wasInPromptMode.col]).filter(dest => dest.dest.row === row && dest.dest.col === col)[0].foe
        killedCell.state = CELL_STATE.KILLED
        changedCells.push(killedCell)
        killed.push(killedCell)

        if (movedFrom === null)
            movedFrom = wasInPromptMode

        calculateSituation()
        changedCells = changedCells.concat(togglePromptMode(targetCell))
    }

    changedCells?.forEach(cell => renderCell(cell.row, cell.col))
}


const startArrangement = () => {
    whoseTurn = 'w'

    for (let row = 0; row < 3; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                place(CHECKER_TYPE.WHITE, row, col)

    for (let row = 3; row < 5; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                clear(row, col)

    for (let row = 5; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                place(CHECKER_TYPE.BLACK, row, col)
}


const example1Arrangement = () => {
    for (let row = 0; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                clear(row, col)

    place(CHECKER_TYPE.WHITE, 3, 5)
    place(CHECKER_TYPE.WHITE, 3, 7)

    place(CHECKER_TYPE.BLACK, 7, 1)
    place(CHECKER_TYPE.BLACK_KING, 0, 2)
    place(CHECKER_TYPE.BLACK, 4, 2)
    place(CHECKER_TYPE.BLACK, 6, 2)
    place(CHECKER_TYPE.BLACK, 6, 4)
    place(CHECKER_TYPE.BLACK, 5, 7)
}


const arrangementButtonOnClick = (arrangement) => {
    arrangement()
    inPromptMode = null
    calculateSituation()

    renderBoard()
    renderTurn()
    moveList.innerHTML = ''
}


const cancelTurnButtonOnClick = () => {
    if (movedFrom === null && inPromptMode === null)
        return

    const curCell = movedTo === null? inPromptMode : movedTo

    SITUATION.get(curCell)?.forEach(dest => {
        dest.dest.state = CELL_STATE.DEFAULT
        renderCell(dest.dest.row, dest.dest.col)
    })

    if (movedFrom !== null) {
        if (becomeKing) {
            movedTo.checker.type = whoseTurn === 'w'? CHECKER_TYPE.WHITE : CHECKER_TYPE.BLACK
            becomeKing = false
        }

        movedFrom.checker = movedTo.checker
        movedTo.checker = null
        movedTo.state = CELL_STATE.DEFAULT

        renderCell(movedFrom.row, movedFrom.col)
        renderCell(movedTo.row, movedTo.col)

        movedFrom = null
        movedTo = null
    }

    else {
        inPromptMode.state = CELL_STATE.DEFAULT
        renderCell(inPromptMode.row, inPromptMode.col)
    }

    inPromptMode = null

    if (killed.length !== 0) {
        for (cell of killed) {
            const {row, col} = cell
            BOARD[row][col].state = CELL_STATE.DEFAULT
            renderCell(row, col)
        }

        killed = []
        calculateSituation()
    }
}


const finishTurnButtonOnClick = () => {
    if (movedFrom === null || inPromptMode !== null)
        return

    movedFrom = null
    movedTo = null
    becomeKing = false
    
    for (cell of killed) {
        const {row, col} = cell
        clear(row, col)
        BOARD[row][col].state = CELL_STATE.DEFAULT
        renderCell(row, col)
    }

    killed = []

    toggleTurn()
    renderTurn()
}


const init = () => {
    for (let row = 0; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                BOARD[row][col] = {row: row, col: col, state: CELL_STATE.DEFAULT}

    let col = 0

    BOARD_VIEW = Array.from(document.querySelectorAll('.board tr td'))
        .reduce((arr, cell, index) => {
            const row = BOARD_SIZE - 1 - Math.floor(index / BOARD_SIZE)

            arr[row] = arr[row] || []
            arr[row].push(isPlayCell(row, col)? cell : null)

            col = (++col) % BOARD_SIZE

            return arr
        }, Array(BOARD_SIZE))

    for (let row = 0; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            BOARD_VIEW[row][col]?.addEventListener('click', () => cellOnClick(row, col))

    startButton.addEventListener('click', () => arrangementButtonOnClick(startArrangement))
    example1button.addEventListener('click', () => arrangementButtonOnClick(example1Arrangement))
    cancelTurnButton.addEventListener('click', () => cancelTurnButtonOnClick())
    finishTurnButton.addEventListener('click', () => finishTurnButtonOnClick())
}


init()