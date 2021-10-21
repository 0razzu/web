const BOARD_SIZE = 8
const BOARD = Array.from(Array(BOARD_SIZE), () => Array(BOARD_SIZE))
let BOARD_VIEW = []

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


const place = (type, row, col) => {
    BOARD[row][col] = type

    BOARD_VIEW[row][col].innerHTML = '<img src="' + CHECKER_PIC[type] + '">'
}


const clear = (row, col) => {
    BOARD[row][col] = null

    BOARD_VIEW[row][col].innerHTML = ''
}


const move = (rowFrom, colFrom, rowTo, colTo) => {
    const type = BOARD[rowFrom][colFrom]

    clear(rowFrom, colFrom)
    place(type, rowTo, colTo)
}


const renderBoard = () => {
    for (let i = 0; i < BOARD_SIZE; i++)
        for (let j = 0; j < BOARD_SIZE; j++) {
            let state = BOARD[i][j].state

            if (state === CELL_STATE.DEFAULT)
                BOARD_VIEW[i][j].removeAttribute('class')
            else
                BOARD_VIEW[i][j].className = CELL_STATE_CLASS[state]
        }
}


const init = () => {
    for (let i = 0; i < BOARD_SIZE; i++)
        for (let j = 0; j < BOARD_SIZE; j++)
            BOARD[i][j] = {state: CELL_STATE.DEFAULT}

    BOARD_VIEW = Array.from(document.querySelectorAll('.board tr td'))
        .reduce((arr, cell, i) => {
            const row = Math.floor(i / BOARD_SIZE)

            arr[row] = arr[row] || []
            arr[row].push(cell)

            return arr
        }, Array(BOARD_SIZE))
}


init()