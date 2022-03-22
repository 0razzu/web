const BOARD_SIZE = 8
const BOARD = Array.from(Array(BOARD_SIZE), () => Array(BOARD_SIZE))
let BOARD_VIEW
let SITUATION = new Map()
let GAME_ID
let inPromptMode = null
let inMove = false
let moveList = []
let becomeKing = false
let killed = []
let whoseTurn = null
let currentStatus = null
let buttonsVisible = false

const CELL_STATE = {
    DEFAULT: 'DEFAULT',
    PROMPT: 'PROMPT',
    CAN_BE_FILLED: 'CAN_BE_FILLED',
    MUST_BE_FILLED: 'MUST_BE_FILLED',
    KILLED: 'KILLED',
}
const CELL_STATE_CLASS = {
    [CELL_STATE.PROMPT]: 'prompt',
    [CELL_STATE.CAN_BE_FILLED]: 'can-be-filled',
    [CELL_STATE.MUST_BE_FILLED]: 'must-be-filled',
    [CELL_STATE.KILLED]: 'killed',
}
const CHECKER_TYPE = {
    BLACK: 'BLACK',
    BLACK_KING: 'BLACK_KING',
    WHITE: 'WHITE',
    WHITE_KING: 'WHITE_KING',
}
const CHECKER_PIC = {
    [CHECKER_TYPE.BLACK]: ROOT + '/img/black-checker.svg',
    [CHECKER_TYPE.BLACK_KING]: ROOT + '/img/black-checker-king.svg',
    [CHECKER_TYPE.WHITE]: ROOT + '/img/white-checker.svg',
    [CHECKER_TYPE.WHITE_KING]: ROOT + '/img/white-checker-king.svg',
}
const TEAM = {
    BLACK: 'BLACK',
    WHITE: 'WHITE',
}
const STATUS = {
    RUNNING: 'RUNNING',
    OVER: 'OVER',
}

const statusStr = document.getElementById('status')
const startButton = document.getElementById('start')
const example1button = document.getElementById('example1')
const inputTurnsButton = document.getElementById('input-turns')
const cancelTurnButton = document.getElementById('cancel-turn')
const finishTurnButton = document.getElementById('finish-turn')
const moveListView = document.getElementById('move-list')
const moveListInputPanel = document.getElementById('move-list-input-panel')
const moveListInput = document.getElementById('move-list-input')
const showTurnsButton = document.getElementById('show-turns')
const errorField = document.getElementById('error-field')


const isPlayCell = (row, col) => (row + col) % 2 === 0 && row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE


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
}


const renderStatus = () => {
    if (!whoseTurn)
        statusStr.innerText = 'Игра завершена'

    else {
        const whoseTurnStr = (whoseTurn === TEAM.WHITE? 'белые' : 'чёрные')

        if (currentStatus === STATUS.OVER)
            statusStr.innerText = 'Выиграли ' + whoseTurnStr

        else
            statusStr.innerText = 'Ходят ' + whoseTurnStr
    }
}


const renderButtons = () => {
    if (buttonsVisible) {
        cancelTurnButton.classList.remove('hidden')
        finishTurnButton.classList.remove('hidden')
    }

    else {
        cancelTurnButton.classList.add('hidden')
        finishTurnButton.classList.add('hidden')
    }
}


const cellToString = cell => {
    const letters = 'abcdefgh'

    return letters[cell.col] + (cell.row + 1)
}


const stringToCell = string => {
    const letters = 'abcdefgh'
    const row = Number(string[1]) - 1
    const col = letters.indexOf(string[0])

    if (!isPlayCell(row, col) || string.length != 2)
        return null

    return BOARD[row][col]
}


const pushCurMovesToMoveList = () => {
    moveList.push({
        moves: [...curMoves],
        haveKilled: killed.length !== 0,
        whoseTurn: whoseTurn,
    })
}


const renderMoveListEntry = move => {
    const delimiter = move.haveKilled? ':' : '-'
    const steps = move.steps
    let cells = steps.map(step => step.from)
    cells.push(steps[steps.length - 1].to)
    const moveStr = cells.map(cell => cellToString(cell)).join(delimiter)

    if (move.whoseTurn === TEAM.WHITE) {
        const turnView = document.createElement('li')
        turnView.appendChild(document.createTextNode(moveStr))
        moveListView.appendChild(turnView)
    }

    else {
        const moveViews = moveListView.getElementsByTagName('li')
        const turnView = moveViews[moveViews.length - 1]
        turnView.textContent += ' ' + moveStr
    }

    moveListView.scrollTop = moveListView.scrollHeight
}


const renderMoveList = () => {
    moveListView.innerHTML = ''
    moveList.forEach(entry => renderMoveListEntry(entry))
}


const isTurnOf = (row, col) => {
    if (!hasChecker(row, col))
        return false

    const type = BOARD[row][col].checker.type

    return (whoseTurn === TEAM.WHITE && (type === CHECKER_TYPE.WHITE || type === CHECKER_TYPE.WHITE_KING)) ||
        (whoseTurn === TEAM.BLACK && (type === CHECKER_TYPE.BLACK || type === CHECKER_TYPE.BLACK_KING))
}


const togglePromptMode = cell => {
    if (!isTurnOf(cell.row, cell.col))
        return []

    const dests = SITUATION.get(cell)?? []

    if (inPromptMode === cell) {
        inPromptMode = null
        cell.state = CELL_STATE.DEFAULT

        for (let dest of dests)
            dest.dest.state = CELL_STATE.DEFAULT
    }

    else if (inPromptMode === null && (!inMove || (killed.length !== 0 && dests.length !== 0))) {
        inPromptMode = cell
        cell.state = CELL_STATE.PROMPT

        for (let dest of dests)
            dest.dest.state = dest.state
    }

    let changedCells = dests.map(dest => dest.dest)
    changedCells.push(cell)

    return changedCells
}


const hintOrMove = async (row, col) => {
    let changedCells = []
    let targetCell = BOARD[row][col]

    if (inPromptMode === null || (!inMove && inPromptMode === targetCell))
        changedCells = togglePromptMode(targetCell)

    else if (targetCell.state === CELL_STATE.CAN_BE_FILLED || targetCell.state === CELL_STATE.MUST_BE_FILLED) {
        changedCells = await makeStep({from: inPromptMode, to: targetCell})

        inPromptMode = (targetCell.state === CELL_STATE.PROMPT)? targetCell : null
        inMove = true
        buttonsVisible = true
    }

    return changedCells
}


const performHalfTurn = (halfTurn, haveKilled) => {
    let changedCells = []

    killed = []
    halfTurn.forEach(cell => {
        changedCells = hintOrMove(cell.row, cell.col)
    })

    if (changedCells.length === 0)
        throw new Error('Useless click')

    else if ((killed.length === 0 && haveKilled) || (killed.length !== 0 && !haveKilled))
        throw new Error('Factual killed do not correspond stated ones')

    finishTurn()
    clearAfterTurnFinish()
}


const performTurns = turns => {
    for (let lineIndex = 0; lineIndex < turns.length; lineIndex++) {
        const turn = turns[lineIndex]

        try {
            if (turn.white !== undefined)
                performHalfTurn(turn.white, turn.whiteHaveKilled)

            if (turn.black !== undefined)
                performHalfTurn(turn.black, turn.blackHaveKilled)
        } catch (e) {
            return lineIndex
        }

        if (turn.white === undefined || (turn.black === undefined && lineIndex !== turns.length - 1))
            return lineIndex
    }
}


const cellOnClick = (row, col) => {
    if (currentStatus === STATUS.RUNNING)
        hintOrMove(row, col)
            .then(changedCells => {
                changedCells.forEach(cell => renderCell(cell.row, cell.col))
                renderButtons()
            })
}


const startArrangement = () => {
    for (let row = 0; row < 3; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                place(CHECKER_TYPE.WHITE, row, col)

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

    // place(CHECKER_TYPE.WHITE, 5, 1)
    // place(CHECKER_TYPE.WHITE, 5, 3)
    // place(CHECKER_TYPE.WHITE, 5, 5)
    // place(CHECKER_TYPE.WHITE, 5, 7)
    //
    // place(CHECKER_TYPE.BLACK, 6, 0)
    // place(CHECKER_TYPE.BLACK, 6, 2)
    // place(CHECKER_TYPE.BLACK, 6, 4)
    // place(CHECKER_TYPE.BLACK, 6, 6)
}


const resetEverything = () => {
    for (let row = 0; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col)) {
                BOARD[row][col].state = CELL_STATE.DEFAULT
                clear(row, col)
            }

    SITUATION.clear()
    inPromptMode = null
    inMove = false
    moveList = []
    becomeKing = false
    killed = []
    whoseTurn = null
    buttonsVisible = false
}


const renderEverything = () => {
    renderBoard()
    renderStatus()
    renderButtons()
    renderMoveList()
}


const toggleInputTurnsButtonCaption = () => {
    inputTurnsButton.innerText = inputTurnsButton.innerText === 'Ввести ходы'? 'Закрыть' : 'Ввести ходы'
}


const clearErrorField = () => {
    if (!errorField.classList.contains('removed'))
        errorField.classList.add('removed')
    errorField.innerHTML = ''
}


const writeToErrorField = (...lines) => {
    if (lines.length !== 0) {
        errorField.innerHTML = ''
        lines.forEach(line => {
            const par = document.createElement('p')
            par.appendChild(document.createTextNode(line))
            errorField.appendChild(par)
        })
        errorField.classList.remove('removed')
    }
}


const toggleMoveListViewAndInputVisibility = () => {
    if (moveListView.classList.contains('removed'))
        moveListInput.value = ''

    else
        clearErrorField()

    moveListView.classList.toggle('removed')
    moveListInputPanel.classList.toggle('removed')
}


const arrangementButtonOnClick = arrangement => {
    resetEverything()
    arrangement()
    createGame().then(() => renderEverything())
}


const inputTurnsButtonOnClick = () => {
    toggleInputTurnsButtonCaption()
    toggleMoveListViewAndInputVisibility()
}


const cancelTurnButtonOnClick = () => {
    if (!inMove && inPromptMode === null)
        return

    cancelTurn()
        .then(changedCells => {
            changedCells.forEach(cell => renderCell(cell.row, cell.col))
            buttonsVisible = false
            inMove = false
            inPromptMode = null
            renderButtons()
        })
}


const finishTurnButtonOnClick = () => {
    if (!inMove || inPromptMode !== null)
        return

    applyTurn()
        .then(({changedCells, lastMove}) => {
            changedCells.forEach(cell => renderCell(cell.row, cell.col))
            buttonsVisible = false
            inMove = false
            inPromptMode = null
            renderStatus()
            renderButtons()
            renderMoveListEntry(lastMove)
        })
}


const moveListViewOnCopy = event => {
    event.preventDefault()
    event.clipboardData.setData('text', document.getSelection().toString().split('\n')
        .map((line, index) => (index + 1) + '. ' + line).join('\n'))
}


// const parseTurns = lines => {
//     let result = {turns: []}
//
//     for (let line of lines) {
//         const splitLine = line.split(/\s+/)
//
//         try {
//             result.turns.push({
//                 white: splitLine[1]?.split(/[-:]/).map(cellStr => stringToCell(cellStr)),
//                 whiteHaveKilled: splitLine[1]?.includes(':'),
//                 black: splitLine[2]?.split(/[-:]/).map(cellStr => stringToCell(cellStr)),
//                 blackHaveKilled: splitLine[2]?.includes(':'),
//             })
//         } catch (e) {
//             result.errorLine = line
//             break
//         }
//     }
//
//     return result
// }


const showTurnsButtonOnClick = () => {
    resetEverything()
    startArrangement()

    parseTurns(moveListInput.value.split('\n').map(line => line.trim()).filter(line => line !== ''))
        .then(() => {

        })

    // const lines = moveListInput.value.split('\n').map(line => line.trim()).filter(line => line !== '')
    // let {turns, errorLine} = parseTurns(lines)
    // errorLine = lines[performTurns(turns)]?? errorLine
    //
    // renderEverything()
    //
    // if (errorLine !== undefined) {
    //     writeToErrorField('Не\xa0удалось прочитать партию. Строка с\xa0ошибкой:', errorLine)
    //     return
    // }
    //
    // toggleInputTurnsButtonCaption()
    // toggleMoveListViewAndInputVisibility()
}


const init = () => {
    renderButtons()

    for (let row = 0; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                BOARD[row][col] = {row: row, col: col, state: CELL_STATE.DEFAULT}

    let col = 0

    BOARD_VIEW = Array.from(document.querySelectorAll('.board tr td'))
        .reduce((arr, cell, index) => {
            const row = BOARD_SIZE - 1 - Math.floor(index / BOARD_SIZE)

            arr[row] = arr[row] ?? []
            arr[row].push(isPlayCell(row, col)? cell : null)

            col = (++col) % BOARD_SIZE

            return arr
        }, Array(BOARD_SIZE))

    for (let row = 0; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            BOARD_VIEW[row][col]?.addEventListener('click', () => cellOnClick(row, col))

    startButton.addEventListener('click', () => arrangementButtonOnClick(startArrangement))
    example1button.addEventListener('click', () => arrangementButtonOnClick(example1Arrangement))
    inputTurnsButton.addEventListener('click', () => inputTurnsButtonOnClick())
    cancelTurnButton.addEventListener('click', () => cancelTurnButtonOnClick())
    finishTurnButton.addEventListener('click', () => finishTurnButtonOnClick())
    moveListView.addEventListener('copy', event => moveListViewOnCopy(event))
    showTurnsButton.addEventListener('click', () => showTurnsButtonOnClick())
}


init()
