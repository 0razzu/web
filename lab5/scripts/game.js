const BOARD_SIZE = 8
const BOARD = Array.from(Array(BOARD_SIZE), () => Array(BOARD_SIZE))
let BOARD_VIEW
let SITUATION = new Map()
let promptMode = false
let whoseTurn = 'w'

const CELL_STATE = {
    DEFAULT: 0,
    PROMPT: 1,
    CAN_BE_FILLED: 2,
    MUST_BE_FILLED: 3
}
const CELL_STATE_CLASS = {
    [CELL_STATE.PROMPT]: 'prompt',
    [CELL_STATE.CAN_BE_FILLED]: 'can-be-filled',
    [CELL_STATE.MUST_BE_FILLED]: 'must-be-filled'
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
const finishTurnButton = document.getElementById('finish-turn')
const cancelTurnButton = document.getElementById('cancel-turn')
const moveList = document.getElementById('move-list')


const isPlayCell = (row, col) => (row + col) % 2 === 0


const hasChecker = (row, col) => BOARD[row][col]?.checker != null


const renderChecker = (row, col) => {
    const checker = BOARD[row][col].checker

    BOARD_VIEW[row][col].innerHTML = checker == null? '' : '<img src="' + CHECKER_PIC[checker.type] + '">'
}


const place = (type, row, col) => {
    BOARD[row][col].checker = {type: type}

    renderChecker(row, col)
}


const clear = (row, col) => {
    BOARD[row][col].checker = null

    BOARD_VIEW[row][col].innerHTML = ''
}


const move = (rowFrom, colFrom, rowTo, colTo) => {
    const type = BOARD[rowFrom][colFrom].type

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


const addToSituation = (rowFrom, colFrom, rowTo, colTo, state) => {
    const cellFrom = BOARD[rowFrom][colFrom]
    let cellsTo = SITUATION.get(cellFrom)
    const newCell = {row: rowTo, col: colTo, state: state}

    if (cellsTo == null) {
        cellsTo = [newCell]
        SITUATION.set(cellFrom, cellsTo)
    }

    else
        cellsTo.push(newCell)
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
                    let foundFoe = false

                    while (!res.done) {
                        let {row: rowTo, col: colTo} = res.value

                        if (!hasChecker(rowTo, colTo)) {
                            if (foundFoe) {
                                addToSituation(row, col, rowTo, colTo, CELL_STATE.MUST_BE_FILLED)
                                foundMustBeFilled = true
                                break
                            }

                            else if (!foundMustBeFilled)
                                addToSituation(row, col, rowTo, colTo, CELL_STATE.CAN_BE_FILLED)
                        }

                        else if (areFoes(row, col, rowTo, colTo))
                            foundFoe = true

                        else
                            break

                        res = it.next()
                    } 
                }

            else {
                if (row < BOARD_SIZE - 2) {
                    if (col > 1 && areFoes(row, col, row + 1, col - 1) && !hasChecker(row + 2, col - 2)) {
                        addToSituation(row, col, row + 2, col - 2, CELL_STATE.MUST_BE_FILLED)
                        foundMustBeFilled = true
                    }

                    if (col < BOARD_SIZE - 2 && areFoes(row, col, row + 1, col + 1) && !hasChecker(row + 2, col + 2)) {
                        addToSituation(row, col, row + 2, col + 2, CELL_STATE.MUST_BE_FILLED)
                        foundMustBeFilled = true
                    }
                }

                if (row > 1) {
                    if (col > 1 && areFoes(row, col, row - 1, col - 1) && !hasChecker(row - 2, col - 2)) {
                        addToSituation(row, col, row - 2, col - 2, CELL_STATE.MUST_BE_FILLED)
                        foundMustBeFilled = true
                    }

                    if (col < BOARD_SIZE - 2 && areFoes(row, col, row - 1, col + 1) && !hasChecker(row - 2, col + 2)) {
                        addToSituation(row, col, row - 2, col + 2, CELL_STATE.MUST_BE_FILLED)
                        foundMustBeFilled = true
                    }
                }

                if (!foundMustBeFilled) {
                    if (isWhite(row, col) && row < BOARD_SIZE - 1) {
                        if (col > 0 && !hasChecker(row + 1, col - 1))
                            addToSituation(row, col, row + 1, col - 1, CELL_STATE.CAN_BE_FILLED)

                        if (col < BOARD_SIZE - 1 && !hasChecker(row + 1, col + 1))
                            addToSituation(row, col, row + 1, col + 1, CELL_STATE.CAN_BE_FILLED)
                    }

                    else if (isBlack(row, col) && row > 0) {
                        if (col > 0 && !hasChecker(row - 1, col - 1))
                            addToSituation(row, col, row - 1, col - 1, CELL_STATE.CAN_BE_FILLED)

                        if (col < BOARD_SIZE - 1 && !hasChecker(row - 1, col + 1))
                            addToSituation(row, col, row - 1, col + 1, CELL_STATE.CAN_BE_FILLED)
                    }
                }
            }
        }

    if (foundMustBeFilled)
        for (let entry of SITUATION) {
            const [cellFrom, cellsTo] = entry
            const filteredCellsTo = cellsTo.filter(cellTo => cellTo.state === CELL_STATE.MUST_BE_FILLED)

            if (filteredCellsTo.length === 0)
                SITUATION.delete(cellFrom)
            else
                SITUATION.set(cellFrom, filteredCellsTo)
        }
}


const togglePromptMode = (row, col) => {
    if (!isTurnOf(row, col))
        return []

    const state = BOARD[row][col].state
    const cellsTo = SITUATION.get(BOARD[row][col]) || []

    if (promptMode && state === CELL_STATE.PROMPT) {
        promptMode = false
        BOARD[row][col].state = CELL_STATE.DEFAULT

        for (cell of cellsTo)
            BOARD[cell.row][cell.col].state = CELL_STATE.DEFAULT
    }

    else if (!promptMode && state === CELL_STATE.DEFAULT) {
        promptMode = true
        BOARD[row][col].state = CELL_STATE.PROMPT

        for (cell of cellsTo)
            BOARD[cell.row][cell.col].state = cell.state
    }

    return cellsTo.map(cell => ({row: cell.row, col: cell.col}))
}


const cellOnClick = (row, col) => {
    const changedCells = togglePromptMode(row, col)

    renderCell(row, col)
    changedCells.forEach(cell => renderCell(cell.row, cell.col))
}


const startArrangement = () => {
    whoseTurn = 'w'

    for (let row = 0; row < 3; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                BOARD[row][col].checker = {type: CHECKER_TYPE.WHITE}

    for (let row = 3; row < 5; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                BOARD[row][col].checker = null

    for (let row = 5; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                BOARD[row][col].checker = {type: CHECKER_TYPE.BLACK}
}


const example1Arrangement = () => {
    for (let row = 0; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                BOARD[row][col].checker = null

    BOARD[3][5].checker = {type: CHECKER_TYPE.WHITE}
    BOARD[3][7].checker = {type: CHECKER_TYPE.WHITE}

    BOARD[7][1].checker = {type: CHECKER_TYPE.BLACK}
    BOARD[0][2].checker = {type: CHECKER_TYPE.BLACK_KING}
    BOARD[4][2].checker = {type: CHECKER_TYPE.BLACK}
    BOARD[6][2].checker = {type: CHECKER_TYPE.BLACK}
    BOARD[6][4].checker = {type: CHECKER_TYPE.BLACK}
    BOARD[5][7].checker = {type: CHECKER_TYPE.BLACK}
}


const arrangementButtonOnClick = (arrangement) => {
    arrangement()
    promptMode = false
    calculateSituation()

    renderBoard()
    renderTurn()
    moveList.innerHTML = ''
}


const init = () => {
    for (let row = 0; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                BOARD[row][col] = {state: CELL_STATE.DEFAULT}

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
    finishTurnButton.addEventListener('click', () => {
        toggleTurn()
        renderTurn()
    })
    cancelTurnButton.addEventListener('click', () => console.log('stub'))
}


init()