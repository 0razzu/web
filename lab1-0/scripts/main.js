function getCityFromCookies() {
    const entry = document.cookie.match(/city=([A-Za-z ]+)/)

    if (entry == null)
        return undefined

    return entry[1]
}


function updateCat(tag = undefined) {
    const url = `https://cataas.com/cat/${tag == undefined? 'gif' : tag}?p=${new Date().getTime()}`
    
    document.getElementById('cat').innerHTML = `<img src="${url}">`
}


function updateWeatherInfo() {
    const city = getCityFromCookies()
    let weatherSpan = document.getElementById('weather')

    if (city == undefined) {
        updateCat("waiting")
        weatherSpan.innerHTML = '<p class="error">I’m waiting for you to enter your city</p>'
    }
    
    else {
        let request = new XMLHttpRequest()
        request.open('GET', `http://api.openweathermap.org/data/2.5/weather?q=${city}&units=metric&appid=9b784d72d3912029459eec61a320286e&p=${new Date().getTime()}`, false)
        request.send()

        if (request.status != 200) {
            updateCat("sad")
            weatherSpan.innerHTML = '<p class="error">Couldn’t find your city</p>'
        }

        else {
            updateCat()
            const weatherObj = JSON.parse(request.responseText)
            document.getElementById('weather').innerHTML =
                `<p class="temperature">${weatherObj.main.temp} &deg;C</p>` +
                `<p class="brief">${weatherObj.weather[0].main}</p>` +
                `<p class="wind">Wind speed: ${weatherObj.wind.speed} mps</p>`
        }
    }
}


document.getElementById('change-city').onclick = () => {
    document.cookie = 'city=' + document.getElementById('city').value
    updateWeatherInfo()
}


document.getElementById('city').value = getCityFromCookies()?? ''
updateWeatherInfo()
