const BOARD_SIZE = 8
const BOARD = Array.from(Array(BOARD_SIZE), () => Array(BOARD_SIZE))
let BOARD_VIEW
let SITUATION = new Map()
let inPromptMode = null
let moveList = []
let becomeKing = false
let killed = []
let whoseTurn = 'w'
let buttonsVisible = false
let whiteCounter = 0
let blackCounter = 0

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


const renderStatus = () => {
    if (whiteCounter === 0 || blackCounter === 0)
        statusStr.innerText = 'Выиграли ' + (whoseTurn === 'w'? 'чёрные' : 'белые')

    else
        statusStr.innerText = 'Ходят ' + (whoseTurn === 'w'? 'белые' : 'чёрные')
}


const renderButtons = () => {
    if (buttonsVisible) {
        cancelTurnButton.removeAttribute('class')
        finishTurnButton.removeAttribute('class')
    }

    else {
        cancelTurnButton.className = 'hidden'
        finishTurnButton.className = 'hidden'
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


const renderMoveList = () => {
    const delimiter = killed.length === 0? '-' : ':'

    if (whoseTurn === 'w') {
        const turnView = document.createElement('li')
        turnView.appendChild(document.createTextNode(moveList.map(cell => cellToString(cell)).join(delimiter)))
        moveListView.appendChild(turnView)
    }

    else {
        const moveViews = moveListView.getElementsByTagName('li')
        const turnView = moveViews[moveViews.length - 1]
        turnView.textContent += ' ' + moveList.map(cell => cellToString(cell)).join(delimiter)
    }

    moveListView.scrollTop = moveListView.scrollHeight
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
                            }

                            else if (!foundMustBeFilled)
                                addToSituation(BOARD[row][col], BOARD[rowTo][colTo], CELL_STATE.CAN_BE_FILLED)
                        }

                        else if (foe === null && areFoes(row, col, rowTo, colTo))
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

    else if (inPromptMode === null && (moveList.length === 0 || (killed.length !== 0 && dests.length !== 0))) {
        inPromptMode = cell
        cell.state = CELL_STATE.PROMPT

        for (dest of dests)
            dest.dest.state = dest.state
    }

    let changedCells = dests.map(dest => dest.dest)
    changedCells.push(cell)

    return changedCells
}


const makeKingIfNeeded = cell => {
    if (whoseTurn === 'w' && cell.row === BOARD_SIZE - 1) {
        cell.checker.type = CHECKER_TYPE.WHITE_KING
        becomeKing = true
    }

    else if (whoseTurn === 'b' && cell.row === 0) {
        cell.checker.type = CHECKER_TYPE.BLACK_KING
        becomeKing = true
    }
}


const hintOrMove = (row, col) => {
    let changedCells = []
    let targetCell = BOARD[row][col]

    if (inPromptMode === null || (moveList.length === 0 && inPromptMode === targetCell))
        changedCells = togglePromptMode(targetCell)

    else if (targetCell.state === CELL_STATE.CAN_BE_FILLED) {
        moveList = [inPromptMode]
        changedCells = togglePromptMode(inPromptMode)
        move(moveList[0].row, moveList[0].col, row, col)
        moveList[1] = targetCell

        makeKingIfNeeded(targetCell)
    }

    else if (targetCell.state === CELL_STATE.MUST_BE_FILLED) {
        const wasInPromptMode = inPromptMode
        changedCells = togglePromptMode(inPromptMode)
        move(wasInPromptMode.row, wasInPromptMode.col, row, col)

        if (moveList.length === 0)
            moveList = [wasInPromptMode]
        moveList.push(BOARD[row][col])

        makeKingIfNeeded(targetCell)

        const killedCell = SITUATION.get(BOARD[wasInPromptMode.row][wasInPromptMode.col]).filter(dest => dest.dest.row === row && dest.dest.col === col)[0].foe
        killedCell.state = CELL_STATE.KILLED
        changedCells.push(killedCell)
        killed.push(killedCell)

        calculateSituation()
        changedCells = changedCells.concat(togglePromptMode(targetCell))
    }

    buttonsVisible = (moveList.length !== 0)

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

    renderMoveList()
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

            if (turn.white === undefined || (turn.black === undefined && lineIndex !== turns.length - 1))
                return lineIndex
        } catch (e) {
            return lineIndex
        }
    }
}


const cellOnClick = (row, col) => {
    hintOrMove(row, col).forEach(cell => renderCell(cell.row, cell.col))
    renderButtons()
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
}


const countCheckers = () => {
    whiteCounter = 0
    blackCounter = 0

    for (let row = 0; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++) {
            if (isWhite(row, col))
                whiteCounter++

            else if (isBlack(row, col))
                blackCounter++
        }
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
    moveList = []
    becomeKing = false
    killed = []
    whoseTurn = 'w'
    buttonsVisible = false
}


const renderEverything = () => {
    renderBoard()
    renderStatus()
    renderButtons()
}


const arrangementButtonOnClick = arrangement => {
    resetEverything()
    arrangement()
    countCheckers()
    calculateSituation()
    renderEverything()
    moveListView.innerHTML = ''
}


const toggleInputTurnsButtonCaption = () => {
    inputTurnsButton.innerText = inputTurnsButton.innerText === 'Ввести ходы'? 'Закрыть' : 'Ввести ходы'
}


const clearErrorField = () => {
    if (!errorField.classList.contains('removed'))
        errorField.className = 'removed'
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
        errorField.removeAttribute('class')
    }
}


const toggleMoveListViewAndInputVisibility = () => {
    if (moveListView.classList.contains('removed')) {
        moveListView.removeAttribute('class')
        moveListInputPanel.className = 'removed'
        moveListInput.value = ''
    }

    else {
        moveListView.className = 'removed'
        moveListInputPanel.removeAttribute('class')
        clearErrorField()
    }
}


const inputTurnsButtonOnClick = () => {
    toggleInputTurnsButtonCaption()
    toggleMoveListViewAndInputVisibility()
}


const cancelTurn = () => {
    const changedCells = []
    const curCell = moveList.length === 0? inPromptMode : moveList[moveList.length - 1]

    SITUATION.get(curCell)?.forEach(dest => {
        dest.dest.state = CELL_STATE.DEFAULT
        changedCells.push(dest.dest)
    })

    if (moveList.length !== 0) {
        if (becomeKing) {
            curCell.checker.type = whoseTurn === 'w'? CHECKER_TYPE.WHITE : CHECKER_TYPE.BLACK
            becomeKing = false
        }

        moveList[0].checker = curCell.checker
        curCell.checker = null
        curCell.state = CELL_STATE.DEFAULT

        changedCells.push(moveList[0])
        changedCells.push(curCell)
    }

    else {
        inPromptMode.state = CELL_STATE.DEFAULT
        changedCells.push(inPromptMode)
    }

    if (killed.length !== 0) {
        for (cell of killed) {
            cell.state = CELL_STATE.DEFAULT
            changedCells.push(cell)
        }
    }
    
    return changedCells
}


const clearAfterTurnCancel = () => {
    moveList = []
    inPromptMode = null
    killed = []
    buttonsVisible = false
}


const cancelTurnButtonOnClick = () => {
    if (moveList.length === 0 && inPromptMode === null)
        return

    cancelTurn().forEach(cell => renderCell(cell.row, cell.col))

    clearAfterTurnCancel()

    calculateSituation()

    renderButtons()
}


const finishTurn = () => {
    for (cell of killed) {
        const {row, col} = cell
        clear(row, col)
        BOARD[row][col].state = CELL_STATE.DEFAULT
    }

    if (whoseTurn === 'w')
        blackCounter -= killed.length
    else
        whiteCounter -= killed.length

    toggleTurn()
}


const clearAfterTurnFinish = () => {
    moveList = []
    becomeKing = false
    killed = []
    buttonsVisible = false
}


const finishTurnButtonOnClick = () => {
    if (moveList.length === 0 || inPromptMode !== null)
        return

    renderMoveList()

    finishTurn()
    
    for (cell of killed)
        renderCell(cell.row, cell.col)

    clearAfterTurnFinish()

    renderStatus()
    renderButtons()
}


const moveListViewOnCopy = event => {
    event.preventDefault()
    event.clipboardData.setData('text', document.getSelection().toString().split('\n').map((line, index) => (index + 1) + '. ' + line).join('\n'))
}


const parseTurns = lines => {
    let result = {turns: []}

    for (let line of lines) {
        const splitLine = line.split(/\s+/)

        try {
            result.turns.push({
                white: splitLine[1]?.split(/-|:/).map(cellStr => stringToCell(cellStr)),
                whiteHaveKilled: splitLine[1]?.includes(':'),
                black: splitLine[2]?.split(/-|:/).map(cellStr => stringToCell(cellStr)),
                blackHaveKilled: splitLine[2]?.includes(':')
            })
        } catch (e) {
            result.errorLine = line
            break
        }
    }

    return result
}


const showTurnsButtonOnClick = () => {
    arrangementButtonOnClick(startArrangement)

    const lines = moveListInput.value.split('\n').map(line => line.trim()).filter(line => line !== '')
    let {turns, errorLine} = parseTurns(lines)
    errorLine = lines[performTurns(turns)] || errorLine

    renderEverything()

    if (errorLine !== undefined) {
        writeToErrorField('Не\xa0удалось прочитать партию. Строка с\xa0ошибкой:', errorLine)
        return
    }

    toggleInputTurnsButtonCaption()
    toggleMoveListViewAndInputVisibility()
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
    inputTurnsButton.addEventListener('click', () => inputTurnsButtonOnClick())
    cancelTurnButton.addEventListener('click', () => cancelTurnButtonOnClick())
    finishTurnButton.addEventListener('click', () => finishTurnButtonOnClick())
    moveListView.addEventListener('copy', event => moveListViewOnCopy(event))
    showTurnsButton.addEventListener('click', () => showTurnsButtonOnClick())
}


init()