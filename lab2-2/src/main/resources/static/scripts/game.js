const BOARD_SIZE = 8
const BOARD = Array.from(Array(BOARD_SIZE), () => Array(BOARD_SIZE))
let BOARD_VIEW
let SITUATION = new Map()
let GAME_ID
let inPromptMode = null
let inMove = false
let becomeKing = false
let killed = []
let whoseTurn = null
let currentStatus = null

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
const STATUS_VIEW = {
    [STATUS.RUNNING]: 'Продолжается',
    [STATUS.OVER]: 'Завершена',
}

const statusStr = document.getElementById('status')
const startButton = document.getElementById('start')
const example1button = document.getElementById('example1')
const inputTurnsButton = document.getElementById('input-turns')
const showGameListButton = document.getElementById('show-game-list')
const cancelTurnButton = document.getElementById('cancel-turn')
const finishTurnButton = document.getElementById('finish-turn')
const gameHistoryHeader = document.getElementById('game-history-header')
const moveListPanel = document.getElementById('move-list-panel')
const moveList = document.getElementById('move-list')
const moveListInputPanel = document.getElementById('move-list-input-panel')
const moveListInput = document.getElementById('move-list-input')
const showTurnsButton = document.getElementById('show-turns')
const errorField = document.getElementById('error-field')
const gameListPanel = document.getElementById('game-list-panel')
const gameList = document.getElementById('game-list')


const visibilityToggler = elem => {
    return isVisible => {
        if (isVisible)
            elem.classList.remove('removed')
        else
            elem.classList.add('removed')
    }
}


const buttonCaptionToggler = (button, captionTrue, captionFalse) => {
    return condition => button.innerText = condition? captionTrue : captionFalse
}


class GameHistoryElem {
    constructor(isVisible, setVisibility, header) {
        this.isVisible = isVisible
        this.setVisibility = setVisibility
        this.header = header
    }
}


const gameHistoryInteriorElems = {
    [moveListPanel.id]: new GameHistoryElem(
        true,
        function (isVisible) {
            visibilityToggler(moveListPanel)(isVisible)
            this.isVisible = isVisible
        },
        'Ходы',
    ),
    [moveListInputPanel.id]: new GameHistoryElem(
        false,
        function (isVisible) {
            visibilityToggler(moveListInputPanel)(isVisible)
            this.isVisible = isVisible
            buttonCaptionToggler(inputTurnsButton, 'Закрыть', 'Ввести ходы')(isVisible)
            moveListInput.value = ''
            clearErrorField()
        },
        'Ввод ходов',
    ),
    [gameListPanel.id]: new GameHistoryElem(
        false,
        function (isVisible) {
            visibilityToggler(gameListPanel)(isVisible)
            this.isVisible = isVisible
            buttonCaptionToggler(showGameListButton, 'Закрыть', 'Список игр')(isVisible)
        },
        'Список игр',
    ),
}


const gameHistoryInterior = new class {
    constructor(gameHistoryInteriorElems, defaultVisible) {
        this.gameHistoryInteriorElems = gameHistoryInteriorElems
        this.currentVisibleId = defaultVisible.id
        this.defaultVisibleId = defaultVisible.id
    }


    toggleVisibility(elem) {
        const elemId = elem.id

        this.gameHistoryInteriorElems[this.currentVisibleId].setVisibility(false)

        if (this.currentVisibleId === elemId) {
            this.gameHistoryInteriorElems[this.defaultVisibleId].setVisibility(true)
            this.currentVisibleId = this.defaultVisibleId
        }

        else {
            this.gameHistoryInteriorElems[elemId].setVisibility(true)
            this.currentVisibleId = elemId
        }

        gameHistoryHeader.innerText = gameHistoryInteriorElems[this.currentVisibleId].header
    }
} (gameHistoryInteriorElems, moveListPanel)


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


const renderStatus = () => {
    if (!GAME_ID)
        statusStr.innerText = 'Никто не ходит'

    else if (!whoseTurn)
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
    if (inMove) {
        cancelTurnButton.classList.remove('hidden')
        finishTurnButton.classList.remove('hidden')
    }

    else {
        cancelTurnButton.classList.add('hidden')
        finishTurnButton.classList.add('hidden')
    }
}


const renderMove = moveStr => {
    const moveViews = moveList.getElementsByTagName('li')
    let turnView = moveViews[moveViews.length - 1]

    if (!turnView || turnView.textContent.split(' ').length === 2) {
        turnView = document.createElement('li')
        turnView.appendChild(document.createTextNode(moveStr))
        moveList.appendChild(turnView)
    }

    else
        turnView.textContent += ' ' + moveStr

    moveList.scrollTop = moveList.scrollHeight
}


const renderMoveListEntry = movePairStr => {
    const movePairStrNoNum = movePairStr.split('. ')[1]

    const turnView = document.createElement('li')
    turnView.appendChild(document.createTextNode(movePairStrNoNum))
    moveList.appendChild(turnView)

    moveList.scrollTop = moveList.scrollHeight
}


const renderMoveList = moves => {
    moveList.innerHTML = ''
    moves.forEach(line => renderMoveListEntry(line))
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

    else if (inPromptMode === null && !inMove && dests.length !== 0) {
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
    }

    return changedCells
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
}


const resetEverything = (resetGameHistoryInterior = true) => {
    for (let row = 0; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col)) {
                BOARD[row][col].state = CELL_STATE.DEFAULT
                clear(row, col)
            }

    SITUATION.clear()
    inPromptMode = null
    inMove = false
    becomeKing = false
    killed = []
    whoseTurn = null

    if (resetGameHistoryInterior)
        gameHistoryInterior.toggleVisibility(moveListPanel)
}


const renderEverything = () => {
    renderBoard()
    renderStatus()
    renderButtons()
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


const arrangementButtonOnClick = arrangement => {
    resetEverything()
    arrangement()
    createGame().then(() => renderEverything())
    moveList.innerText = ''
}


const inputTurnsButtonOnClick = () => gameHistoryInterior.toggleVisibility(moveListInputPanel)


const idLinkOnClick = id => {
    resetEverything()

    getGame(id)
        .then(moveList => {
            renderEverything()
            renderMoveList(moveList)
        })
}


const parseGameList = games => {
    gameList.innerHTML = ''

    games.forEach(({id: gameId, status: gameStatus}) => {
        const idLink = document.createElement('a')
        idLink.appendChild(document.createTextNode(gameId))
        idLink.href = 'javascript:void(0)'
        idLink.onclick = () => idLinkOnClick(gameId)
        const status = document.createTextNode(STATUS_VIEW[gameStatus])

        const idLinkTd = document.createElement('td')
        idLinkTd.appendChild(idLink)
        const statusTd = document.createElement('td')
        statusTd.appendChild(status)

        const gameEntry = document.createElement('tr')
        gameEntry.append(idLinkTd, statusTd)

        gameList.appendChild(gameEntry)
    })
}



const showGameListButtonOnClick = () => {
    if (!gameHistoryInteriorElems[gameListPanel.id].isVisible)
        getGames().then(games => parseGameList(games))
    gameHistoryInterior.toggleVisibility(gameListPanel)
}


const cancelTurnButtonOnClick = () => {
    if (!inMove && inPromptMode === null)
        return

    cancelTurn()
        .then(changedCells => {
            changedCells.forEach(cell => renderCell(cell.row, cell.col))
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
            inMove = false
            inPromptMode = null
            renderStatus()
            renderButtons()
            renderMove(lastMove)
        })
}


const moveListViewOnCopy = event => {
    event.preventDefault()
    event.clipboardData.setData('text', document.getSelection().toString().split('\n')
        .map((line, index) => (index + 1) + '. ' + line).join('\n'))
}


const showTurnsButtonOnClick = () => {
    resetEverything(false)
    startArrangement()

    parseTurns(moveListInput.value.split('\n').map(line => line.trim()).filter(line => line !== ''))
        .then(moveList => {
            gameHistoryInterior.toggleVisibility(moveListInputPanel)

            renderEverything()
            renderMoveList(moveList)
        })
        .catch(({errorCode, reason}) => {
            console.dir({errorCode, reason})

            writeToErrorField('Не\xa0удалось прочитать партию. Строка с\xa0ошибкой:', reason)
            renderEverything()
        })
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

            arr[row] = arr[row]?? []
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
    showGameListButton.addEventListener('click', () => showGameListButtonOnClick())
    cancelTurnButton.addEventListener('click', () => cancelTurnButtonOnClick())
    finishTurnButton.addEventListener('click', () => finishTurnButtonOnClick())
    moveList.addEventListener('copy', event => moveListViewOnCopy(event))
    showTurnsButton.addEventListener('click', () => showTurnsButtonOnClick())
}


init()
