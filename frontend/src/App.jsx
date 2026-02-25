import { useState, useEffect } from 'react'
import './App.css'

function App() {
  const [message, setMessage] = useState('Loading...')

  useEffect(() => {
    fetch('http://localhost:8081/hello')
      .then((res) => res.text())
      .then((text) => setMessage(text))
      .catch(() => setMessage('Failed to load'))
  }, [])

  return (
    <div className="App">
      <h1>{message}</h1>
    </div>
  )
}

export default App
