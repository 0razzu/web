const BOARD_SIZE = 8
const BOARD = Array.from(Array(BOARD_SIZE), () => Array(BOARD_SIZE))
let BOARD_VIEW
let promptMode = false

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


const isPlayCell = (row, col) => (row + col) % 2 === 0


const renderChecker = (row, col) => {
    const checker = BOARD[row][col].checker

    if (checker)
        BOARD_VIEW[row][col].innerHTML = '<img src="' + CHECKER_PIC[checker.type] + '">'
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
    const type = BOARD[rowFrom][colFrom]

    clear(rowFrom, colFrom)
    place(type, rowTo, colTo)
}


const renderCell = (row, col) => {
    if (!isPlayCell(row, col))
        return

    let state = BOARD[row][col].state

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


const cellOnClick = (row, col) => {
    let state = BOARD[row][col].state

    if (promptMode && state === CELL_STATE.PROMPT) {
        promptMode = false
        BOARD[row][col].state = CELL_STATE.DEFAULT
    }

    else if (!promptMode && state === CELL_STATE.DEFAULT) {
        promptMode = true
        BOARD[row][col].state = CELL_STATE.PROMPT
    }

    renderCell(row, col)
}


const startArrangement = () => {
    for (let row = 0; row < 3; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                BOARD[row][col].checker = {type: CHECKER_TYPE.WHITE}

    for (let row = 5; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                BOARD[row][col].checker = {type: CHECKER_TYPE.BLACK}
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
}


init()