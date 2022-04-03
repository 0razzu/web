const BOARD_SIZE = 8
const BOARD = Array.from(Array(BOARD_SIZE), () => Array(BOARD_SIZE))
let BOARD_VIEW
let SITUATION = new Map()
let GAME_ID
let inPromptMode = null
let inMove = false
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
const CHECKER = {
    BLACK: 'BLACK',
    BLACK_KING: 'BLACK_KING',
    WHITE: 'WHITE',
    WHITE_KING: 'WHITE_KING',
}
const CHECKER_PIC = {
    [CHECKER.BLACK]: ROOT + '/img/black-checker.svg',
    [CHECKER.BLACK_KING]: ROOT + '/img/black-checker-king.svg',
    [CHECKER.WHITE]: ROOT + '/img/white-checker.svg',
    [CHECKER.WHITE_KING]: ROOT + '/img/white-checker-king.svg',
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

const gameHeader = document.getElementById('game-header')
const statusStr = document.getElementById('status')
const startButton = document.getElementById('start')
const example1button = document.getElementById('example1')
const inputTurnsButton = document.getElementById('input-turns')
const showGameListButton = document.getElementById('show-game-list')
const surrenderButton = document.getElementById('surrender')
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


const defaultErrorHandler = e => {
    console.dir(e)

    const {errorCode, reason, message} = e

    if (message === 'Load failed')
        writeToErrorField('Ошибка соединения')

    else
        switch (errorCode) {
            case 'NO_SUCH_CELL': {
                writeToErrorField('Нет такой клетки')
                break
            }
            case 'IMPOSSIBLE_STEP': {
                writeToErrorField('Невозможный ход')
                break
            }
            case 'PARSING_ERROR': {
                writeToErrorField('Не\xa0удалось прочитать партию. Строка с\xa0ошибкой:', reason)
                break
            }
            case 'GAME_OVER': {
                writeToErrorField('Игра окончена')

                break
            }
            case undefined: {
                writeToErrorField('Неизвестная ошибка')
                break
            }
            default: {
                writeToErrorField("Не\xa0удалось выполнить действие. Код ошибки:", errorCode)
                break
            }
    }
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
            clearErrorField()
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
            clearErrorField()
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


const renderGameHeader = () => {
    gameHeader.innerText = 'Игра' + (GAME_ID? ` ${GAME_ID}` : '')
}


const renderChecker = (row, col) => {
    const checker = BOARD[row][col].checker

    BOARD_VIEW[row][col].innerHTML = checker == null? '' : '<img src="' + CHECKER_PIC[checker] + '">'
}


const place = (checker, row, col) => {
    BOARD[row][col].checker = checker
}


const clear = (row, col) => {
    BOARD[row][col].checker = null
}


const clearBoard = () => {
    for (let row = 0; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col)) {
                BOARD[row][col].state = CELL_STATE.DEFAULT
                clear(row, col)
            }
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


const hideButton = button => button.classList.add('hidden')


const showButton = button => button.classList.remove('hidden')


const renderButtons = () => {
    if (GAME_ID && currentStatus !== STATUS.OVER)
        showButton(surrenderButton)
    else
        hideButton(surrenderButton)

    if (inMove) {
        showButton(cancelTurnButton)
        showButton(finishTurnButton)
    }

    else {
        hideButton(cancelTurnButton)
        hideButton(finishTurnButton)
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

    moveListPanel.scrollTop = moveListPanel.scrollHeight
}


const renderMoveListEntry = movePairStr => {
    const movePairStrNoNum = movePairStr.split('. ')[1]

    const turnView = document.createElement('li')
    turnView.appendChild(document.createTextNode(movePairStrNoNum))
    moveList.appendChild(turnView)

    moveListPanel.scrollTop = moveListPanel.scrollHeight
}


const renderMoveList = moves => {
    moveList.innerHTML = ''
    moves.forEach(line => renderMoveListEntry(line))
}


const isTurnOf = (row, col) => {
    if (!hasChecker(row, col))
        return false

    const checker = BOARD[row][col].checker

    return (whoseTurn === TEAM.WHITE && (checker === CHECKER.WHITE || checker === CHECKER.WHITE_KING)) ||
        (whoseTurn === TEAM.BLACK && (checker === CHECKER.BLACK || checker === CHECKER.BLACK_KING))
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
    let targetCell = BOARD[row][col]

    if (inPromptMode === null || (!inMove && inPromptMode === targetCell))
        return togglePromptMode(targetCell)

    else if (targetCell.state === CELL_STATE.CAN_BE_FILLED || targetCell.state === CELL_STATE.MUST_BE_FILLED)
        return await makeStep({from: inPromptMode, to: targetCell})
}


const cellOnClick = (row, col) => {
    if (currentStatus === STATUS.RUNNING)
        hintOrMove(row, col)
            .then(changedCells => {
                if (changedCells) {
                    changedCells.forEach(cell => renderCell(cell.row, cell.col))
                    renderButtons()
                    renderStatus()
                }
            })
            .catch(e => defaultErrorHandler(e))
}


const startArrangement = () => {
    for (let row = 0; row < 3; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                place(CHECKER.WHITE, row, col)

    for (let row = 5; row < BOARD_SIZE; row++)
        for (let col = 0; col < BOARD_SIZE; col++)
            if (isPlayCell(row, col))
                place(CHECKER.BLACK, row, col)
}


const example1Arrangement = () => {
    place(CHECKER.WHITE, 3, 5)
    place(CHECKER.WHITE, 3, 7)

    place(CHECKER.BLACK, 7, 1)
    place(CHECKER.BLACK_KING, 0, 2)
    place(CHECKER.BLACK, 4, 2)
    place(CHECKER.BLACK, 6, 2)
    place(CHECKER.BLACK, 6, 4)
    place(CHECKER.BLACK, 5, 7)
}


const resetEverything = (resetGameHistoryInterior = true) => {
    clearBoard()

    SITUATION.clear()
    inPromptMode = null
    inMove = false
    whoseTurn = null

    renderEverything()

    if (resetGameHistoryInterior)
        gameHistoryInterior.toggleVisibility(moveListPanel)
}


const renderEverything = () => {
    renderGameHeader()
    renderBoard()
    renderStatus()
    renderButtons()
}


const arrangementButtonOnClick = arrangement => {
    resetEverything()
    arrangement()
    createGame()
        .then(() => renderEverything())
        .catch(e => defaultErrorHandler(e))
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
        .catch(e => defaultErrorHandler(e))
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
        getGames()
            .then(games => parseGameList(games))
            .catch(e => defaultErrorHandler(e))
    gameHistoryInterior.toggleVisibility(gameListPanel)
}


const surrenderButtonOnClick = () => {
    surrender()
        .then(changedCells => {
            changedCells.forEach(cell => renderCell(cell.row, cell.col))
            renderButtons()
            renderStatus()
        })
        .catch(e => defaultErrorHandler(e))
}


const cancelTurnButtonOnClick = () => {
    if (!inMove && inPromptMode === null)
        return

    cancelTurn()
        .then(changedCells => {
            changedCells.forEach(cell => renderCell(cell.row, cell.col))
            renderButtons()
            renderStatus()
        })
        .catch(e => defaultErrorHandler(e))
}


const finishTurnButtonOnClick = () => {
    if (!inMove || inPromptMode !== null)
        return

    applyTurn()
        .then(({changedCells, lastMove}) => {
            changedCells.forEach(cell => renderCell(cell.row, cell.col))
            renderStatus()
            renderButtons()

            if (lastMove)
                renderMove(lastMove)
        })
        .catch(e => defaultErrorHandler(e))
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
        .catch(e => {
            defaultErrorHandler(e)

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
    surrenderButton.addEventListener('click', () => surrenderButtonOnClick())
    cancelTurnButton.addEventListener('click', () => cancelTurnButtonOnClick())
    finishTurnButton.addEventListener('click', () => finishTurnButtonOnClick())
    moveList.addEventListener('copy', event => moveListViewOnCopy(event))
    showTurnsButton.addEventListener('click', () => showTurnsButtonOnClick())
    errorField.addEventListener('click', () => clearErrorField())
}


init()
